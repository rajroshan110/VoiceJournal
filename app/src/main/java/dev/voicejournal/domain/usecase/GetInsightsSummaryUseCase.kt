package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import javax.inject.Inject

enum class TimePeriod { WEEK, MONTH }

data class DailyActivityCount(
    val date: LocalDate,
    val count: Int,
    val dayLabel: String
)

data class DailyMoodPoint(
    val date: LocalDate,
    val dayLabel: String,
    val entryEmojis: List<String>,
    val dominantEmoji: String?
)

data class MoodMetric(
    val emoji: String,
    val name: String,
    val percentage: Float,
    val count: Int
)

data class TagMetric(
    val name: String,
    val count: Int,
    val percentage: Float
)

data class PersonMetric(
    val name: String,
    val avatarUri: String? = null,
    val count: Int
)

data class TimeOfDayMetric(
    val periodName: String,
    val icon: String,
    val count: Int,
    val percentage: Float
)

data class InsightsSummary(
    val period: TimePeriod = TimePeriod.WEEK,
    val dateRangeMode: InsightDateRangeMode = InsightDateRangeMode.LAST_DAYS,
    val totalEntries: Int = 0,
    val dailyCounts: List<DailyActivityCount> = emptyList(),
    val dailyMoodPoints: List<DailyMoodPoint> = emptyList(),
    val averagePerDay: Float = 0f,
    val activeStreak: Int = 0,
    val moodDistribution: List<MoodMetric> = emptyList(),
    val dominantMood: MoodMetric? = null,
    val topTags: List<TagMetric> = emptyList(),
    val topPeople: List<PersonMetric> = emptyList(),
    val timeOfDayDistribution: List<TimeOfDayMetric> = emptyList(),
    val peakDay: String = "",
    val peakCount: Int = 0
)

class GetInsightsSummaryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(period: TimePeriod, dateRangeMode: InsightDateRangeMode = InsightDateRangeMode.LAST_DAYS): Flow<InsightsSummary> {
        return repository.getAllEntries().map { allEntries ->
            calculateSummary(allEntries, period, dateRangeMode)
        }
    }

    private fun calculateSummary(
        allEntries: List<JournalEntry>,
        period: TimePeriod,
        dateRangeMode: InsightDateRangeMode
    ): InsightsSummary {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        val startDate = when (dateRangeMode) {
            InsightDateRangeMode.LAST_DAYS -> when (period) {
                TimePeriod.WEEK -> today.minusDays(6) // Last 7 days including today
                TimePeriod.MONTH -> today.minusDays(29) // Last 30 days including today
            }
            InsightDateRangeMode.CURRENT_CALENDAR -> when (period) {
                TimePeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                TimePeriod.MONTH -> today.withDayOfMonth(1)
            }
        }

        val periodEntries = allEntries.filter { entry ->
            val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalDate()
            !entryDate.isBefore(startDate) && !entryDate.isAfter(today)
        }

        val totalEntries = periodEntries.size

        // 1. Daily Activity Breakdown & Daily Mood Points
        val entriesByDate = periodEntries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalDate()
        }

        val dailyCounts = mutableListOf<DailyActivityCount>()
        val dailyMoodPoints = mutableListOf<DailyMoodPoint>()
        val dayFormatter = when (period) {
            TimePeriod.WEEK -> DateTimeFormatter.ofPattern("EEE")
            TimePeriod.MONTH -> DateTimeFormatter.ofPattern("dd MMM")
        }

        var currentDate = startDate
        while (!currentDate.isAfter(today)) {
            val dateEntries = entriesByDate[currentDate] ?: emptyList()
            val count = dateEntries.size
            val label = currentDate.format(dayFormatter)
            dailyCounts.add(DailyActivityCount(currentDate, count, label))

            val emojis = dateEntries.mapNotNull { it.moodEmoji }.ifEmpty {
                dateEntries.map { "😊" } // Default emoji if none set
            }

            val dominantEmoji = if (emojis.isNotEmpty()) {
                emojis.groupBy { it }.maxByOrNull { it.value.size }?.key
            } else null

            dailyMoodPoints.add(
                DailyMoodPoint(
                    date = currentDate,
                    dayLabel = label,
                    entryEmojis = emojis,
                    dominantEmoji = dominantEmoji
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        // Peak day
        val peak = dailyCounts.maxByOrNull { it.count }
        val peakDay = peak?.dayLabel ?: ""
        val peakCount = peak?.count ?: 0

        // Daily average calculation based on active days span
        val totalDaysCalculated = (java.time.temporal.ChronoUnit.DAYS.between(startDate, today) + 1).coerceAtLeast(1).toFloat()
        val averagePerDay = (totalEntries.toFloat() / totalDaysCalculated)

        // 2. Active Streak (all-time streak leading to today)
        val allEntryDates = allEntries.map { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalDate()
        }.toSet()

        var streak = 0
        var streakCheckDate = today
        if (allEntryDates.contains(today) || allEntryDates.contains(today.minusDays(1))) {
            if (!allEntryDates.contains(today)) {
                streakCheckDate = today.minusDays(1)
            }
            while (allEntryDates.contains(streakCheckDate)) {
                streak++
                streakCheckDate = streakCheckDate.minusDays(1)
            }
        }

        // 3. Mood Distribution (using canonical emoji set)
        val canonicalMoodNames = mapOf(
            "😊" to "Happy",
            "😌" to "Peaceful",
            "😔" to "Sad",
            "😤" to "Frustrated",
            "😡" to "Angry"
        )

        val moodCounts = periodEntries.groupBy { it.moodEmoji ?: "😊" }
            .mapValues { it.value.size }

        val moodDistribution = moodCounts.map { (emoji, count) ->
            val percentage = if (totalEntries > 0) (count.toFloat() / totalEntries.toFloat()) * 100f else 0f
            val name = canonicalMoodNames[emoji] ?: "Good"
            MoodMetric(emoji, name, percentage, count)
        }.sortedByDescending { it.count }

        val dominantMood = moodDistribution.firstOrNull()

        val nonArchivedPeriodEntries = periodEntries.filter { !it.isArchived }

        // 4. Top Tags (limited to top 3, plain text without '#') - Excluding Archived Entries
        val tagCounts = nonArchivedPeriodEntries.flatMap { entry ->
            val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
            val regexTags = Regex("#\\w+").findAll(textContent).map { it.value.removePrefix("#") }.toList()
            (entry.tags.map { it.name.removePrefix("#") } + regexTags).distinct()
        }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()

        val totalTagOccurrences = tagCounts.values.sum().coerceAtLeast(1)
        val topTags = tagCounts.map { (tagName, count) ->
            TagMetric(
                name = tagName.removePrefix("#"),
                count = count,
                percentage = (count.toFloat() / totalTagOccurrences.toFloat()) * 100f
            )
        }.sortedByDescending { it.count }.take(3)

        // 5. Top People Mentioned (limited to top 10 values) - Excluding Archived Entries
        val peopleCounts = nonArchivedPeriodEntries.flatMap { entry ->
            val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
            val mentions = Regex("@\\w+").findAll(textContent).map { it.value.removePrefix("@") }.toList()
            (entry.people.map { it.removePrefix("@") } + mentions).distinct()
        }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()

        val topPeople = peopleCounts.map { (name, count) ->
            PersonMetric(name = name.removePrefix("@"), count = count)
        }.sortedByDescending { it.count }.take(10)

        // 6. Time of Day Recording Distribution (Peak Reflection Hours)
        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var nightCount = 0

        periodEntries.forEach { entry ->
            val time = Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalTime()
            val hour = time.hour
            when (hour) {
                in 5..11 -> morningCount++
                in 12..16 -> afternoonCount++
                in 17..20 -> eveningCount++
                else -> nightCount++
            }
        }

        val totalTimeEntries = totalEntries.coerceAtLeast(1).toFloat()
        val timeOfDayDistribution = listOf(
            TimeOfDayMetric("Morning", "🌅", morningCount, (morningCount / totalTimeEntries) * 100f),
            TimeOfDayMetric("Afternoon", "☀️", afternoonCount, (afternoonCount / totalTimeEntries) * 100f),
            TimeOfDayMetric("Evening", "🌆", eveningCount, (eveningCount / totalTimeEntries) * 100f),
            TimeOfDayMetric("Night", "🌙", nightCount, (nightCount / totalTimeEntries) * 100f)
        )

        return InsightsSummary(
            period = period,
            dateRangeMode = dateRangeMode,
            totalEntries = totalEntries,
            dailyCounts = dailyCounts,
            dailyMoodPoints = dailyMoodPoints,
            averagePerDay = averagePerDay,
            activeStreak = streak,
            moodDistribution = moodDistribution,
            dominantMood = dominantMood,
            topTags = topTags,
            topPeople = topPeople,
            timeOfDayDistribution = timeOfDayDistribution,
            peakDay = peakDay,
            peakCount = peakCount
        )
    }
}
