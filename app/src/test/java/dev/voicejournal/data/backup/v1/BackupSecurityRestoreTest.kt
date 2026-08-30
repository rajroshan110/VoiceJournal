package dev.voicejournal.data.backup.v1

import android.content.Context
import dev.voicejournal.data.backup.v1.dto.AppearancePreferences
import dev.voicejournal.data.backup.v1.dto.BackupPreferences
import dev.voicejournal.data.backup.v1.dto.DataManagementPreferences
import dev.voicejournal.data.backup.v1.dto.DisplayPreferences
import dev.voicejournal.data.backup.v1.dto.EditorPreferences
import dev.voicejournal.data.backup.v1.dto.RecordingPreferences
import dev.voicejournal.data.backup.v1.dto.SecurityPreferences
import dev.voicejournal.data.backup.v1.dto.TranscriptionPreferences
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.journal.components.SortOption
import dev.voicejournal.ui.theme.AppThemeMode
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackupSecurityRestoreTest {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockPrefsManager: UserPreferencesManager
    private lateinit var restorer: BackupRestorer

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        mockContext = mockk(relaxed = true)
        mockDatabase = mockk(relaxed = true)
        mockPrefsManager = mockk(relaxed = true)
        restorer = BackupRestorer(mockContext, mockDatabase, mockPrefsManager)
    }

    @After
    fun teardown() {
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

        // Verify non-security preferences were restored
        coVerify(exactly = 1) { mockPrefsManager.setAudioFormat(AudioFormat.M4A_AAC_128KBPS) }
        coVerify(exactly = 1) { mockPrefsManager.setAppThemeMode(AppThemeMode.LIGHT_PREMIUM) }

        // Verify security state was NEVER mutated
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
                        appLockTimeout = timeout
                    )
                )

                restorer.restorePreferences(backup)
            }
        }

        // Across all 16 combinations, security mutation methods must NEVER be called
        coVerify(exactly = 0) { mockPrefsManager.setAppLockMode(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setAppLockTimeout(any()) }
        coVerify(exactly = 0) { mockPrefsManager.setCustomPin(any()) }
        coVerify(exactly = 0) { mockPrefsManager.resetSecurityState(any()) }
    }

    @Test
    fun `restorePreferences successfully restores all non-security user preferences`() = runBlocking {
        val backup = BackupPreferences(
            recording = RecordingPreferences(audioFormat = "wav_16khz"),
            transcription = TranscriptionPreferences(whisperModel = "ggml-tiny-q5_1"),
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
            dataManagement = DataManagementPreferences(dailyReminder = true),
            security = SecurityPreferences(appLockMode = "custom_pin") // Must be ignored
        )

        restorer.restorePreferences(backup)

        coVerify(exactly = 1) { mockPrefsManager.setAudioFormat(AudioFormat.WAV_16KHZ) }
        coVerify(exactly = 1) { mockPrefsManager.setWhisperModel("ggml-tiny-q5_1") }
        coVerify(exactly = 1) { mockPrefsManager.setAppThemeMode(AppThemeMode.DARK) }
        coVerify(exactly = 1) { mockPrefsManager.setTimeFormat(TimeFormat.TWENTY_FOUR_HOUR) }
        coVerify(exactly = 1) { mockPrefsManager.setStartOfWeek(StartOfWeek.MONDAY) }
        coVerify(exactly = 1) { mockPrefsManager.setInsightDateRangeMode(InsightDateRangeMode.CURRENT_CALENDAR) }
        coVerify(exactly = 1) { mockPrefsManager.setSortOption(SortOption.CREATED_ASC) }
        coVerify(exactly = 1) { mockPrefsManager.setFolderIsGridView(false) }
        coVerify(exactly = 1) { mockPrefsManager.setTagIsGridView(false) }
        coVerify(exactly = 1) { mockPrefsManager.setMarkdownEnabled(true) }
        coVerify(exactly = 1) { mockPrefsManager.setDailyReminder(true) }

        // AppLockMode was in backup but must NOT be touched
        coVerify(exactly = 0) { mockPrefsManager.setAppLockMode(any()) }
    }
}
