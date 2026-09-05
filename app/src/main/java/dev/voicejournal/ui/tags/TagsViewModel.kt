package dev.voicejournal.ui.tags

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
import dev.voicejournal.ui.journal.CardPlaybackState
import dev.voicejournal.ui.journal.PlaybackStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File
import javax.inject.Inject

data class TagsUiState(
    val isLoading: Boolean = false,
    val selectedCategory: TagType? = null,
    val selectedTag: Tag? = null,
    val topicTags: List<TagItem> = emptyList(),
    val personTags: List<TagItem> = emptyList(),
    val notesInSelectedTag: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val isCreateTagDialogOpen: Boolean = false,
    val tagToRename: Tag? = null,
    val tagToMerge: Tag? = null,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: JournalRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState(isLoading = true))
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private val _cardPlaybackState = MutableStateFlow(CardPlaybackState())
    val cardPlaybackState: StateFlow<CardPlaybackState> = _cardPlaybackState.asStateFlow()

    private var allEntries: List<JournalEntry> = emptyList()
    private var allTags: List<Tag> = emptyList()

    init {
        observePreferences()
        observeData()
        observeAudioPlayback()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesManager.tagIsGridView.collect { isGrid ->
                _uiState.update { it.copy(isGridView = isGrid) }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.update { it.copy(timeFormat = format) }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isTopicsEnabled.collect { enabled ->
                _uiState.update { state ->
                    val shouldResetCategory = !enabled && state.selectedCategory == TagType.TOPIC
                    state.copy(
                        isTopicsEnabled = enabled,
                        selectedCategory = if (shouldResetCategory) null else state.selectedCategory,
                        selectedTag = if (shouldResetCategory) null else state.selectedTag
                    )
                }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isPeopleEnabled.collect { enabled ->
                _uiState.update { state ->
                    val shouldResetCategory = !enabled && state.selectedCategory == TagType.PERSON
                    state.copy(
                        isPeopleEnabled = enabled,
                        selectedCategory = if (shouldResetCategory) null else state.selectedCategory,
                        selectedTag = if (shouldResetCategory) null else state.selectedTag
                    )
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getAllEntries(),
                repository.getAllTags()
            ) { entries, tags ->
                Pair(entries, tags)
            }.catch {
                _uiState.update { state -> state.copy(isLoading = false) }
            }.collect { (entries, tags) ->
                allEntries = entries
                allTags = tags
                recalculateTags()
            }
        }
    }

    private fun recalculateTags() {
        // Topics
        val explicitTopics = allTags.filter { it.type == TagType.TOPIC }
        val entryTopics = allEntries.flatMap { entry -> entry.tags.filter { it.type == TagType.TOPIC } }
        val allTopicsByName = (explicitTopics + entryTopics)
            .distinctBy { it.id }
            .sortedBy { it.name }

        val topicItems = allTopicsByName.map { tag ->
            val matchingEntries = allEntries.filter { entry ->
                entry.tags.any { it.type == TagType.TOPIC && it.name.equals(tag.name, ignoreCase = true) }
            }
            val noteCount = matchingEntries.size
            val latestDate = matchingEntries.maxOfOrNull { it.createdAt } ?: 0L
            val previewTitle = matchingEntries.maxByOrNull { it.createdAt }?.title?.takeIf { it.isNotBlank() }
                ?: matchingEntries.maxByOrNull { it.createdAt }?.plainUserText?.take(30)

            TagItem(
                tag = tag,
                noteCount = noteCount,
                latestNoteDateMillis = latestDate,
                previewNoteTitle = previewTitle
            )
        }

        // People
        val explicitPeople = allTags.filter { it.type == TagType.PERSON }
        val entryPeople = allEntries.flatMap { entry -> entry.tags.filter { it.type == TagType.PERSON } }
        val allPeopleByName = (explicitPeople + entryPeople)
            .distinctBy { it.id }
            .sortedBy { it.name }

        val personItems = allPeopleByName.map { tag ->
            val matchingEntries = allEntries.filter { entry ->
                entry.tags.any { it.type == TagType.PERSON && it.name.equals(tag.name, ignoreCase = true) }
            }
            val noteCount = matchingEntries.size
            val latestDate = matchingEntries.maxOfOrNull { it.createdAt } ?: 0L
            val previewTitle = matchingEntries.maxByOrNull { it.createdAt }?.title?.takeIf { it.isNotBlank() }
                ?: matchingEntries.maxByOrNull { it.createdAt }?.plainUserText?.take(30)

            TagItem(
                tag = tag,
                noteCount = noteCount,
                latestNoteDateMillis = latestDate,
                previewNoteTitle = previewTitle
            )
        }

        val currentSelected = _uiState.value.selectedTag
        val updatedNotesInSelected = if (currentSelected != null) {
            allEntries.filter { entry ->
                entry.tags.any { it.type == currentSelected.type && it.name.equals(currentSelected.name, ignoreCase = true) }
            }
        } else {
            emptyList()
        }

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                topicTags = topicItems,
                personTags = personItems,
                notesInSelectedTag = updatedNotesInSelected
            )
        }
    }

    private fun observeAudioPlayback() {
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                val current = _cardPlaybackState.value
                when (state) {
                    is PlayerState.Playing -> {
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = state.entryId ?: current.activeEntryId,
                            status = PlaybackStatus.Playing,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Paused -> {
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = state.entryId ?: current.activeEntryId,
                            status = PlaybackStatus.Paused,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Ended -> {
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = null,
                            status = PlaybackStatus.Idle,
                            currentPositionMs = 0L
                        )
                    }
                    is PlayerState.Idle -> {
                        if (current.status != PlaybackStatus.Error) {
                            _cardPlaybackState.value = current.copy(
                                activeEntryId = null,
                                status = PlaybackStatus.Idle,
                                currentPositionMs = 0L
                            )
                        }
                    }
                    is PlayerState.Error -> {
                        _cardPlaybackState.value = current.copy(
                            status = PlaybackStatus.Error,
                            currentPositionMs = 0L
                        )
                    }
                }
            }
        }
    }

    fun selectCategory(category: TagType?) {
        _uiState.update { it.copy(selectedCategory = category, selectedTag = null, searchQuery = "") }
    }

    fun selectTag(tag: Tag?) {
        val notes = if (tag != null) {
            allEntries.filter { entry ->
                entry.tags.any { it.type == tag.type && it.name.equals(tag.name, ignoreCase = true) }
            }
        } else {
            emptyList()
        }
        _uiState.update { it.copy(selectedTag = tag, notesInSelectedTag = notes) }
    }

    fun createTag(tagName: String, tagType: TagType) {
        val cleanName = tagName.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            repository.getOrCreateTag(cleanName, tagType)
            recalculateTags()
            setCreateTagDialogOpen(false)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        val nextMode = !_uiState.value.isGridView
        _uiState.update { it.copy(isGridView = nextMode) }
        viewModelScope.launch {
            userPreferencesManager.setTagIsGridView(nextMode)
        }
    }

    fun setCreateTagDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isCreateTagDialogOpen = open) }
    }

    fun setTagToRename(tag: Tag?) {
        _uiState.update { it.copy(tagToRename = tag) }
    }

    fun setTagToMerge(tag: Tag?) {
        _uiState.update { it.copy(tagToMerge = tag) }
    }

    fun renameTag(tag: Tag, newName: String) {
        viewModelScope.launch {
            repository.renameTag(tag.id, newName, tag.type)
            recalculateTags()
            setTagToRename(null)
        }
    }

    fun mergeTags(sourceTag: Tag, targetTag: Tag) {
        viewModelScope.launch {
            repository.mergeTags(sourceTag.id, targetTag.id)
            recalculateTags()
            setTagToMerge(null)
        }
    }

    fun playTrack(entry: JournalEntry, track: AudioTrack) {
        val current = _cardPlaybackState.value
        val isSameTrack = current.activeEntryId == entry.id && current.activeTrackId == track.id
        if (isSameTrack) {
            if (current.status == PlaybackStatus.Playing) {
                audioPlayerManager.pause()
            } else if (current.status == PlaybackStatus.Paused) {
                audioPlayerManager.resume()
            } else {
                startPlaybackForTrack(entry, track)
            }
        } else {
            audioPlayerManager.stop()
            startPlaybackForTrack(entry, track)
        }
    }

    private fun startPlaybackForTrack(entry: JournalEntry, track: AudioTrack) {
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
                activeEntryId = entry.id,
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
            activeEntryId = entry.id,
            activeTrackId = track.id,
            status = PlaybackStatus.Buffering,
            currentPositionMs = 0L
        )

        audioPlayerManager.play(fileToPlay.absolutePath, entry.title ?: "Voice Note Playback", entry.id)
    }

    fun clearPlaybackError() {
        if (_cardPlaybackState.value.status == PlaybackStatus.Error) {
            _cardPlaybackState.value = _cardPlaybackState.value.copy(
                status = PlaybackStatus.Idle,
                errorMessage = null
            )
        }
    }

    fun seekTrackToFraction(entry: JournalEntry, track: AudioTrack, fraction: Float) {
        val duration = track.durationMs.coerceAtLeast(1000L)
        val targetMs = (duration * fraction).toLong()
        if (_cardPlaybackState.value.activeEntryId == entry.id && _cardPlaybackState.value.activeTrackId == track.id) {
            audioPlayerManager.seekTo(targetMs)
        }
    }

    fun stopAudioOnLeave() {
        audioPlayerManager.stop()
        _cardPlaybackState.value = CardPlaybackState()
    }
}
