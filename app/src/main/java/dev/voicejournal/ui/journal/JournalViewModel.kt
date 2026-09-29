package dev.voicejournal.ui.journal

import dev.voicejournal.domain.usecase.ExtractUnifiedTagsUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.PlayerState
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import dev.voicejournal.domain.usecase.DeleteEntryUseCase
import dev.voicejournal.domain.usecase.GetAllEntriesUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import android.net.Uri
import dev.voicejournal.data.backup.ImportManager
import dev.voicejournal.ui.journal.components.SortOption
import dev.voicejournal.ui.settings.BackupResultDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File
import javax.inject.Inject

enum class PlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Error
}

data class CardPlaybackState(
    val activeEntryId: Long? = null,
    val activeTrackId: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

sealed interface FeedState {
    data object Loading : FeedState
    data object EmptyGlobal : FeedState
    data object EmptyFiltered : FeedState
    data class Success(val entries: List<JournalEntry>) : FeedState
    data class Error(val message: String) : FeedState
}

data class FilterState(
    val selectedTags: Set<String> = emptySet(),
    val selectedPeople: Set<String> = emptySet(),
    val selectedMoods: Set<String> = emptySet()
)

data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedEntryIds: Set<Long> = emptySet()
)

data class JournalUiState(
    val feedState: FeedState = FeedState.Loading,
    val entries: List<JournalEntry> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val availablePeople: List<String> = emptyList(),
    val filterState: FilterState = FilterState(),
    val sortOption: SortOption = SortOption.MODIFIED_DESC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val permissionGranted: Boolean = true,
    val isRefreshing: Boolean = false,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isFolderEnabled: Boolean = false,
    val isNotesOrganisationEnabled: Boolean = false,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true,
    val isImporting: Boolean = false,
    val backupProgressText: String? = null,
    val backupResultDialog: BackupResultDialog? = null
)

private data class JournalPrefConfig(
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isFolderEnabled: Boolean = false,
    val isNotesOrganisationEnabled: Boolean = false,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true
)

private data class JournalStatusState(
    val searchActive: Boolean,
    val permission: Boolean,
    val refreshing: Boolean,
    val errorMsg: String?
)

private data class JournalBackupState(
    val isImporting: Boolean,
    val progressText: String?,
    val resultDialog: BackupResultDialog?
)


