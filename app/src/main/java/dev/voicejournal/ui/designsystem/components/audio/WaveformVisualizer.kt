package dev.voicejournal.ui.designsystem.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.theme.AppTheme

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
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = "Audio Waveform Visualizer"
                stateDescription = "${(currentFraction * 100).toInt()}% played"
                if (isSeekable && onSeekFraction != null) {
                    setProgress { targetProgress ->
                        onSeekFraction(targetProgress.coerceIn(0f, 1f))
                        true
                    }
                }
            }
            .pointerInput(isSeekable, onSeekFraction) {
                if (!isSeekable || onSeekFraction == null) return@pointerInput

                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        if (!fraction.isNaN()) {
                            onSeekFraction(fraction)
                        }
                    }
                }
            }
            .pointerInput(isSeekable, onSeekFraction) {
                if (!isSeekable || onSeekFraction == null) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (size.width > 0) {
                            isDragging = true
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            if (!frac.isNaN()) {
                                dragFraction = frac
                            }
                        }
                    },
                    onDragEnd = {
                        if (isDragging) {
                            if (!dragFraction.isNaN()) {
                                onSeekFraction(dragFraction)
                            }
                            isDragging = false
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        if (size.width > 0) {
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            if (!frac.isNaN()) {
                                dragFraction = frac
                            }
                        }
                    }
                )
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clipToBounds()
        ) {
        if (amplitudes.isEmpty()) return@Canvas

        val barCount = amplitudes.size
        val totalWidth = size.width
        val barHeightMax = size.height

        val step = totalWidth / barCount
        val barWidth = (step * 0.60f).coerceAtLeast(1.5.dp.toPx())

        val activeBarIndex = if (currentFraction <= 0f) -1 else (currentFraction * barCount).toInt().coerceIn(0, barCount - 1)

        val isAtRest = currentFraction <= 0f && amplitudes.all { it == 0.toByte() || it == amplitudes.firstOrNull() }
        amplitudes.forEachIndexed { index, ampByte ->
            val rawAmp = if (isAtRest) 15 else ampByte.toInt().coerceIn(10, 100)
            val normalizedHeight = (rawAmp / 100f) * barHeightMax
            val barHeight = normalizedHeight.coerceAtLeast(4.dp.toPx())

            val x = index * step
            val y = (barHeightMax - barHeight) / 2f

            val color = if (index <= activeBarIndex) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
}
