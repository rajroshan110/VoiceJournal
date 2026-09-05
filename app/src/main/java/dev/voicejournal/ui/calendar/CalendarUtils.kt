package dev.voicejournal.ui.calendar

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.StartOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

object CalendarUtils {
    val START_YEAR_MONTH: YearMonth = YearMonth.of(1970, 1)

    fun yearMonthToPage(yearMonth: YearMonth): Int {
        val months = (yearMonth.year - START_YEAR_MONTH.year) * 12 + (yearMonth.monthValue - START_YEAR_MONTH.monthValue)
        return months.coerceAtLeast(0)
    }

    fun pageToYearMonth(page: Int): YearMonth {
        return START_YEAR_MONTH.plusMonths(page.coerceAtLeast(0).toLong())
    }

    fun getMaxPage(now: YearMonth = YearMonth.now()): Int {
        return yearMonthToPage(now)
    }

    fun getPageCount(now: YearMonth = YearMonth.now()): Int {
        return getMaxPage(now) + 1
    }

    fun buildGridDays(
        yearMonth: YearMonth,
        selectedDate: LocalDate,
        startOfWeek: StartOfWeek,
        entriesByDate: Map<LocalDate, List<JournalEntry>>,
        today: LocalDate = LocalDate.now()
    ): List<CalendarDayItem> {
        val firstDayOfWeek = when (startOfWeek) {
            StartOfWeek.SYSTEM_DEFAULT -> WeekFields.of(Locale.getDefault()).firstDayOfWeek
            StartOfWeek.MONDAY -> DayOfWeek.MONDAY
            StartOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
        val firstOfMonth = yearMonth.atDay(1)

        var startLocalDate = firstOfMonth
        while (startLocalDate.dayOfWeek != firstDayOfWeek) {
            startLocalDate = startLocalDate.minusDays(1)
        }

        return (0 until 42).map { dayIndex ->
            val date = startLocalDate.plusDays(dayIndex.toLong())
            val dayEntries = entriesByDate[date] ?: emptyList()
            val categories = dayEntries.flatMap { entry ->
                val list = mutableListOf<String>()
                if (entry.tags.isNotEmpty()) list.add("Work")
                if (entry.people.isNotEmpty()) list.add("Personal")
                if (entry.moodEmoji != null) list.add("Mood")
                list
            }.distinct()

            CalendarDayItem(
                date = date,
                isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
                isToday = date == today,
                isSelected = date == selectedDate,
                isFuture = date.isAfter(today),
                entries = dayEntries,
                categories = categories
            )
        }
    }
}
