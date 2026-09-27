package dev.voicejournal.data.backup.v1.dto

import org.json.JSONArray
import org.json.JSONObject

// ─── Manifest DTO ──────────────────────────────────────────────────────────

data class GeneratorInfo(
    val appId: String = "dev.voicejournal",
    val appVersionName: String,
    val appVersionCode: Int,
    val platform: String = "android"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("app_id", appId)
        put("app_version_name", appVersionName)
        put("app_version_code", appVersionCode)
        put("platform", platform)
    }
}

data class DataCounts(
    val entries: Int,
    val tags: Int,
    val attachments: Int,
    val mediaFiles: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("entries", entries)
        put("tags", tags)
        put("attachments", attachments)
        put("media_files", mediaFiles)
    }
}

data class ChecksumMap(
    val algorithm: String = "SHA-256",
    val entriesJson: String,
    val tagsJson: String,
    val attachmentsJson: String,
    val preferencesJson: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("algorithm", algorithm)
        put("entries_json", entriesJson)
        put("tags_json", tagsJson)
        put("attachments_json", attachmentsJson)
        put("preferences_json", preferencesJson)
    }
}

data class BackupManifest(
    val formatName: String = "dev.voicejournal.backup",
    val formatVersion: Int = 1,
    val minReaderVersion: Int = 1,
    val backupUuid: String,
    val createdAt: Long,
    val timezoneId: String,
    val generator: GeneratorInfo,
    val counts: DataCounts,
    val features: List<String> = emptyList(),
    val checksums: ChecksumMap,
    val extensions: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("format_name", formatName)
        put("format_version", formatVersion)
        put("min_reader_version", minReaderVersion)
        put("backup_uuid", backupUuid)
        put("created_at", createdAt)
        put("timezone_id", timezoneId)
        put("generator", generator.toJson())
        put("counts", counts.toJson())
        put("features", JSONArray(features))
        put("checksums", checksums.toJson())
        put("extensions", JSONObject(extensions))
    }
}

// ─── Entry DTO ─────────────────────────────────────────────────────────────

data class BackupMood(
    val emoji: String?,
    val level: Int?,
    val category: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("emoji", emoji ?: JSONObject.NULL)
        put("level", level ?: JSONObject.NULL)
        put("category", category ?: JSONObject.NULL)
    }
}

data class BackupEntry(
    val uuid: String,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String?,
    val userText: String?,
    val userTextFormat: String, // "plain" or "html"
    val mood: BackupMood?,
    val status: String, // "active", "archived", "draft", "trashed"
    val deletedAt: Long?,
    val tagUuids: List<String>,
    val attachmentUuids: List<String>,
    val extensions: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uuid", uuid)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
        put("title", title ?: JSONObject.NULL)
        put("user_text", userText ?: JSONObject.NULL)
        put("user_text_format", userTextFormat)
        put("mood", mood?.toJson() ?: JSONObject.NULL)
        put("status", status)
        put("deleted_at", deletedAt ?: JSONObject.NULL)
        put("tag_uuids", JSONArray(tagUuids))
        put("attachment_uuids", JSONArray(attachmentUuids))
        put("extensions", JSONObject(extensions))
    }
}

// ─── Tag DTO ───────────────────────────────────────────────────────────────

data class BackupTag(
    val uuid: String,
    val name: String,
    val type: String, // "topic", "person", "mood", "thing", "folder"
    val extensions: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uuid", uuid)
        put("name", name)
        put("type", type)
        put("extensions", JSONObject(extensions))
    }
}

// ─── Attachment DTO ────────────────────────────────────────────────────────

data class TranscriptDto(
    val text: String,
    val language: String?,
    val model: String?,
    val modelVersion: String?,
    val createdAt: Long?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text)
        put("language", language ?: JSONObject.NULL)
        put("model", model ?: JSONObject.NULL)
        put("model_version", modelVersion ?: JSONObject.NULL)
        put("created_at", createdAt ?: JSONObject.NULL)
    }
}

data class AudioMetadata(
    val durationMs: Long,
    val sampleRateHz: Int?,
    val transcript: TranscriptDto?,
    val waveformAmplitudes: List<Int>? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("duration_ms", durationMs)
        put("sample_rate_hz", sampleRateHz ?: JSONObject.NULL)
        put("transcript", transcript?.toJson() ?: JSONObject.NULL)
        put("waveform_amplitudes", waveformAmplitudes?.let { JSONArray(it) } ?: JSONObject.NULL)
    }
}

