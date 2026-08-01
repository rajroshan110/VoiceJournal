package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEntriesByDateUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(dateStr: String): Flow<List<JournalEntry>> {
        return repository.getEntriesByDate(dateStr)
    }
}
