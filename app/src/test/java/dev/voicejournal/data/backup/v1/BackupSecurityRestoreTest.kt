package dev.voicejournal.data.backup.v1

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import dev.voicejournal.data.backup.v1.dto.AppearancePreferences
import dev.voicejournal.data.backup.v1.dto.BackupAttachment
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.DataManagementPreferences
import dev.voicejournal.data.backup.v1.dto.DisplayPreferences
import dev.voicejournal.data.backup.v1.dto.EditorPreferences
import dev.voicejournal.data.backup.v1.dto.RecordingPreferences
import dev.voicejournal.data.backup.v1.dto.SecurityPreferences
import dev.voicejournal.data.backup.v1.dto.TranscriptionPreferences
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.dao.EntryImageDao
import dev.voicejournal.data.local.db.dao.JournalEntryDao
import dev.voicejournal.data.local.db.dao.TagDao
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.journal.components.SortOption
import dev.voicejournal.ui.theme.AppThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.UUID

class BackupSecurityRestoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockPrefsManager: UserPreferencesManager
    private lateinit var mockEntryDao: JournalEntryDao
    private lateinit var mockTagDao: TagDao
    private lateinit var mockImageDao: EntryImageDao
    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var restorer: BackupRestorer

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        mockkStatic("androidx.room.RoomDatabaseKt")

        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")

        mockEntryDao = mockk(relaxed = true)
        mockTagDao = mockk(relaxed = true)
        mockImageDao = mockk(relaxed = true)

        mockContext = mockk(relaxed = true) {
            every { this@mockk.filesDir } returns this@BackupSecurityRestoreTest.filesDir
            every { this@mockk.cacheDir } returns this@BackupSecurityRestoreTest.cacheDir
        }
        mockDatabase = mockk(relaxed = true) {
            every { journalEntryDao() } returns mockEntryDao
            every { tagDao() } returns mockTagDao
            every { entryImageDao() } returns mockImageDao
        }
        coEvery { mockDatabase.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            val block = secondArg<suspend () -> Any?>()
            block()
        }
        mockPrefsManager = mockk(relaxed = true)
        restorer = BackupRestorer(mockContext, mockDatabase, mockPrefsManager)
    }

    @After
    fun teardown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
        unmockkStatic(Log::class)
    }

    @Test
    fun `restorePreferences never modifies appLockMode, customPin, or timeout regardless of backup security settings`() = runBlocking {
        // Combination 1: Backup contains CUSTOM_PIN with IMMEDIATELY timeout
        val backupWithPin = BackupPreferences(
            recording = RecordingPreferences(audioFormat = "m4a_aac_128kbps"),
            appearance = AppearancePreferences(themeMode = "light_premium"),
            security = SecurityPreferences(
                appLockMode = "custom_pin",
                appLockTimeout = "immediately",
                screenPrivacyEnabled = true
            )
        )

        restorer.restorePreferences(backupWithPin)

        // Verify non-security and standalone screen privacy preferences were restored
        coVerify(exactly = 1) { mockPrefsManager.setAudioFormat(AudioFormat.M4A_AAC_128KBPS) }
        coVerify(exactly = 1) { mockPrefsManager.setAppThemeMode(AppThemeMode.LIGHT_PREMIUM) }
        coVerify(exactly = 1) { mockPrefsManager.setScreenPrivacyEnabled(true) }

        // Verify App Lock state was NEVER mutated
        coVerify(exactly = 0) { mockPrefsManager.setAppLockMode(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setAppLockTimeout(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setCustomPin(any()) }
        coVerify(exactly = 0) { mockPrefsManager.resetSecurityState(any()) }
    }

    @Test
    fun `restorePreferences preserves destination App Lock state for all backup security modes`() = runBlocking {
        val testModes = listOf("none", "biometric", "custom_pin", "unknown_mode")
        val testTimeouts = listOf("immediately", "one_minute", "five_minutes", "ten_minutes")

        for (mode in testModes) {
            for (timeout in testTimeouts) {
                val backup = BackupPreferences(
                    security = SecurityPreferences(
                        appLockMode = mode,
                        appLockTimeout = timeout,
                        screenPrivacyEnabled = false
                    )
                )

                restorer.restorePreferences(backup)
            }
        }

        // Across all 16 combinations, App Lock mutation methods must NEVER be called
        coVerify(exactly = 0) { mockPrefsManager.setAppLockMode(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setAppLockTimeout(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setCustomPin(any()) }
        coVerify(exactly = 0) { mockPrefsManager.resetSecurityState(any()) }
    }

    @Test
    fun `restorePreferences successfully restores all user preferences including standalone screen privacy`() = runBlocking {
        val backup = BackupPreferences(
            recording = RecordingPreferences(audioFormat = "wav_16khz"),
            transcription = TranscriptionPreferences(
                whisperModel = "ggml-tiny-q5_1",
                speechToTextEnabled = false
            ),
            appearance = AppearancePreferences(themeMode = "dark"),
            display = DisplayPreferences(
                timeFormat = "twenty_four_hour",
                startOfWeek = "monday",
                insightDateRangeMode = "current_calendar",
                sortOption = "created_asc",
                folderGridView = false,
                tagGridView = false
            ),
            editor = EditorPreferences(markdownEnabled = true),
            dataManagement = DataManagementPreferences(trashRetentionDays = 14),
            security = SecurityPreferences(
                appLockMode = "custom_pin",
                screenPrivacyEnabled = true
            )
        )

        restorer.restorePreferences(backup)

        coVerify(exactly = 1) { mockPrefsManager.setAudioFormat(AudioFormat.WAV_16KHZ) }
        coVerify(exactly = 1) { mockPrefsManager.setWhisperModel("ggml-tiny-q5_1") }
        coVerify(exactly = 1) { mockPrefsManager.setSpeechToTextEnabled(false) }
        coVerify(exactly = 1) { mockPrefsManager.setAppThemeMode(AppThemeMode.DARK) }
        coVerify(exactly = 1) { mockPrefsManager.setTimeFormat(TimeFormat.TWENTY_FOUR_HOUR) }
        coVerify(exactly = 1) { mockPrefsManager.setStartOfWeek(StartOfWeek.MONDAY) }
        coVerify(exactly = 1) { mockPrefsManager.setInsightDateRangeMode(InsightDateRangeMode.CURRENT_CALENDAR) }
        coVerify(exactly = 1) { mockPrefsManager.setSortOption(SortOption.CREATED_ASC) }
        coVerify(exactly = 1) { mockPrefsManager.setFolderIsGridView(false) }
        coVerify(exactly = 1) { mockPrefsManager.setTagIsGridView(false) }
        coVerify(exactly = 1) { mockPrefsManager.setMarkdownEnabled(true) }
        coVerify(exactly = 1) { mockPrefsManager.setScreenPrivacyEnabled(true) }

        // AppLockMode was in backup but must NOT be touched
        coVerify(exactly = 0) { mockPrefsManager.setAppLockMode(any()) }
    }

    @Test
    fun `restore rejects invalid attachment uuid with SecurityException cause`() = runBlocking {
        val stagingDir = tempFolder.newFolder("staging_bad_uuid")
        val mediaFile = File(stagingDir, "media/test.wav").apply {
            parentFile?.mkdirs()
            writeText("audio")
        }
        val badAttachment = BackupAttachment(
            uuid = "../../evil",
            entryUuid = UUID.randomUUID().toString(),
            type = "audio",
            mimeType = "audio/wav",
            archivePath = "media/${mediaFile.name}",
            sha256 = "00".repeat(32),
            fileSizeBytes = mediaFile.length(),
            displayOrder = 0,
            createdAt = 1000L,
            metadata = mockk<JSONObject>(relaxed = true)
        )

        try {
            restorer.restore(
                stagingDir = stagingDir,
                entries = emptyList(),
                tags = emptyList(),
                attachments = listOf(badAttachment),
                preferences = null
            )
            fail("Expected IOException wrapping SecurityException")
        } catch (e: IOException) {
            assertTrue(e.cause is SecurityException)
        }

        coVerify(exactly = 0) { mockEntryDao.deleteAllJournalEntries() }
    }

    @Test
    fun `restore rejects invalid attachment archivePath with SecurityException cause`() = runBlocking {
        val stagingDir = tempFolder.newFolder("staging_bad_path")
        val badPaths = listOf("media/../secret.wav", "/media/test.wav", "recordings/test.wav")

        for (badPath in badPaths) {
            val badAttachment = BackupAttachment(
                uuid = UUID.randomUUID().toString(),
                entryUuid = UUID.randomUUID().toString(),
                type = "audio",
                mimeType = "audio/wav",
                archivePath = badPath,
                sha256 = "00".repeat(32),
                fileSizeBytes = 5L,
                displayOrder = 0,
                createdAt = 1000L,
                metadata = mockk<JSONObject>(relaxed = true)
            )

            try {
                restorer.restore(
                    stagingDir = stagingDir,
                    entries = emptyList(),
                    tags = emptyList(),
                    attachments = listOf(badAttachment),
                    preferences = null
                )
                fail("Expected IOException wrapping SecurityException for $badPath")
            } catch (e: IOException) {
                assertTrue("Expected SecurityException cause for $badPath", e.cause is SecurityException)
            }
        }
    }

    @Test
    fun `restore sanitizes unsafe attachment extension to default extension`() = runBlocking {
        val stagingDir = tempFolder.newFolder("staging_ext")
        val audioUuid = UUID.randomUUID().toString()
        val imageUuid = UUID.randomUUID().toString()

        val badAudio = File(stagingDir, "media/track.evil_long_ext").apply {
            parentFile?.mkdirs()
            writeText("audio-bytes")
        }
        val badImage = File(stagingDir, "media/pic.sh!").apply {
            parentFile?.mkdirs()
            writeText("image-bytes")
        }

        val attachments = listOf(
            BackupAttachment(
                uuid = audioUuid,
                entryUuid = UUID.randomUUID().toString(),
                type = "audio",
                mimeType = "audio/wav",
                archivePath = "media/${badAudio.name}",
                sha256 = "00".repeat(32),
                fileSizeBytes = badAudio.length(),
                displayOrder = 0,
                createdAt = 1000L,
                metadata = mockk<JSONObject>(relaxed = true)
            ),
            BackupAttachment(
                uuid = imageUuid,
                entryUuid = UUID.randomUUID().toString(),
                type = "image",
                mimeType = "image/jpeg",
                archivePath = "media/${badImage.name}",
                sha256 = "00".repeat(32),
                fileSizeBytes = badImage.length(),
                displayOrder = 1,
                createdAt = 1000L,
                metadata = mockk<JSONObject>(relaxed = true)
            )
        )

        restorer.restore(
            stagingDir = stagingDir,
            entries = emptyList(),
            tags = emptyList(),
            attachments = attachments,
            preferences = null
        )

        val restoredAudio = File(filesDir, "recordings/$audioUuid.wav")
        val restoredImage = File(filesDir, "images/$imageUuid.jpg")
        assertTrue("Expected sanitized .wav audio file", restoredAudio.exists())
        assertTrue("Expected sanitized .jpg image file", restoredImage.exists())
        assertFalse(File(filesDir, "recordings/$audioUuid.evil_long_ext").exists())
        assertFalse(File(filesDir, "images/$imageUuid.sh!").exists())
    }
}
