package dev.voicejournal.transcription

import android.util.Log

object WhisperLib {
    private const val TAG = "WhisperLib"

    init {
        try {
            System.loadLibrary("whisper_jni")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load whisper_jni native library: ${e.javaClass.simpleName}")
        }
    }

    fun interface SegmentCallback {
        fun onNewSegment(segmentText: String)
    }

    external fun initContext(modelPath: String): Long
    external fun fullTranscribe(
        contextPtr: Long,
        audioSamples: FloatArray,
        numThreads: Int,
        language: String = "auto",
        callback: SegmentCallback? = null
    ): String
    external fun freeContext(contextPtr: Long)
}
