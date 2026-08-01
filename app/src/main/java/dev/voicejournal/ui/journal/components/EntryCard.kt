package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.components.UnifiedAudioPlayerBar
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.theme.AppTheme
import dev.voicejournal.util.TimeFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    entry: JournalEntry,
    activeTrackId: String? = null,
    isPlaying: Boolean = false,
    isBuffering: Boolean = false,
    isAudioError: Boolean = false,
    currentPositionMs: Long = 0L,
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    onPlayPauseTrackClick: (AudioTrack) -> Unit = {},
    onSeekTrackFraction: (AudioTrack, Float) -> Unit = { _, _ -> },
    onCardClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onTagClick: (Tag) -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Format date & time adhering strictly to user timeFormat preference
    val formattedDateText = remember(entry.timestamp, timeFormat, context) {
        TimeFormatter.formatDateWithTime(entry.timestamp, timeFormat, context)
    }

    val hasHeader = !entry.title.isNullOrBlank()
    val bodyText = entry.plainUserText?.ifBlank { null } ?: entry.transcript?.ifBlank { null }
    val hasBodyText = !bodyText.isNullOrBlank()

    val previewTextToShow: String? = when {
        hasHeader -> entry.title
        hasBodyText -> bodyText
        else -> null
    }

    val colors = AppTheme.colors
    val cardBorderColor = if (isSelected) colors.primary else colors.border
    val cardBgColor = colors.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceX2s)
            .clip(RoundedCornerShape(Radius.RadiusLg))
            .border(
                Border.getSelectionBorder(isSelected, colors.primary, colors.border),
                shape = RoundedCornerShape(Radius.RadiusLg)
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(Radius.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceXs)
        ) {
            // 1. Header Layout (Row: Date/Time + Mood + Selection Checkmark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.mood,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = colors.primary,
                            modifier = Modifier.size(IconSize.IconSm)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Body Layout (Column)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Image Preview Row (render up to 4 thumbnails in fixed 44dp square horizontal row)
                if (entry.mediaThumbnails.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        val imagesToShow = entry.mediaThumbnails.take(4)
                        imagesToShow.forEach { imagePath ->
                            val modelObj: Any = if (imagePath.startsWith("/")) File(imagePath) else imagePath
                            Box(modifier = Modifier.size(44.dp)) {
                                AsyncImage(
                                    model = modelObj,
                                    contentDescription = "Media Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceVariant)
                                        .clickable { onImageClick(imagePath) }
                                )
                            }
                        }
                    }
                }

                // Title / Body Preview Text Line
                if (previewTextToShow != null) {
                    Text(
                        text = previewTextToShow,
                        style = if (hasHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (hasHeader) FontWeight.Bold else FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }

                // Render ALL Audio Tracks (Up to 3 recorded files per note)
                val tracksToRender = entry.allAudioTracks.take(3)
                if (tracksToRender.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        tracksToRender.forEach { track ->
                            val isTrackActive = activeTrackId == track.id
                            val isTrackPlaying = isTrackActive && isPlaying
                            val isTrackBuffering = isTrackActive && isBuffering
                            val isTrackError = isTrackActive && isAudioError
                            val trackPos = if (isTrackActive) currentPositionMs else 0L

                            UnifiedAudioPlayerBar(
                                isPlaying = isTrackPlaying,
                                isBuffering = isTrackBuffering,
                                isAudioError = isTrackError,
                                currentPositionMs = trackPos,
                                durationMs = track.durationMs.coerceAtLeast(1000L),
                                waveformAmplitudes = track.rawWaveformAmplitudes,
                                onPlayPauseClick = { onPlayPauseTrackClick(track) },
                                onSeekFraction = if (isTrackPlaying) { fraction -> onSeekTrackFraction(track, fraction) } else null
                            )
                        }
                    }
                }
            }
        }
    }
}
