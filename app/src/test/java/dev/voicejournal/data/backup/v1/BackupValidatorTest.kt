package dev.voicejournal.data.backup.v1

import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupEntry
import dev.voicejournal.data.backup.v1.dto.BackupManifest
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.ChecksumMap
import dev.voicejournal.data.backup.v1.dto.DataCounts
import dev.voicejournal.data.backup.v1.dto.GeneratorInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class BackupValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mockDeserializer = mockk<BackupDeserializer>()
    private val validator = BackupValidator(mockDeserializer)

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private fun sha256(text: String): String = sha256(text.toByteArray())

    @Test
    fun `test isValidUuid rejects path traversal and malformed strings`() {
        // Valid UUIDs
        assertTrue(BackupValidator.isValidUuid(UUID.randomUUID().toString()))
        assertTrue(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(BackupValidator.isValidUuid("123E4567-E89B-12D3-A456-426614174000"))

        // Path traversal attempts
        assertFalse(BackupValidator.isValidUuid("../../etc/passwd"))
        assertFalse(BackupValidator.isValidUuid("../file.wav"))
        assertFalse(BackupValidator.isValidUuid(".."))
        assertFalse(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456-426614174000/../evil"))

        // Malformed / Non-UUIDs
        assertFalse(BackupValidator.isValidUuid(""))
        assertFalse(BackupValidator.isValidUuid("not-a-uuid"))
        assertFalse(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456"))
        assertFalse(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456-426614174000-extra"))
        assertFalse(BackupValidator.isValidUuid("123e4567_e89b_12d3_a456_426614174000"))
        assertFalse(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456-42661417400z")) // 'z' non-hex
        assertFalse(BackupValidator.isValidUuid(" 123e4567-e89b-12d3-a456-426614174000")) // leading whitespace
        assertFalse(BackupValidator.isValidUuid("123e4567-e89b-12d3-a456-426614174000\n")) // trailing newline
    }

    @Test
    fun `test isValidArchivePath enforces media prefix and rejects path traversal`() {
        // Valid paths
        assertTrue(BackupValidator.isValidArchivePath("media/test.wav"))
        assertTrue(BackupValidator.isValidArchivePath("media/123e4567-e89b-12d3-a456-426614174000.wav"))
        assertTrue(BackupValidator.isValidArchivePath("media/sub/test.jpg"))

        // Path traversal attempts
        assertFalse(BackupValidator.isValidArchivePath("media/../secret.txt"))
        assertFalse(BackupValidator.isValidArchivePath("media/subdir/../../etc/passwd"))
        assertFalse(BackupValidator.isValidArchivePath("media/.."))
        assertFalse(BackupValidator.isValidArchivePath("../media/test.wav"))

        // Leading slashes
        assertFalse(BackupValidator.isValidArchivePath("/media/test.wav"))
        assertFalse(BackupValidator.isValidArchivePath("///media/test.wav"))

        // Missing media prefix / malformed
        assertFalse(BackupValidator.isValidArchivePath(""))
        assertFalse(BackupValidator.isValidArchivePath("media"))
        assertFalse(BackupValidator.isValidArchivePath("recordings/test.wav"))
        assertFalse(BackupValidator.isValidArchivePath("images/test.jpg"))
        assertFalse(BackupValidator.isValidArchivePath("test.wav"))
        assertFalse(BackupValidator.isValidArchivePath("media\\test.wav"))
    }

    private fun setupMockValidationEnvironment(
        attachmentUuid: String,
        archivePath: String
    ): File {
        val staging = tempFolder.newFolder("staging_${UUID.randomUUID()}")
        val mediaDir = File(staging, "media").apply { mkdirs() }

        val mediaBytes = "test audio data".toByteArray()
        val mediaFile = File(staging, archivePath)
        if (!archivePath.contains("..") && !archivePath.startsWith("/")) {
            mediaFile.parentFile?.mkdirs()
            mediaFile.writeBytes(mediaBytes)
        }

        val entriesText = "{}"
        val tagsText = "{}"
        val attachmentsText = "{}"
        val preferencesText = "{}"
        val manifestText = "{}"

        File(staging, "entries.json").writeText(entriesText)
        File(staging, "tags.json").writeText(tagsText)
        File(staging, "attachments.json").writeText(attachmentsText)
        File(staging, "preferences.json").writeText(preferencesText)
        File(staging, "manifest.json").writeText(manifestText)

        val entryUuid = UUID.randomUUID().toString()

        val manifest = BackupManifest(
            formatName = "dev.voicejournal.backup",
            formatVersion = 1,
            minReaderVersion = 1,
            backupUuid = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            timezoneId = "UTC",
            generator = GeneratorInfo("dev.voicejournal", "1.0.3", 103, "android"),
            counts = DataCounts(entries = 1, tags = 0, attachments = 1, mediaFiles = mediaDir.listFiles()?.size ?: 0),
            features = emptyList(),
            checksums = ChecksumMap(
                algorithm = "SHA-256",
                entriesJson = sha256(entriesText),
                tagsJson = sha256(tagsText),
                attachmentsJson = sha256(attachmentsText),
                preferencesJson = sha256(preferencesText)
            )
        )

        val entry = BackupEntry(
            uuid = entryUuid,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            title = "Test Title",
            userText = "Test Body",
            userTextFormat = "plain",
            mood = null,
            status = "active",
            deletedAt = null,
            tagUuids = emptyList(),
            attachmentUuids = listOf(attachmentUuid)
        )

        val attachment = BackupAttachment(
            uuid = attachmentUuid,
            entryUuid = entryUuid,
            type = "audio",
            mimeType = "audio/wav",
            archivePath = archivePath,
            sha256 = sha256(mediaBytes),
            fileSizeBytes = mediaBytes.size.toLong(),
            displayOrder = 0,
            createdAt = System.currentTimeMillis(),
            metadata = mockk(relaxed = true)
        )

        every { mockDeserializer.parseManifest(any()) } returns manifest
        every { mockDeserializer.parseEntries(any()) } returns listOf(entry)
        every { mockDeserializer.parseTags(any()) } returns emptyList()
        every { mockDeserializer.parseAttachments(any()) } returns listOf(attachment)
        every { mockDeserializer.parsePreferences(any()) } returns BackupPreferences()

        return staging
    }

    @Test
    fun `end-to-end validate with valid backup passes validation`() {
        val validUuid = UUID.randomUUID().toString()
        val staging = setupMockValidationEnvironment(
            attachmentUuid = validUuid,
            archivePath = "media/$validUuid.wav"
        )

        val report = validator.validate(staging)
        assertTrue("Expected valid report, but errors were: ${report.errors}", report.isValid)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun `end-to-end validate rejects path traversal in attachment uuid`() {
        val staging = setupMockValidationEnvironment(
            attachmentUuid = "../../etc/passwd",
            archivePath = "media/test.wav"
        )

        val report = validator.validate(staging)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Attachment UUID") && it.contains("invalid") })
    }

    @Test
    fun `end-to-end validate rejects path traversal in attachment archivePath`() {
        val validUuid = UUID.randomUUID().toString()
        val staging = setupMockValidationEnvironment(
            attachmentUuid = validUuid,
            archivePath = "media/../secret.txt"
        )

        val report = validator.validate(staging)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("invalid archivePath") })
    }

    @Test
    fun `end-to-end validate rejects leading slash in attachment archivePath`() {
        val validUuid = UUID.randomUUID().toString()
        val staging = setupMockValidationEnvironment(
            attachmentUuid = validUuid,
            archivePath = "/media/test.wav"
        )

        val report = validator.validate(staging)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("invalid archivePath") })
    }

    @Test
    fun `end-to-end validate rejects non-media directory attachment archivePath`() {
        val validUuid = UUID.randomUUID().toString()
        val staging = setupMockValidationEnvironment(
            attachmentUuid = validUuid,
            archivePath = "recordings/test.wav"
        )

        val report = validator.validate(staging)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("invalid archivePath") })
    }
}

