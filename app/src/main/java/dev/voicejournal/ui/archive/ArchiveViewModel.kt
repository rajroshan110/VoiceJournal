package dev.voicejournal.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val isLoading: Boolean = false,
    val entries: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
)

data class ArchiveSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedIds.size
}

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState(isLoading = true))
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val _selectionState = MutableStateFlow(ArchiveSelectionState())
    val selectionState: StateFlow<ArchiveSelectionState> = _selectionState.asStateFlow()

    private var allArchivedEntries: List<JournalEntry> = emptyList()

    init {
        observeArchiveData()
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.update { it.copy(timeFormat = format) }
            }
        }
    }

    private fun observeArchiveData() {
        viewModelScope.launch {
            repository.getArchiveEntries().collect { list ->
                allArchivedEntries = list
                filterAndEmit()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterAndEmit()
    }

    private fun filterAndEmit() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allArchivedEntries
        } else {
            allArchivedEntries.filter { entry ->
                (entry.title?.lowercase()?.contains(query) == true) ||
                (entry.plainUserText?.lowercase()?.contains(query) == true) ||
                (entry.transcript?.lowercase()?.contains(query) == true)
            }
        }
        _uiState.update { it.copy(isLoading = false, entries = filtered) }
    }

    fun toggleSelection(id: Long) {
        val current = _selectionState.value.selectedIds
        val newSelected = if (id in current) current - id else current + id
        _selectionState.value = ArchiveSelectionState(
            isSelectionMode = newSelected.isNotEmpty(),
            selectedIds = newSelected
        )
    }

    fun toggleSelectionMode(id: Long) {
        val current = _selectionState.value
        if (current.isSelectionMode && current.selectedIds.contains(id) && current.selectedIds.size == 1) {
            _selectionState.value = ArchiveSelectionState(isSelectionMode = false, selectedIds = emptySet())
        } else {
            _selectionState.value = _selectionState.value.copy(
                isSelectionMode = true,
                selectedIds = current.selectedIds + id
            )
        }
    }

    fun clearSelection() {
        _selectionState.value = ArchiveSelectionState(isSelectionMode = false, selectedIds = emptySet())
    }

    fun unarchiveSelectedEntries() {
        val ids = _selectionState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.unarchiveEntries(ids)
            clearSelection()
        }
    }

    fun deleteSelectedEntries() {
        val ids = _selectionState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveAllToTrash(ids)
            clearSelection()
        }
    }
}
