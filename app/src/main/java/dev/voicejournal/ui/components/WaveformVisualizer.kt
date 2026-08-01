package dev.voicejournal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.voicejournal.ui.designsystem.components.audio.WaveformVisualizer as DesignWaveformVisualizer
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun WaveformVisualizer(
    amplitudes: List<Byte>,
    progress: Float,
    activeColor: Color = AppTheme.colors.primary,
    inactiveColor: Color = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
    onSeekFraction: ((Float) -> Unit)? = null,
    isSeekable: Boolean = false,
    modifier: Modifier = Modifier
) {
    DesignWaveformVisualizer(
        amplitudes = amplitudes,
        progress = progress,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        onSeekFraction = onSeekFraction,
        isSeekable = isSeekable,
        modifier = modifier
    )
}
