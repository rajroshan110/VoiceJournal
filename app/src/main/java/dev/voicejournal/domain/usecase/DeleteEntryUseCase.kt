package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.repository.JournalRepository
import javax.inject.Inject

class DeleteEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteEntry(id)
    }
}
