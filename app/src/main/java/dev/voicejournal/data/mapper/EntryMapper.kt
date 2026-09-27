package dev.voicejournal.data.mapper

import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity
import dev.voicejournal.data.local.db.relation.EntryWithTagsAndImages
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.EntryImage
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import org.json.JSONArray
import org.json.JSONObject

fun List<AudioTrack>.toTracksJson(): String {
    val array = JSONArray()
    for (track in this) {
        val obj = JSONObject().apply {
            put("id", track.id)
            put("path", track.path)
            put("durationMs", track.durationMs)
            put("transcript", track.transcript)
            put("transcriptCreatedAt", track.transcriptCreatedAt)
            put("transcriptModel", track.transcriptModel)
            put("transcriptLanguage", track.transcriptLanguage)
            put("transcriptVersion", track.transcriptVersion)
        }
        array.put(obj)
    }
    return array.toString()
}

fun String.toAudioTracks(): List<AudioTrack> {
    if (isBlank()) return emptyList()

    return try {
        val array = JSONArray(this)
        val list = mutableListOf<AudioTrack>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AudioTrack(
                    id = obj.optString("id", ""),
                    path = obj.optString("path", ""),
                    durationMs = obj.optLong("durationMs", 0L),
                    transcript = obj.optString("transcript", "").takeIf { it.isNotEmpty() },
                    transcriptCreatedAt = obj.optLong("transcriptCreatedAt", 0L).takeIf { it > 0L },
                    transcriptModel = obj.optString("transcriptModel", "").takeIf { it.isNotEmpty() },
                    transcriptLanguage = obj.optString("transcriptLanguage", "").takeIf { it.isNotEmpty() },
                    transcriptVersion = obj.optString("transcriptVersion", "").takeIf { it.isNotEmpty() }
                )
            )
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun TagEntity.toDomain(): Tag {
    return Tag(
        id = id,
        uuid = uuid,
        name = name,
        type = try { TagType.valueOf(type) } catch (e: Exception) { TagType.TOPIC }
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        uuid = uuid,
        name = name,
        type = type.name
    )
}

fun EntryImageEntity.toDomain(): EntryImage {
    return EntryImage(
        id = id,
        uuid = uuid,
        entryId = entryId,
        imagePath = imagePath,
        displayOrder = displayOrder
    )
}

fun EntryImage.toEntity(entryId: Long): EntryImageEntity {
    return EntryImageEntity(
        id = id,
        uuid = uuid,
        entryId = entryId,
        imagePath = imagePath,
        displayOrder = displayOrder
    )
}

fun JournalEntryEntity.toDomain(tags: List<Tag> = emptyList(), images: List<EntryImage> = emptyList()): JournalEntry {
    val tracksFromDb = audioTracksJson.toAudioTracks()
    val finalAudioTracks = if (tracksFromDb.isNotEmpty()) {
        tracksFromDb.take(3)
    } else if (audioPath.isNotEmpty()) {
        listOf(
            AudioTrack(
                id = "legacy_$id",
                path = audioPath,
                durationMs = duration,
                transcript = transcript,
                transcriptCreatedAt = transcriptCreatedAt,
                transcriptModel = transcriptModel,
                transcriptLanguage = transcriptLanguage,
                transcriptVersion = transcriptVersion
            )
        )
    } else {
        emptyList()
    }

    val primaryTrack = finalAudioTracks.firstOrNull()

    return JournalEntry(
        id = id,
        uuid = uuid,
        createdAt = createdAt,
        updatedAt = updatedAt,
        title = title,
        audioPath = primaryTrack?.path ?: audioPath,
        audioFormat = try { AudioFormat.valueOf(audioFormat) } catch (e: Exception) { AudioFormat.WAV_16KHZ },
        duration = primaryTrack?.durationMs ?: duration,
        transcript = primaryTrack?.transcript ?: transcript,
        transcriptCreatedAt = primaryTrack?.transcriptCreatedAt ?: transcriptCreatedAt,
        transcriptModel = primaryTrack?.transcriptModel ?: transcriptModel,
        transcriptLanguage = primaryTrack?.transcriptLanguage ?: transcriptLanguage,
        transcriptVersion = primaryTrack?.transcriptVersion ?: transcriptVersion,
        userText = userText,
        moodEmoji = moodEmoji,
        moodLevel = moodLevel,
        hasTranscript = hasTranscript,
        tags = tags,
        images = images,
        audioTracks = finalAudioTracks,
        deletedAt = deletedAt,
        isArchived = isArchived,
        isDraft = isDraft
    )
}

fun JournalEntry.toEntity(): JournalEntryEntity {
    val primaryTrack = audioTracks.firstOrNull()
    val primaryAudioPath = primaryTrack?.path ?: audioPath
    val primaryDuration = primaryTrack?.durationMs ?: duration
    val primaryTranscript = primaryTrack?.transcript ?: transcript
    val primaryCreatedAt = primaryTrack?.transcriptCreatedAt ?: transcriptCreatedAt
    val primaryModel = primaryTrack?.transcriptModel ?: transcriptModel
    val primaryLang = primaryTrack?.transcriptLanguage ?: transcriptLanguage
    val primaryVer = primaryTrack?.transcriptVersion ?: transcriptVersion

    return JournalEntryEntity(
        id = id,
        uuid = uuid,
        createdAt = createdAt,
        updatedAt = updatedAt,
        title = title,
        audioPath = primaryAudioPath,
        audioFormat = audioFormat.name,
        duration = primaryDuration,
        transcript = primaryTranscript,
        transcriptCreatedAt = primaryCreatedAt,
        transcriptModel = primaryModel,
        transcriptLanguage = primaryLang,
        transcriptVersion = primaryVer,
        userText = userText,
        moodEmoji = moodEmoji,
        moodLevel = moodLevel,
        hasTranscript = !primaryTranscript.isNullOrEmpty(),
        audioTracksJson = audioTracks.toTracksJson(),
        deletedAt = deletedAt,
        isArchived = isArchived,
        isDraft = isDraft
    )
}

fun EntryWithTagsAndImages.toDomain(): JournalEntry {
    return entry.toDomain(
        tags = tags.map { it.toDomain() },
        images = images.map { it.toDomain() }
    )
}
