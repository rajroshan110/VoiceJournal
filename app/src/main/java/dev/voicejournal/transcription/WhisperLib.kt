package dev.voicejournal.transcription

object WhisperLib {
    init {
        try { 
            System.loadLibrary("whisper_jni") 
        } catch (e: Throwable) { 
            e.printStackTrace() 
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
