package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import javax.inject.Inject

class SaveEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Long {
        return repository.saveEntry(entry)
    }
}
