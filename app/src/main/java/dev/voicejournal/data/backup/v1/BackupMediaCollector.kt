package dev.voicejournal.data.backup.v1

import android.content.Context
import dev.voicejournal.data.backup.v1.dto.AudioMetadata
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.ImageMetadata
import dev.voicejournal.data.backup.v1.dto.TranscriptDto
import dev.voicejournal.data.local.db.relation.EntryWithTagsAndImages
import dev.voicejournal.data.mapper.toAudioTracks
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

data class CollectedMedia(
    val attachment: BackupAttachment,
    val sourceFile: File
)

class BackupMediaCollector(
    private val context: Context
) {

    fun collectMediaForEntries(entries: List<EntryWithTagsAndImages>): List<CollectedMedia> {
        val collected = mutableListOf<CollectedMedia>()

        entries.forEach { entryRel ->
            val entryEntity = entryRel.entry
            val entryUuid = entryEntity.uuid
            var displayOrder = 0

            // 1. Audio tracks from multi-track JSON
            val tracks = entryEntity.audioTracksJson.toAudioTracks()
            if (tracks.isNotEmpty()) {
                tracks.forEach { track ->
                    val file = resolveFile(track.path)
                    if (file != null && file.exists() && file.isFile) {
                        val extension = file.extension.lowercase(Locale.ROOT).ifBlank { "wav" }
                        val mimeType = getMimeType(extension)
                        val trackUuid = if (isValidUuid(track.id)) track.id else UUID.randomUUID().toString()
                        val sha256 = file.computeSha256()

                        val transcriptDto = track.transcript?.let { text ->
                            TranscriptDto(
                                text = text,
                                language = track.transcriptLanguage,
                                model = track.transcriptModel,
                                modelVersion = track.transcriptVersion,
                                createdAt = track.transcriptCreatedAt
                            )
                        }

                        val audioMetadata = AudioMetadata(
                            durationMs = track.durationMs,
                            sampleRateHz = if (extension == "wav") 16000 else 44100,
                            transcript = transcriptDto,
                            waveformAmplitudes = null
                        )

                        val attachment = BackupAttachment(
                            uuid = trackUuid,
                            entryUuid = entryUuid,
                            type = "audio",
                            mimeType = mimeType,
                            archivePath = "media/$trackUuid.$extension",
                            sha256 = sha256,
                            fileSizeBytes = file.length(),
                            displayOrder = displayOrder++,
                            createdAt = entryEntity.createdAt,
                            originalUri = file.absolutePath,
                            metadataVersion = 1,
                            metadata = audioMetadata.toJson()
                        )

                        collected.add(CollectedMedia(attachment, file))
                    }
                }
            } else if (entryEntity.audioPath.isNotBlank()) {
                // Fallback: Legacy single audio path
                val file = resolveFile(entryEntity.audioPath)
                if (file != null && file.exists() && file.isFile) {
                    val extension = file.extension.lowercase(Locale.ROOT).ifBlank { "wav" }
                    val mimeType = getMimeType(extension)
                    val trackUuid = UUID.randomUUID().toString()
                    val sha256 = file.computeSha256()

                    val transcriptDto = entryEntity.transcript?.let { text ->
                        TranscriptDto(
                            text = text,
                            language = entryEntity.transcriptLanguage,
                            model = entryEntity.transcriptModel,
                            modelVersion = entryEntity.transcriptVersion,
                            createdAt = entryEntity.transcriptCreatedAt
                        )
                    }

                    val audioMetadata = AudioMetadata(
                        durationMs = entryEntity.duration,
                        sampleRateHz = if (extension == "wav") 16000 else 44100,
                        transcript = transcriptDto,
                        waveformAmplitudes = null
                    )

                    val attachment = BackupAttachment(
                        uuid = trackUuid,
                        entryUuid = entryUuid,
                        type = "audio",
                        mimeType = mimeType,
                        archivePath = "media/$trackUuid.$extension",
                        sha256 = sha256,
                        fileSizeBytes = file.length(),
                        displayOrder = displayOrder++,
                        createdAt = entryEntity.createdAt,
                        originalUri = file.absolutePath,
                        metadataVersion = 1,
                        metadata = audioMetadata.toJson()
                    )

                    collected.add(CollectedMedia(attachment, file))
                }
            }

            // 2. Images from entry_images table
            entryRel.images.forEach { imgEntity ->
                val file = resolveFile(imgEntity.imagePath)
                if (file != null && file.exists() && file.isFile) {
                    val extension = file.extension.lowercase(Locale.ROOT).ifBlank { "jpg" }
                    val mimeType = getMimeType(extension)
                    val imgUuid = imgEntity.uuid
                    val sha256 = file.computeSha256()

                    val imageMetadata = ImageMetadata(
                        originalFilename = file.name
                    )

                    val attachment = BackupAttachment(
                        uuid = imgUuid,
                        entryUuid = entryUuid,
                        type = "image",
                        mimeType = mimeType,
                        archivePath = "media/$imgUuid.$extension",
                        sha256 = sha256,
                        fileSizeBytes = file.length(),
                        displayOrder = displayOrder++,
                        createdAt = entryEntity.createdAt,
                        originalUri = file.absolutePath,
                        metadataVersion = 1,
                        metadata = imageMetadata.toJson()
                    )

                    collected.add(CollectedMedia(attachment, file))
                }
            }
        }

        return collected
    }

    private fun resolveFile(path: String): File? {
        if (path.isBlank()) return null
        val f = File(path)
        if (f.isAbsolute && f.exists()) return f
        // Try relative to filesDir
        val inFilesDir = File(context.filesDir, path)
        if (inFilesDir.exists()) return inFilesDir
        return null
    }

    private fun getMimeType(extension: String): String = when (extension) {
        "wav" -> "audio/wav"
        "m4a", "aac" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "heic" -> "image/heic"
        "avif" -> "image/avif"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }

    private fun isValidUuid(str: String): Boolean {
        return try {
            UUID.fromString(str)
            true
        } catch (e: Exception) {
            false
        }
    }
}

fun File.computeSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { isStream ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (isStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
