package dev.voicejournal.transcription.model

data class TranscriptResult(
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val model: String = "Whisper Base",
    val language: String = "en",
    val version: String = "1.0"
)
