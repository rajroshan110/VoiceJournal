package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllEntriesUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.getActiveEntries()
}
