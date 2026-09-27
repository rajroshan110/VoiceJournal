package dev.voicejournal.data.repository

import android.annotation.SuppressLint
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
import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.data.mapper.toAudioTracks
import dev.voicejournal.data.mapper.toTracksJson
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

    override suspend fun setAudioFormat(format: AudioFormat) {
        prefs.setAudioFormat(format)
    }

    override suspend fun setWhisperModel(model: String) {
        prefs.setWhisperModel(model)
    }

    private fun extractAudioDuration(file: File): Long {
        if (!file.exists() || file.length() < 44) return 0L
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val parsed = timeStr?.toLongOrNull() ?: 0L
            if (parsed > 0L) {
                parsed
            } else if (file.extension.equals("wav", ignoreCase = true)) {
                // Fallback for standard 16kHz 16-bit mono WAV: 32000 bytes/sec
                val dataSize = (file.length() - 44).coerceAtLeast(0)
                (dataSize * 1000L) / 32000L
            } else {
                0L
            }
        } catch (_: Exception) {
            if (file.extension.equals("wav", ignoreCase = true)) {
                val dataSize = (file.length() - 44).coerceAtLeast(0)
                (dataSize * 1000L) / 32000L
            } else {
                0L
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("RestrictedApi")
    override suspend fun refreshAndHealData() = withContext(Dispatchers.IO) {
        // 1. Repair WAV file headers with corrupt/zero sizes
        AudioFileRepair.forceRepair(context)

        // 2. Clean up stale transcribe cache files (> 1 hour old)
        try {
            val cacheDir = MediaStorageManager.getTranscribeCacheDir(context)
            if (cacheDir.exists()) {
                val oneHourAgo = System.currentTimeMillis() - 3600_000L
                cacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Purge expired trash entries (> 7 days retention)
        try {
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
            journalEntryDao.deleteExpiredTrashEntries(sevenDaysAgo)
        } catch (_: Exception) {}

        // 4. Database & Media reconciliation
        try {
            // Verify SQLite integrity
            val db = appDatabase.openHelper.writableDatabase
            db.query("PRAGMA quick_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                }
            }

            // Scan all raw entries to reconcile audio paths, durations, tracks, and UUIDs
            val allEntries = journalEntryDao.getAllRawEntriesSync()
            for (entity in allEntries) {
                var needsUpdate = false
                var updatedEntity = entity

                // Ensure UUID is present
                if (updatedEntity.uuid.isBlank()) {
                    updatedEntity = updatedEntity.copy(uuid = java.util.UUID.randomUUID().toString())
                    needsUpdate = true
                }

                // Reconcile primary audio
                if (entity.audioPath.isNotBlank()) {
                    val audioFile = MediaStorageManager.getAudioFile(context, entity.audioPath)
                    if (audioFile.exists() && audioFile.length() > 0) {
                        if (audioFile.extension.equals("wav", ignoreCase = true)) {
                            AudioFileRepair.repairWavFile(audioFile)
                        }
                        if (entity.audioPath != audioFile.absolutePath) {
                            updatedEntity = updatedEntity.copy(audioPath = audioFile.absolutePath)
                            needsUpdate = true
                        }
                        if (updatedEntity.duration <= 0L) {
                            val duration = extractAudioDuration(audioFile)
                            if (duration > 0L) {
                                updatedEntity = updatedEntity.copy(duration = duration)
                                needsUpdate = true
                            }
                        }
                    }
                }

                // Reconcile audio tracks
                if (entity.audioTracksJson.isNotBlank()) {
                    try {
                        val tracks = entity.audioTracksJson.toAudioTracks()
                        var tracksChanged = false
                        val reconciledTracks = tracks.map { track ->
                            var modTrack = track
                            if (track.path.isNotBlank()) {
                                val trackFile = MediaStorageManager.getAudioFile(context, track.path)
                                if (trackFile.exists()) {
                                    if (trackFile.extension.equals("wav", ignoreCase = true)) {
                                        AudioFileRepair.repairWavFile(trackFile)
                                    }
                                    if (track.path != trackFile.absolutePath) {
                                        modTrack = modTrack.copy(path = trackFile.absolutePath)
                                        tracksChanged = true
                                    }
                                    if (modTrack.durationMs <= 0L) {
                                        val dur = extractAudioDuration(trackFile)
                                        if (dur > 0L) {
                                            modTrack = modTrack.copy(durationMs = dur)
                                            tracksChanged = true
                                        }
                                    }
                                }
                            }
                            modTrack
                        }
                        if (tracksChanged) {
                            updatedEntity = updatedEntity.copy(
                                audioTracksJson = reconciledTracks.toTracksJson()
                            )
                            needsUpdate = true
                        }
                    } catch (_: Exception) {}
                }

                // If primary audio is blank but tracks exist, heal primary audio path and duration
                if (updatedEntity.audioPath.isBlank() && updatedEntity.audioTracksJson.isNotBlank()) {
                    try {
                        val tracks = updatedEntity.audioTracksJson.toAudioTracks()
                        val firstValid = tracks.firstOrNull { it.path.isNotBlank() }
                        if (firstValid != null) {
                            updatedEntity = updatedEntity.copy(
                                audioPath = firstValid.path,
                                duration = if (updatedEntity.duration <= 0L) firstValid.durationMs else updatedEntity.duration
                            )
                            needsUpdate = true
                        }
                    } catch (_: Exception) {}
                }

                if (needsUpdate) {
                    journalEntryDao.updateEntry(updatedEntity)
                }
            }
        } finally {
            // 5. Refresh Room invalidation tracker to re-query and re-emit latest data to all active flows
            try {
                appDatabase.invalidationTracker.refreshVersionsSync()
            } catch (_: Exception) {
                try {
                    appDatabase.invalidationTracker.refreshVersionsAsync()
                } catch (_: Exception) {}
            }
        }
    }
}
