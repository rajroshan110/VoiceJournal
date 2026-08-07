package dev.voicejournal.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.dao.EntryImageDao
import dev.voicejournal.data.local.db.dao.JournalEntryDao
import dev.voicejournal.data.local.db.dao.TagDao
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.TagEntity
import dev.voicejournal.data.mapper.toDomain
import dev.voicejournal.data.mapper.toEntity
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val journalEntryDao: JournalEntryDao,
    private val tagDao: TagDao,
    private val entryImageDao: EntryImageDao,
    private val prefs: UserPreferencesManager,
    private val appDatabase: AppDatabase
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAllEntriesWithTagsAndImages().map { list ->
            list.map { it.toDomain() }.filter { !it.isEmpty }
        }
    }

    override fun getActiveEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getActiveEntriesWithTagsAndImages().map { list ->
            list.map { it.toDomain() }.filter { !it.isEmpty }
        }
    }

    override fun getArchiveEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getArchiveEntriesWithTagsAndImages().map { list ->
            list.map { it.toDomain() }.filter { !it.isEmpty }
        }
    }

    override fun getDraftEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getDraftEntriesWithTagsAndImages().map { list ->
            list.map { it.toDomain() }.filter { !it.isEmpty }
        }
    }

    override fun getEntryById(id: Long): Flow<JournalEntry?> {
        return journalEntryDao.getEntryById(id).map { it?.toDomain() }
    }

    override fun getEntriesByDate(dateStr: String): Flow<List<JournalEntry>> {
        return journalEntryDao.getEntriesByDate(dateStr).map { list ->
            list.map { it.toDomain() }.filter { !it.isEmpty }
        }
    }

    override fun getEntryCountsByDate(): Flow<List<Pair<String, Int>>> {
        return journalEntryDao.getEntryCountsByDate().map { list ->
            list.map { it.date to it.count }
        }
    }

    override fun getMoodDistribution(start: Long, end: Long): Flow<List<Pair<String, Int>>> {
        return journalEntryDao.getMoodDistribution(start, end).map { list ->
            list.map { it.moodEmoji to it.count }
        }
    }

    override fun getAverageDuration(start: Long, end: Long): Flow<Float?> {
        return journalEntryDao.getAverageDuration(start, end)
    }

    override suspend fun batchCategorizeEntries(
        ids: List<Long>,
        targetFolder: String?,
        tagsToAssign: List<Tag>,
        tagsToRemove: List<Tag>,
        folderToRemove: String?
    ) {
        if (ids.isEmpty()) return

        appDatabase.withTransaction {
            // 1. Process Folder Removal
            if (!folderToRemove.isNullOrBlank()) {
                journalEntryDao.removeFolderCrossRefs(ids, folderToRemove)
            }

            // 2. Process Tag Removals
            if (tagsToRemove.isNotEmpty()) {
                tagsToRemove.forEach { tag ->
                    val cleanName = tag.name.trim().removePrefix("#").removePrefix("@")
                    val normalizedName = when (tag.type) {
                        TagType.TOPIC -> "#$cleanName"
                        TagType.PERSON -> "@$cleanName"
                        else -> cleanName
                    }
                    val existingTag = tagDao.getTagByNameAndType(normalizedName, tag.type.name)
                        ?: tagDao.getTagByNameAndType(cleanName, tag.type.name)
                    if (existingTag != null) {
                        journalEntryDao.deleteSpecificTagCrossRefs(ids, existingTag.id)
                    }
                }
            }

            // 3. Process Tag & Folder Additions
            val allTagsToApply = mutableListOf<Tag>()
            if (!targetFolder.isNullOrBlank() && targetFolder != folderToRemove) {
                allTagsToApply.add(Tag(name = targetFolder, type = TagType.FOLDER))
            }
            allTagsToApply.addAll(tagsToAssign)

            if (allTagsToApply.isNotEmpty()) {
                val existingCrossRefs = journalEntryDao.getAllEntryTagCrossRefsSync().toSet()
                val newCrossRefs = mutableListOf<EntryTagCrossRef>()

                allTagsToApply.forEach { tag ->
                    val tagId = getOrCreateTag(tag.name, tag.type).id
                    ids.forEach { entryId ->
                        val ref = EntryTagCrossRef(entryId, tagId)
                        if (!existingCrossRefs.contains(ref)) {
                            newCrossRefs.add(ref)
                        }
                    }
                }

                if (newCrossRefs.isNotEmpty()) {
                    journalEntryDao.insertEntryTagCrossRefs(newCrossRefs)
                }
            }
        }
    }

    override suspend fun saveEntry(entry: JournalEntry): Long {
        return appDatabase.withTransaction {
            val entryId = if (entry.id == 0L) {
                journalEntryDao.insertEntry(entry.toEntity())
            } else {
                journalEntryDao.updateEntry(entry.toEntity())
                journalEntryDao.deleteEntryTagCrossRefs(entry.id)
                entryImageDao.deleteEntryImages(entry.id)
                entry.id
            }

            entry.tags.forEach { tag ->
                val tagId = getOrCreateTag(tag.name, tag.type).id
                journalEntryDao.insertEntryTagCrossRef(EntryTagCrossRef(entryId, tagId))
            }

            val images = entry.images.map { it.toEntity(entryId) }
            if (images.isNotEmpty()) {
                entryImageDao.insertEntryImages(images)
            }

            entryId
        }
    }

    override fun getTrashEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getTrashEntriesWithTagsAndImages().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun deleteEntry(id: Long) {
        moveToTrash(id)
    }

    override suspend fun archiveEntry(id: Long) {
        journalEntryDao.setArchived(id, true)
    }

    override suspend fun archiveEntries(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            journalEntryDao.setArchivedEntries(ids, true)
        }
    }

    override suspend fun unarchiveEntry(id: Long) {
        journalEntryDao.setArchived(id, false)
    }

    override suspend fun unarchiveEntries(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            journalEntryDao.setArchivedEntries(ids, false)
        }
    }

    override suspend fun moveToTrash(id: Long) {
        journalEntryDao.softDeleteEntry(id, System.currentTimeMillis())
    }

    override suspend fun moveAllToTrash(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            journalEntryDao.softDeleteEntries(ids, System.currentTimeMillis())
        }
    }

    override suspend fun restoreFromTrash(id: Long) {
        journalEntryDao.restoreEntry(id)
    }

    override suspend fun restoreAllFromTrash(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            journalEntryDao.restoreEntries(ids)
        }
    }

    override suspend fun permanentlyDeleteEntry(id: Long) {
        permanentlyDeleteEntries(listOf(id))
    }

    override suspend fun permanentlyDeleteEntries(ids: List<Long>) {
        if (ids.isEmpty()) return
        
        val pathsToDelete = mutableListOf<String>()
        
        appDatabase.withTransaction {
            ids.forEach { id ->
                // Collect media paths before deletion
                val entryWithDetails = journalEntryDao.getEntryByIdSync(id)
                if (entryWithDetails != null) {
                    val entry = entryWithDetails.entry
                    if (entry.audioPath.isNotBlank()) pathsToDelete.add(entry.audioPath)
                    
                    val tracksJson = entry.audioTracksJson
                    if (!tracksJson.isNullOrBlank()) {
                        try {
                            val array = org.json.JSONArray(tracksJson)
                            for (i in 0 until array.length()) {
                                val path = array.optJSONObject(i)?.optString("path")
                                if (!path.isNullOrBlank()) pathsToDelete.add(path)
                            }
                        } catch (e: Exception) {}
                    }
                    
                    entryWithDetails.images.forEach { image ->
                        if (image.imagePath.isNotBlank()) pathsToDelete.add(image.imagePath)
                    }
                }
                
                journalEntryDao.deleteEntryTagCrossRefs(id)
                entryImageDao.deleteEntryImages(id)
                journalEntryDao.deleteEntry(id)
            }
        }
        
        // Safely delete collected files
        withContext(Dispatchers.IO) {
            pathsToDelete.forEach { path ->
                if (MediaStorageManager.isInternalMedia(context, path)) {
                    try {
                        val file = File(path)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {}
                }
            }
        }
    }

    override suspend fun emptyTrash() {
        // Fetch all trash entry IDs first so their media is deleted
        val trashIds = journalEntryDao.getAllTrashEntryIds()
        permanentlyDeleteEntries(trashIds)
    }

    override suspend fun purgeExpiredTrashEntries(retentionDays: Int) {
        val threshold = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val expiredIds = journalEntryDao.getExpiredTrashEntryIds(threshold)
        permanentlyDeleteEntries(expiredIds)
    }

    override suspend fun wipeAllData() {
        withContext(Dispatchers.IO) {
            appDatabase.withTransaction {
                journalEntryDao.deleteAllJournalEntries()
                journalEntryDao.deleteAllCrossRefs()
                tagDao.deleteAllTags()
                entryImageDao.deleteAllEntryImages()
            }

            try {
                MediaStorageManager.clearAllMedia(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { list -> list.map { it.toDomain() } }
    }

    override fun getTagsByType(type: TagType): Flow<List<Tag>> {
        return tagDao.getTagsByType(type.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOrCreateTag(name: String, type: TagType): Tag {
        return appDatabase.withTransaction {
            val cleanName = name.trim().removePrefix("#").removePrefix("@")
            val normalizedName = when (type) {
                TagType.TOPIC -> "#$cleanName"
                TagType.PERSON -> "@$cleanName"
                else -> cleanName
            }
            val existing = tagDao.getTagByNameAndType(normalizedName, type.name)
                ?: tagDao.getTagByNameAndType(cleanName, type.name)
            if (existing != null) {
                return@withTransaction existing.toDomain()
            }
            val newTag = TagEntity(name = normalizedName, type = type.name)
            val id = tagDao.insertTag(newTag)
            
            if (id == -1L) {
                val racedTag = tagDao.getTagByNameAndType(normalizedName, type.name)
                    ?: tagDao.getTagByNameAndType(cleanName, type.name)
                if (racedTag != null) return@withTransaction racedTag.toDomain()
            }
            
            newTag.copy(id = id).toDomain()
        }
    }

    override suspend fun renameTag(tagId: Long, newName: String, type: TagType) {
        val cleanName = newName.trim().removePrefix("#").removePrefix("@")
        if (cleanName.isBlank()) return
        val normalizedName = when (type) {
            TagType.TOPIC -> "#$cleanName"
            TagType.PERSON -> "@$cleanName"
            else -> cleanName
        }

        appDatabase.withTransaction {
            val existing = tagDao.getTagByNameAndType(normalizedName, type.name)
                ?: tagDao.getTagByNameAndType(cleanName, type.name)

            if (existing != null && existing.id != tagId) {
                // Target tag already exists: merge into existing target tag
                tagDao.mergeTags(tagId, existing.id)
            } else {
                // Rename tag
                tagDao.updateTagName(tagId, normalizedName)
            }
        }
    }

    override suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) {
        if (sourceTagId == targetTagId) return
        tagDao.mergeTags(sourceTagId, targetTagId)
    }

    override suspend fun deleteTag(id: Long) {
        tagDao.deleteTag(id)
    }

    override val audioFormat: Flow<AudioFormat> = prefs.audioFormat
    override val whisperModel: Flow<String> = prefs.whisperModel
    override val dailyReminder: Flow<Boolean> = prefs.dailyReminder

    override suspend fun setAudioFormat(format: AudioFormat) {
        prefs.setAudioFormat(format)
    }

    override suspend fun setWhisperModel(model: String) {
        prefs.setWhisperModel(model)
    }

    override suspend fun setDailyReminder(enabled: Boolean) {
        prefs.setDailyReminder(enabled)
    }
}
