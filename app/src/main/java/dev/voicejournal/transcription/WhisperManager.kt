package dev.voicejournal.transcription

import dev.voicejournal.transcription.engine.SpeechToTextEngine
import dev.voicejournal.transcription.model.TranscriptResult
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperManager @Inject constructor(
    private val speechToTextEngine: SpeechToTextEngine
) {
    val downloadProgress: StateFlow<Float?> = speechToTextEngine.downloadProgress
    val isModelDownloaded: StateFlow<Boolean> = speechToTextEngine.isModelDownloaded

    suspend fun ensureModelDownloaded(): Boolean = speechToTextEngine.ensureModelDownloaded()

    fun deleteModel(): Boolean = speechToTextEngine.deleteModel()

    suspend fun transcribe(audioFile: File): Result<String> {
        return speechToTextEngine.transcribe(audioFile).map { it.text }
    }

    suspend fun transcribeFull(audioFile: File, language: String? = "en"): Result<TranscriptResult> {
        return speechToTextEngine.transcribe(audioFile, language)
    }
}
