package dev.voicejournal.domain.model

enum class AudioFormat(val extension: String, val mimeType: String) {
    WAV_16KHZ("wav", "audio/wav"),
    M4A_AAC_128KBPS("m4a", "audio/mp4")
}
