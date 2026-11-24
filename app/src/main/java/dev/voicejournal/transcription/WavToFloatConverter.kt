package dev.voicejournal.transcription

import android.util.Log
import dev.voicejournal.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavToFloatConverter {
    private const val TAG = "WhisperProfile"
    private const val BUFFER_SIZE = 16 * 1024

    fun convertWavToFloatArray(file: File): FloatArray {
        val start = System.currentTimeMillis()
        FileInputStream(file).use { fis ->
            val header = ByteArray(44)
            val headerBytesRead = fis.read(header)
            if (headerBytesRead < 44) {
                throw IllegalArgumentException("Invalid WAV file: header too short")
            }

            val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val channels = byteBuffer.getShort(22).toInt()
            val sampleRate = byteBuffer.getInt(24)
            val bitsPerSample = byteBuffer.getShort(34).toInt()

            if (bitsPerSample != 16) {
                throw IllegalArgumentException("Unsupported WAV format: $bitsPerSample bits per sample (16-bit required)")
            }
            if (channels <= 0) {
                throw IllegalArgumentException("Unsupported WAV channel count: $channels")
            }

            val rawDataSize = byteBuffer.getInt(40).toLong() and 0xFFFFFFFFL
            val remainingFileBytes = (file.length() - 44L).coerceAtLeast(0L)
            val effectiveDataBytes = minOf(rawDataSize, remainingFileBytes)

            val bytesPerFrame = channels * 2
            val numFrames = (effectiveDataBytes / bytesPerFrame)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val floatArray = FloatArray(numFrames)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRemaining = effectiveDataBytes
            var carryBytes = 0
            var frameIndex = 0

            while (bytesRemaining > 0L && frameIndex < numFrames) {
                val maxRead = minOf((buffer.size - carryBytes).toLong(), bytesRemaining).toInt()
                if (maxRead <= 0) break
                val bytesRead = fis.read(buffer, carryBytes, maxRead)
                if (bytesRead <= 0) break
                bytesRemaining -= bytesRead

                val availableBytes = carryBytes + bytesRead
                val completeFrames = minOf(availableBytes / bytesPerFrame, numFrames - frameIndex)
                val consumedBytes = completeFrames * bytesPerFrame

                var offset = 0
                if (channels == 1) {
                    for (i in 0 until completeFrames) {
                        val sample = ((buffer[offset].toInt() and 0xFF) or (buffer[offset + 1].toInt() shl 8)).toShort()
                        floatArray[frameIndex++] = sample.toFloat() / 32768.0f
                        offset += 2
                    }
                } else if (channels == 2) {
                    for (i in 0 until completeFrames) {
                        val left = ((buffer[offset].toInt() and 0xFF) or (buffer[offset + 1].toInt() shl 8)).toShort()
                        val right = ((buffer[offset + 2].toInt() and 0xFF) or (buffer[offset + 3].toInt() shl 8)).toShort()
                        floatArray[frameIndex++] = (left.toFloat() / 32768.0f + right.toFloat() / 32768.0f) / 2.0f
                        offset += 4
                    }
                } else {
                    val invChannels = 1.0f / channels.toFloat()
                    for (i in 0 until completeFrames) {
                        var sum = 0.0f
                        for (ch in 0 until channels) {
                            val sample = ((buffer[offset].toInt() and 0xFF) or (buffer[offset + 1].toInt() shl 8)).toShort()
                            sum += sample.toFloat() / 32768.0f
                            offset += 2
                        }
                        floatArray[frameIndex++] = sum * invChannels
                    }
                }

                carryBytes = availableBytes - consumedBytes
                if (carryBytes > 0) {
                    System.arraycopy(buffer, consumedBytes, buffer, 0, carryBytes)
                }
            }

            val monoArray = if (frameIndex < floatArray.size) floatArray.copyOf(frameIndex) else floatArray
            val resampled = AudioResampler.resampleTo16kHz(monoArray, sampleRate)
            val duration = System.currentTimeMillis() - start
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "[Pipeline Profiler] Audio Decoding (WAV): ${duration}ms (Decoded ${resampled.size} samples at 16kHz)")
            }
            return resampled
        }
    }
}
