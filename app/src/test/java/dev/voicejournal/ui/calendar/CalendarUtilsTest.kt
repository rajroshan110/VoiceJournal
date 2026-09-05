package dev.voicejournal.ui.calendar

import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarUtilsTest {

    @Test
    fun testYearMonthToPageAndPageToYearMonthRoundTrip() {
        val testMonths = listOf(
            YearMonth.of(1970, 1),
            YearMonth.of(1990, 6),
            YearMonth.of(2000, 12),
            YearMonth.of(2024, 2),
            YearMonth.of(2026, 9),
            YearMonth.of(2030, 1)
        )

        for (ym in testMonths) {
            val page = CalendarUtils.yearMonthToPage(ym)
            val convertedBack = CalendarUtils.pageToYearMonth(page)
            assertEquals("Round trip failed for $ym", ym, convertedBack)
        }
    }

    @Test
    fun testYearMonthToPageBoundary() {
        val startPage = CalendarUtils.yearMonthToPage(CalendarUtils.START_YEAR_MONTH)
        assertEquals(0, startPage)

        val beforeStart = YearMonth.of(1960, 1)
        val coercedPage = CalendarUtils.yearMonthToPage(beforeStart)
        assertEquals(0, coercedPage)
    }

    @Test
    fun testPageCountAndMaxPage() {
        val testNow = YearMonth.of(2026, 9)
        val maxPage = CalendarUtils.getMaxPage(testNow)
        val pageCount = CalendarUtils.getPageCount(testNow)

        assertEquals(maxPage + 1, pageCount)
        assertEquals(testNow, CalendarUtils.pageToYearMonth(maxPage))
    }

    @Test
    fun testBuildGridDaysProduces42Days() {
        val ym = YearMonth.of(2026, 9)
        val selectedDate = LocalDate.of(2026, 9, 6)
        val today = LocalDate.of(2026, 9, 6)

        val days = CalendarUtils.buildGridDays(
            yearMonth = ym,
            selectedDate = selectedDate,
            startOfWeek = StartOfWeek.MONDAY,
            entriesByDate = emptyMap(),
            today = today
        )

        assertEquals(42, days.size)
        // First day of grid for Monday start of week in Sept 2026
        // Sept 1, 2026 is a Tuesday, so Monday Aug 31 is the first grid day
        assertEquals(LocalDate.of(2026, 8, 31), days.first().date)
        assertEquals(DayOfWeek.MONDAY, days.first().date.dayOfWeek)
        assertFalse(days.first().isCurrentMonth)

        // Sept 1 is index 1
        assertEquals(LocalDate.of(2026, 9, 1), days[1].date)
        assertTrue(days[1].isCurrentMonth)

        // Sept 6 is selected and today
        val day6 = days.first { it.date == selectedDate }
        assertTrue(day6.isSelected)
        assertTrue(day6.isToday)
        assertFalse(day6.isFuture)

        // Future day check
        val futureDay = days.first { it.date == LocalDate.of(2026, 9, 7) }
        assertTrue(futureDay.isFuture)
    }

    @Test
    fun testBuildGridDaysStartOfWeekSunday() {
        val ym = YearMonth.of(2026, 9)
        val days = CalendarUtils.buildGridDays(
            yearMonth = ym,
            selectedDate = LocalDate.of(2026, 9, 1),
            startOfWeek = StartOfWeek.SUNDAY,
            entriesByDate = emptyMap()
        )

        assertEquals(42, days.size)
        assertEquals(DayOfWeek.SUNDAY, days.first().date.dayOfWeek)
        // Sept 1, 2026 is Tuesday, so Sunday is Aug 30
        assertEquals(LocalDate.of(2026, 8, 30), days.first().date)
    }

    @Test
    fun testBuildGridDaysEntryMappingAndCategories() {
        val ym = YearMonth.of(2026, 9)
        val entryDate = LocalDate.of(2026, 9, 5)
        val sampleEntry = JournalEntry(
            id = 1L,
            createdAt = 1000L,
            updatedAt = 1000L,
            audioPath = "/path/to/audio",
            duration = 5000L,
            title = "Test Entry",
            userText = "Test Note",
            transcript = null,
            moodEmoji = "😊",
            tags = listOf(
                Tag(1L, "Work", TagType.TOPIC),
                Tag(2L, "Alice", TagType.PERSON)
            ),
            audioTracks = listOf(AudioTrack(id = "t1", path = "/path/to/audio", durationMs = 5000L))
        )

        val entriesByDate = mapOf(entryDate to listOf(sampleEntry))

        val days = CalendarUtils.buildGridDays(
            yearMonth = ym,
            selectedDate = LocalDate.of(2026, 9, 1),
            startOfWeek = StartOfWeek.MONDAY,
            entriesByDate = entriesByDate
        )

        val dayWithEntry = days.first { it.date == entryDate }
        assertEquals(1, dayWithEntry.entries.size)
        assertTrue(dayWithEntry.categories.contains("Work"))
        assertTrue(dayWithEntry.categories.contains("Personal"))
        assertTrue(dayWithEntry.categories.contains("Mood"))
    }
}
