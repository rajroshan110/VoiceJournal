package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.journal.CardPlaybackState
import dev.voicejournal.ui.journal.PlaybackStatus
import dev.voicejournal.ui.journal.components.ShimmerSkeletonCard
import dev.voicejournal.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun getScheduleIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Schedule",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(11.99f, 2f)
            curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
            reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
            reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
            reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
            close()
            moveTo(12.5f, 7f)
            horizontalLineTo(11f)
            verticalLineToRelative(6f)
            lineToRelative(5.25f, 3.15f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.5f, -2.67f)
            close()
        }
    }.build()
}

private fun getFilterListOffIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "FilterListOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(10f, 18f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 6f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(18f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
            moveTo(6f, 13f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(-2f)
            horizontalLineTo(6f)
            verticalLineToRelative(2f)
            close()
        }
    }.build()
}

private fun getEventNoteIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "EventNote",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(17f, 10f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(-2f)
            close()
            moveTo(19f, 3f)
            horizontalLineToRelative(-1f)
            verticalLineTo(1f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(2f)
            horizontalLineTo(8f)
            verticalLineTo(1f)
            horizontalLineTo(6f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(8f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(11f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayEntryList(
    selectedDate: LocalDate,
    entries: List<JournalEntry>,
    hasActiveFilters: Boolean,
    isLoading: Boolean,
    playbackState: CardPlaybackState,
    onPlayPauseClick: (JournalEntry) -> Unit,
    onEntryClick: (Long) -> Unit,
    onClearFiltersClick: () -> Unit,
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {}
) {
    val colors = AppTheme.colors
    val formattedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))
    val headerText = "$formattedDateStr — ${entries.size} ${if (entries.size == 1) "entry" else "entries"}"
    val isFutureDate = selectedDate.isAfter(LocalDate.now())

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            headerContent()
        }

        // Sticky Header: Month DD - X entries pins to the top when scrolling past calendar
        stickyHeader {
            Surface(
                color = colors.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        when {
            isLoading -> {
                items(2) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShimmerSkeletonCard()
                    }
                }
            }
            isFutureDate -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = getScheduleIcon(colors.textSecondary),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Future date — no entries recorded.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            entries.isEmpty() && hasActiveFilters -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = getFilterListOffIcon(colors.textSecondary),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No entries match the selected filter.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onClearFiltersClick,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Clear Filter", color = colors.primary)
                        }
                    }
                }
            }
            entries.isEmpty() -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = getEventNoteIcon(colors.textSecondary),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No voice entries recorded for this date.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                items(entries, key = { it.id }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        dev.voicejournal.ui.folders.components.FolderNoteCard(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            timeFormat = timeFormat,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
