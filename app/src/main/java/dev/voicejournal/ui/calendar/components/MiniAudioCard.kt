package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.components.TagChip
import dev.voicejournal.ui.theme.AppTheme
import dev.voicejournal.util.TimeFormatter

private fun getChevronRightIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "ChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(10f, 6f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12f)
            lineToRelative(-4.58f, 4.59f)
            lineTo(10f, 18f)
            lineToRelative(6f, -6f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiniAudioCard(
    entry: JournalEntry,
    onCardClick: () -> Unit,
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors

    val timeText = remember(entry.timestamp, timeFormat, context) {
        TimeFormatter.formatTime(entry.timestamp, timeFormat, context)
    }

    val audioTrackCount = entry.allAudioTracks.size
    val hasAudio = audioTrackCount > 0

    val plainText = entry.plainUserText
    val titleOrPreview: String = when {
        !entry.title.isNullOrBlank() -> entry.title
        !plainText.isNullOrEmpty() -> plainText
        !entry.transcript.isNullOrEmpty() -> entry.transcript
        else -> "Journal Entry"
    }

    OutlinedCard(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header Row: Time Readout & Mood Emoji (matching EntryCard)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = entry.mood,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title / Body Text Preview
                Text(
                    text = titleOrPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (hasAudio || entry.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1-Line Limit FlowRow: Audio Chip (First Priority) & Tags with Overflow
                    FlowRow(
                        maxLines = 1,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Audio Track Chip (FIRST PRIORITY if voice notes exist)
                        if (hasAudio) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = "🎙️ $audioTrackCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // 2. Tags Chips (Limit to 2 tags max on single line)
                        val visibleTags = entry.tags.take(2)
                        visibleTags.forEach { tag ->
                            TagChip(tag = tag, onClick = {})
                        }
                        if (entry.tags.size > 2) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant
                            ) {
                                Text(
                                    text = "...+${entry.tags.size - 2}",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Action: Chevron right arrow
            Icon(
                imageVector = getChevronRightIcon(colors.textSecondary),
                contentDescription = "View Note Detail",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
