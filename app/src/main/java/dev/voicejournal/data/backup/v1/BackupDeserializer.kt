package dev.voicejournal.data.backup.v1

import dev.voicejournal.data.backup.v1.dto.AppearancePreferences
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupManifest
import dev.voicejournal.data.backup.v1.dto.BackupMood
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.BackupTag
import dev.voicejournal.data.backup.v1.dto.ChecksumMap
import dev.voicejournal.data.backup.v1.dto.DataCounts
import dev.voicejournal.data.backup.v1.dto.DataManagementPreferences
import dev.voicejournal.data.backup.v1.dto.DisplayPreferences
import dev.voicejournal.data.backup.v1.dto.EditorPreferences
import dev.voicejournal.data.backup.v1.dto.GeneratorInfo
import dev.voicejournal.data.backup.v1.dto.RecordingPreferences
import dev.voicejournal.data.backup.v1.dto.SecurityPreferences
import dev.voicejournal.data.backup.v1.dto.TagOrganiserPreferences
import dev.voicejournal.data.backup.v1.dto.TranscriptionPreferences
import org.json.JSONArray
import org.json.JSONObject

class BackupDeserializer {

    fun parseManifest(jsonStr: String): BackupManifest {
        val root = JSONObject(jsonStr)
        val genObj = root.getJSONObject("generator")
        val countsObj = root.getJSONObject("counts")
        val checkObj = root.getJSONObject("checksums")

        val generator = GeneratorInfo(
            appId = genObj.optString("app_id", "dev.voicejournal"),
            appVersionName = genObj.optString("app_version_name", "0.1.0"),
            appVersionCode = genObj.optInt("app_version_code", 1),
            platform = genObj.optString("platform", "android")
        )

        val counts = DataCounts(
            entries = countsObj.getInt("entries"),
            tags = countsObj.getInt("tags"),
            attachments = countsObj.getInt("attachments"),
            mediaFiles = countsObj.getInt("media_files")
        )

        val checksums = ChecksumMap(
            algorithm = checkObj.optString("algorithm", "SHA-256"),
            entriesJson = checkObj.getString("entries_json"),
            tagsJson = checkObj.getString("tags_json"),
            attachmentsJson = checkObj.getString("attachments_json"),
            preferencesJson = checkObj.getString("preferences_json")
        )

        val featuresArray = root.optJSONArray("features")
        val featuresList = mutableListOf<String>()
        if (featuresArray != null) {
            for (i in 0 until featuresArray.length()) {
                featuresList.add(featuresArray.getString(i))
            }
        }

        return BackupManifest(
            formatName = root.getString("format_name"),
            formatVersion = root.getInt("format_version"),
            minReaderVersion = root.getInt("min_reader_version"),
            backupUuid = root.getString("backup_uuid"),
            createdAt = root.getLong("created_at"),
            timezoneId = root.optString("timezone_id", "UTC"),
            generator = generator,
            counts = counts,
            features = featuresList,
            checksums = checksums
        )
    }

    fun parseEntries(jsonStr: String): List<BackupEntry> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<BackupEntry>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            val moodObj = obj.optJSONObject("mood")
            val mood = if (moodObj != null && !moodObj.isNull("emoji")) {
                BackupMood(
                    emoji = moodObj.optString("emoji").takeIf { it.isNotEmpty() },
                    level = if (moodObj.has("level") && !moodObj.isNull("level")) moodObj.getInt("level") else null,
                    category = if (moodObj.has("category") && !moodObj.isNull("category")) moodObj.getString("category") else null
                )
            } else null

            val tagUuidsArray = obj.optJSONArray("tag_uuids")
            val tagUuids = mutableListOf<String>()
            if (tagUuidsArray != null) {
                for (j in 0 until tagUuidsArray.length()) {
                    tagUuids.add(tagUuidsArray.getString(j))
                }
            }

            val attUuidsArray = obj.optJSONArray("attachment_uuids")
            val attUuids = mutableListOf<String>()
            if (attUuidsArray != null) {
                for (j in 0 until attUuidsArray.length()) {
                    attUuids.add(attUuidsArray.getString(j))
                }
            }

