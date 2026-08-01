package dev.voicejournal.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.usecase.EmptyTrashUseCase
import dev.voicejournal.domain.usecase.GetTrashEntriesUseCase
import dev.voicejournal.domain.usecase.PermanentlyDeleteEntryUseCase
import dev.voicejournal.domain.usecase.PurgeExpiredTrashUseCase
import dev.voicejournal.domain.usecase.RestoreEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val allTrashEntries: List<JournalEntry> = emptyList(),
    val expiringSoonEntries: List<JournalEntry> = emptyList(),
    val deletingLaterEntries: List<JournalEntry> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedEntryIds: Set<Long> = emptySet(),
    val isClearTrashDialogOpen: Boolean = false,
    val isDeleteSelectedDialogOpen: Boolean = false,
    val isRestoreDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrashEntriesUseCase: GetTrashEntriesUseCase,
    private val restoreEntryUseCase: RestoreEntryUseCase,
    private val permanentlyDeleteEntryUseCase: PermanentlyDeleteEntryUseCase,
    private val emptyTrashUseCase: EmptyTrashUseCase,
    private val purgeExpiredTrashUseCase: PurgeExpiredTrashUseCase,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        // Auto purge notes in trash older than 7 days on initialization
        viewModelScope.launch {
            purgeExpiredTrashUseCase(retentionDays = 7)
        }

        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.update { it.copy(timeFormat = format) }
            }
        }

        // Observe Trash flow
        viewModelScope.launch {
            getTrashEntriesUseCase().collect { entries ->
                val expiringSoon = entries.filter { it.isExpiringSoon }
                val deletingLater = entries.filter { !it.isExpiringSoon }

                _uiState.value = _uiState.value.copy(
                    allTrashEntries = entries,
                    expiringSoonEntries = expiringSoon,
                    deletingLaterEntries = deletingLater,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSelectionMode(initialSelectedId: Long? = null) {
        val current = _uiState.value
        val isCurrentlySelectionMode = current.isSelectionMode
        if (!isCurrentlySelectionMode) {
            _uiState.value = current.copy(
                isSelectionMode = true,
                selectedEntryIds = if (initialSelectedId != null) setOf(initialSelectedId) else emptySet()
            )
        } else {
            if (initialSelectedId != null) {
                toggleEntrySelection(initialSelectedId)
            } else {
                clearSelection()
            }
        }
    }

    fun toggleEntrySelection(entryId: Long) {
        val current = _uiState.value
        if (!current.isSelectionMode) {
            toggleSelectionMode(entryId)
            return
        }

        val selected = current.selectedEntryIds
        val nextSelected = if (entryId in selected) selected - entryId else selected + entryId

        if (nextSelected.isEmpty()) {
            _uiState.value = current.copy(isSelectionMode = false, selectedEntryIds = emptySet())
        } else {
            _uiState.value = current.copy(selectedEntryIds = nextSelected)
        }
    }

    fun selectAll() {
        val current = _uiState.value
        val allIds = current.allTrashEntries.map { it.id }.toSet()
        _uiState.value = current.copy(
            isSelectionMode = true,
            selectedEntryIds = allIds
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedEntryIds = emptySet(),
            isRestoreDialogOpen = false,
            isDeleteSelectedDialogOpen = false
        )
    }

    fun setClearTrashDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isClearTrashDialogOpen = isOpen)
    }

    fun setDeleteSelectedDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isDeleteSelectedDialogOpen = isOpen)
    }

    fun setRestoreDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isRestoreDialogOpen = isOpen)
    }

    fun restoreSelectedEntries() {
        val selected = _uiState.value.selectedEntryIds
        if (selected.isEmpty()) return

        viewModelScope.launch {
            restoreEntryUseCase.restoreAll(selected.toList())
            setRestoreDialogOpen(false)
            clearSelection()
        }
    }

    fun restoreSingleEntry(id: Long) {
        viewModelScope.launch {
            restoreEntryUseCase(id)
        }
    }

    fun permanentlyDeleteSelectedEntries() {
        val selected = _uiState.value.selectedEntryIds
        if (selected.isEmpty()) return

        viewModelScope.launch {
            permanentlyDeleteEntryUseCase.deleteAll(selected.toList())
            setDeleteSelectedDialogOpen(false)
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            emptyTrashUseCase()
            setClearTrashDialogOpen(false)
            clearSelection()
        }
    }
}
