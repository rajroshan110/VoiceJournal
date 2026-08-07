package dev.voicejournal.audio

import android.content.Context
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dev.voicejournal.data.storage.MediaStorageManager
import dev.voicejournal.domain.model.AudioFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val durationMs: Long, val amplitude: Float) : RecordingState()
    data class Stopped(val file: File, val durationMs: Long, val format: AudioFormat) : RecordingState()
    data class Error(val msg: String) : RecordingState()
}

class AudioRecorderManager(private val context: Context) {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    private var activeFormat: AudioFormat = AudioFormat.M4A_AAC_128KBPS

    private var totalActiveDurationMs: Long = 0L
    private var segmentStartTimeMs: Long = 0L
    private val isStopRequested = AtomicBoolean(false)
    @Volatile var isPaused: Boolean = false
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startRecording(format: AudioFormat): File {
        activeFormat = format
        totalActiveDurationMs = 0L
        isPaused = false
        isStopRequested.set(false)
        val extension = if (format == AudioFormat.WAV_16KHZ) ".wav" else ".m4a"
        val file = MediaStorageManager.generateRecordingFile(context, extension)
        outputFile = file

        val minBufferSize = AudioRecord.getMinBufferSize(
            16000,
            AndroidAudioFormat.CHANNEL_IN_MONO,
            AndroidAudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AndroidAudioFormat.CHANNEL_IN_MONO,
                AndroidAudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = RecordingState.Error("Failed to initialize AudioRecord")
                return file
            }

            audioRecord?.startRecording()
            segmentStartTimeMs = System.currentTimeMillis()

            recordingJob = scope.launch {
                val buffer = ByteArray(minBufferSize)
                var wavWriter: WavFileWriter? = null
                var m4aEncoder: M4aEncoder? = null

                if (format == AudioFormat.WAV_16KHZ) {
                    wavWriter = WavFileWriter().apply { start(file) }
                } else {
                    m4aEncoder = M4aEncoder().apply { start(file) }
                }

                try {
                    while (!isStopRequested.get()) {
                        val read = audioRecord?.read(buffer, 0, minBufferSize) ?: 0
                        if (read > 0) {
                            if (!isPaused) {
                                wavWriter?.writePcmChunk(buffer, read)
                                m4aEncoder?.encodePcmChunk(buffer, read)
    
                                val activeSegment = (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
                                val liveDur = totalActiveDurationMs + activeSegment
                                val amplitude = calculateRms(buffer, read)
                                _state.value = RecordingState.Recording(liveDur, amplitude)
                            } else {
                                _state.value = RecordingState.Recording(totalActiveDurationMs, 0f)
                            }
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        wavWriter?.finish()
                        m4aEncoder?.finish()
                    }
                }
            }
        } catch (e: SecurityException) {
            _state.value = RecordingState.Error("Permission denied: ${e.message}")
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Unknown error")
        }

        return file
    }

    fun pauseRecording() {
        if (!isPaused && segmentStartTimeMs > 0L) {
            totalActiveDurationMs += (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
            isPaused = true
            _state.value = RecordingState.Recording(totalActiveDurationMs, 0f)
        }
    }

    fun resumeRecording() {
        if (isPaused) {
            segmentStartTimeMs = System.currentTimeMillis()
            isPaused = false
        }
    }

    suspend fun stopRecording(): File? {
        if (!isPaused && segmentStartTimeMs > 0L) {
            totalActiveDurationMs += (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
        }
        isPaused = false

        audioRecord?.stop()
        isStopRequested.set(true)
        recordingJob?.join()
        
        audioRecord?.release()
        audioRecord = null

        val file = outputFile
        if (file != null) {
            _state.value = RecordingState.Stopped(file, totalActiveDurationMs, activeFormat)
        } else {
            _state.value = RecordingState.Idle
        }
        return file
    }

    suspend fun cancelRecording() {
        isPaused = false
        audioRecord?.stop()
        isStopRequested.set(true)
        recordingJob?.join()
        
        audioRecord?.release()
        audioRecord = null
        outputFile?.delete()
        _state.value = RecordingState.Idle
    }

    private fun calculateRms(buffer: ByteArray, read: Int): Float {
        var sum = 0.0
        for (i in 0 until read step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val shortSample = sample.toShort()
            sum += shortSample * shortSample
        }
        val rms = sqrt(sum / (read / 2))
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }
}