            val entry = BackupEntry(
                uuid = obj.getString("uuid"),
                createdAt = obj.getLong("created_at"),
                updatedAt = obj.getLong("updated_at"),
                title = if (obj.has("title") && !obj.isNull("title")) obj.getString("title") else null,
                userText = if (obj.has("user_text") && !obj.isNull("user_text")) obj.getString("user_text") else null,
                userTextFormat = obj.optString("user_text_format", "plain"),
                mood = mood,
                status = obj.optString("status", "active"),
                deletedAt = if (obj.has("deleted_at") && !obj.isNull("deleted_at")) obj.getLong("deleted_at") else null,
                tagUuids = tagUuids,
                attachmentUuids = attUuids
            )
            list.add(entry)
        }

        return list
    }

    fun parseTags(jsonStr: String): List<BackupTag> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<BackupTag>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                BackupTag(
                    uuid = obj.getString("uuid"),
                    name = obj.getString("name"),
                    type = obj.optString("type", "topic")
                )
            )
        }

        return list
    }

    fun parseAttachments(jsonStr: String): List<BackupAttachment> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<BackupAttachment>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                BackupAttachment(
                    uuid = obj.getString("uuid"),
                    entryUuid = obj.getString("entry_uuid"),
                    type = obj.optString("type", "audio"),
                    mimeType = obj.optString("mime_type", "audio/wav"),
                    archivePath = obj.getString("archive_path"),
                    sha256 = obj.getString("sha256"),
                    fileSizeBytes = obj.getLong("file_size_bytes"),
                    displayOrder = obj.optInt("display_order", 0),
                    createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                    originalUri = if (obj.has("original_uri") && !obj.isNull("original_uri")) obj.getString("original_uri") else null,
                    metadataVersion = obj.optInt("metadata_version", 1),
                    metadata = obj.optJSONObject("metadata") ?: JSONObject()
                )
            )
        }

        return list
    }

    fun parsePreferences(jsonStr: String): BackupPreferences {
        val root = JSONObject(jsonStr)

        val recObj = root.optJSONObject("recording") ?: JSONObject()
        val transObj = root.optJSONObject("transcription") ?: JSONObject()
        val appObj = root.optJSONObject("appearance") ?: JSONObject()
        val dispObj = root.optJSONObject("display") ?: JSONObject()
        val edObj = root.optJSONObject("editor") ?: JSONObject()
        val secObj = root.optJSONObject("security") ?: JSONObject()
        val dmObj = root.optJSONObject("data_management") ?: JSONObject()
        val toObj = root.optJSONObject("tag_organiser") ?: JSONObject()

        return BackupPreferences(
            schemaVersion = root.optInt("schema_version", 1),
            recording = RecordingPreferences(
                audioFormat = recObj.optString("audio_format", "wav_16khz"),
                autoTranscribe = recObj.optBoolean("auto_transcribe", true)
            ),
            transcription = TranscriptionPreferences(
                whisperModel = transObj.optString("whisper_model", "ggml-base-q5_1"),
                speechToTextEnabled = transObj.optBoolean("speech_to_text_enabled", true)
            ),
            appearance = AppearancePreferences(
                themeMode = appObj.optString("theme_mode", "system"),
                colorTheme = appObj.optString("color_theme", "Default")
            ),
            display = DisplayPreferences(
                timeFormat = dispObj.optString("time_format", "system_default"),
                startOfWeek = dispObj.optString("start_of_week", "system_default"),
                insightDateRangeMode = dispObj.optString("insight_date_range_mode", "last_days"),
                sortOption = dispObj.optString("sort_option", "modified_desc"),
                folderGridView = dispObj.optBoolean("folder_grid_view", true),
                tagGridView = dispObj.optBoolean("tag_grid_view", true)
            ),
            editor = EditorPreferences(
                markdownEnabled = edObj.optBoolean("markdown_enabled", false)
            ),
            security = SecurityPreferences(
                appLockMode = secObj.optString("app_lock_mode", "none"),
                appLockTimeout = secObj.optString("app_lock_timeout", "immediately"),
                screenPrivacyEnabled = secObj.optBoolean("screen_privacy_enabled", false)
            ),
            dataManagement = DataManagementPreferences(
                trashRetentionDays = dmObj.optInt("trash_retention_days", 7)
            ),
            tagOrganiser = TagOrganiserPreferences(
                isFolderEnabled = toObj.optBoolean("is_folder_enabled", false),
                isNotesOrganisationEnabled = toObj.optBoolean("is_notes_organisation_enabled", false),
                isTopicsEnabled = toObj.optBoolean("is_topics_enabled", true),
                isPeopleEnabled = toObj.optBoolean("is_people_enabled", true),
                isMoodEnabled = toObj.optBoolean("is_mood_enabled", true)
            )
        )
    }
}
