package dev.voicejournal.data.backup.v1

import android.content.Context
import androidx.room.withTransaction
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.BackupTag
import dev.voicejournal.data.backup.v1.dto.RestoreStats
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.datastore.SecurityRecoveryReason
import dev.voicejournal.data.security.KeyStoreHelper
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity
import dev.voicejournal.BuildConfig
import dev.voicejournal.data.mapper.toTracksJson
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.journal.components.SortOption
import dev.voicejournal.ui.theme.AppThemeMode
import org.json.JSONArray
import java.io.File
import java.util.Locale

import android.util.Log

class BackupRestorer(
    private val context: Context,
    private val database: AppDatabase,
    private val prefsManager: UserPreferencesManager
) {
    private class RestoreRollbackManager {
        private val actions = mutableListOf<suspend () -> Unit>()

        fun register(action: suspend () -> Unit) {
            actions.add(0, action)
        }

        suspend fun executeRollback() {
            if (actions.isEmpty()) return
            Log.i("Backup", "Initiating rollback of ${actions.size} actions")
            var rollbackSuccess = true
            actions.forEachIndexed { index, action ->
                try {
                    action()
                    Log.i("Backup", "Rollback action ${index + 1} succeeded")
                } catch (e: Exception) {
                    rollbackSuccess = false
                    Log.e("Backup", "CRITICAL: Rollback action ${index + 1} failed!", e)
                }
            }
            if (!rollbackSuccess) {
                throw IOException("Restore failed AND rollback partially failed. App may be in an inconsistent state.")
            }
        }
    }


    suspend fun restore(
        stagingDir: File,
        entries: List<BackupEntry>,
        tags: List<BackupTag>,
        attachments: List<BackupAttachment>,
        preferences: BackupPreferences?,
        onProgress: ((String) -> Unit)? = null
    ): RestoreStats {
        val copiedMediaFiles = mutableListOf<File>()

        val stagingRecordingsDir = File(context.filesDir, "recordings_staging_${System.currentTimeMillis()}")
        val stagingImagesDir = File(context.filesDir, "images_staging_${System.currentTimeMillis()}")
        val rollbackManager = RestoreRollbackManager()

        // Debug Failure Simulation Flags
        val simulateFailureAfterDb = false
        val simulateFailureAfterMedia = false
        val simulateFailureAfterPrefs = false

        try {
            // PHASE 1: Prepare In-Memory Snapshots
            Log.i("Backup", "Phase 1: Preparing snapshots")
            onProgress?.invoke("Preparing restore environment…")
            
            val dbEntriesSnapshot = database.journalEntryDao().getAllEntriesSync()
            val dbTagsSnapshot = database.tagDao().getAllTagsSync()
            val dbImagesSnapshot = database.entryImageDao().getAllEntryImagesSync()
            val dbCrossRefsSnapshot = database.journalEntryDao().getAllEntryTagCrossRefsSync()
            val prefsSnapshot = prefsManager.getRawPreferencesSnapshot()

            // 1. Prepare Internal Media Directories for Staged Restore
            onProgress?.invoke("Staging media…")
            stagingRecordingsDir.mkdirs()
            stagingImagesDir.mkdirs()

            val attachmentsByEntryUuid = attachments.groupBy { it.entryUuid }
            val mediaPathMap = mutableMapOf<String, String>()

            attachments.forEach { att ->
                val sourceFile = File(stagingDir, att.archivePath)
                if (sourceFile.exists()) {
                    val isImage = att.type == "image"
                    val ext = sourceFile.extension.ifBlank { if (isImage) "jpg" else "wav" }
                    val targetStagingDir = if (isImage) stagingImagesDir else stagingRecordingsDir
                    val stagingFile = File(targetStagingDir, "${att.uuid}.$ext")
                    sourceFile.copyTo(stagingFile, overwrite = true)
                    
                    val finalActiveDir = if (isImage) MediaStorageManager.getImagesDir(context) else MediaStorageManager.getRecordingsDir(context)
                    val finalActiveFile = File(finalActiveDir, "${att.uuid}.$ext")
                    mediaPathMap[att.uuid] = finalActiveFile.absolutePath
                    copiedMediaFiles.add(stagingFile)
                }
            }
            Log.d("Backup", "Media staged: ${copiedMediaFiles.size}")

            var entriesCount = 0
            var tagsCount = 0
            var attachmentsCount = 0

            // PHASE 2: Database Replace
            Log.i("Backup", "Phase 2: Database Replace")
            onProgress?.invoke("Restoring notes…")
            
            database.withTransaction {
                database.journalEntryDao().deleteAllJournalEntries()
                database.tagDao().deleteAllTags()
                database.entryImageDao().deleteAllEntryImages()
                database.journalEntryDao().deleteAllCrossRefs()

                val tagIdMap = mutableMapOf<String, Long>()
                tags.forEach { bTag ->
                    val tagTypeStr = bTag.type.uppercase(Locale.ROOT)
                    val newEntity = TagEntity(name = bTag.name, type = tagTypeStr, uuid = bTag.uuid)
                    val tagId = database.tagDao().insertTag(newEntity)
                    tagIdMap[bTag.uuid] = tagId
                    tagsCount++
                }

                entries.forEach { bEntry ->
                    val entryAtts = attachmentsByEntryUuid[bEntry.uuid] ?: emptyList()
                    val audioAtts = entryAtts.filter { it.type == "audio" }
                    val imageAtts = entryAtts.filter { it.type == "image" }

                    var legacyAudioPath = ""
                    var duration = 0L
                    var transcriptText: String? = null
                    var transcriptLang: String? = null
                    var transcriptModel: String? = null
                    var transcriptVersion: String? = null
                    var transcriptCreated: Long? = null

                    val audioTracksList = mutableListOf<AudioTrack>()
                    audioAtts.forEach { att ->
                        val relPath = mediaPathMap[att.uuid] ?: ""
                        val metaObj = att.metadata
                        val durationMs = metaObj.optLong("duration_ms", 0L)
                        val transObj = metaObj.optJSONObject("transcript")

                        val tText = transObj?.optString("text")?.takeIf { it.isNotBlank() }
                        val tLang = transObj?.optString("language")?.takeIf { it.isNotBlank() }
                        val tModel = transObj?.optString("model")?.takeIf { it.isNotBlank() }
                        val tVer = transObj?.optString("model_version")?.takeIf { it.isNotBlank() }
                        val tCreated = if (transObj?.has("created_at") == true && !transObj.isNull("created_at")) transObj.getLong("created_at") else null

                        audioTracksList.add(
                            AudioTrack(
                                id = att.uuid,
                                path = relPath,
                                durationMs = durationMs,
                                transcript = tText,
                                transcriptLanguage = tLang,
                                transcriptModel = tModel,
                                transcriptVersion = tVer,
                                transcriptCreatedAt = tCreated
                            )
                        )

                        if (legacyAudioPath.isEmpty()) {
                            legacyAudioPath = relPath
                            duration = durationMs
                            transcriptText = tText
                            transcriptLang = tLang
                            transcriptModel = tModel
                            transcriptVersion = tVer
                            transcriptCreated = tCreated
                        }
                    }

                    val audioTracksJsonStr = audioTracksList.toTracksJson()
                    val deletedAt = when (bEntry.status) {
                        "trashed" -> bEntry.deletedAt ?: bEntry.createdAt
                        else -> null
                    }
                    val isArchived = bEntry.status == "archived"
                    val isDraft = bEntry.status == "draft"

                    val entryEntity = JournalEntryEntity(
                        id = 0L,
                        createdAt = bEntry.createdAt,
                        updatedAt = bEntry.updatedAt,
                        title = bEntry.title,
                        audioPath = legacyAudioPath,
                        audioFormat = "WAV_16KHZ",
                        duration = duration,
                        transcript = transcriptText,
                        transcriptCreatedAt = transcriptCreated,
                        transcriptModel = transcriptModel,
                        transcriptLanguage = transcriptLang,
                        transcriptVersion = transcriptVersion,
                        userText = bEntry.userText,
                        moodEmoji = bEntry.mood?.emoji,
                        moodLevel = bEntry.mood?.level,
                        hasTranscript = !transcriptText.isNullOrBlank(),
                        audioTracksJson = audioTracksJsonStr,
                        deletedAt = deletedAt,
                        isArchived = isArchived,
                        isDraft = isDraft,
                        uuid = bEntry.uuid
                    )

                    val entryDbId = database.journalEntryDao().insertEntry(entryEntity)
                    entriesCount++

                    database.entryImageDao().deleteEntryImages(entryDbId)
                    val imageEntities = imageAtts.mapIndexed { idx, att ->
                        val relPath = mediaPathMap[att.uuid] ?: ""
                        EntryImageEntity(
                            entryId = entryDbId,
                            imagePath = relPath,
                            displayOrder = att.displayOrder.takeIf { it >= 0 } ?: idx,
                            uuid = att.uuid
                        )
                    }
                    if (imageEntities.isNotEmpty()) {
                        database.entryImageDao().insertEntryImages(imageEntities)
                    }

                    bEntry.tagUuids.forEach { tUuid ->
                        val tagDbId = tagIdMap[tUuid]
                        if (tagDbId != null) {
                            database.journalEntryDao().insertEntryTagCrossRef(
                                EntryTagCrossRef(entryId = entryDbId, tagId = tagDbId)
                            )
                        }
                    }
                    attachmentsCount += entryAtts.size
                }
            }
            
            // Database Replace succeeded. Register rollback to restore original DB snapshot.
            rollbackManager.register {
                database.withTransaction {
                    database.journalEntryDao().deleteAllJournalEntries()
                    database.tagDao().deleteAllTags()
                    database.entryImageDao().deleteAllEntryImages()
                    database.journalEntryDao().deleteAllCrossRefs()
                    
                    dbEntriesSnapshot.forEach { database.journalEntryDao().insertEntry(it) }
                    dbTagsSnapshot.forEach { database.tagDao().insertTag(it) }
                    if (dbImagesSnapshot.isNotEmpty()) database.entryImageDao().insertEntryImages(dbImagesSnapshot)
                    if (dbCrossRefsSnapshot.isNotEmpty()) database.journalEntryDao().insertEntryTagCrossRefs(dbCrossRefsSnapshot)
                }
            }
            Log.d("Backup", "Database replaced successfully")

            if (simulateFailureAfterDb) throw IOException("Simulated failure after database restore")

            // PHASE 3: Swap Staging Directories to Active atomically
            Log.i("Backup", "Phase 3: Media Swap")
            val finalRecordingsDir = MediaStorageManager.getRecordingsDir(context)
            val finalImagesDir = MediaStorageManager.getImagesDir(context)
            val legacyAudioDir = File(context.filesDir, "audio")
            val transcribeCacheDir = MediaStorageManager.getTranscribeCacheDir(context)

            val timestamp = System.currentTimeMillis()
            val backupRecordingsDir = File(context.filesDir, "recordings_backup_$timestamp")
            val backupImagesDir = File(context.filesDir, "images_backup_$timestamp")
            val backupLegacyAudioDir = File(context.filesDir, "audio_backup_$timestamp")
            val backupTranscribeCacheDir = File(context.cacheDir, "audio_transcribe_cache_backup_$timestamp")

            if (finalRecordingsDir.exists()) {
                if (!moveDirSafe(finalRecordingsDir, backupRecordingsDir)) throw IOException("Failed to backup recordings")
                rollbackManager.register { moveDirSafe(backupRecordingsDir, finalRecordingsDir) }
            }
            if (finalImagesDir.exists()) {
                if (!moveDirSafe(finalImagesDir, backupImagesDir)) throw IOException("Failed to backup images")
                rollbackManager.register { moveDirSafe(backupImagesDir, finalImagesDir) }
            }
            if (legacyAudioDir.exists()) {
                if (!moveDirSafe(legacyAudioDir, backupLegacyAudioDir)) throw IOException("Failed to backup legacy audio")
                rollbackManager.register { moveDirSafe(backupLegacyAudioDir, legacyAudioDir) }
            }
            if (transcribeCacheDir.exists()) {
                if (!moveDirSafe(transcribeCacheDir, backupTranscribeCacheDir)) throw IOException("Failed to backup transcribe cache")
                rollbackManager.register { moveDirSafe(backupTranscribeCacheDir, transcribeCacheDir) }
            }

            if (stagingRecordingsDir.exists()) {
                if (!moveDirSafe(stagingRecordingsDir, finalRecordingsDir)) throw IOException("Failed to promote staging recordings")
                rollbackManager.register { moveDirSafe(finalRecordingsDir, stagingRecordingsDir) }
            }
            if (stagingImagesDir.exists()) {
                if (!moveDirSafe(stagingImagesDir, finalImagesDir)) throw IOException("Failed to promote staging images")
                rollbackManager.register { moveDirSafe(finalImagesDir, stagingImagesDir) }
            }

            if (simulateFailureAfterMedia) throw IOException("Simulated failure after media swap")

            // PHASE 4: Restore DataStore Preferences
            Log.i("Backup", "Phase 4: Preferences Replace")
            if (preferences != null) {
                onProgress?.invoke("Restoring preferences…")
                restorePreferences(preferences)
                rollbackManager.register { prefsManager.restoreRawPreferences(prefsSnapshot) }
                Log.d("Backup", "Preferences replaced successfully")
            }

            if (simulateFailureAfterPrefs) throw IOException("Simulated failure after preferences restore")

            // PHASE 5: Cleanup Backups safely
            Log.i("Backup", "Phase 5: Cleanup")
            backupRecordingsDir.deleteRecursively()
            backupImagesDir.deleteRecursively()
            backupLegacyAudioDir.deleteRecursively()
            backupTranscribeCacheDir.deleteRecursively()
            stagingRecordingsDir.deleteRecursively()
            stagingImagesDir.deleteRecursively()

            return RestoreStats(
                entriesRestored = entriesCount,
                tagsRestored = tagsCount,
                attachmentsRestored = attachmentsCount,
                mediaFilesRestored = copiedMediaFiles.size
            )
        } catch (e: Exception) {
            Log.e("Backup", "Restore workflow failed, executing rollback saga…", e)
            rollbackManager.executeRollback()
            
            // Cleanup staging directories after successful rollback
            if (stagingRecordingsDir.exists()) stagingRecordingsDir.deleteRecursively()
            if (stagingImagesDir.exists()) stagingImagesDir.deleteRecursively()
            
            throw IOException("Restore failed, successfully rolled back to pre-restore state", e)
        }
    }

    internal suspend fun restorePreferences(p: BackupPreferences) {
        // Recording format
        val audioFormat = try {
            AudioFormat.valueOf(p.recording.audioFormat.uppercase(Locale.ROOT))
        } catch (e: Exception) { AudioFormat.WAV_16KHZ }
        prefsManager.setAudioFormat(audioFormat)

        // Whisper model & speech to text
        if (p.transcription.whisperModel.isNotBlank()) {
            prefsManager.setWhisperModel(p.transcription.whisperModel)
        }

        // Appearance
        val themeMode = try {
            AppThemeMode.valueOf(p.appearance.themeMode.uppercase(Locale.ROOT))
        } catch (e: Exception) { AppThemeMode.DARK }
        prefsManager.setAppThemeMode(themeMode)

        // Display
        val timeFormat = try {
            TimeFormat.valueOf(p.display.timeFormat.uppercase(Locale.ROOT))
        } catch (e: Exception) { TimeFormat.SYSTEM_DEFAULT }
        prefsManager.setTimeFormat(timeFormat)

        val startOfWeek = try {
            StartOfWeek.valueOf(p.display.startOfWeek.uppercase(Locale.ROOT))
        } catch (e: Exception) { StartOfWeek.SYSTEM_DEFAULT }
        prefsManager.setStartOfWeek(startOfWeek)

        val insightMode = try {
            InsightDateRangeMode.valueOf(p.display.insightDateRangeMode.uppercase(Locale.ROOT))
        } catch (e: Exception) { InsightDateRangeMode.LAST_DAYS }
        prefsManager.setInsightDateRangeMode(insightMode)

        val sortOpt = try {
            SortOption.valueOf(p.display.sortOption.uppercase(Locale.ROOT))
        } catch (e: Exception) { SortOption.MODIFIED_DESC }
        prefsManager.setSortOption(sortOpt)

        prefsManager.setFolderIsGridView(p.display.folderGridView)
        prefsManager.setTagIsGridView(p.display.tagGridView)

        // Editor
        prefsManager.setMarkdownEnabled(p.editor.markdownEnabled)

        // Security: App Lock state (AppLockMode, custom PIN, timeout) is strictly destination-device-owned
        // and intentionally NEVER mutated by backup restore.
        Log.i("Backup", "Preserving destination device App Lock state; security configuration from backup is intentionally ignored.")

        // Data Management
        prefsManager.setDailyReminder(p.dataManagement.dailyReminder)

        // Tag Organiser & Visibility
        prefsManager.setFolderEnabled(p.tagOrganiser.isFolderEnabled)
        prefsManager.setNotesOrganisationEnabled(p.tagOrganiser.isNotesOrganisationEnabled)
        prefsManager.setTopicsEnabled(p.tagOrganiser.isTopicsEnabled)
        prefsManager.setPeopleEnabled(p.tagOrganiser.isPeopleEnabled)
        prefsManager.setMoodEnabled(p.tagOrganiser.isMoodEnabled)
    }

    private fun moveDirSafe(src: File, dest: File): Boolean {
        if (!src.exists()) return true
        return try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE)
            true
        } catch (e: Exception) {
            try {
                Files.move(src.toPath(), dest.toPath())
                true
            } catch (e2: Exception) {
                src.renameTo(dest)
            }
        }
    }
}
