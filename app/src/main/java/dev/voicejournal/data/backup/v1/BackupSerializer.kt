package dev.voicejournal.data.backup.v1

import dev.voicejournal.data.backup.v1.dto.AppearancePreferences
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupMood
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.BackupTag
import dev.voicejournal.data.backup.v1.dto.DataManagementPreferences
import dev.voicejournal.data.backup.v1.dto.DisplayPreferences
import dev.voicejournal.data.backup.v1.dto.EditorPreferences
import dev.voicejournal.data.backup.v1.dto.RecordingPreferences
import dev.voicejournal.data.backup.v1.dto.SecurityPreferences
import dev.voicejournal.data.backup.v1.dto.TagOrganiserPreferences
import dev.voicejournal.data.backup.v1.dto.TranscriptionPreferences
import dev.voicejournal.data.local.db.relation.EntryWithTagsAndImages
import dev.voicejournal.data.local.db.entity.TagEntity
import org.json.JSONArray
import java.util.Locale

class BackupSerializer {

    fun serializeEntries(
        entries: List<EntryWithTagsAndImages>,
        attachmentsByEntryUuid: Map<String, List<BackupAttachment>>
    ): String {
        val jsonArray = JSONArray()
        entries.forEach { rel ->
            val e = rel.entry
            val entryUuid = e.uuid

            val status = when {
                e.deletedAt != null -> "trashed"
                e.isArchived -> "archived"
                e.isDraft -> "draft"
                else -> "active"
            }

            val userTextFormat = if (e.userText?.contains("<") == true) "html" else "plain"

            val mood = if (e.moodEmoji != null || e.moodLevel != null) {
                BackupMood(
                    emoji = e.moodEmoji,
                    level = e.moodLevel,
                    category = null
                )
            } else null

            val tagUuids = rel.tags.map { it.uuid }
            val attachmentUuids = attachmentsByEntryUuid[entryUuid]?.map { it.uuid } ?: emptyList()

            val backupEntry = BackupEntry(
                uuid = entryUuid,
                createdAt = e.createdAt,
                updatedAt = e.updatedAt,
                title = e.title,
                userText = e.userText,
                userTextFormat = userTextFormat,
                mood = mood,
                status = status,
                deletedAt = e.deletedAt,
                tagUuids = tagUuids,
                attachmentUuids = attachmentUuids
            )

            jsonArray.put(backupEntry.toJson())
        }
        return jsonArray.toString(2)
    }

    fun serializeTags(tags: List<TagEntity>): String {
        val jsonArray = JSONArray()
        tags.forEach { t ->
            val backupTag = BackupTag(
                uuid = t.uuid,
                name = t.name,
                type = t.type.lowercase(Locale.ROOT)
            )
            jsonArray.put(backupTag.toJson())
        }
        return jsonArray.toString(2)
    }

    fun serializeAttachments(attachments: List<BackupAttachment>): String {
        val jsonArray = JSONArray()
        attachments.forEach { a ->
            jsonArray.put(a.toJson())
        }
        return jsonArray.toString(2)
    }

    fun serializePreferences(
        audioFormat: String,
        whisperModel: String,
        dailyReminder: Boolean,
        themeMode: String,
        colorTheme: String,
        timeFormat: String,
        startOfWeek: String,
        appLockMode: String,
        appLockTimeout: String,
        insightDateRangeMode: String,
        autoTranscribe: Boolean,
        trashRetentionDays: Int,
        sortOption: String = "modified_desc",
        folderGridView: Boolean = true,
        tagGridView: Boolean = true,
        markdownEnabled: Boolean = false,
        screenPrivacyEnabled: Boolean = false,
        speechToTextEnabled: Boolean = true,
        isFolderEnabled: Boolean = false,
        isNotesOrganisationEnabled: Boolean = false,
        isTopicsEnabled: Boolean = true,
        isPeopleEnabled: Boolean = true,
        isMoodEnabled: Boolean = true
    ): String {
        val prefs = BackupPreferences(
            schemaVersion = 1,
            recording = RecordingPreferences(
                audioFormat = audioFormat.lowercase(Locale.ROOT),
                autoTranscribe = autoTranscribe
            ),
            transcription = TranscriptionPreferences(
                whisperModel = whisperModel,
                speechToTextEnabled = speechToTextEnabled
            ),
            appearance = AppearancePreferences(
                themeMode = themeMode.lowercase(Locale.ROOT),
                colorTheme = colorTheme
            ),
            display = DisplayPreferences(
                timeFormat = timeFormat.lowercase(Locale.ROOT),
                startOfWeek = startOfWeek.lowercase(Locale.ROOT),
                insightDateRangeMode = insightDateRangeMode.lowercase(Locale.ROOT),
                sortOption = sortOption.lowercase(Locale.ROOT),
                folderGridView = folderGridView,
                tagGridView = tagGridView
            ),
            editor = EditorPreferences(
                markdownEnabled = markdownEnabled
            ),
            security = SecurityPreferences(
                appLockMode = appLockMode.lowercase(Locale.ROOT),
                appLockTimeout = appLockTimeout.lowercase(Locale.ROOT),
                screenPrivacyEnabled = screenPrivacyEnabled
            ),
            dataManagement = DataManagementPreferences(
                trashRetentionDays = trashRetentionDays,
                dailyReminder = dailyReminder
            ),
            tagOrganiser = TagOrganiserPreferences(
                isFolderEnabled = isFolderEnabled,
                isNotesOrganisationEnabled = isNotesOrganisationEnabled,
                isTopicsEnabled = isTopicsEnabled,
                isPeopleEnabled = isPeopleEnabled,
                isMoodEnabled = isMoodEnabled
            )
        )

        return prefs.toJson().toString(2)
    }
}
