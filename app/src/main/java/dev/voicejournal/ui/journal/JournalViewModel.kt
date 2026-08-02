package dev.voicejournal.ui.journal

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
import dev.voicejournal.ui.journal.components.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val currentPositionMs: Long = 0L,
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
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val journalRepository: JournalRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)
    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>("search_query") ?: "")
    private val _isSearchActive = MutableStateFlow(false)
    private val _permissionGranted = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _loadError = MutableStateFlow<String?>(null)

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
                        _loadError.value = state.message
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
                val personTagNames = rawTags.filter { it.type == TagType.PERSON }.map { it.name.removePrefix("@") }
                val entryPersonTags = rawEntries.flatMap { entry ->
                    entry.tags.filter { it.type == TagType.PERSON }.map { it.name.removePrefix("@") }
                }
                val textMentions = rawEntries.flatMap { entry ->
                    val text = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
                    Regex("@\\w+").findAll(text).map { it.value.removePrefix("@") }.toList()
                }
                val extractedPeople = (personTagNames + entryPersonTags + textMentions)
                    .distinct()
                    .filter { it.isNotBlank() }
                    .sorted()

                val extractedHashtags = rawEntries.flatMap { entry ->
                    val text = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
                    Regex("#\\w+").findAll(text).map { it.value.removePrefix("#") }.toList()
                }.distinct().filter { it.isNotBlank() }

                val topicTagNames = rawTags.filter { it.type == TagType.TOPIC }.map { it.name.removePrefix("#") }
                val allTagNames = (topicTagNames + extractedHashtags).distinct()
                val allTags = allTagNames.mapIndexed { i, name -> Tag(i.toLong(), name) }

                Triple(rawEntries, allTags, extractedPeople)
            }.distinctUntilChanged().flowOn(Dispatchers.Default)

            val filteredDataFlow = combine(
                extractedDataFlow,
                _filterState,
                _sortOption,
                _searchQuery,
                userPreferencesManager.timeFormat
            ) { data, filters, sort, query, timeFormatPref ->
                val rawEntries = data.first
                val allTags = data.second
                val extractedPeople = data.third

                val filtered = rawEntries.filter { entry ->
                    val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"

                    val matchesQuery = query.isBlank() || textContent.contains(query, ignoreCase = true)

                    val matchesTags = filters.selectedTags.isEmpty() ||
                            entry.tags.any { (it.type == TagType.TOPIC || it.type == TagType.THING) && it.name.removePrefix("#") in filters.selectedTags } ||
                            filters.selectedTags.any { tag -> textContent.contains("#$tag", ignoreCase = true) }

                    val matchesPeople = filters.selectedPeople.isEmpty() ||
                            entry.tags.any { it.type == TagType.PERSON && (it.name.removePrefix("@") in filters.selectedPeople || it.name in filters.selectedPeople) } ||
                            entry.people.any { it.removePrefix("@") in filters.selectedPeople } ||
                            filters.selectedPeople.any { person ->
                                textContent.contains("@$person", ignoreCase = true)
                            }

                    val matchesMoods = filters.selectedMoods.isEmpty() || (entry.mood in filters.selectedMoods)

                    matchesQuery && matchesTags && matchesPeople && matchesMoods
                }

                val sorted = when (sort) {
                    SortOption.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                    SortOption.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                    SortOption.MODIFIED_DESC -> filtered.sortedByDescending { it.updatedAt }
                    SortOption.MODIFIED_ASC -> filtered.sortedBy { it.updatedAt }
                }

                val hasAnyFilters = filters.selectedTags.isNotEmpty() ||
                        filters.selectedPeople.isNotEmpty() ||
                        filters.selectedMoods.isNotEmpty() ||
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
                    timeFormat = timeFormatPref
                )
            }.distinctUntilChanged().flowOn(Dispatchers.Default)

            combine(
                filteredDataFlow,
                _isSearchActive,
                _permissionGranted,
                _isRefreshing,
                _loadError
            ) { baseState, searchActive, permission, refreshing, errorMsg ->
                if (errorMsg != null) {
                    baseState.copy(
                        feedState = FeedState.Error(errorMsg),
                        isSearchActive = searchActive,
                        permissionGranted = permission,
                        isRefreshing = refreshing
                    )
                } else {
                    baseState.copy(
                        isSearchActive = searchActive,
                        permissionGranted = permission,
                        isRefreshing = refreshing
                    )
                }
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
        val path = track.path
        if (path.isEmpty() || !File(path).exists()) {
            _cardPlaybackState.value = CardPlaybackState(
                activeEntryId = parentEntryId,
                activeTrackId = track.id,
                status = PlaybackStatus.Error,
                errorMessage = "Audio track file unavailable"
            )
            return
        }

        _cardPlaybackState.value = CardPlaybackState(
            activeEntryId = parentEntryId,
            activeTrackId = track.id,
            status = PlaybackStatus.Buffering,
            currentPositionMs = 0L
        )

        audioPlayerManager.play(path, "Voice Note Track", parentEntryId)
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
        val cleanTag = tagName.removePrefix("#")
        val current = _filterState.value.selectedTags
        val next = if (cleanTag in current) current - cleanTag else current + cleanTag
        _filterState.value = _filterState.value.copy(selectedTags = next)
    }

    fun setTagFilter(tag: String) = toggleTagFilter(tag)

    fun togglePersonFilter(person: String) {
        val cleanPerson = person.removePrefix("@")
        val current = _filterState.value.selectedPeople
        val next = if (cleanPerson in current) current - cleanPerson else current + cleanPerson
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
            kotlinx.coroutines.delay(400)
            _isRefreshing.value = false
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
}
