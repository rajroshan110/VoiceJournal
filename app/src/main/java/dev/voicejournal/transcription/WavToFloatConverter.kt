package dev.voicejournal.transcription

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavToFloatConverter {
    private const val TAG = "WhisperProfile"

    fun convertWavToFloatArray(file: File): FloatArray {
        val start = System.currentTimeMillis()
        FileInputStream(file).use { fis ->
            val header = ByteArray(44)
            val bytesRead = fis.read(header)
            if (bytesRead < 44) {
                throw IllegalArgumentException("Invalid WAV file: header too short")
            }

            val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val channels = byteBuffer.getShort(22).toInt()
            val sampleRate = byteBuffer.getInt(24)
            val bitsPerSample = byteBuffer.getShort(34).toInt()

            if (bitsPerSample != 16) {
                throw IllegalArgumentException("Unsupported WAV format: $bitsPerSample bits per sample (16-bit required)")
            }

            val dataSize = byteBuffer.getInt(40)
            val numSamples = dataSize / 2 // 16-bit = 2 bytes per sample

            val pcmData = ByteArray(dataSize)
            var totalRead = 0
            while (totalRead < dataSize) {
                val read = fis.read(pcmData, totalRead, dataSize - totalRead)
                if (read == -1) break
                totalRead += read
            }

            val floatArray = FloatArray(numSamples)
            val pcmBuffer = ByteBuffer.wrap(pcmData, 0, totalRead).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                if (pcmBuffer.hasRemaining()) {
                    val sample = pcmBuffer.short
                    floatArray[i] = sample.toFloat() / 32768.0f
                } else {
                    break
                }
            }

            val monoArray = if (channels == 2) {
                val monoLength = floatArray.size / 2
                val monoFloatArray = FloatArray(monoLength)
                for (i in 0 until monoLength) {
                    monoFloatArray[i] = (floatArray[i * 2] + floatArray[i * 2 + 1]) / 2.0f
                }
                monoFloatArray
            } else {
                floatArray
            }

            val resampled = AudioResampler.resampleTo16kHz(monoArray, sampleRate)
            val duration = System.currentTimeMillis() - start
            Log.i(TAG, "[Pipeline Profiler] Audio Decoding (WAV): ${duration}ms (Decoded ${resampled.size} samples at 16kHz)")
            return resampled
        }
    }
}
