package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetEntriesByTagUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(tagId: Long): Flow<List<JournalEntry>> {
        return repository.getAllEntries().map { entries ->
            entries.filter { entry -> entry.tags.any { it.id == tagId } }
        }
    }
}
