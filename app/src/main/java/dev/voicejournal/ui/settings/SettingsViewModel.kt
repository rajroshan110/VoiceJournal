package dev.voicejournal.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.backup.ExportManager
import dev.voicejournal.data.backup.ImportManager
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.ModelDownloadState
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import dev.voicejournal.transcription.WhisperManager
import dev.voicejournal.ui.settings.model.SearchResultItem
import dev.voicejournal.ui.settings.model.SettingRegistry
import dev.voicejournal.ui.settings.model.SettingsSubScreen
import dev.voicejournal.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupResultDialog(
    val title: String,
    val message: String
)

data class SettingsUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchResultItem> = emptyList(),
    val activeSubScreen: SettingsSubScreen = SettingsSubScreen.GENERAL,
    val isViewingAppLockDetail: Boolean = false,
    val audioFormat: AudioFormat = AudioFormat.WAV_16KHZ,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val insightDateRangeMode: InsightDateRangeMode = InsightDateRangeMode.LAST_DAYS,
    val whisperModel: String = "base.en",
    val isMarkdownEnabled: Boolean = false,
    val timeFormat: TimeFormat = TimeFormat.TWELVE_HOUR,
    val startOfWeek: StartOfWeek = StartOfWeek.SUNDAY,
    val appLockMode: AppLockMode = AppLockMode.NONE,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATELY,
    val customPin: String? = null,
    val isScreenPrivacyEnabled: Boolean = false,
    val isSpeechToTextEnabled: Boolean = true,
    val isModelDownloaded: Boolean = false,
    val downloadProgress: Float? = null,
    val sttModelDownloadState: ModelDownloadState = ModelDownloadState.Idle,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isWipingData: Boolean = false,
    val backupProgressText: String? = null,
    val backupResultDialog: BackupResultDialog? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showPinSetupDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager,
    private val journalRepository: JournalRepository,
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
    private val whisperManager: WhisperManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesManager.audioFormat.collect { format ->
                _uiState.value = _uiState.value.copy(audioFormat = format)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.appThemeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(appThemeMode = mode)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.insightDateRangeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(insightDateRangeMode = mode)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.whisperModel.collect { model ->
                _uiState.value = _uiState.value.copy(whisperModel = model)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isMarkdownEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isMarkdownEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.value = _uiState.value.copy(timeFormat = format)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.startOfWeek.collect { start ->
                _uiState.value = _uiState.value.copy(startOfWeek = start)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.appLockMode.collect { lockMode ->
                _uiState.value = _uiState.value.copy(appLockMode = lockMode)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.appLockTimeout.collect { timeout ->
                _uiState.value = _uiState.value.copy(appLockTimeout = timeout)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.customPin.collect { pin ->
                _uiState.value = _uiState.value.copy(customPin = pin)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isScreenPrivacyEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isScreenPrivacyEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isSpeechToTextEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isSpeechToTextEnabled = enabled)
            }
        }
        viewModelScope.launch {
            whisperManager.isModelDownloaded.collect { downloaded ->
                val state = if (downloaded) ModelDownloadState.Downloaded else ModelDownloadState.Idle
                _uiState.value = _uiState.value.copy(
                    isModelDownloaded = downloaded,
                    sttModelDownloadState = state
                )
            }
        }
        viewModelScope.launch {
            whisperManager.downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
                if (progress != null && progress < 1.0f) {
                    _uiState.value = _uiState.value.copy(
                        sttModelDownloadState = ModelDownloadState.Downloading(progress)
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val results = SettingRegistry.search(query)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = query.isNotBlank(),
            searchResults = results
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            isSearching = false,
            searchResults = emptyList()
        )
    }

    fun setActiveSubScreen(subScreen: SettingsSubScreen) {
        _uiState.value = _uiState.value.copy(
            activeSubScreen = subScreen,
            isViewingAppLockDetail = false
        )
    }

    fun setViewingAppLockDetail(viewing: Boolean) {
        _uiState.value = _uiState.value.copy(isViewingAppLockDetail = viewing)
    }

    fun setAudioFormat(format: AudioFormat) {
        viewModelScope.launch {
            userPreferencesManager.setAudioFormat(format)
        }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            userPreferencesManager.setAppThemeMode(mode)
        }
    }

    fun setInsightDateRangeMode(mode: InsightDateRangeMode) {
        viewModelScope.launch {
            userPreferencesManager.setInsightDateRangeMode(mode)
        }
    }

    fun setMarkdownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setMarkdownEnabled(enabled)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            userPreferencesManager.setTimeFormat(format)
        }
    }

    fun setStartOfWeek(start: StartOfWeek) {
        viewModelScope.launch {
            userPreferencesManager.setStartOfWeek(start)
        }
    }

    fun setAppLockMode(mode: AppLockMode) {
        viewModelScope.launch {
            if (mode == AppLockMode.NONE || mode == AppLockMode.BIOMETRIC) {
                userPreferencesManager.setCustomPin(null)
                userPreferencesManager.setAppLockMode(mode)
            } else if (mode == AppLockMode.CUSTOM_PIN) {
                userPreferencesManager.setCustomPin(null)
                _uiState.value = _uiState.value.copy(showPinSetupDialog = true)
            }
        }
    }

    fun modifyCustomPin() {
        viewModelScope.launch {
            userPreferencesManager.setCustomPin(null)
            _uiState.value = _uiState.value.copy(showPinSetupDialog = true)
        }
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        viewModelScope.launch {
            userPreferencesManager.setAppLockTimeout(timeout)
        }
    }

    fun saveCustomPin(pin: String) {
        viewModelScope.launch {
            userPreferencesManager.setCustomPin(pin)
            userPreferencesManager.setAppLockMode(AppLockMode.CUSTOM_PIN)
            _uiState.value = _uiState.value.copy(showPinSetupDialog = false)
        }
    }

    fun dismissPinSetupDialog() {
        _uiState.value = _uiState.value.copy(showPinSetupDialog = false)
        if (_uiState.value.customPin.isNullOrBlank() && _uiState.value.appLockMode == AppLockMode.CUSTOM_PIN) {
            viewModelScope.launch {
                userPreferencesManager.setAppLockMode(AppLockMode.NONE)
            }
        }
    }

    fun setScreenPrivacyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setScreenPrivacyEnabled(enabled)
        }
    }

    fun setSpeechToTextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setSpeechToTextEnabled(enabled)
        }
    }

    fun downloadWhisperModel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sttModelDownloadState = ModelDownloadState.Downloading(0f)
            )
            val success = whisperManager.ensureModelDownloaded()
            if (success) {
                userPreferencesManager.setWhisperModel("base.en")
                _uiState.value = _uiState.value.copy(
                    isModelDownloaded = true,
                    sttModelDownloadState = ModelDownloadState.Downloaded
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    sttModelDownloadState = ModelDownloadState.Error("Download failed — Insufficient storage or network error")
                )
            }
        }
    }

    fun deleteWhisperModel() {
        viewModelScope.launch {
            whisperManager.deleteModel()
            userPreferencesManager.setWhisperModel("NONE")
            _uiState.value = _uiState.value.copy(
                isModelDownloaded = false,
                sttModelDownloadState = ModelDownloadState.Idle
            )
        }
    }

    fun cancelModelDownload() {
        _uiState.value = _uiState.value.copy(
            sttModelDownloadState = ModelDownloadState.Idle,
            downloadProgress = null
        )
    }

    fun initiateDeleteAllJournals() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmationDialog = true)
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmationDialog = false)
    }

    fun confirmDeleteAllJournals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showDeleteConfirmationDialog = false,
                isWipingData = true
            )
            journalRepository.wipeAllData()
            _uiState.value = _uiState.value.copy(
                isWipingData = false
            )
        }
    }

    fun exportBackup(targetFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, backupProgressText = "Preparing backup…", backupResultDialog = null)
            val result = exportManager.exportData(targetFile) { progress ->
                _uiState.value = _uiState.value.copy(backupProgressText = progress)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    backupProgressText = null,
                    backupResultDialog = BackupResultDialog(
                        title = "Backup Completed",
                        message = "Your backup has been created successfully."
                    )
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    backupProgressText = null,
                    backupResultDialog = mapImportErrorToDialog(emptyList(), err)
                )
            }
        }
    }

    fun exportBackupToUri(targetUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, backupProgressText = "Preparing backup…", backupResultDialog = null)
            val result = exportManager.exportToUri(targetUri) { progress ->
                _uiState.value = _uiState.value.copy(backupProgressText = progress)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    backupProgressText = null,
                    backupResultDialog = BackupResultDialog(
                        title = "Backup Completed",
                        message = "Your backup has been created successfully."
                    )
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    backupProgressText = null,
                    backupResultDialog = mapImportErrorToDialog(emptyList(), err)
                )
            }
        }
    }

    fun importBackup(sourceFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, backupProgressText = "Validating backup…", backupResultDialog = null)
            val result = importManager.importData(sourceFile) { progress ->
                _uiState.value = _uiState.value.copy(backupProgressText = progress)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    backupProgressText = null,
                    backupResultDialog = BackupResultDialog(
                        title = "Backup Restored",
                        message = "Your backup has been restored successfully."
                    )
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    backupProgressText = null,
                    backupResultDialog = mapImportErrorToDialog(listOf(err.localizedMessage ?: ""), err)
                )
            }
        }
    }

    fun importBackupFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, backupProgressText = "Validating backup…", backupResultDialog = null)
            val result = importManager.importFromUri(sourceUri) { progress ->
                _uiState.value = _uiState.value.copy(backupProgressText = progress)
            }
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    backupProgressText = null,
                    backupResultDialog = BackupResultDialog(
                        title = "Backup Restored",
                        message = "Your backup has been restored successfully."
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    backupProgressText = null,
                    backupResultDialog = mapImportErrorToDialog(result.validationErrors, null)
                )
            }
        }
    }

    fun dismissBackupResultDialog() {
        _uiState.value = _uiState.value.copy(backupResultDialog = null)
    }

    private fun mapImportErrorToDialog(errors: List<String>, exception: Throwable?): BackupResultDialog {
        val joined = errors.joinToString(" ").lowercase()
        val exMsg = exception?.localizedMessage?.lowercase() ?: ""

        return when {
            joined.contains("unsupported format_version") || joined.contains("min_reader_version") -> BackupResultDialog(
                title = "Unsupported Backup",
                message = "This backup was created by a newer version of Voice Journal and cannot be restored by this version."
            )
            joined.contains("sha-256 mismatch") || joined.contains("size mismatch") || joined.contains("integrity") -> BackupResultDialog(
                title = "Backup Verification Failed",
                message = "One or more files failed integrity verification. The restore has been cancelled to protect your existing data."
            )
            joined.contains("corrupted") || joined.contains("missing required file") || joined.contains("failed to parse") || joined.contains("invalid format_name") -> BackupResultDialog(
                title = "Backup Corrupted",
                message = "This backup file is incomplete, corrupted, or has been modified and cannot be restored."
            )
            exMsg.contains("enospc") || exMsg.contains("no space left") || exMsg.contains("storage") -> BackupResultDialog(
                title = "Insufficient Storage",
                message = "There isn't enough storage space available to complete the restore."
            )
            else -> BackupResultDialog(
                title = "Restore Failed",
                message = "The backup could not be restored. Your existing data has not been modified."
            )
        }
    }
}
