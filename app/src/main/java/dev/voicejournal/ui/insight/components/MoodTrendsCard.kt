package dev.voicejournal.ui.insight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.usecase.DailyMoodPoint
import dev.voicejournal.domain.usecase.MoodMetric
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.util.Locale

// 5 Canonical Emojis used in VoiceJournal filter & note detail (ordered top to bottom: 5 to 1)
private val EMOJI_LEVELS = listOf(
    "😊" to 5.0f, // Happy
    "😌" to 4.0f, // Peaceful
    "😔" to 3.0f, // Sad
    "😤" to 2.0f, // Frustrated
    "😡" to 1.0f  // Angry
)

private fun emojiToScore(emoji: String): Float {
    return when (emoji) {
        "😊" -> 5.0f
        "😌" -> 4.0f
        "😔" -> 3.0f
        "😤" -> 2.0f
        "😡" -> 1.0f
        else -> 5.0f
    }
}

@Composable
fun MoodTrendsCard(
    dominantMood: MoodMetric?,
    moodDistribution: List<MoodMetric>,
    dailyMoodPoints: List<DailyMoodPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val dominantMoodName = dominantMood?.name ?: "No Data"
    val percentageStr = if (dominantMood != null) String.format(Locale.US, "%.0f", dominantMood.percentage) else "0"
    val accessibilityDesc = "Mood trend chart showing daily weighted average mood curve. Dominant mood is $dominantMoodName at $percentageStr percent."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
            .semantics { contentDescription = accessibilityDesc }
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mood Over Time",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Dominant Chip Header
            dominantMood?.let { mood ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${mood.emoji} ${mood.name}",
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Weighted Average Curve Graph Area
        val activePoints = dailyMoodPoints.filter { it.entryEmojis.isNotEmpty() }
        if (dailyMoodPoints.isEmpty() || activePoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(colors.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No mood data recorded for this period",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val primaryColor = colors.primary
                val textSecondary = colors.textSecondary

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val leftMargin = 32.dp.toPx()
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 12.dp.toPx()

                    val chartWidth = canvasWidth - leftMargin
                    val chartHeight = canvasHeight - bottomPadding - topPadding

                    val levelCount = EMOJI_LEVELS.size
                    val levelStep = chartHeight / (levelCount - 1)

                    // 1. Draw Y-Axis Emoji Labels on Left (5 to 1)
                    EMOJI_LEVELS.forEachIndexed { idx, (emoji, _) ->
                        val y = topPadding + (idx * levelStep)
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                textSize = 14.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            canvas.nativeCanvas.drawText(
                                emoji,
                                leftMargin / 2f,
                                y + 5.dp.toPx(),
                                paint
                            )
                        }
                    }

                    fun scoreToY(score: Float): Float {
                        val clamped = score.coerceIn(1.0f, 5.0f)
                        val levelIdx = 5.0f - clamped
                        return topPadding + (levelIdx * levelStep)
                    }

                    val count = dailyMoodPoints.size
                    val slotWidth = chartWidth / count

                    // 2. Compute Daily Weighted Average Points for Active Days
                    val activeOffsets = mutableListOf<Triple<Int, Offset, Boolean>>() // index, Offset, isActive

                    for (i in 0 until count) {
                        val point = dailyMoodPoints[i]
                        val x = leftMargin + (i * slotWidth) + (slotWidth / 2f)

                        if (point.entryEmojis.isNotEmpty()) {
                            val avgScore = point.entryEmojis.map { emojiToScore(it) }.average().toFloat()
                            val y = scoreToY(avgScore)
                            activeOffsets.add(Triple(i, Offset(x, y), true))
                        }
                    }

                    // 3. Draw Solid Line for Continuous Days & Dotted Bridge for Skipped Days
                    if (activeOffsets.size > 1) {
                        val dottedPath = Path()
                        val solidPath = Path()

                        for (k in 0 until activeOffsets.size - 1) {
                            val current = activeOffsets[k]
                            val next = activeOffsets[k + 1]

                            val indexDiff = next.first - current.first
                            if (indexDiff == 1) {
                                // Consecutive active days -> Draw Solid Line
                                solidPath.moveTo(current.second.x, current.second.y)
                                solidPath.lineTo(next.second.x, next.second.y)
                            } else {
                                // Skipped day gap -> Draw Dotted Bridge Line
                                dottedPath.moveTo(current.second.x, current.second.y)
                                dottedPath.lineTo(next.second.x, next.second.y)
                            }
                        }

                        // Draw Solid Path
                        drawPath(
                            path = solidPath,
                            color = primaryColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Draw Dotted Path across missing days
                        drawPath(
                            path = dottedPath,
                            color = primaryColor.copy(alpha = 0.5f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()), 0f)
                            )
                        )
                    }

                    // 4. Draw Weighted Average Nodes for Active Days
                    activeOffsets.forEach { (_, offset, _) ->
                        // Outer accent node
                        drawCircle(
                            color = primaryColor,
                            radius = 5.dp.toPx(),
                            center = offset
                        )
                        // Inner core
                        drawCircle(
                            color = colors.surface,
                            radius = 2.5.dp.toPx(),
                            center = offset
                        )
                    }

                    // 5. Draw X-Axis Date Labels
                    for (i in 0 until count) {
                        val point = dailyMoodPoints[i]
                        val x = leftMargin + (i * slotWidth) + (slotWidth / 2f)

                        val showLabel = when (count) {
                            in 0..7 -> true
                            else -> i % 5 == 0 || i == count - 1
                        }

                        if (showLabel) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = textSecondary.hashCode()
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                canvas.nativeCanvas.drawText(
                                    point.dayLabel,
                                    x,
                                    canvasHeight - 4.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Distribution summary chips or placeholder footer
        if (moodDistribution.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                moodDistribution.take(3).forEach { mood ->
                    val pct = String.format(Locale.US, "%.0f", mood.percentage)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${mood.emoji} ${mood.name} $pct%",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Track your moods daily with voice notes 🎙️",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
