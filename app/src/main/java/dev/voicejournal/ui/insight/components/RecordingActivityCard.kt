package dev.voicejournal.ui.insight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.usecase.DailyActivityCount
import dev.voicejournal.domain.usecase.TimePeriod
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.theme.tabularNumbers
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import java.util.Locale

@Composable
fun RecordingActivityCard(
    dailyCounts: List<DailyActivityCount>,
    totalEntries: Int,
    averagePerDay: Float,
    activeStreak: Int,
    period: TimePeriod,
    peakDay: String,
    peakCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val labelFontSize = MaterialTheme.typography.labelSmall.fontSize
    val periodLabel = if (period == TimePeriod.WEEK) "this week" else "this month"
    val accessibilityDesc = "Bar chart showing recording frequency. Total $totalEntries entries $periodLabel, highest activity on $peakDay with $peakCount entries."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.RadiusLg))
            .background(colors.surface)
            .padding(Spacing.SpaceLg)
            .semantics { contentDescription = accessibilityDesc }
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recording Activity",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(Spacing.SpaceXs))

            // Total Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.RadiusMd))
                    .background(colors.primaryContainer)
                    .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
            ) {
                Text(
                    text = "$totalEntries entries",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.SpaceLg))

        // Chart Area: Custom Canvas Graph without Gridlines
        if (dailyCounts.isEmpty() || totalEntries == 0) {
            val emptyMessage = if (period == TimePeriod.WEEK) {
                "No entries has been recorded for this week"
            } else {
                "No entries has been recorded for this month"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(colors.surfaceVariant, RoundedCornerShape(Radius.RadiusMd))
                    .padding(horizontal = Spacing.SpaceLg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val maxCount = (dailyCounts.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val primaryColor = colors.primary
                val surfaceVariant = colors.surfaceVariant
                val textSecondaryArgb = colors.textSecondary.toArgb()

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomLabelPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val chartHeight = canvasHeight - bottomLabelPadding - topPadding

                    val count = dailyCounts.size
                    val slotWidth = canvasWidth / count
                    val barWidth = (slotWidth * 0.55f).coerceAtMost(28.dp.toPx()).coerceAtLeast(6.dp.toPx())

                    for (i in 0 until count) {
                        val item = dailyCounts[i]
                        val barHeight = if (item.count > 0) {
                            (item.count.toFloat() / maxCount.toFloat()) * chartHeight
                        } else {
                            4.dp.toPx()
                        }

                        val left = (i * slotWidth) + (slotWidth - barWidth) / 2f
                        val top = topPadding + (chartHeight - barHeight)

                        // Draw Bar
                        val barColor = if (item.count > 0) primaryColor.copy(alpha = 0.9f) else surfaceVariant

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Draw Entry Count Label on top if peak or week
                        if (item.count == maxCount && item.count > 0 && period == TimePeriod.WEEK) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = textSecondaryArgb
                                    textSize = labelFontSize.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    isFakeBoldText = true
                                }
                                canvas.nativeCanvas.drawText(
                                    "${item.count}",
                                    left + (barWidth / 2f),
                                    top - 4.dp.toPx(),
                                    paint
                                )
                            }
                        }

                        // Draw Date X-Axis Label
                        val showLabel = when (period) {
                            TimePeriod.WEEK -> true
                            TimePeriod.MONTH -> i % 5 == 0 || i == count - 1
                        }

                        if (showLabel) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = textSecondaryArgb
                                    textSize = labelFontSize.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                canvas.nativeCanvas.drawText(
                                    item.dayLabel,
                                    left + (barWidth / 2f),
                                    canvasHeight - 4.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.SpaceMd))

        // Footer
        val avgText = String.format(Locale.US, "%.1f", averagePerDay)
        Text(
            text = "Average: $avgText per day · Streak: $activeStreak days 🔥",
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall.tabularNumbers(),
            fontWeight = FontWeight.Medium
        )
    }
}
