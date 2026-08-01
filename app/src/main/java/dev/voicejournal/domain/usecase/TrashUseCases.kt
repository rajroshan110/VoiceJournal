package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrashEntriesUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.getTrashEntries()
}

class RestoreEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.restoreFromTrash(id)
    }

    suspend fun restoreAll(ids: List<Long>) {
        repository.restoreAllFromTrash(ids)
    }
}

class PermanentlyDeleteEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.permanentlyDeleteEntry(id)
    }

    suspend fun deleteAll(ids: List<Long>) {
        repository.permanentlyDeleteEntries(ids)
    }
}

class EmptyTrashUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}

class PurgeExpiredTrashUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(retentionDays: Int = 7) {
        repository.purgeExpiredTrashEntries(retentionDays)
    }
}
