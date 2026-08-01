package dev.voicejournal.domain.repository

import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    fun getActiveEntries(): Flow<List<JournalEntry>>
    fun getArchiveEntries(): Flow<List<JournalEntry>>
    fun getDraftEntries(): Flow<List<JournalEntry>>
    fun getEntryById(id: Long): Flow<JournalEntry?>
    fun getEntriesByDate(dateStr: String): Flow<List<JournalEntry>>
    fun getEntryCountsByDate(): Flow<List<Pair<String, Int>>>
    fun getMoodDistribution(start: Long, end: Long): Flow<List<Pair<String, Int>>>
    fun getAverageDuration(start: Long, end: Long): Flow<Float?>
    
    fun getTrashEntries(): Flow<List<JournalEntry>>
    
    suspend fun batchCategorizeEntries(
        ids: List<Long>,
        targetFolder: String?,
        tagsToAssign: List<Tag>,
        tagsToRemove: List<Tag> = emptyList(),
        folderToRemove: String? = null
    )
    suspend fun saveEntry(entry: JournalEntry): Long
    suspend fun deleteEntry(id: Long)
    suspend fun archiveEntry(id: Long)
    suspend fun archiveEntries(ids: List<Long>)
    suspend fun unarchiveEntry(id: Long)
    suspend fun unarchiveEntries(ids: List<Long>)
    suspend fun moveToTrash(id: Long)
    suspend fun moveAllToTrash(ids: List<Long>)
    suspend fun restoreFromTrash(id: Long)
    suspend fun restoreAllFromTrash(ids: List<Long>)
    suspend fun permanentlyDeleteEntry(id: Long)
    suspend fun permanentlyDeleteEntries(ids: List<Long>)
    suspend fun emptyTrash()
    suspend fun purgeExpiredTrashEntries(retentionDays: Int = 7)
    suspend fun wipeAllData()
    
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsByType(type: TagType): Flow<List<Tag>>
    suspend fun getOrCreateTag(name: String, type: TagType): Tag
    suspend fun renameTag(tagId: Long, newName: String, type: TagType)
    suspend fun mergeTags(sourceTagId: Long, targetTagId: Long)
    suspend fun deleteTag(id: Long)
    
    val audioFormat: Flow<AudioFormat>
    val whisperModel: Flow<String>
    val dailyReminder: Flow<Boolean>
    
    suspend fun setAudioFormat(format: AudioFormat)
    suspend fun setWhisperModel(model: String)
    suspend fun setDailyReminder(enabled: Boolean)
}
