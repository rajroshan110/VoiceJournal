package dev.voicejournal.ui.tags

import dev.voicejournal.domain.usecase.ExtractUnifiedTagsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val repository: JournalRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState(isLoading = true))
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private var allEntries: List<JournalEntry> = emptyList()
    private var allTags: List<Tag> = emptyList()

    init {
        observePreferences()
        observeData()
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
        val extractedData = extractUnifiedTagsUseCase(allEntries, allTags)
        
        // Topics
        val allTopics = extractedData.allTopics

        val topicItems = allTopics.map { tag ->
            val matchingEntries = allEntries.filter { entry ->
                entry.tags.any { it.type == TagType.TOPIC && it.name.equals(tag.name, ignoreCase = true) }
            }
            val noteCount = matchingEntries.size
            val latestDate = matchingEntries.maxOfOrNull { it.createdAt } ?: 0L
            
            val noteWithText = matchingEntries.sortedByDescending { it.createdAt }.firstOrNull { 
                !it.title.isNullOrBlank() || !it.plainUserText.isNullOrBlank() 
            }
            val previewTitle = if (matchingEntries.isEmpty()) {
                "Empty tag"
            } else if (noteWithText != null) {
                noteWithText.title?.takeIf { it.isNotBlank() } ?: noteWithText.plainUserText?.takeIf { it.isNotBlank() }?.take(30)
            } else {
                "Audio Tracks"
            }

            TagItem(
                tag = tag,
                noteCount = noteCount,
                latestNoteDateMillis = latestDate,
                previewNoteTitle = previewTitle
            )
        }

        // People
        val allPeople = extractedData.allPersonTags

        val personItems = allPeople.map { tag ->
            val matchingEntries = allEntries.filter { entry ->
                entry.tags.any { it.type == TagType.PERSON && it.name.equals(tag.name, ignoreCase = true) } ||
                entry.people.any { it.equals(tag.name, ignoreCase = true) }
            }
            val noteCount = matchingEntries.size
            val latestDate = matchingEntries.maxOfOrNull { it.createdAt } ?: 0L
            
            val noteWithText = matchingEntries.sortedByDescending { it.createdAt }.firstOrNull { 
                !it.title.isNullOrBlank() || !it.plainUserText.isNullOrBlank() 
            }
            val previewTitle = if (matchingEntries.isEmpty()) {
                "Empty tag"
            } else if (noteWithText != null) {
                noteWithText.title?.takeIf { it.isNotBlank() } ?: noteWithText.plainUserText?.takeIf { it.isNotBlank() }?.take(30)
            } else {
                "Audio Tracks"
            }

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
                if (currentSelected.type == TagType.PERSON) {
                    entry.tags.any { it.type == TagType.PERSON && it.name.equals(currentSelected.name, ignoreCase = true) } ||
                    entry.people.any { it.equals(currentSelected.name, ignoreCase = true) }
                } else {
                    entry.tags.any { it.type == TagType.TOPIC && it.name.equals(currentSelected.name, ignoreCase = true) }
                }
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

    fun selectCategory(category: TagType?) {
        _uiState.update { it.copy(selectedCategory = category, selectedTag = null, searchQuery = "") }
    }

    fun selectTag(tag: Tag?) {
        val notes = if (tag != null) {
            allEntries.filter { entry ->
                if (tag.type == TagType.PERSON) {
                    entry.tags.any { it.type == TagType.PERSON && it.name.equals(tag.name, ignoreCase = true) } ||
                    entry.people.any { it.equals(tag.name, ignoreCase = true) }
                } else {
                    entry.tags.any { it.type == TagType.TOPIC && it.name.equals(tag.name, ignoreCase = true) }
                }
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
}
