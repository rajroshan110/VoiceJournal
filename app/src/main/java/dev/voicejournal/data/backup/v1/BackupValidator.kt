package dev.voicejournal.data.backup.v1

import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupManifest
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.BackupTag
import java.io.File
import java.security.MessageDigest

data class ValidationReport(
    val isValid: Boolean,
    val errors: List<String>,
    val manifest: BackupManifest? = null,
    val entries: List<BackupEntry> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val attachments: List<BackupAttachment> = emptyList(),
    val preferences: BackupPreferences? = null
)

class BackupValidator {

    private val deserializer = BackupDeserializer()

    fun validate(stagingDir: File): ValidationReport {
        val errors = mutableListOf<String>()

        // 1. Required Manifest Files Check
        val manifestFile = File(stagingDir, "manifest.json")
        val entriesFile = File(stagingDir, "entries.json")
        val tagsFile = File(stagingDir, "tags.json")
        val attachmentsFile = File(stagingDir, "attachments.json")
        val preferencesFile = File(stagingDir, "preferences.json")

        if (!manifestFile.exists()) errors.add("Missing required file: manifest.json")
        if (!entriesFile.exists()) errors.add("Missing required file: entries.json")
        if (!tagsFile.exists()) errors.add("Missing required file: tags.json")
        if (!attachmentsFile.exists()) errors.add("Missing required file: attachments.json")
        if (!preferencesFile.exists()) errors.add("Missing required file: preferences.json")

        if (errors.isNotEmpty()) {
            return ValidationReport(isValid = false, errors = errors)
        }

        // 2. Parse Manifest & Check Version Compatibility
        val manifest = try {
            deserializer.parseManifest(manifestFile.readText())
        } catch (e: Exception) {
            errors.add("Failed to parse manifest.json: ${e.message}")
            return ValidationReport(isValid = false, errors = errors)
        }

        if (manifest.formatName != "dev.voicejournal.backup") {
            errors.add("Invalid format_name: '${manifest.formatName}'. Expected 'dev.voicejournal.backup'")
        }
        if (manifest.formatVersion != 1) {
            errors.add("Unsupported format_version: ${manifest.formatVersion}. Expected 1")
        }
        if (manifest.minReaderVersion > 1) {
            errors.add("Archive requires min_reader_version ${manifest.minReaderVersion}, current app supports up to version 1")
        }

        // 3. Manifest Checksums Validation
        val entriesSha = computeFileSha256(entriesFile)
        val tagsSha = computeFileSha256(tagsFile)
        val attachmentsSha = computeFileSha256(attachmentsFile)
        val preferencesSha = computeFileSha256(preferencesFile)

        if (!entriesSha.equals(manifest.checksums.entriesJson, ignoreCase = true)) {
            errors.add("entries.json SHA-256 mismatch! Manifest: ${manifest.checksums.entriesJson}, Actual: $entriesSha")
        }
        if (!tagsSha.equals(manifest.checksums.tagsJson, ignoreCase = true)) {
            errors.add("tags.json SHA-256 mismatch! Manifest: ${manifest.checksums.tagsJson}, Actual: $tagsSha")
        }
        if (!attachmentsSha.equals(manifest.checksums.attachmentsJson, ignoreCase = true)) {
            errors.add("attachments.json SHA-256 mismatch! Manifest: ${manifest.checksums.attachmentsJson}, Actual: $attachmentsSha")
        }
        if (!preferencesSha.equals(manifest.checksums.preferencesJson, ignoreCase = true)) {
            errors.add("preferences.json SHA-256 mismatch! Manifest: ${manifest.checksums.preferencesJson}, Actual: $preferencesSha")
        }

        // 4. Parse DTOs
        val entries = try {
            deserializer.parseEntries(entriesFile.readText())
        } catch (e: Exception) {
            errors.add("Failed to parse entries.json: ${e.message}")
            emptyList()
        }

        val tags = try {
            deserializer.parseTags(tagsFile.readText())
        } catch (e: Exception) {
            errors.add("Failed to parse tags.json: ${e.message}")
            emptyList()
        }

        val attachments = try {
            deserializer.parseAttachments(attachmentsFile.readText())
        } catch (e: Exception) {
            errors.add("Failed to parse attachments.json: ${e.message}")
            emptyList()
        }

        val preferences = try {
            deserializer.parsePreferences(preferencesFile.readText())
        } catch (e: Exception) {
            errors.add("Failed to parse preferences.json: ${e.message}")
            null
        }

        // 5. Manifest Counts Validation
        val mediaDir = File(stagingDir, "media")
        val mediaFilesCount = if (mediaDir.exists() && mediaDir.isDirectory) {
            mediaDir.listFiles()?.size ?: 0
        } else 0

        if (manifest.counts.entries != entries.size) {
            errors.add("Manifest entries count mismatch: manifest says ${manifest.counts.entries}, actual parsed is ${entries.size}")
        }
        if (manifest.counts.tags != tags.size) {
            errors.add("Manifest tags count mismatch: manifest says ${manifest.counts.tags}, actual parsed is ${tags.size}")
        }
        if (manifest.counts.attachments != attachments.size) {
            errors.add("Manifest attachments count mismatch: manifest says ${manifest.counts.attachments}, actual parsed is ${attachments.size}")
        }
        if (manifest.counts.mediaFiles != mediaFilesCount) {
            errors.add("Manifest media_files count mismatch: manifest says ${manifest.counts.mediaFiles}, actual files in media/ is $mediaFilesCount")
        }

        // 6. UUID Reference Integrity
        val tagUuidSet = tags.map { it.uuid }.toSet()
        val entryUuidSet = entries.map { it.uuid }.toSet()
        val attachmentUuidSet = attachments.map { it.uuid }.toSet()

        entries.forEach { entry ->
            entry.tagUuids.forEach { tUuid ->
                if (tUuid !in tagUuidSet) {
                    errors.add("Entry '${entry.uuid}' references missing tag UUID: '$tUuid'")
                }
            }
            entry.attachmentUuids.forEach { aUuid ->
                if (aUuid !in attachmentUuidSet) {
                    errors.add("Entry '${entry.uuid}' references missing attachment UUID: '$aUuid'")
                }
            }
        }

        attachments.forEach { att ->
            if (att.entryUuid !in entryUuidSet) {
                errors.add("Attachment '${att.uuid}' references missing entry UUID: '${att.entryUuid}'")
            }

            // 7. Media File Existence, SHA-256 and Size Check
            val mediaFile = File(stagingDir, att.archivePath)
            if (!mediaFile.exists() || !mediaFile.isFile) {
                errors.add("Media file missing from archive: '${att.archivePath}' for attachment '${att.uuid}'")
            } else {
                val actualSize = mediaFile.length()
                if (actualSize != att.fileSizeBytes) {
                    errors.add("Media file size mismatch for '${att.archivePath}': metadata size is ${att.fileSizeBytes}, actual file size is $actualSize")
                }
                val actualSha = computeFileSha256(mediaFile)
                if (!actualSha.equals(att.sha256, ignoreCase = true)) {
                    errors.add("Media file SHA-256 mismatch for '${att.archivePath}': metadata SHA is ${att.sha256}, actual SHA is $actualSha")
                }
            }
        }

        val isValid = errors.isEmpty()
        return ValidationReport(
            isValid = isValid,
            errors = errors,
            manifest = manifest,
            entries = entries,
            tags = tags,
            attachments = attachments,
            preferences = preferences
        )
    }

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { isStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (isStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
