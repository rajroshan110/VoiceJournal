package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
}
