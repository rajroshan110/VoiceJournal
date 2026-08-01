package dev.voicejournal.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AmplitudeExtractor {
    suspend fun extractAmplitudes(audioFile: File, barCount: Int = 60): List<Float> = withContext(Dispatchers.IO) {
        if (!audioFile.exists()) return@withContext List(barCount) { 0f }

        if (audioFile.name.endsWith(".wav", ignoreCase = true)) {
            extractFromWav(audioFile, barCount)
        } else {
            extractFromM4a(audioFile, barCount)
        }
    }

    private fun extractFromWav(file: File, barCount: Int): List<Float> {
        val bytes = ByteArray(file.length().toInt() - 44)
        FileInputStream(file).use {
            it.skip(44)
            it.read(bytes)
        }

        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        return processShorts(shorts, barCount)
    }

    private fun extractFromM4a(file: File, barCount: Int): List<Float> {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return List(barCount) { 0f }
            
            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return List(barCount) { 0f }
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            val shortList = mutableListOf<Short>()

            while (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    val sampleSize = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(info, 10000)
                while (outIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val shortBuf = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining()) {
                            shortList.add(shortBuf.get())
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(info, 10000)
                }
            }

            codec.stop()
            codec.release()

            processShorts(shortList.toShortArray(), barCount)
        } catch (e: Exception) {
            List(barCount) { 0f }
        } finally {
            extractor.release()
        }
    }

    private fun processShorts(samples: ShortArray, barCount: Int): List<Float> {
        if (samples.isEmpty()) return List(barCount) { 0f }
        val chunkSize = maxOf(1, samples.size / barCount)
        val amplitudes = mutableListOf<Float>()

        for (i in 0 until barCount) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, samples.size)
            var sum = 0.0
            for (j in start until end) {
                sum += samples[j] * samples[j]
            }
            val rms = sqrt(sum / maxOf(1, end - start))
            amplitudes.add((rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f))
        }

        return amplitudes
    }
}