data class ImageMetadata(
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val originalFilename: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("width_px", widthPx ?: JSONObject.NULL)
        put("height_px", heightPx ?: JSONObject.NULL)
        put("original_filename", originalFilename ?: JSONObject.NULL)
    }
}

data class BackupAttachment(
    val uuid: String,
    val entryUuid: String,
    val type: String, // "audio", "image", etc.
    val mimeType: String, // "audio/wav", "audio/mp4", "image/jpeg", etc.
    val archivePath: String, // "media/<uuid>.<ext>"
    val sha256: String,
    val fileSizeBytes: Long,
    val displayOrder: Int,
    val createdAt: Long,
    val originalUri: String? = null,
    val metadataVersion: Int = 1,
    val metadata: JSONObject,
    val extensions: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uuid", uuid)
        put("entry_uuid", entryUuid)
        put("type", type)
        put("mime_type", mimeType)
        put("archive_path", archivePath)
        put("sha256", sha256)
        put("file_size_bytes", fileSizeBytes)
        put("display_order", displayOrder)
        put("created_at", createdAt)
        put("original_uri", originalUri ?: JSONObject.NULL)
        put("metadata_version", metadataVersion)
        put("metadata", metadata)
        put("extensions", JSONObject(extensions))
    }
}

// ─── Preferences DTO ───────────────────────────────────────────────────────

data class RecordingPreferences(
    val audioFormat: String = "wav_16khz",
    val autoTranscribe: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("audio_format", audioFormat)
        put("auto_transcribe", autoTranscribe)
    }
}

data class TranscriptionPreferences(
    val whisperModel: String = "ggml-base-q5_1",
    val speechToTextEnabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("whisper_model", whisperModel)
        put("speech_to_text_enabled", speechToTextEnabled)
    }
}

data class AppearancePreferences(
    val themeMode: String = "system",
    val colorTheme: String = "Default"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("theme_mode", themeMode)
        put("color_theme", colorTheme)
    }
}

data class DisplayPreferences(
    val timeFormat: String = "system_default",
    val startOfWeek: String = "system_default",
    val insightDateRangeMode: String = "last_days",
    val sortOption: String = "modified_desc",
    val folderGridView: Boolean = true,
    val tagGridView: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("time_format", timeFormat)
        put("start_of_week", startOfWeek)
        put("insight_date_range_mode", insightDateRangeMode)
        put("sort_option", sortOption)
        put("folder_grid_view", folderGridView)
        put("tag_grid_view", tagGridView)
    }
}

data class EditorPreferences(
    val markdownEnabled: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("markdown_enabled", markdownEnabled)
    }
}

data class SecurityPreferences(
    val appLockMode: String = "none",
    val appLockTimeout: String = "immediately",
    val screenPrivacyEnabled: Boolean = false
    // NOTE: Credentials (PIN / keys) are strictly EXCLUDED
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("app_lock_mode", appLockMode)
        put("app_lock_timeout", appLockTimeout)
        put("screen_privacy_enabled", screenPrivacyEnabled)
    }
}

data class DataManagementPreferences(
    val trashRetentionDays: Int = 7
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("trash_retention_days", trashRetentionDays)
    }
}

data class TagOrganiserPreferences(
    val isFolderEnabled: Boolean = false,
    val isNotesOrganisationEnabled: Boolean = false,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("is_folder_enabled", isFolderEnabled)
        put("is_notes_organisation_enabled", isNotesOrganisationEnabled)
        put("is_topics_enabled", isTopicsEnabled)
        put("is_people_enabled", isPeopleEnabled)
        put("is_mood_enabled", isMoodEnabled)
    }
}

data class BackupPreferences(
    val schemaVersion: Int = 1,
    val recording: RecordingPreferences = RecordingPreferences(),
    val transcription: TranscriptionPreferences = TranscriptionPreferences(),
    val appearance: AppearancePreferences = AppearancePreferences(),
    val display: DisplayPreferences = DisplayPreferences(),
    val editor: EditorPreferences = EditorPreferences(),
    val security: SecurityPreferences = SecurityPreferences(),
    val dataManagement: DataManagementPreferences = DataManagementPreferences(),
    val tagOrganiser: TagOrganiserPreferences = TagOrganiserPreferences(),
    val extensions: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("schema_version", schemaVersion)
        put("recording", recording.toJson())
        put("transcription", transcription.toJson())
        put("appearance", appearance.toJson())
        put("display", display.toJson())
        put("editor", editor.toJson())
        put("security", security.toJson())
        put("data_management", dataManagement.toJson())
        put("tag_organiser", tagOrganiser.toJson())
        put("extensions", JSONObject(extensions))
    }
}
