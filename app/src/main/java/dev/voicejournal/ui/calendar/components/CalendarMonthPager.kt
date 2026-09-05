package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.ui.calendar.CalendarDayItem
import dev.voicejournal.ui.calendar.CalendarUtils
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarMonthPager(
    pagerState: PagerState,
    selectedDate: LocalDate?,
    currentYearMonth: YearMonth,
    currentGridDays: List<CalendarDayItem>,
    entriesByDate: Map<LocalDate, List<JournalEntry>>,
    startOfWeek: StartOfWeek,
    onDateSelect: (CalendarDayItem) -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val dayLabels = remember(startOfWeek) {
        val firstDayOfWeek = when (startOfWeek) {
            StartOfWeek.SYSTEM_DEFAULT -> WeekFields.of(Locale.getDefault()).firstDayOfWeek
            StartOfWeek.MONDAY -> DayOfWeek.MONDAY
            StartOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
        (0 until 7).map { i ->
            firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Header Row: Days of Week (Mon, Tue, Wed...) - Fixed at top of calendar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Horizontal Pager for months - smooth smartphone page swipe gesture with preview and page spacing
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { page -> page },
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val pageYearMonth = remember(page) {
                CalendarUtils.pageToYearMonth(page)
            }
            val gridDays = remember(pageYearMonth, selectedDate, startOfWeek, entriesByDate, currentGridDays) {
                if (pageYearMonth == currentYearMonth && currentGridDays.isNotEmpty()) {
                    currentGridDays
                } else {
                    val isPageCurrentMonth = pageYearMonth == YearMonth.now()
                    val hasPageSelection = selectedDate != null && selectedDate.year == pageYearMonth.year && selectedDate.month == pageYearMonth.month
                    val pageSelectedDate = if (hasPageSelection) selectedDate else if (isPageCurrentMonth) LocalDate.now() else null
                    CalendarUtils.buildGridDays(
                        yearMonth = pageYearMonth,
                        selectedDate = pageSelectedDate,
                        startOfWeek = startOfWeek,
                        entriesByDate = entriesByDate
                    )
                }
            }

            CalendarGrid(
                gridDays = gridDays,
                startOfWeek = startOfWeek,
                onDateSelect = onDateSelect,
                isCompact = isCompact,
                showWeekHeader = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }
    }
}
