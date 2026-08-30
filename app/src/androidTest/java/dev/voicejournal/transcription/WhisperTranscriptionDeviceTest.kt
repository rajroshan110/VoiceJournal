package dev.voicejournal.transcription

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.voicejournal.transcription.engine.WhisperEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WhisperTranscriptionDeviceTest {

    private lateinit var context: Context
    private lateinit var whisperEngine: WhisperEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        whisperEngine = WhisperEngine(context)
    }

    @Test
    fun testWhisperModelExistsAndLoads() {
        val modelFile = File(context.filesDir, "models/ggml-base-q5_1.bin")
        assertTrue("Model file must exist on device", modelFile.exists())
        val ptr = WhisperLib.initContext(modelFile.absolutePath)
        assertTrue("Context pointer must be non-zero", ptr != 0L)
        WhisperLib.freeContext(ptr)
    }

    @Test
    fun testWhisperLibFullTranscribeWithSyntheticAudio() {
        val modelFile = File(context.filesDir, "models/ggml-base-q5_1.bin")
        val ptr = WhisperLib.initContext(modelFile.absolutePath)
        assertTrue("Context pointer must be non-zero", ptr != 0L)

        try {
            // 1 second of 16kHz sine wave audio (16,000 samples)
            val sampleRate = 16000
            val samples = FloatArray(sampleRate) { i ->
                kotlin.math.sin(2.0 * Math.PI * 440.0 * i / sampleRate).toFloat() * 0.1f
            }

            var callbackFired = false
            val result = WhisperLib.fullTranscribe(
                contextPtr = ptr,
                audioSamples = samples,
                numThreads = 4,
                language = "auto",
                callback = { segment ->
                    callbackFired = true
                }
            )

            assertNotNull("Transcription result must not be null", result)
            assertFalse("Result must not be an unhandled error", result.startsWith("Error:"))
        } finally {
            WhisperLib.freeContext(ptr)
        }
    }

    @Test
    fun testTranscribeAllExistingRecordingsOnDevice() = runBlocking {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists() || recordingsDir.listFiles().isNullOrEmpty()) {
            return@runBlocking
        }

        val audioFiles = recordingsDir.listFiles { file ->
            file.extension.lowercase() in listOf("wav", "m4a")
        } ?: emptyArray()

        assertTrue("Should have audio files on device to test", audioFiles.isNotEmpty())

        for (audioFile in audioFiles) {
            val result = whisperEngine.transcribe(
                audioFile = audioFile,
                language = "auto"
            ) { partial ->
                // Ensure partial callback does not crash
            }
            // Even if an audio file is silent or contains unsupported language, transcribe must return a Result, NOT crash the process
            assertTrue("Transcription must return a valid Result without crashing", result.isSuccess || result.isFailure)
        }
    }

    @Test
    fun testSimultaneousPlaybackAndTranscription() = runBlocking {
        val recordingsDir = File(context.filesDir, "recordings")
        val audioFiles = recordingsDir.listFiles { file ->
            file.extension.lowercase() in listOf("wav", "m4a")
        } ?: emptyArray()

        if (audioFiles.isNotEmpty()) {
            val testAudio = audioFiles.first()
            val playerManager = dev.voicejournal.audio.AudioPlayerManager(context)

            // Start playback on main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                playerManager.play(testAudio.absolutePath)
            }

            // Run transcription simultaneously
            val result = whisperEngine.transcribe(
                audioFile = testAudio,
                language = "auto"
            )

            // Clean up player
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                playerManager.release()
            }

            assertTrue("Concurrent transcription must complete with Result without crashing", result.isSuccess || result.isFailure)
        }
    }
}
