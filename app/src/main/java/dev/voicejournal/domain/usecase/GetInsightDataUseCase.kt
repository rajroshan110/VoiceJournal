package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class InsightData(
    val entryCountsByDate: List<Pair<String, Int>>,
    val moodDistribution: List<Pair<String, Int>>,
    val averageDuration: Float?
)

class GetInsightDataUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(start: Long, end: Long): Flow<InsightData> {
        return combine(
            repository.getEntryCountsByDate(),
            repository.getMoodDistribution(start, end),
            repository.getAverageDuration(start, end)
        ) { counts, moods, avgDuration ->
            InsightData(counts, moods, avgDuration)
        }
    }
}
