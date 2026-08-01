package dev.voicejournal.ui.designsystem.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.theme.AppTheme
import java.util.Locale

private fun getPauseIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }.build()
}

/**
 * Single centralized Unified Audio Player Bar component for all screens:
 * - Journal tab entry cards
 * - Calendar tab mini cards
 * - Note Detail screen
 * - Global Audio Player bar
 */
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
    val colors = AppTheme.colors

    val progress = remember(currentPositionMs, durationMs) {
        if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val byteAmplitudes = remember(waveformAmplitudes) {
        if (waveformAmplitudes.size == 32) {
            waveformAmplitudes
        } else if (waveformAmplitudes.isNotEmpty()) {
            val targetCount = 32
            List(targetCount) { idx ->
                val srcIdx = (idx * waveformAmplitudes.size) / targetCount
                waveformAmplitudes[srcIdx]
            }
        } else {
            List(32) { i ->
                val base = kotlin.math.sin(i.toDouble() * 0.5) * 40 + 50
                base.coerceIn(10.0, 100.0).toInt().toByte()
            }
        }
    }

    val totalSec = (durationMs / 1000).toInt()
    val currSec = (currentPositionMs / 1000).toInt()
    val timeText = String.format(
        Locale.US,
        "%02d:%02d / %02d:%02d",
        currSec / 60,
        currSec % 60,
        totalSec / 60,
        totalSec % 60
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.SpaceX3s)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(colors.surfaceVariant, shape = RoundedCornerShape(Radius.RadiusPill))
                .padding(horizontal = Spacing.SpaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Play / Pause Circular Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(colors.primary, CircleShape)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isBuffering -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(IconSize.IconMd),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    isAudioError -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Audio Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(IconSize.IconMd)
                        )
                    }
                    isPlaying -> {
                        Icon(
                            imageVector = getPauseIcon(colors.onPrimary),
                            contentDescription = "Pause",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(IconSize.IconMd)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(IconSize.IconMd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.SpaceXs))

            // 2. Waveform Visualizer
            WaveformVisualizer(
                amplitudes = byteAmplitudes,
                progress = progress,
                activeColor = colors.primary,
                inactiveColor = colors.textSecondary.copy(alpha = 0.3f),
                onSeekFraction = if (isPlaying) onSeekFraction else null,
                isSeekable = isPlaying,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.SpaceXs))

            // 3. Single-line Running Timer Readout
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                softWrap = false
            )

            // 4. Trailing Action Content (placed inline right beside the playback timer readout)
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                trailingContent()
            }
        }
    }
}
