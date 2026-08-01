package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalendarDataUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<Pair<String, Int>>> {
        return repository.getEntryCountsByDate()
    }
}
