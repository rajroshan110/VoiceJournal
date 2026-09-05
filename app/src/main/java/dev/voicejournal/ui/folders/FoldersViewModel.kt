package dev.voicejournal.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import javax.inject.Inject

data class FolderItem(
    val tag: Tag,
    val noteCount: Int,
    val latestNoteDateMillis: Long = 0L,
    val previewNoteTitle: String? = null
)

data class FoldersUiState(
    val isLoading: Boolean = false,
    val folders: List<FolderItem> = emptyList(),
    val selectedFolder: Tag? = null,
    val notesInSelectedFolder: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val isCreateFolderDialogOpen: Boolean = false,
    val folderToRename: Tag? = null,
    val folderToMerge: Tag? = null,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState(isLoading = true))
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private var allEntries: List<JournalEntry> = emptyList()
    private var allTags: List<Tag> = emptyList()

    init {
        observePreferences()
        observeData()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesManager.folderIsGridView.collect { isGrid ->
                _uiState.update { it.copy(isGridView = isGrid) }
            }
        }
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.update { it.copy(timeFormat = format) }
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
                recalculateFolders()
            }
        }
    }

    private fun recalculateFolders() {
        val explicitFolderTags = allTags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }
        val entryFolderTags = allEntries.flatMap { entry ->
            entry.tags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }
        }

        val allFolderTagsByName = (explicitFolderTags + entryFolderTags)
            .distinctBy { it.id }
            .sortedBy { it.name }

        val folderItems = allFolderTagsByName.map { folderTag ->
            val matchingEntries = allEntries.filter { entry ->
                entry.tags.any {
                    (it.type == TagType.FOLDER || it.type == TagType.THING) &&
                            it.name.equals(folderTag.name, ignoreCase = true)
                }
            }
            val noteCount = matchingEntries.size
            val latestDate = matchingEntries.maxOfOrNull { it.createdAt } ?: 0L
            val previewTitle = matchingEntries.maxByOrNull { it.createdAt }?.title?.takeIf { it.isNotBlank() }
                ?: matchingEntries.maxByOrNull { it.createdAt }?.plainUserText?.take(30)

            FolderItem(
                tag = folderTag,
                noteCount = noteCount,
                latestNoteDateMillis = latestDate,
                previewNoteTitle = previewTitle
            )
        }

        val currentSelected = _uiState.value.selectedFolder
        val updatedNotesInSelected = if (currentSelected != null) {
            allEntries.filter { entry ->
                entry.tags.any {
                    (it.type == TagType.FOLDER || it.type == TagType.THING) &&
                            it.name.equals(currentSelected.name, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                folders = folderItems,
                notesInSelectedFolder = updatedNotesInSelected
            )
        }
    }

    fun selectFolder(folderTag: Tag?) {
        val notes = if (folderTag != null) {
            allEntries.filter { entry ->
                entry.tags.any {
                    (it.type == TagType.FOLDER || it.type == TagType.THING) &&
                            it.name.equals(folderTag.name, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
        _uiState.update { it.copy(selectedFolder = folderTag, notesInSelectedFolder = notes) }
    }

    fun createFolder(folderName: String) {
        val cleanName = folderName.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            repository.getOrCreateTag(cleanName, TagType.FOLDER)
            recalculateFolders()
            setCreateFolderDialogOpen(false)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        val nextMode = !_uiState.value.isGridView
        _uiState.update { it.copy(isGridView = nextMode) }
        viewModelScope.launch {
            userPreferencesManager.setFolderIsGridView(nextMode)
        }
    }

    fun setCreateFolderDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isCreateFolderDialogOpen = open) }
    }

    fun setFolderToRename(tag: Tag?) {
        _uiState.update { it.copy(folderToRename = tag) }
    }

    fun setFolderToMerge(tag: Tag?) {
        _uiState.update { it.copy(folderToMerge = tag) }
    }

    fun renameFolder(tag: Tag, newName: String) {
        viewModelScope.launch {
            repository.renameTag(tag.id, newName, TagType.FOLDER)
            recalculateFolders()
            setFolderToRename(null)
        }
    }

    fun mergeFolder(sourceTag: Tag, targetTag: Tag) {
        viewModelScope.launch {
            repository.mergeTags(sourceTag.id, targetTag.id)
            recalculateFolders()
            setFolderToMerge(null)
        }
    }
}
