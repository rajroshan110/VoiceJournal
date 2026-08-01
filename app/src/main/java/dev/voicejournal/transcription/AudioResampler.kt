package dev.voicejournal.transcription

import android.util.Log

object AudioResampler {
    private const val TAG = "WhisperProfile"

    /**
     * Linearly resample PCM audio float array from [srcSampleRate] to 16000 Hz.
     * Whisper.cpp requires exactly 16000 Hz mono PCM float input.
     */
    fun resampleTo16kHz(samples: FloatArray, srcSampleRate: Int): FloatArray {
        val start = System.currentTimeMillis()
        if (srcSampleRate == 16000 || samples.isEmpty()) {
            Log.i(TAG, "[Pipeline Profiler] Audio Resampling: 0ms (already 16000 Hz, ${samples.size} samples)")
            return samples
        }
        val ratio = srcSampleRate.toDouble() / 16000.0
        val targetSize = (samples.size / ratio).toInt()
        if (targetSize <= 0) return FloatArray(0)

        val resampled = FloatArray(targetSize)
        for (i in 0 until targetSize) {
            val srcIndex = i * ratio
            val index1 = srcIndex.toInt().coerceIn(0, samples.size - 1)
            val index2 = (index1 + 1).coerceIn(0, samples.size - 1)
            val fraction = (srcIndex - index1).toFloat()
            resampled[i] = samples[index1] * (1.0f - fraction) + samples[index2] * fraction
        }

        val duration = System.currentTimeMillis() - start
        Log.i(TAG, "[Pipeline Profiler] Audio Resampling: ${duration}ms (resampled ${samples.size} @ ${srcSampleRate}Hz -> ${resampled.size} @ 16000Hz)")
        return resampled
    }
}
