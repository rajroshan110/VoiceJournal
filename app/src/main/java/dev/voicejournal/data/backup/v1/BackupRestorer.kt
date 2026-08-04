package dev.voicejournal.data.backup.v1

import android.content.Context
import androidx.room.withTransaction
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.BackupTag
import dev.voicejournal.data.backup.v1.dto.RestoreStats
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity
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

class BackupRestorer(
    private val context: Context,
    private val database: AppDatabase,
    private val prefsManager: UserPreferencesManager
) {

    suspend fun restore(
        stagingDir: File,
        entries: List<BackupEntry>,
        tags: List<BackupTag>,
        attachments: List<BackupAttachment>,
        preferences: BackupPreferences?
    ): RestoreStats {
        val copiedMediaFiles = mutableListOf<File>()

        try {
            // 1. Copy Media Files to Internal App Storage
            val attachmentsByEntryUuid = attachments.groupBy { it.entryUuid }
            val mediaPathMap = mutableMapOf<String, String>() // attachmentUuid -> relative target path

            attachments.forEach { att ->
                val sourceFile = File(stagingDir, att.archivePath)
                if (sourceFile.exists()) {
                    val subFolder = if (att.type == "image") "images" else "audio"
                    val ext = sourceFile.extension.ifBlank { if (att.type == "image") "jpg" else "wav" }
                    val relTargetName = "$subFolder/${att.uuid}.$ext"
                    val targetFile = File(context.filesDir, relTargetName)
                    targetFile.parentFile?.mkdirs()

                    sourceFile.copyTo(targetFile, overwrite = true)
                    copiedMediaFiles.add(targetFile)
                    mediaPathMap[att.uuid] = relTargetName
                }
            }

            // 2. Perform Single Room Database Transaction
            var entriesCount = 0
            var tagsCount = 0
            var attachmentsCount = 0

            database.withTransaction {
                // A. Restore Tags
                val tagIdMap = mutableMapOf<String, Long>() // tagUuid -> tagDbId
                tags.forEach { bTag ->
                    val tagTypeStr = bTag.type.uppercase(Locale.ROOT)
                    val existingTag = database.tagDao().getTagByUuid(bTag.uuid)
                    val tagId = if (existingTag != null) {
                        val updated = existingTag.copy(name = bTag.name, type = tagTypeStr)
                        database.tagDao().insertTag(updated)
                        existingTag.id
                    } else {
                        val newEntity = TagEntity(
                            name = bTag.name,
                            type = tagTypeStr,
                            uuid = bTag.uuid
                        )
                        database.tagDao().insertTag(newEntity)
                    }
                    tagIdMap[bTag.uuid] = tagId
                    tagsCount++
                }

                // B. Restore Entries & Attachments
                entries.forEach { bEntry ->
                    val entryAtts = attachmentsByEntryUuid[bEntry.uuid] ?: emptyList()
                    val audioAtts = entryAtts.filter { it.type == "audio" }
                    val imageAtts = entryAtts.filter { it.type == "image" }

                    // Construct Audio Tracks JSON or Legacy Audio Path
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

                    val existingEntry = database.journalEntryDao().getEntryByUuidSync(bEntry.uuid)
                    val entryEntity = JournalEntryEntity(
                        id = existingEntry?.entry?.id ?: 0L,
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

                    // Restore Entry Images
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

                    // Restore Entry Tags Junction
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

            // 3. Restore DataStore Preferences (Excluding Credentials)
            if (preferences != null) {
                restorePreferences(preferences)
            }

            return RestoreStats(
                entriesRestored = entriesCount,
                tagsRestored = tagsCount,
                attachmentsRestored = attachmentsCount,
                mediaFilesRestored = copiedMediaFiles.size
            )
        } catch (e: Exception) {
            // Atomic Rollback: Delete any newly copied media files
            copiedMediaFiles.forEach { file ->
                if (file.exists()) {
                    try { file.delete() } catch (_: Exception) {}
                }
            }
            throw e
        }
    }

    private suspend fun restorePreferences(p: BackupPreferences) {
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

        // Security (App lock mode/timeout & screen privacy; PIN strictly EXCLUDED!)
        val lockMode = try {
            AppLockMode.valueOf(p.security.appLockMode.uppercase(Locale.ROOT))
        } catch (e: Exception) { AppLockMode.NONE }
        prefsManager.setAppLockMode(lockMode)

        val lockTimeout = try {
            AppLockTimeout.valueOf(p.security.appLockTimeout.uppercase(Locale.ROOT))
        } catch (e: Exception) { AppLockTimeout.IMMEDIATELY }
        prefsManager.setAppLockTimeout(lockTimeout)

        prefsManager.setScreenPrivacyEnabled(p.security.screenPrivacyEnabled)

        // Data Management
        prefsManager.setDailyReminder(p.dataManagement.dailyReminder)
    }
}
