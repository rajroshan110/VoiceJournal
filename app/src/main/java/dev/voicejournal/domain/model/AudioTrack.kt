package dev.voicejournal.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AudioTrack(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val durationMs: Long = 0L,
    val transcript: String? = null,
    val transcriptCreatedAt: Long? = null,
    val transcriptModel: String? = null,
    val transcriptLanguage: String? = null,
    val transcriptVersion: String? = null,
    val isTranscriptionFailed: Boolean = false,
    val rawWaveformAmplitudes: List<Byte> = emptyList()
) {
    val uuid: String get() = id

    val waveformAmplitudes: List<Byte> by lazy {
        if (rawWaveformAmplitudes.size == 32) rawWaveformAmplitudes
        else List(32) { i ->
            val base = kotlin.math.sin((i + id.hashCode()).toDouble() * 0.5) * 40 + 50
            base.coerceIn(10.0, 100.0).toInt().toByte()
        }
    }
}
