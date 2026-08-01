package dev.voicejournal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.voicejournal.ui.designsystem.components.audio.UnifiedAudioPlayerBar as DesignUnifiedAudioPlayerBar

@Composable
fun UnifiedAudioPlayerBar(
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    isAudioError: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    waveformAmplitudes: List<Byte> = emptyList(),
    onPlayPauseClick: () -> Unit,
    onSeekFraction: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    DesignUnifiedAudioPlayerBar(
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        isAudioError = isAudioError,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        waveformAmplitudes = waveformAmplitudes,
        onPlayPauseClick = onPlayPauseClick,
        onSeekFraction = onSeekFraction,
        modifier = modifier,
        trailingContent = trailingContent
    )
}
