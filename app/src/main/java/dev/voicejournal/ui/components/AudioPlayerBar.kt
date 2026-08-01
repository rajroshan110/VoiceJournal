package dev.voicejournal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AudioPlayerBar(
    isPlaying: Boolean,
    progress: Float = 0f,
    amplitudes: List<Float> = emptyList(),
    currentTimeStr: String = "",
    totalTimeStr: String = "",
    onPlayPauseClick: () -> Unit,
    onSeek: ((Float) -> Unit)? = null,
    isCompact: Boolean = true,
    modifier: Modifier = Modifier
) {
    val byteAmplitudes = amplitudes.map { (it.coerceIn(0f, 1f) * 100).toInt().toByte() }
    UnifiedAudioPlayerBar(
        isPlaying = isPlaying,
        durationMs = 0L,
        currentPositionMs = 0L,
        waveformAmplitudes = byteAmplitudes,
        onPlayPauseClick = onPlayPauseClick,
        onSeekFraction = onSeek,
        modifier = modifier
    )
}
