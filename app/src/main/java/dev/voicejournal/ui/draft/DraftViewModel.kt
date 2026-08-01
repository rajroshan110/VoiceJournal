package dev.voicejournal.ui.draft

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

data class DraftUiState(
    val isLoading: Boolean = false,
    val entries: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
)

data class DraftSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedIds.size
}

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftUiState(isLoading = true))
    val uiState: StateFlow<DraftUiState> = _uiState.asStateFlow()

    private val _selectionState = MutableStateFlow(DraftSelectionState())
    val selectionState: StateFlow<DraftSelectionState> = _selectionState.asStateFlow()

    private var allDraftEntries: List<JournalEntry> = emptyList()

    init {
        observeDraftData()
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.update { it.copy(timeFormat = format) }
            }
        }
    }

    private fun observeDraftData() {
        viewModelScope.launch {
            repository.getDraftEntries().collect { list ->
                allDraftEntries = list
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
            allDraftEntries
        } else {
            allDraftEntries.filter { entry ->
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
        _selectionState.value = DraftSelectionState(
            isSelectionMode = newSelected.isNotEmpty(),
            selectedIds = newSelected
        )
    }

    fun toggleSelectionMode(id: Long) {
        val current = _selectionState.value
        if (current.isSelectionMode && current.selectedIds.contains(id) && current.selectedIds.size == 1) {
            _selectionState.value = DraftSelectionState(isSelectionMode = false, selectedIds = emptySet())
        } else {
            _selectionState.value = _selectionState.value.copy(
                isSelectionMode = true,
                selectedIds = current.selectedIds + id
            )
        }
    }

    fun clearSelection() {
        _selectionState.value = DraftSelectionState(isSelectionMode = false, selectedIds = emptySet())
    }

    fun deleteSelectedDrafts() {
        val ids = _selectionState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveAllToTrash(ids)
            clearSelection()
        }
    }
}
