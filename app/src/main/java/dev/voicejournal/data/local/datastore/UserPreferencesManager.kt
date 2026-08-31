package dev.voicejournal.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.voicejournal.data.security.KeyStoreHelper
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.journal.components.SortOption
import dev.voicejournal.ui.theme.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import dev.voicejournal.data.security.KeystoreInvalidatedException
import kotlinx.coroutines.flow.catch
import android.util.Log

import javax.inject.Inject
import javax.inject.Singleton

enum class SecurityRecoveryReason {
    NONE,
    KEYSTORE_INVALIDATED,
    RESTORE_INCONSISTENT_SECURITY_STATE
}

@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val WHISPER_MODEL = stringPreferencesKey("whisper_model")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val FOLDER_IS_GRID_VIEW = booleanPreferencesKey("folder_is_grid_view")
        val TAG_IS_GRID_VIEW = booleanPreferencesKey("tag_is_grid_view")
        val INSIGHT_DATE_RANGE_MODE = stringPreferencesKey("insight_date_range_mode")
        
        // New preference keys specified in redesign matrix
        val IS_MARKDOWN_ENABLED = booleanPreferencesKey("is_markdown_enabled")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val START_OF_WEEK = stringPreferencesKey("start_of_week")
        val APP_LOCK_MODE = stringPreferencesKey("app_lock_mode")
        val SECURITY_RECOVERY_REASON = stringPreferencesKey("security_recovery_reason")
        val APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
        val CUSTOM_PIN = stringPreferencesKey("custom_pin") // Legacy plaintext PIN
        val CUSTOM_PIN_ENCRYPTED = stringPreferencesKey("custom_pin_encrypted")
        val IS_SCREEN_PRIVACY_ENABLED = booleanPreferencesKey("is_screen_privacy_enabled")
        val IS_SPEECH_TO_TEXT_ENABLED = booleanPreferencesKey("is_speech_to_text_enabled")
        val IS_FOLDER_ENABLED = booleanPreferencesKey("is_folder_enabled")
        val IS_NOTES_ORGANISATION_ENABLED = booleanPreferencesKey("is_notes_organisation_enabled")
        val IS_TOPICS_ENABLED = booleanPreferencesKey("is_topics_enabled")
        val IS_PEOPLE_ENABLED = booleanPreferencesKey("is_people_enabled")
        val IS_MOOD_ENABLED = booleanPreferencesKey("is_mood_enabled")
        
        // Lockout State
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKOUT_END_TIME = longPreferencesKey("pin_lockout_end_time")
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    val folderIsGridView: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FOLDER_IS_GRID_VIEW] ?: true
    }

    val tagIsGridView: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TAG_IS_GRID_VIEW] ?: true
    }

    val insightDateRangeMode: Flow<InsightDateRangeMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[INSIGHT_DATE_RANGE_MODE] ?: InsightDateRangeMode.LAST_DAYS.name
        try {
            InsightDateRangeMode.valueOf(modeStr)
        } catch (e: Exception) {
            InsightDateRangeMode.LAST_DAYS
        }
    }

    val audioFormat: Flow<AudioFormat> = dataStore.data.map { prefs ->
        val formatStr = prefs[AUDIO_FORMAT] ?: AudioFormat.M4A_AAC_128KBPS.name
        try {
            AudioFormat.valueOf(formatStr)
        } catch (e: Exception) {
            AudioFormat.M4A_AAC_128KBPS
        }
    }

    val whisperModel: Flow<String> = dataStore.data.map { prefs ->
        prefs[WHISPER_MODEL] ?: "NONE"
    }

    val dailyReminder: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DAILY_REMINDER] ?: false
    }

    val appThemeMode: StateFlow<AppThemeMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[APP_THEME_MODE] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }.stateIn(scope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)

    val sortOption: Flow<SortOption> = dataStore.data.map { prefs ->
        val sortStr = prefs[SORT_OPTION] ?: SortOption.MODIFIED_DESC.name
        try {
            SortOption.valueOf(sortStr)
        } catch (e: Exception) {
            SortOption.MODIFIED_DESC
        }
    }

    val isMarkdownEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_MARKDOWN_ENABLED] ?: true
    }

    val timeFormat: Flow<TimeFormat> = dataStore.data.map { prefs ->
        val formatStr = prefs[TIME_FORMAT] ?: TimeFormat.SYSTEM_DEFAULT.name
        try {
            TimeFormat.valueOf(formatStr)
        } catch (e: Exception) {
            TimeFormat.SYSTEM_DEFAULT
        }
    }

    val startOfWeek: Flow<StartOfWeek> = dataStore.data.map { prefs ->
        val startStr = prefs[START_OF_WEEK] ?: StartOfWeek.SYSTEM_DEFAULT.name
        try {
            StartOfWeek.valueOf(startStr)
        } catch (e: Exception) {
            StartOfWeek.SYSTEM_DEFAULT
        }
    }

    val securityRecoveryReason: Flow<SecurityRecoveryReason> = dataStore.data.map { prefs ->
        try {
            SecurityRecoveryReason.valueOf(prefs[SECURITY_RECOVERY_REASON] ?: SecurityRecoveryReason.NONE.name)
        } catch (e: Exception) {
            SecurityRecoveryReason.NONE
        }
    }

    val appLockMode: Flow<AppLockMode> = dataStore.data.map { prefs ->
        val lockStr = prefs[APP_LOCK_MODE] ?: AppLockMode.NONE.name
        try {
            AppLockMode.valueOf(lockStr)
        } catch (e: Exception) {
            AppLockMode.NONE
        }
    }

    val appLockTimeout: Flow<AppLockTimeout> = dataStore.data.map { prefs ->
        val timeoutStr = prefs[APP_LOCK_TIMEOUT] ?: AppLockTimeout.IMMEDIATELY.name
        try {
            AppLockTimeout.valueOf(timeoutStr)
        } catch (e: Exception) {
            AppLockTimeout.IMMEDIATELY
        }
    }

    val customPin: Flow<String?> = dataStore.data.map { prefs ->
        val encrypted = prefs[CUSTOM_PIN_ENCRYPTED]
        if (encrypted != null) {
            KeyStoreHelper.decrypt(encrypted)
        } else {
            prefs[CUSTOM_PIN] // Legacy plaintext
        }
    }.catch { e ->
        if (e is KeystoreInvalidatedException) {
            resetSecurityState(SecurityRecoveryReason.KEYSTORE_INVALIDATED)
            emit(null)
        } else {
            throw e
        }
    }

    val isScreenPrivacyEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_SCREEN_PRIVACY_ENABLED] ?: false
    }

    val isSpeechToTextEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_SPEECH_TO_TEXT_ENABLED] ?: true
    }

    val isFolderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_FOLDER_ENABLED] ?: false
    }

    val isFolderEnabledState: StateFlow<Boolean> = isFolderEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isNotesOrganisationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_NOTES_ORGANISATION_ENABLED] ?: false
    }

    val isNotesOrganisationEnabledState: StateFlow<Boolean> = isNotesOrganisationEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isTopicsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_TOPICS_ENABLED] ?: true
    }

    val isTopicsEnabledState: StateFlow<Boolean> = isTopicsEnabled
        .stateIn(scope, SharingStarted.Eagerly, true)

    val isPeopleEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_PEOPLE_ENABLED] ?: true
    }

    val isPeopleEnabledState: StateFlow<Boolean> = isPeopleEnabled
        .stateIn(scope, SharingStarted.Eagerly, true)

    val isMoodEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_MOOD_ENABLED] ?: true
    }

    val isMoodEnabledState: StateFlow<Boolean> = isMoodEnabled
        .stateIn(scope, SharingStarted.Eagerly, true)

    val pinFailedAttempts: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PIN_FAILED_ATTEMPTS] ?: 0
    }

    val pinLockoutEndTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PIN_LOCKOUT_END_TIME] ?: 0L
    }

    suspend fun setInsightDateRangeMode(mode: InsightDateRangeMode) {
        dataStore.edit { prefs ->
            prefs[INSIGHT_DATE_RANGE_MODE] = mode.name
        }
    }

    suspend fun setAudioFormat(format: AudioFormat) {
        dataStore.edit { prefs ->
            prefs[AUDIO_FORMAT] = format.name
        }
    }

    suspend fun setWhisperModel(model: String) {
        dataStore.edit { prefs ->
            prefs[WHISPER_MODEL] = model
        }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DAILY_REMINDER] = enabled
        }
    }

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[APP_THEME_MODE] = mode.name
        }
    }

    suspend fun setSortOption(option: SortOption) {
        dataStore.edit { prefs ->
            prefs[SORT_OPTION] = option.name
        }
    }

    suspend fun setFolderIsGridView(isGrid: Boolean) {
        dataStore.edit { prefs ->
            prefs[FOLDER_IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun setTagIsGridView(isGrid: Boolean) {
        dataStore.edit { prefs ->
            prefs[TAG_IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun setMarkdownEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_MARKDOWN_ENABLED] = enabled
        }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        dataStore.edit { prefs ->
            prefs[TIME_FORMAT] = format.name
        }
    }

    suspend fun setStartOfWeek(start: StartOfWeek) {
        dataStore.edit { prefs ->
            prefs[START_OF_WEEK] = start.name
        }
    }

    suspend fun setAppLockMode(mode: AppLockMode) {
        dataStore.edit { prefs ->
            prefs[APP_LOCK_MODE] = mode.name
        }
    }

    suspend fun clearSecurityRecoveryReason() {
        dataStore.edit { prefs ->
            prefs[SECURITY_RECOVERY_REASON] = SecurityRecoveryReason.NONE.name
        }
    }

    suspend fun resetSecurityState(reason: SecurityRecoveryReason) {
        dataStore.edit { prefs ->
            if (prefs[APP_LOCK_MODE] != AppLockMode.NONE.name || prefs.contains(CUSTOM_PIN_ENCRYPTED)) {
                prefs.remove(CUSTOM_PIN_ENCRYPTED)
                prefs.remove(CUSTOM_PIN)
                prefs[APP_LOCK_MODE] = AppLockMode.NONE.name
                prefs[PIN_FAILED_ATTEMPTS] = 0
                prefs[PIN_LOCKOUT_END_TIME] = 0L
                prefs[SECURITY_RECOVERY_REASON] = reason.name
                Log.i("Security", "Security state reset due to: $reason")
            }
        }
    }

    suspend fun setAppLockTimeout(timeout: AppLockTimeout) {
        dataStore.edit { prefs ->
            prefs[APP_LOCK_TIMEOUT] = timeout.name
        }
    }

    suspend fun setCustomPin(pin: String?) {
        dataStore.edit { prefs ->
            if (pin != null) {
                val encrypted = KeyStoreHelper.encrypt(pin)
                prefs[CUSTOM_PIN_ENCRYPTED] = encrypted
                prefs.remove(CUSTOM_PIN)
            } else {
                prefs.remove(CUSTOM_PIN_ENCRYPTED)
                prefs.remove(CUSTOM_PIN)
            }
        }
    }

    suspend fun migratePinIfNeeded(verifiedPin: String) {
        dataStore.edit { prefs ->
            if (prefs.contains(CUSTOM_PIN)) {
                prefs[CUSTOM_PIN_ENCRYPTED] = KeyStoreHelper.encrypt(verifiedPin)
                prefs.remove(CUSTOM_PIN)
            }
        }
    }

    suspend fun registerFailedPinAttempt(lockoutDurationMs: Long = 30000L) {
        dataStore.edit { prefs ->
            val attempts = (prefs[PIN_FAILED_ATTEMPTS] ?: 0) + 1
            prefs[PIN_FAILED_ATTEMPTS] = attempts
            if (attempts >= 5) {
                prefs[PIN_LOCKOUT_END_TIME] = System.currentTimeMillis() + lockoutDurationMs
            }
        }
    }

    suspend fun clearFailedPinAttempts() {
        dataStore.edit { prefs ->
            prefs[PIN_FAILED_ATTEMPTS] = 0
            prefs[PIN_LOCKOUT_END_TIME] = 0L
        }
    }

    suspend fun setScreenPrivacyEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_SCREEN_PRIVACY_ENABLED] = enabled
        }
    }

    suspend fun setSpeechToTextEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_SPEECH_TO_TEXT_ENABLED] = enabled
        }
    }

    suspend fun setFolderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_FOLDER_ENABLED] = enabled
        }
    }

    suspend fun setNotesOrganisationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_NOTES_ORGANISATION_ENABLED] = enabled
        }
    }

    suspend fun setTopicsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_TOPICS_ENABLED] = enabled
        }
    }

    suspend fun setPeopleEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_PEOPLE_ENABLED] = enabled
        }
    }

    suspend fun setMoodEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_MOOD_ENABLED] = enabled
        }
    }

    suspend fun getRawPreferencesSnapshot(): Preferences {

        return dataStore.data.first()
    }

    suspend fun restoreRawPreferences(snapshot: Preferences) {
        dataStore.edit { prefs ->
            prefs.clear()
            snapshot.asMap().forEach { (key, value) -> prefs[key as androidx.datastore.preferences.core.Preferences.Key<Any>] = value }
        }
    }
}
