package dev.voicejournal.domain.usecase

import dev.voicejournal.transcription.engine.SpeechToTextEngine
import dev.voicejournal.transcription.model.TranscriptResult
import java.io.File
import javax.inject.Inject

class GenerateTranscriptUseCase @Inject constructor(
    private val speechToTextEngine: SpeechToTextEngine
) {
    suspend operator fun invoke(
        audioFile: File,
        language: String? = null,
        onPartialResult: ((String) -> Unit)? = null
    ): Result<TranscriptResult> {
        return speechToTextEngine.transcribe(audioFile, language, onPartialResult)
    }

    fun isModelDownloadedSync(): Boolean {
        return speechToTextEngine.isModelDownloadedSync()
    }

    suspend fun ensureModelDownloaded(): Boolean {
        return speechToTextEngine.ensureModelDownloaded()
    }
}
