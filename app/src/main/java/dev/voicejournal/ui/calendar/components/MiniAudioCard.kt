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
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.components.TagChip
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.theme.AppTheme
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
    isMoodEnabled: Boolean = true,
    isTopicsEnabled: Boolean = true,
    isPeopleEnabled: Boolean = true,
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
            .padding(vertical = Spacing.SpaceX2s),
        shape = RoundedCornerShape(Radius.RadiusMd),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(Border.WidthThin, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.SpaceMd),
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
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isMoodEnabled) {
                        Text(
                            text = entry.mood,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.SpaceX2s))

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
                    Spacer(modifier = Modifier.height(Spacing.SpaceXs))

                    // 1-Line Limit FlowRow: Audio Chip (First Priority) & Tags with Overflow
                    FlowRow(
                        maxLines = 1,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Audio Track Chip (FIRST PRIORITY if voice notes exist)
                        if (hasAudio) {
                            Surface(
                                shape = RoundedCornerShape(Radius.RadiusSm),
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = "🎙️ $audioTrackCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    modifier = Modifier.padding(horizontal = Spacing.SpaceXs, vertical = Spacing.SpaceX3s)
                                )
                            }
                        }

                        // 2. Tags Chips (Limit to 2 tags max on single line)
                        val filteredTags = entry.tags.filter { tag ->
                            when (tag.type) {
                                TagType.TOPIC -> isTopicsEnabled
                                TagType.PERSON -> isPeopleEnabled
                                TagType.FOLDER, TagType.THING -> true
                                TagType.MOOD -> isMoodEnabled
                            }
                        }
                        val visibleTags = filteredTags.take(2)
                        visibleTags.forEach { tag ->
                            TagChip(tag = tag, onClick = {})
                        }
                        if (filteredTags.size > 2) {
                            Surface(
                                shape = RoundedCornerShape(Radius.RadiusSm),
                                color = colors.surfaceVariant
                            ) {
                                Text(
                                    text = "...+${filteredTags.size - 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Spacing.SpaceXs, vertical = Spacing.SpaceX3s)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.SpaceMd))

            // Right Action: Chevron right arrow
            Icon(
                imageVector = getChevronRightIcon(colors.textSecondary),
                contentDescription = "View Note Detail",
                tint = colors.textSecondary,
                modifier = Modifier.size(IconSize.IconMd)
            )
        }
    }
}
