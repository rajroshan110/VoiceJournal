package dev.voicejournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun WaveformVisualizer(
    amplitudes: List<Byte>,
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    inactiveColor: Color? = null,
    onSeekFraction: ((Float) -> Unit)? = null,
    isSeekable: Boolean = true
) {
    val colors = AppTheme.colors
    val effectiveActiveColor = activeColor ?: colors.primary
    val effectiveInactiveColor = inactiveColor ?: colors.divider

    var canvasWidth by remember { mutableFloatStateOf(0f) }

    val barAmplitudes = remember(amplitudes) {
        val targetCount = 32
        if (amplitudes.size == targetCount) {
            amplitudes.map { (it.toInt() and 0xFF) / 100f }
        } else if (amplitudes.isNotEmpty()) {
            List(targetCount) { idx ->
                val srcIdx = (idx * amplitudes.size) / targetCount
                (amplitudes[srcIdx].toInt() and 0xFF) / 100f
            }
        } else {
            List(targetCount) { 0.3f }
        }
    }

    val semanticsModifier = if (onSeekFraction != null && isSeekable) {
        Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            customActions = listOf(
                CustomAccessibilityAction("Forward 10%") {
                    onSeekFraction((progress + 0.1f).coerceAtMost(1f))
                    true
                },
                CustomAccessibilityAction("Rewind 10%") {
                    onSeekFraction((progress - 0.1f).coerceAtLeast(0f))
                    true
                }
            )
        }
    } else Modifier

    val enableTouch = onSeekFraction != null && isSeekable

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(semanticsModifier)
            .then(
                if (enableTouch) {
                    Modifier
                        .pointerInput(onSeekFraction) {
                            detectTapGestures { offset ->
                                if (canvasWidth > 0) {
                                    val newProgress = (offset.x / canvasWidth).coerceIn(0f, 1f)
                                    onSeekFraction?.invoke(newProgress)
                                }
                            }
                        }
                        .pointerInput(onSeekFraction) {
                            detectDragGestures { change, _ ->
                                if (canvasWidth > 0) {
                                    val newProgress = (change.position.x / canvasWidth).coerceIn(0f, 1f)
                                    onSeekFraction?.invoke(newProgress)
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        canvasWidth = size.width
        val barCount = barAmplitudes.size
        if (barCount == 0 || size.width == 0f) return@Canvas

        val gapRatio = 0.35f
        val totalBarWidth = size.width / barCount
        val barWidth = totalBarWidth * (1f - gapRatio)
        val gapWidth = totalBarWidth * gapRatio

        barAmplitudes.forEachIndexed { index, normAmp ->
            val xOffset = index * totalBarWidth + gapWidth / 2f
            val barProgress = (index + 0.5f) / barCount
            val color = if (barProgress <= progress) effectiveActiveColor else effectiveInactiveColor

            val minHeightPx = 6.dp.toPx()
            val maxBarHeightPx = size.height * 0.85f
            val barHeight = (normAmp * maxBarHeightPx).coerceAtLeast(minHeightPx)
            val yOffset = (size.height - barHeight) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
