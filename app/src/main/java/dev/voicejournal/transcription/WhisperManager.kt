package dev.voicejournal.transcription

import dev.voicejournal.transcription.engine.SpeechToTextEngine
import kotlinx.coroutines.flow.StateFlow
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
}
