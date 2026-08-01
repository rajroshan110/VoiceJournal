package dev.voicejournal.transcription.engine

import dev.voicejournal.transcription.model.TranscriptResult
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface SpeechToTextEngine {
    val isModelDownloaded: StateFlow<Boolean>
    val downloadProgress: StateFlow<Float?>
    fun isModelDownloadedSync(): Boolean
    suspend fun ensureModelDownloaded(): Boolean
    fun deleteModel(): Boolean
    suspend fun transcribe(
        audioFile: File,
        language: String? = null,
        onPartialResult: ((String) -> Unit)? = null
    ): Result<TranscriptResult>
}