@HiltViewModel
class JournalViewModel internal constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase,
    private val journalRepository: JournalRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val importManager: ImportManager,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
        getAllEntriesUseCase: GetAllEntriesUseCase,
        getAllTagsUseCase: GetAllTagsUseCase,
        deleteEntryUseCase: DeleteEntryUseCase,
        extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase,
        journalRepository: JournalRepository,
        audioPlayerManager: AudioPlayerManager,
        userPreferencesManager: UserPreferencesManager,
        importManager: ImportManager
    ) : this(
        context = context,
        savedStateHandle = savedStateHandle,
        getAllEntriesUseCase = getAllEntriesUseCase,
        getAllTagsUseCase = getAllTagsUseCase,
        deleteEntryUseCase = deleteEntryUseCase,
        extractUnifiedTagsUseCase = extractUnifiedTagsUseCase,
        journalRepository = journalRepository,
        audioPlayerManager = audioPlayerManager,
        userPreferencesManager = userPreferencesManager,
        importManager = importManager,
        defaultDispatcher = Dispatchers.Default
    )

    private val _filterState = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)
    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>("search_query") ?: "")
    private val _isSearchActive = MutableStateFlow(false)
    private val _permissionGranted = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _isImporting = MutableStateFlow(false)
    private val _backupProgressText = MutableStateFlow<String?>(null)
    private val _backupResultDialog = MutableStateFlow<BackupResultDialog?>(null)

    private val _cardPlaybackState = MutableStateFlow(CardPlaybackState())
    val cardPlaybackState: StateFlow<CardPlaybackState> = _cardPlaybackState.asStateFlow()

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private fun findTrackId(entryId: Long?, audioPath: String?): String? {
        if (audioPath.isNullOrBlank()) return null
        val targetEntry = _uiState.value.entries.firstOrNull { it.id == entryId }
            ?: _uiState.value.entries.firstOrNull { entry -> entry.allAudioTracks.any { it.path == audioPath || it.id == audioPath } }
        return targetEntry?.allAudioTracks?.firstOrNull { it.path == audioPath || it.id == audioPath }?.id
            ?: targetEntry?.allAudioTracks?.firstOrNull()?.id
    }

    init {
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                val current = _cardPlaybackState.value
                when (state) {
                    is PlayerState.Playing -> {
                        val entryId = state.entryId ?: current.activeEntryId
                        val trackId = findTrackId(entryId, state.audioPath) ?: current.activeTrackId
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = entryId,
                            activeTrackId = trackId,
                            status = PlaybackStatus.Playing,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Paused -> {
                        val entryId = state.entryId ?: current.activeEntryId
                        val trackId = findTrackId(entryId, state.audioPath) ?: current.activeTrackId
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = entryId,
                            activeTrackId = trackId,
                            status = PlaybackStatus.Paused,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Ended -> {
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = null,
                            activeTrackId = null,
                            status = PlaybackStatus.Idle,
                            currentPositionMs = 0L
                        )
                    }
                    is PlayerState.Idle -> {
                        if (current.status != PlaybackStatus.Error) {
                            _cardPlaybackState.value = current.copy(
                                activeEntryId = null,
                                activeTrackId = null,
                                status = PlaybackStatus.Idle,
                                currentPositionMs = 0L
                            )
                        }
                    }
                    is PlayerState.Error -> {
                        _cardPlaybackState.value = current.copy(
                            status = PlaybackStatus.Error,
                            currentPositionMs = 0L,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            userPreferencesManager.sortOption.catch { }.collect { opt ->
                _sortOption.value = opt
            }
        }

        viewModelScope.launch {
            val rawEntriesFlow = getAllEntriesUseCase().catch { e ->
                _loadError.value = e.message ?: "Failed to load journal entries"
            }
            val rawTagsFlow = getAllTagsUseCase().catch { }

            val extractedDataFlow = combine(rawEntriesFlow, rawTagsFlow) { rawEntries, rawTags ->
                val extracted = extractUnifiedTagsUseCase(rawEntries, rawTags)
                Triple(rawEntries, extracted.allTags, extracted.allPeople)
            }.distinctUntilChanged().flowOn(defaultDispatcher)

            val prefConfigFlow = combine(
                combine(
                    userPreferencesManager.timeFormat,
                    userPreferencesManager.isFolderEnabled,
                    userPreferencesManager.isNotesOrganisationEnabled
                ) { tf, fe, noe -> Triple(tf, fe, noe) },
                combine(
                    userPreferencesManager.isTopicsEnabled,
                    userPreferencesManager.isPeopleEnabled,
                    userPreferencesManager.isMoodEnabled
                ) { te, pe, me -> Triple(te, pe, me) }
            ) { p1, p2 ->
                JournalPrefConfig(
                    timeFormat = p1.first,
                    isFolderEnabled = p1.second,
                    isNotesOrganisationEnabled = p1.third,
                    isTopicsEnabled = p2.first,
                    isPeopleEnabled = p2.second,
                    isMoodEnabled = p2.third
                )
            }

            val filteredDataFlow = combine(
                extractedDataFlow,
                _filterState,
                _sortOption,
                _searchQuery,
                prefConfigFlow
            ) { data, filters, sort, query, prefs ->
                val rawEntries = data.first
                val allTags = data.second
                val extractedPeople = data.third

                val filtered = rawEntries.filter { entry ->
                    val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"

                    val matchesQuery = query.isBlank() || textContent.contains(query, ignoreCase = true)

                    val matchesTags = !prefs.isTopicsEnabled || filters.selectedTags.isEmpty() ||
                            entry.tags.any { tag ->
                                tag.type == TagType.TOPIC &&
                                filters.selectedTags.any { it.equals(tag.name.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            }

                    val matchesPeople = !prefs.isPeopleEnabled || filters.selectedPeople.isEmpty() ||
                            entry.tags.any { tag ->
                                tag.type == TagType.PERSON &&
                                filters.selectedPeople.any { it.equals(tag.name.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            } ||
                            entry.people.any { p ->
                                filters.selectedPeople.any { it.equals(p.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            }

                    val matchesMoods = !prefs.isMoodEnabled || filters.selectedMoods.isEmpty() || (entry.mood in filters.selectedMoods)

                    matchesQuery && matchesTags && matchesPeople && matchesMoods
                }

                val sorted = when (sort) {
                    SortOption.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                    SortOption.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                    SortOption.MODIFIED_DESC -> filtered.sortedByDescending { it.updatedAt }
                    SortOption.MODIFIED_ASC -> filtered.sortedBy { it.updatedAt }
                }

                val hasAnyFilters = (prefs.isTopicsEnabled && filters.selectedTags.isNotEmpty()) ||
                        (prefs.isPeopleEnabled && filters.selectedPeople.isNotEmpty()) ||
                        (prefs.isMoodEnabled && filters.selectedMoods.isNotEmpty()) ||
                        query.isNotBlank()

                val feed = when {
                    rawEntries.isEmpty() -> FeedState.EmptyGlobal
                    sorted.isEmpty() && hasAnyFilters -> FeedState.EmptyFiltered
                    else -> FeedState.Success(sorted)
                }

                JournalUiState(
                    feedState = feed,
                    entries = sorted,
                    availableTags = allTags,
                    availablePeople = extractedPeople,
                    filterState = filters,
                    sortOption = sort,
                    searchQuery = query,
                    timeFormat = prefs.timeFormat,
                    isFolderEnabled = prefs.isFolderEnabled,
                    isNotesOrganisationEnabled = prefs.isNotesOrganisationEnabled,
                    isTopicsEnabled = prefs.isTopicsEnabled,
                    isPeopleEnabled = prefs.isPeopleEnabled,
                    isMoodEnabled = prefs.isMoodEnabled
                )
            }.distinctUntilChanged().flowOn(defaultDispatcher)

            val statusFlow = combine(
                _isSearchActive,
                _permissionGranted,
                _isRefreshing,
                _loadError
            ) { searchActive, permission, refreshing, errorMsg ->
                JournalStatusState(searchActive, permission, refreshing, errorMsg)
            }

            val backupFlow = combine(
                _isImporting,
                _backupProgressText,
                _backupResultDialog
            ) { isImporting, progressText, resultDialog ->
                JournalBackupState(isImporting, progressText, resultDialog)
            }

            combine(
                filteredDataFlow,
                statusFlow,
                backupFlow
            ) { baseState, status, backup ->
                val stateWithStatus = if (status.errorMsg != null) {
                    baseState.copy(
                        feedState = FeedState.Error(status.errorMsg),
                        isSearchActive = status.searchActive,
                        permissionGranted = status.permission,
                        isRefreshing = status.refreshing
                    )
                } else {
                    baseState.copy(
                        isSearchActive = status.searchActive,
                        permissionGranted = status.permission,
                        isRefreshing = status.refreshing
                    )
                }
                stateWithStatus.copy(
                    isImporting = backup.isImporting,
                    backupProgressText = backup.progressText,
                    backupResultDialog = backup.resultDialog
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun playTrack(entry: JournalEntry, track: AudioTrack) {
        val currentPlayback = _cardPlaybackState.value

        if (currentPlayback.activeEntryId == entry.id && currentPlayback.activeTrackId == track.id) {
            if (currentPlayback.status == PlaybackStatus.Playing) {
                audioPlayerManager.pause()
            } else if (currentPlayback.status == PlaybackStatus.Paused) {
                audioPlayerManager.resume()
            } else {
                startPlaybackForTrack(track, entry.id)
            }
        } else {
            audioPlayerManager.stop()
            startPlaybackForTrack(track, entry.id)
        }
    }

    private fun startPlaybackForTrack(track: AudioTrack, parentEntryId: Long) {
        val resolvedFile = MediaStorageManager.getAudioFile(context, track.path)
        val fileToPlay = if (track.path.isNotEmpty() && File(track.path).exists() && File(track.path).length() > 0L) {
            File(track.path)
        } else if (resolvedFile.exists() && resolvedFile.length() > 0L) {
            resolvedFile
        } else {
            null
        }

        if (fileToPlay == null || !fileToPlay.exists() || fileToPlay.length() == 0L) {
            _cardPlaybackState.value = CardPlaybackState(
                activeEntryId = parentEntryId,
                activeTrackId = track.id,
                status = PlaybackStatus.Error,
                errorMessage = "Audio track file unavailable"
            )
            return
        }

        if (fileToPlay.extension.equals("wav", ignoreCase = true)) {
            AudioFileRepair.repairWavFile(fileToPlay)
        }

        _cardPlaybackState.value = CardPlaybackState(
            activeEntryId = parentEntryId,
            activeTrackId = track.id,
            status = PlaybackStatus.Buffering,
            currentPositionMs = 0L
        )

        audioPlayerManager.play(fileToPlay.absolutePath, "Voice Note Track", parentEntryId)
    }

    fun clearPlaybackError() {
        if (_cardPlaybackState.value.status == PlaybackStatus.Error) {
            _cardPlaybackState.value = CardPlaybackState()
        }
        if (audioPlayerManager.playbackState.value is PlayerState.Error) {
            audioPlayerManager.clearError()
        }
    }

    fun seekTrackToFraction(entry: JournalEntry, track: AudioTrack, fraction: Float) {
        val targetPos = (track.durationMs * fraction).toLong()
        if (_cardPlaybackState.value.activeEntryId == entry.id && _cardPlaybackState.value.activeTrackId == track.id) {
            audioPlayerManager.seekTo(targetPos)
        }
    }

    fun toggleSearch() {
        val next = !_isSearchActive.value
        _isSearchActive.value = next
        if (!next) {
            savedStateHandle["search_query"] = ""
            _searchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        savedStateHandle["search_query"] = query
        _searchQuery.value = query
    }

    fun setSearchQuery(query: String) = updateSearchQuery(query)

    fun toggleTagFilter(tagName: String) {
        val cleanTag = tagName.trim().trimStart('#', '@').trim()
        val current = _filterState.value.selectedTags
        val existing = current.firstOrNull { it.equals(cleanTag, ignoreCase = true) }
        val next = if (existing != null) {
            current.filterNot { it.equals(cleanTag, ignoreCase = true) }.toSet()
        } else {
            current + cleanTag
        }
        _filterState.value = _filterState.value.copy(selectedTags = next)
    }

    fun setTagFilter(tag: String) = toggleTagFilter(tag)

    fun togglePersonFilter(person: String) {
        val cleanPerson = person.trim().trimStart('#', '@').trim()
        val current = _filterState.value.selectedPeople
        val existing = current.firstOrNull { it.equals(cleanPerson, ignoreCase = true) }
        val next = if (existing != null) {
            current.filterNot { it.equals(cleanPerson, ignoreCase = true) }.toSet()
        } else {
            current + cleanPerson
        }
        _filterState.value = _filterState.value.copy(selectedPeople = next)
    }

    fun setPersonFilter(person: String) = togglePersonFilter(person)

    fun toggleMoodFilter(mood: String) {
        val current = _filterState.value.selectedMoods
        val next = if (mood in current) current - mood else current + mood
        _filterState.value = _filterState.value.copy(selectedMoods = next)
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
        _searchQuery.value = ""
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        viewModelScope.launch {
            userPreferencesManager.setSortOption(option)
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = null
            val startTime = System.currentTimeMillis()
            try {
                journalRepository.refreshAndHealData()
            } catch (e: Exception) {
                if (_uiState.value.entries.isEmpty()) {
                    _loadError.value = e.message ?: "Failed to refresh data"
                }
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = 400L - elapsed
                if (remaining > 0L) {
                    kotlinx.coroutines.delay(remaining)
                }
                _isRefreshing.value = false
            }
        }
    }

    fun retryFeedLoad() {
        _loadError.value = null
    }

    fun setPermissionGranted(granted: Boolean) {
        _permissionGranted.value = granted
    }

    fun toggleEntrySelection(entryId: Long) {
        val current = _selectionState.value.selectedEntryIds
        val next = if (entryId in current) current - entryId else current + entryId
        _selectionState.value = SelectionState(
            isSelectionMode = next.isNotEmpty(),
            selectedEntryIds = next
        )
    }

    fun clearSelection() {
        _selectionState.value = SelectionState()
    }

    fun deleteSelectedEntries() {
        val idsToDelete = _selectionState.value.selectedEntryIds
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            val entriesToDelete = _uiState.value.entries.filter { it.id in idsToDelete }
            entriesToDelete.forEach { entry ->
                deleteEntryUseCase(entry.id)
            }
            clearSelection()
        }
    }

    fun deleteSingleEntry(entryId: Long) {
        viewModelScope.launch {
            deleteEntryUseCase(entryId)
        }
    }

    fun archiveSelectedEntries() {
        val idsToArchive = _selectionState.value.selectedEntryIds
        if (idsToArchive.isEmpty()) return

        viewModelScope.launch {
            val entriesToArchive = _uiState.value.entries.filter { it.id in idsToArchive }
            entriesToArchive.forEach { entry ->
                journalRepository.archiveEntry(entry.id)
            }
            clearSelection()
        }
    }

    fun batchCategorizeSelectedEntries(
        targetFolder: String?,
        tagsToAssign: List<Tag>,
        tagsToRemove: List<Tag>,
        folderToRemove: String?
    ) {
        val ids = _selectionState.value.selectedEntryIds
        if (ids.isEmpty()) return

        viewModelScope.launch {
            journalRepository.batchCategorizeEntries(
                ids = ids.toList(),
                targetFolder = targetFolder,
                tagsToAssign = tagsToAssign,
                tagsToRemove = tagsToRemove,
                folderToRemove = folderToRemove
            )
            clearSelection()
        }
    }

    fun stopAudioOnLeave(navigatingToEntryId: Long? = null) {
        val activeId = _cardPlaybackState.value.activeEntryId
        if (navigatingToEntryId == null || activeId == null || navigatingToEntryId != activeId) {
            audioPlayerManager.stop()
            _cardPlaybackState.value = CardPlaybackState()
        }
    }

    fun importBackupFromUri(sourceUri: android.net.Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _backupProgressText.value = "Validating backup…"
            _backupResultDialog.value = null

            val result = importManager.importFromUri(sourceUri) { progress ->
                _backupProgressText.value = progress
            }
            _isImporting.value = false
            _backupProgressText.value = null
            if (result.success) {
                _backupResultDialog.value = BackupResultDialog(
                    title = "Backup Restored",
                    message = "Your backup has been restored successfully."
                )
            } else {
                _backupResultDialog.value = mapImportErrorToDialog(result.validationErrors, null)
            }
        }
    }

    fun dismissBackupResultDialog() {
        _backupResultDialog.value = null
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
