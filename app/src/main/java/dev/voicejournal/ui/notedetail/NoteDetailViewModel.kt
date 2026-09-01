package dev.voicejournal.ui.notedetail

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.AudioRecorderManager
import dev.voicejournal.audio.PlayerState
import dev.voicejournal.audio.RecordingState
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.EntryImage
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.usecase.DeleteEntryUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import dev.voicejournal.domain.usecase.GetEntryByIdUseCase
import dev.voicejournal.domain.usecase.SaveEntryUseCase
import dev.voicejournal.transcription.WhisperManager
import dev.voicejournal.domain.usecase.GenerateTranscriptUseCase
import dev.voicejournal.transcription.AudioFileResolver
import dev.voicejournal.transcription.engine.SpeechToTextEngine
import dev.voicejournal.ui.notedetail.components.MicFabState
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.serializer.RichTextHtmlSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File
import javax.inject.Inject

data class TrackSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedTrackIds: Set<String> = emptySet(),
    val selectedImagePaths: Set<String> = emptySet()
) {
    val totalSelectedCount: Int get() = selectedTrackIds.size + selectedImagePaths.size
}

data class NoteSnapshot(
    val titleText: String = "",
    val userHtml: String = "",
    val selectedMood: String = "😊",
    val attachedImages: List<String> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val audioTrackPaths: List<String> = emptyList()
)

data class NoteDetailUiState(
    val entryId: Long = -1L,
    val entry: JournalEntry? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val micFabState: MicFabState = MicFabState.IDLE,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playingTrackId: String? = null,
    val currentPositionMs: Long = 0L,
    val transcriptExpanded: Boolean = false,
    val expandedTrackId: String? = null,
    val titleText: String = "",
    val userText: String = "",
    val attachedImages: List<String> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val allAvailableTags: List<Tag> = emptyList(),
    val selectedMood: String = "😊",
    val audioFormat: AudioFormat = AudioFormat.WAV_16KHZ,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isMarkdownEnabled: Boolean = false,
    val isSpeechToTextEnabled: Boolean = true,
    val isFolderEnabled: Boolean = true,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true,
    val audioTracks: List<AudioTrack> = emptyList(),
    val isTranscribing: Boolean = false,
    val transcribingTrackId: String? = null,
    val showTranscriptConfirmTrack: AudioTrack? = null,
    val showModelDownloadPromptTrack: AudioTrack? = null,
    val isModelDownloaded: Boolean = false,
    val modelDownloadProgress: Float? = null,
    val hasUnsavedChanges: Boolean = false,
    val isDraft: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val saveEntryUseCase: SaveEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val audioRecorderManager: AudioRecorderManager,
    private val audioPlayerManager: AudioPlayerManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val whisperManager: WhisperManager,
    private val speechToTextEngine: SpeechToTextEngine,
    private val generateTranscriptUseCase: GenerateTranscriptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoteDetailUiState(
            isFolderEnabled = userPreferencesManager.isFolderEnabledState.value,
            isTopicsEnabled = userPreferencesManager.isTopicsEnabledState.value,
            isPeopleEnabled = userPreferencesManager.isPeopleEnabledState.value,
            isMoodEnabled = userPreferencesManager.isMoodEnabledState.value
        )
    )
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private val _trackSelectionState = MutableStateFlow(TrackSelectionState())
    val trackSelectionState: StateFlow<TrackSelectionState> = _trackSelectionState.asStateFlow()

    val richTextState = RichTextState()

    private var pausedRecordingAccumulatedMs: Long = 0L
    private var initialSnapshot: NoteSnapshot = NoteSnapshot()

    private val pendingFileDeletions = mutableSetOf<String>()
    private val pendingFileAdditions = mutableSetOf<String>()
    @Volatile private var isSaving = false

    private var isInitialized = false
    private var loadedEntryId: Long? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            userPreferencesManager.timeFormat.collect { format ->
                _uiState.value = _uiState.value.copy(timeFormat = format)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isMarkdownEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isMarkdownEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isSpeechToTextEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isSpeechToTextEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isFolderEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isFolderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isTopicsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isTopicsEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isPeopleEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isPeopleEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isMoodEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isMoodEnabled = enabled)
            }
        }
        // Observe SpeechToTextEngine model download state and progress
        viewModelScope.launch {
            speechToTextEngine.isModelDownloaded.collect { downloaded ->
                _uiState.value = _uiState.value.copy(isModelDownloaded = downloaded)
            }
        }
        viewModelScope.launch {
            speechToTextEngine.downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(modelDownloadProgress = progress)
            }
        }
        // Observe all tags in database for suggestion bar
        viewModelScope.launch {
            getAllTagsUseCase().catch { }.collect { tags ->
                _uiState.value = _uiState.value.copy(allAvailableTags = tags)
            }
        }

        // Observe recorder state
        viewModelScope.launch {
            audioRecorderManager.state.collect { recState ->
                when (recState) {
                    is RecordingState.Recording -> {
                        if (_uiState.value.micFabState != MicFabState.IDLE) {
                            _uiState.value = _uiState.value.copy(
                                isRecording = true,
                                recordingDurationMs = recState.durationMs
                            )
                        }
                    }
                    is RecordingState.Stopped -> {
                        _uiState.value = _uiState.value.copy(isRecording = false)
                    }
                    is RecordingState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRecording = false,
                            micFabState = MicFabState.IDLE
                        )
                    }
                    else -> {}
                }
            }
        }

        // Observe player state for live playback position & duration updates
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { playState ->
                val currentTracks = _uiState.value.audioTracks
                when (playState) {
                    is PlayerState.Playing -> {
                        val matchingTrack = findMatchingTrack(playState.audioPath, playState.entryId, currentTracks)
                        val isMatchingEntry = playState.entryId == _uiState.value.entryId || matchingTrack != null
                        if (isMatchingEntry && matchingTrack != null) {
                            _uiState.value = _uiState.value.copy(
                                isPlaying = true,
                                playingTrackId = matchingTrack.id,
                                currentPositionMs = playState.currentPosition
                            )
                        }
                    }
                    is PlayerState.Paused -> {
                        val matchingTrack = findMatchingTrack(playState.audioPath, playState.entryId, currentTracks)
                        val isMatchingEntry = playState.entryId == _uiState.value.entryId || matchingTrack != null
                        if (isMatchingEntry && matchingTrack != null) {
                            _uiState.value = _uiState.value.copy(
                                isPlaying = false,
                                playingTrackId = matchingTrack.id,
                                currentPositionMs = playState.currentPosition
                            )
                        }
                    }
                    is PlayerState.Ended -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            playingTrackId = null,
                            currentPositionMs = 0L
                        )
                    }
                    is PlayerState.Idle -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            playingTrackId = null,
                            currentPositionMs = 0L
                        )
                    }
                    is PlayerState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            playingTrackId = null,
                            currentPositionMs = 0L,
                            errorMessage = playState.message
                        )
                    }
                }
            }
        }

        // Automatically observe rich text document updates to track unsaved modifications immediately
        viewModelScope.launch {
            snapshotFlow { richTextState.document }
                .collect {
                    updateUnsavedChangesState()
                }
        }
    }

    private fun createCurrentSnapshot(): NoteSnapshot {
        val state = _uiState.value
        val html = RichTextHtmlSerializer.toHtml(richTextState.document)
        return NoteSnapshot(
            titleText = state.titleText,
            userHtml = html,
            selectedMood = state.selectedMood,
            attachedImages = state.attachedImages,
            tags = state.tags,
            audioTrackPaths = state.audioTracks.map { it.path }
        )
    }

    private fun updateUnsavedChangesState() {
        val current = createCurrentSnapshot()
        val isDirty = current != initialSnapshot || pendingFileDeletions.isNotEmpty() || pendingFileAdditions.isNotEmpty()
        if (_uiState.value.hasUnsavedChanges != isDirty) {
            _uiState.value = _uiState.value.copy(hasUnsavedChanges = isDirty)
        }
    }

    // --- Multi-Item (Audio & Images) Selection & Deletion Methods ---

    fun toggleTrackSelection(trackId: String) {
        val currentTracks = _trackSelectionState.value.selectedTrackIds
        val currentImages = _trackSelectionState.value.selectedImagePaths
        val newTracks = if (trackId in currentTracks) {
            currentTracks - trackId
        } else {
            currentTracks + trackId
        }

        val hasSelectedItems = newTracks.isNotEmpty() || currentImages.isNotEmpty()
        _trackSelectionState.value = _trackSelectionState.value.copy(
            isSelectionMode = hasSelectedItems,
            selectedTrackIds = newTracks
        )
    }

    fun toggleTrackSelectionMode(trackId: String) {
        val current = _trackSelectionState.value
        val totalCount = current.totalSelectedCount
        if (current.isSelectionMode && current.selectedTrackIds.contains(trackId) && totalCount == 1) {
            _trackSelectionState.value = TrackSelectionState(isSelectionMode = false, selectedTrackIds = emptySet(), selectedImagePaths = emptySet())
        } else {
            _trackSelectionState.value = _trackSelectionState.value.copy(
                isSelectionMode = true,
                selectedTrackIds = current.selectedTrackIds + trackId
            )
        }
    }

    fun toggleImageSelection(imagePath: String) {
        val currentTracks = _trackSelectionState.value.selectedTrackIds
        val currentImages = _trackSelectionState.value.selectedImagePaths
        val newImages = if (imagePath in currentImages) {
            currentImages - imagePath
        } else {
            currentImages + imagePath
        }

        val hasSelectedItems = currentTracks.isNotEmpty() || newImages.isNotEmpty()
        _trackSelectionState.value = _trackSelectionState.value.copy(
            isSelectionMode = hasSelectedItems,
            selectedImagePaths = newImages
        )
    }

    fun toggleImageSelectionMode(imagePath: String) {
        val current = _trackSelectionState.value
        val totalCount = current.totalSelectedCount
        if (current.isSelectionMode && current.selectedImagePaths.contains(imagePath) && totalCount == 1) {
            _trackSelectionState.value = TrackSelectionState(isSelectionMode = false, selectedTrackIds = emptySet(), selectedImagePaths = emptySet())
        } else {
            _trackSelectionState.value = _trackSelectionState.value.copy(
                isSelectionMode = true,
                selectedImagePaths = current.selectedImagePaths + imagePath
            )
        }
    }

    fun clearTrackSelection() {
        _trackSelectionState.value = TrackSelectionState(isSelectionMode = false, selectedTrackIds = emptySet(), selectedImagePaths = emptySet())
    }

    fun deleteSelectedAudioTracks() {
        deleteSelectedItems()
    }

    fun deleteSelectedItems() {
        val idsToDelete = _trackSelectionState.value.selectedTrackIds
        val imagesToDelete = _trackSelectionState.value.selectedImagePaths
        if (idsToDelete.isEmpty() && imagesToDelete.isEmpty()) return

        val tracksToDelete = _uiState.value.audioTracks.filter { it.id in idsToDelete }
        val remainingTracks = _uiState.value.audioTracks.filter { it.id !in idsToDelete }

        // Stop audio playback if deleting currently playing track
        if (_uiState.value.playingTrackId in idsToDelete) {
            audioPlayerManager.stop()
        }

        // 1. Stage audio track deletions
        tracksToDelete.forEach { track ->
            if (track.path in pendingFileAdditions) {
                pendingFileAdditions.remove(track.path)
                deletePhysicalFile(track.path)
            } else {
                pendingFileDeletions.add(track.path)
            }
        }

        // 2. Stage image deletions
        val remainingImages = _uiState.value.attachedImages.filter { it !in imagesToDelete }
        imagesToDelete.forEach { imgPath ->
            if (imgPath in pendingFileAdditions) {
                pendingFileAdditions.remove(imgPath)
                deletePhysicalFile(imgPath)
            } else {
                pendingFileDeletions.add(imgPath)
            }
        }

        _uiState.value = _uiState.value.copy(
            audioTracks = remainingTracks,
            attachedImages = remainingImages,
            playingTrackId = if (_uiState.value.playingTrackId in idsToDelete) null else _uiState.value.playingTrackId,
            expandedTrackId = if (_uiState.value.expandedTrackId in idsToDelete) null else _uiState.value.expandedTrackId
        )
        clearTrackSelection()
        updateUnsavedChangesState()
    }

    fun resetNewEntryState(
        initialFolder: String? = null,
        initialTag: String? = null,
        initialTagType: String? = null
    ) {
        if (isInitialized) return
        isInitialized = true
        loadedEntryId = null

        audioPlayerManager.stop()
        viewModelScope.launch {
            audioRecorderManager.cancelRecording()
        }
        pausedRecordingAccumulatedMs = 0L
        pendingFileDeletions.clear()
        pendingFileAdditions.clear()
        clearTrackSelection()
        richTextState.setDocument(RichTextDocument.EMPTY)
        val initialTags = mutableListOf<Tag>()
        if (!initialFolder.isNullOrBlank()) {
            initialTags.add(Tag(name = initialFolder, type = TagType.FOLDER))
        }
        if (!initialTag.isNullOrBlank()) {
            val type = try {
                if (!initialTagType.isNullOrBlank()) TagType.valueOf(initialTagType) else TagType.TOPIC
            } catch (e: Exception) {
                TagType.TOPIC
            }
            initialTags.add(Tag(name = initialTag, type = type))
        }
        _uiState.value = NoteDetailUiState(
            entryId = -1L,
            entry = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            micFabState = MicFabState.IDLE,
            isRecording = false,
            isPlaying = false,
            titleText = "",
            userText = "",
            attachedImages = emptyList(),
            tags = initialTags,
            audioTracks = emptyList(),
            hasUnsavedChanges = false
        )
        initialSnapshot = createCurrentSnapshot()
    }

    private fun findMatchingTrack(path: String?, entryId: Long?, tracks: List<AudioTrack>): AudioTrack? {
        if (tracks.isEmpty()) return null
        if (!path.isNullOrEmpty()) {
            val foundByPath = tracks.find { it.path == path }
            if (foundByPath != null) return foundByPath
        }
        return tracks.firstOrNull()
    }

    fun loadEntry(id: Long) {
        if (id <= 0) {
            resetNewEntryState()
            return
        }
        if (isInitialized && loadedEntryId == id) return
        isInitialized = true
        loadedEntryId = id

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getEntryByIdUseCase(id).collect { entry ->
                entry?.let { e ->
                    val tracks = e.allAudioTracks.take(3)
                    val currentPlayState = audioPlayerManager.playbackState.value
                    var activeTrackId: String? = null
                    var isPlaying = false
                    var currentPos = 0L

                    when (currentPlayState) {
                        is PlayerState.Playing -> {
                            if (currentPlayState.entryId == e.id || tracks.any { it.path == currentPlayState.audioPath }) {
                                val match = tracks.find { it.path == currentPlayState.audioPath } ?: tracks.firstOrNull()
                                activeTrackId = match?.id
                                isPlaying = true
                                currentPos = currentPlayState.currentPosition
                            }
                        }
                        is PlayerState.Paused -> {
                            if (currentPlayState.entryId == e.id || tracks.any { it.path == currentPlayState.audioPath }) {
                                val match = tracks.find { it.path == currentPlayState.audioPath } ?: tracks.firstOrNull()
                                activeTrackId = match?.id
                                isPlaying = false
                                currentPos = currentPlayState.currentPosition
                            }
                        }
                        else -> {}
                    }

                    val doc = RichTextHtmlSerializer.fromHtml(e.userText)
                    richTextState.setDocument(doc)

                    pendingFileDeletions.clear()
                    pendingFileAdditions.clear()

                    _uiState.value = _uiState.value.copy(
                        entryId = e.id,
                        entry = e,
                        createdAt = e.createdAt,
                        updatedAt = e.updatedAt,
                        titleText = e.title ?: "",
                        userText = RichTextHtmlSerializer.toHtml(doc),
                        attachedImages = e.images.map { it.imagePath },
                        tags = e.tags,
                        selectedMood = e.moodEmoji ?: "😊",
                        audioFormat = e.audioFormat,
                        audioTracks = tracks,
                        playingTrackId = activeTrackId,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPos,
                        hasUnsavedChanges = false,
                        isDraft = e.isDraft
                    )
                    initialSnapshot = createCurrentSnapshot()
                    updateUnsavedChangesState()
                }
            }
        }
    }

    fun startRecordingTrack(onLimitReached: () -> Unit) {
        if (_uiState.value.audioTracks.size >= 3) {
            onLimitReached()
            return
        }
        clearTrackSelection()
        viewModelScope.launch {
            pausedRecordingAccumulatedMs = 0L
            val prefFormat = userPreferencesManager.audioFormat.first()
            audioRecorderManager.startRecording(prefFormat)
            _uiState.value = _uiState.value.copy(
                micFabState = MicFabState.RECORDING,
                isRecording = true,
                audioFormat = prefFormat,
                recordingDurationMs = 0L
            )
        }
    }

    fun pauseRecordingTrack() {
        if (_uiState.value.micFabState == MicFabState.RECORDING) {
            audioRecorderManager.pauseRecording()
            _uiState.value = _uiState.value.copy(micFabState = MicFabState.PAUSED)
        }
    }

    fun resumeRecordingTrack() {
        if (_uiState.value.micFabState == MicFabState.PAUSED) {
            audioRecorderManager.resumeRecording()
            _uiState.value = _uiState.value.copy(micFabState = MicFabState.RECORDING)
        }
    }

    fun saveCurrentRecordingTrack(onLimitReached: () -> Unit) {
        val currentTracks = _uiState.value.audioTracks
        if (currentTracks.size >= 3) {
            viewModelScope.launch {
                audioRecorderManager.cancelRecording()
                pausedRecordingAccumulatedMs = 0L
                _uiState.value = _uiState.value.copy(micFabState = MicFabState.IDLE, isRecording = false)
                onLimitReached()
            }
            return
        }

        viewModelScope.launch {
            val resultFile = audioRecorderManager.stopRecording()
            val totalDur = _uiState.value.recordingDurationMs
            pausedRecordingAccumulatedMs = 0L

            if (resultFile != null && resultFile.exists() && resultFile.length() > 0L) {
                val newTrack = AudioTrack(
                    path = resultFile.absolutePath,
                    durationMs = if (totalDur > 0) totalDur else 1000L
                )
                pendingFileAdditions.add(resultFile.absolutePath)
                val updatedTracks = (currentTracks + newTrack).take(3)
                _uiState.value = _uiState.value.copy(
                    audioTracks = updatedTracks,
                    micFabState = MicFabState.IDLE,
                    isRecording = false,
                    recordingDurationMs = 0L
                )
                updateUnsavedChangesState()
            } else {
                _uiState.value = _uiState.value.copy(
                    micFabState = MicFabState.IDLE,
                    isRecording = false,
                    recordingDurationMs = 0L
                )
            }
        }
    }

    fun discardRecordingAndReset() {
        viewModelScope.launch {
            audioRecorderManager.cancelRecording()
            pausedRecordingAccumulatedMs = 0L
            _uiState.value = _uiState.value.copy(
                micFabState = MicFabState.IDLE,
                isRecording = false,
                recordingDurationMs = 0L
            )
        }
    }

    fun togglePlaybackForTrack(track: AudioTrack) {
        val path = track.path
        if (path.isEmpty() || !File(path).exists()) return

        val currentPlayState = audioPlayerManager.playbackState.value
        val isCurrentTrackActive = _uiState.value.playingTrackId == track.id

        if (isCurrentTrackActive) {
            when (currentPlayState) {
                is PlayerState.Playing -> {
                    audioPlayerManager.pause()
                }
                is PlayerState.Paused -> {
                    audioPlayerManager.resume()
                }
                else -> {
                    audioPlayerManager.play(path, "Voice Note", _uiState.value.entryId)
                    _uiState.value = _uiState.value.copy(playingTrackId = track.id)
                }
            }
        } else {
            audioPlayerManager.play(path, "Voice Note", _uiState.value.entryId)
            _uiState.value = _uiState.value.copy(playingTrackId = track.id)
        }
    }

    fun onLeaveScreen() {
        audioPlayerManager.stop()
        // Discard any uncommitted temporary draft files created during unsaved session
        pendingFileAdditions.forEach { path ->
            deletePhysicalFile(path)
        }
        pendingFileAdditions.clear()
        pendingFileDeletions.clear()
    }

    fun seekTrackToFraction(track: AudioTrack, fraction: Float) {
        val total = track.durationMs.coerceAtLeast(1000L)
        val targetPos = (fraction * total).toLong()
        if (_uiState.value.playingTrackId == track.id) {
            audioPlayerManager.seekTo(targetPos)
            _uiState.value = _uiState.value.copy(currentPositionMs = targetPos)
        }
    }

    fun toggleAaTranscriptForTrack(track: AudioTrack) {
        if (_uiState.value.isTranscribing) return
        val isExpanded = _uiState.value.expandedTrackId == track.id
        if (isExpanded) {
            _uiState.value = _uiState.value.copy(expandedTrackId = null)
        } else {
            if (!track.transcript.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(expandedTrackId = track.id)
            } else {
                val isDownloaded = generateTranscriptUseCase.isModelDownloadedSync()
                _uiState.value = _uiState.value.copy(isModelDownloaded = isDownloaded)
                if (isDownloaded) {
                    _uiState.value = _uiState.value.copy(showTranscriptConfirmTrack = track)
                } else {
                    _uiState.value = _uiState.value.copy(showModelDownloadPromptTrack = track)
                }
            }
        }
    }

    fun confirmGenerateTranscript() {
        val track = _uiState.value.showTranscriptConfirmTrack ?: return
        _uiState.value = _uiState.value.copy(showTranscriptConfirmTrack = null)
        startTranscriptionForTrack(track)
    }

    fun dismissTranscriptConfirmDialog() {
        _uiState.value = _uiState.value.copy(showTranscriptConfirmTrack = null)
    }

    fun confirmDownloadModel() {
        val track = _uiState.value.showModelDownloadPromptTrack ?: return
        viewModelScope.launch {
            val success = generateTranscriptUseCase.ensureModelDownloaded()
            _uiState.value = _uiState.value.copy(showModelDownloadPromptTrack = null, isModelDownloaded = success)
            if (success) {
                startTranscriptionForTrack(track)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to download Whisper model. Please check network connection."
                )
            }
        }
    }

    fun dismissModelDownloadPrompt() {
        _uiState.value = _uiState.value.copy(showModelDownloadPromptTrack = null)
    }

    fun regenerateTranscriptForTrack(track: AudioTrack) {
        if (_uiState.value.isTranscribing) return
        val isDownloaded = generateTranscriptUseCase.isModelDownloadedSync()
        _uiState.value = _uiState.value.copy(isModelDownloaded = isDownloaded)
        if (isDownloaded) {
            startTranscriptionForTrack(track)
        } else {
            _uiState.value = _uiState.value.copy(showModelDownloadPromptTrack = track)
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startTranscriptionForTrack(track: AudioTrack) {
        val totalPipelineStart = System.currentTimeMillis()
        Log.i("WhisperProfile", "=== [Pipeline Profiler] Starting Transcription User Request ===")

        viewModelScope.launch {
            val initialTracks = _uiState.value.audioTracks.map { t ->
                if (t.id == track.id) t.copy(isTranscriptionFailed = false) else t
            }
            _uiState.value = _uiState.value.copy(
                isTranscribing = true,
                transcribingTrackId = track.id,
                expandedTrackId = track.id,
                audioTracks = initialTracks
            )

            val rawPath = track.path
            val resolvedFile = AudioFileResolver.resolveAudioFile(context, rawPath)

            if (resolvedFile == null || !resolvedFile.exists() || resolvedFile.length() == 0L) {
                _uiState.value = _uiState.value.copy(
                    isTranscribing = false,
                    transcribingTrackId = null,
                    errorMessage = "Audio file could not be accessed or contains no data."
                )
                return@launch
            }

            val result = generateTranscriptUseCase(
                audioFile = resolvedFile,
                language = null,
                onPartialResult = { liveText ->
                    val updatedTracks = _uiState.value.audioTracks.map { t ->
                        if (t.id == track.id) {
                            t.copy(transcript = liveText, isTranscriptionFailed = false)
                        } else t
                    }
                    _uiState.value = _uiState.value.copy(audioTracks = updatedTracks)
                }
            )
            result.onSuccess { transcriptRes ->
                val updatedTracks = _uiState.value.audioTracks.map { t ->
                    if (t.id == track.id) {
                        t.copy(
                            transcript = transcriptRes.text,
                            transcriptCreatedAt = transcriptRes.createdAt,
                            transcriptModel = transcriptRes.model,
                            transcriptLanguage = transcriptRes.language,
                            transcriptVersion = transcriptRes.version,
                            isTranscriptionFailed = false
                        )
                    } else t
                }
                _uiState.value = _uiState.value.copy(
                    audioTracks = updatedTracks,
                    isTranscribing = false,
                    transcribingTrackId = null,
                    expandedTrackId = track.id
                )
                updateUnsavedChangesState()

                val totalPipelineTime = System.currentTimeMillis() - totalPipelineStart
                Log.i("WhisperProfile", "=== [Pipeline Profiler] TOTAL PIPELINE ELAPSED TIME: ${totalPipelineTime}ms ===")

                // Automatically persist transcript to database if entry exists
                if (_uiState.value.entryId > 0) {
                    saveCurrentEntryStateSilently()
                }
            }.onFailure { err ->
                val updatedTracks = _uiState.value.audioTracks.map { t ->
                    if (t.id == track.id) {
                        t.copy(isTranscriptionFailed = true)
                    } else t
                }
                _uiState.value = _uiState.value.copy(
                    audioTracks = updatedTracks,
                    isTranscribing = false,
                    transcribingTrackId = null,
                    errorMessage = "Transcription failed: ${err.message}"
                )
            }
        }
    }

    private fun saveCurrentEntryStateSilently() {
        val dbStart = System.currentTimeMillis()
        val state = _uiState.value
        if (state.entryId <= 0) return
        val primaryTrack = state.audioTracks.firstOrNull()
        val userTextToSave = RichTextHtmlSerializer.toHtml(richTextState.document)
        val entryToSave = JournalEntry(
            id = state.entryId,
            createdAt = state.createdAt,
            updatedAt = System.currentTimeMillis(),
            title = state.titleText.ifBlank { null },
            audioPath = primaryTrack?.path ?: "",
            audioFormat = state.audioFormat,
            duration = primaryTrack?.durationMs ?: 0L,
            transcript = primaryTrack?.transcript,
            transcriptCreatedAt = primaryTrack?.transcriptCreatedAt,
            transcriptModel = primaryTrack?.transcriptModel,
            transcriptLanguage = primaryTrack?.transcriptLanguage,
            transcriptVersion = primaryTrack?.transcriptVersion,
            userText = userTextToSave,
            moodEmoji = state.selectedMood,
            hasTranscript = !primaryTrack?.transcript.isNullOrEmpty(),
            tags = state.tags,
            images = state.attachedImages.mapIndexed { index, p ->
                EntryImage(entryId = state.entryId, imagePath = p, displayOrder = index)
            },
            audioTracks = state.audioTracks,
            isArchived = state.entry?.isArchived ?: false,
            deletedAt = state.entry?.deletedAt
        )
        viewModelScope.launch {
            saveEntryUseCase(entryToSave)
        }
    }

    fun setDateTime(createdAt: Long) {
        _uiState.value = _uiState.value.copy(createdAt = createdAt)
        updateUnsavedChangesState()
    }

    fun setTitleText(title: String) {
        _uiState.value = _uiState.value.copy(titleText = title)
        updateUnsavedChangesState()
    }

    fun setInputText(text: String) {
        if (richTextState.document.text != text) {
            richTextState.setDocument(richTextState.document.copy(text = text))
        }
        _uiState.value = _uiState.value.copy(userText = RichTextHtmlSerializer.toHtml(richTextState.document))
        updateUnsavedChangesState()
    }

    fun setMood(mood: String) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
        updateUnsavedChangesState()
    }

    fun setTags(newTags: List<Tag>) {
        _uiState.value = _uiState.value.copy(tags = newTags)
        updateUnsavedChangesState()
    }

    fun addAudioFile(uriString: String) {
        if (_uiState.value.audioTracks.size >= 3) return
        viewModelScope.launch {
            val destFile = saveAudioToInternalStorage(uriString)
            if (destFile != null && destFile.exists()) {
                val durMs = try {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(destFile.absolutePath)
                    val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    mmr.release()
                    durStr?.toLongOrNull() ?: 1000L
                } catch (e: Exception) {
                    1000L
                }

                val newTrack = AudioTrack(path = destFile.absolutePath, durationMs = durMs)
                pendingFileAdditions.add(destFile.absolutePath)
                val updatedTracks = (_uiState.value.audioTracks + newTrack).take(3)
                _uiState.value = _uiState.value.copy(audioTracks = updatedTracks)
                updateUnsavedChangesState()
            }
        }
    }

    private suspend fun saveAudioToInternalStorage(uriString: String): File? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val destFile = dev.voicejournal.data.storage.MediaStorageManager.generateRecordingFile(context, ".m4a")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile
            } catch (e: Exception) {
                null
            }
        }
    }

    fun addImage(uriString: String) {
        val current = _uiState.value.attachedImages
        if (current.size < 4) {
            val permanentPath = saveImageToInternalStorage(uriString)
            if (permanentPath != null) {
                pendingFileAdditions.add(permanentPath)
                _uiState.value = _uiState.value.copy(attachedImages = current + permanentPath)
                updateUnsavedChangesState()
            }
        }
    }

    private fun saveImageToInternalStorage(uriString: String): String? {
        if (MediaStorageManager.isInternalMedia(context, uriString)) {
            return uriString
        }
        return try {
            val uri = android.net.Uri.parse(uriString)
            val destFile = MediaStorageManager.generateImageFile(context, "jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            if (uriString.startsWith("/")) uriString else uriString
        }
    }

    private fun deletePhysicalFile(path: String) {
        if (MediaStorageManager.isInternalMedia(context, path)) {
            val file = File(path)
            if (file.exists()) {
                try {
                    file.delete()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun removeImage(uriString: String) {
        if (uriString in pendingFileAdditions) {
            pendingFileAdditions.remove(uriString)
            deletePhysicalFile(uriString)
        } else {
            pendingFileDeletions.add(uriString)
        }
        _uiState.value = _uiState.value.copy(
            attachedImages = _uiState.value.attachedImages.filter { it != uriString }
        )
        updateUnsavedChangesState()
    }

    fun deleteSingleImage(uriString: String) {
        removeImage(uriString)
    }

    fun addTag(tag: Tag) {
        val current = _uiState.value.tags
        if (current.none { it.name.equals(tag.name, ignoreCase = true) }) {
            _uiState.value = _uiState.value.copy(tags = current + tag)
            updateUnsavedChangesState()
        }
    }

    fun removeTag(tag: Tag) {
        _uiState.value = _uiState.value.copy(
            tags = _uiState.value.tags.filter { it != tag }
        )
        updateUnsavedChangesState()
    }

    fun saveEntryWithFeedback(onFeedback: (String) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            var updatedTracks = state.audioTracks
            if (state.micFabState != MicFabState.IDLE) {
                val resultFile = audioRecorderManager.stopRecording()
                val totalDur = state.recordingDurationMs
                if (resultFile != null && resultFile.exists() && resultFile.length() > 0L && updatedTracks.size < 3) {
                    val newTrack = AudioTrack(path = resultFile.absolutePath, durationMs = totalDur)
                    pendingFileAdditions.add(resultFile.absolutePath)
                    updatedTracks = (updatedTracks + newTrack).take(3)
                }
            }

            val primaryTrack = updatedTracks.firstOrNull()
            val primaryAudioPath = primaryTrack?.path ?: ""
            val primaryDuration = primaryTrack?.durationMs ?: 0L

            val userTextToSave = RichTextHtmlSerializer.toHtml(richTextState.document)
            val plainText = RichTextHtmlSerializer.toPlainText(richTextState.document)

            val isEmpty = state.titleText.isBlank() && plainText.isBlank() && updatedTracks.isEmpty() && state.attachedImages.isEmpty()
            if (isEmpty) {
                onFeedback("Empty note!")
                return@launch
            }

            val entryToSave = JournalEntry(
                id = if (state.entryId > 0) state.entryId else 0L,
                createdAt = state.createdAt,
                updatedAt = System.currentTimeMillis(),
                title = state.titleText.ifBlank { null },
                audioPath = primaryAudioPath,
                audioFormat = state.audioFormat,
                duration = primaryDuration,
                transcript = primaryTrack?.transcript,
                transcriptCreatedAt = primaryTrack?.transcriptCreatedAt,
                transcriptModel = primaryTrack?.transcriptModel,
                transcriptLanguage = primaryTrack?.transcriptLanguage,
                transcriptVersion = primaryTrack?.transcriptVersion,
                userText = userTextToSave,
                moodEmoji = state.selectedMood,
                hasTranscript = !primaryTrack?.transcript.isNullOrEmpty(),
                tags = state.tags,
                images = state.attachedImages.mapIndexed { index, path ->
                    EntryImage(entryId = if (state.entryId > 0) state.entryId else 0L, imagePath = path, displayOrder = index)
                },
                audioTracks = updatedTracks,
                isArchived = state.entry?.isArchived ?: false,
                isDraft = false,
                deletedAt = state.entry?.deletedAt
            )

            val committedAdditions = pendingFileAdditions.toSet()
            pendingFileAdditions.clear()
            isSaving = true

            val savedId = saveEntryUseCase(entryToSave)
            audioPlayerManager.stop()

            pendingFileDeletions.forEach { path ->
                deletePhysicalFile(path)
            }
            pendingFileDeletions.clear()

            val savedEntry = entryToSave.copy(id = savedId)

            _uiState.value = _uiState.value.copy(
                entryId = savedId,
                entry = savedEntry,
                audioTracks = updatedTracks,
                micFabState = MicFabState.IDLE,
                isRecording = false,
                hasUnsavedChanges = false,
                isDraft = false
            )
            initialSnapshot = createCurrentSnapshot()
            clearTrackSelection()
            isSaving = false
            onFeedback("Note saved")
        }
    }

    fun hasContentModifications(): Boolean {
        val state = _uiState.value
        val plainText = RichTextHtmlSerializer.toPlainText(richTextState.document)
        val initialText = state.entry?.plainUserText ?: ""
        val hasTextChanged = plainText.trim() != initialText.trim()
        val hasTitleChanged = state.titleText.trim() != (state.entry?.title ?: "").trim()
        val hasAudioChanged = state.audioTracks.size != (state.entry?.audioTracks?.size ?: 0)
        val hasImagesChanged = state.attachedImages.size != (state.entry?.images?.size ?: 0)

        val hasAnyContent = state.titleText.isNotBlank() || plainText.isNotBlank() || state.audioTracks.isNotEmpty() || state.attachedImages.isNotEmpty()

        return (hasTextChanged || hasTitleChanged || hasAudioChanged || hasImagesChanged) && hasAnyContent
    }

    fun saveDraftOnExit(onSuccess: () -> Unit) {
        if (isSaving) {
            onSuccess()
            return
        }
        if (!hasContentModifications()) {
            onSuccess()
            return
        }
        val state = _uiState.value
        
        viewModelScope.launch {
            var updatedTracks = state.audioTracks
            if (state.micFabState != MicFabState.IDLE) {
                val resultFile = audioRecorderManager.stopRecording()
                val totalDur = state.recordingDurationMs
                if (resultFile != null && resultFile.exists() && resultFile.length() > 0L && updatedTracks.size < 3) {
                    val newTrack = AudioTrack(path = resultFile.absolutePath, durationMs = totalDur)
                    pendingFileAdditions.add(resultFile.absolutePath)
                    updatedTracks = (updatedTracks + newTrack).take(3)
                }
            }

            val primaryTrack = updatedTracks.firstOrNull()
            val primaryAudioPath = primaryTrack?.path ?: ""
            val primaryDuration = primaryTrack?.durationMs ?: 0L
            val userTextToSave = RichTextHtmlSerializer.toHtml(richTextState.document)
    
            val entryToSave = JournalEntry(
                id = if (state.entryId > 0) state.entryId else 0L,
                createdAt = state.createdAt,
                updatedAt = System.currentTimeMillis(),
                title = state.titleText.ifBlank { null },
                audioPath = primaryAudioPath,
                audioFormat = state.audioFormat,
                duration = primaryDuration,
                transcript = primaryTrack?.transcript,
                transcriptCreatedAt = primaryTrack?.transcriptCreatedAt,
                transcriptModel = primaryTrack?.transcriptModel,
                transcriptLanguage = primaryTrack?.transcriptLanguage,
                transcriptVersion = primaryTrack?.transcriptVersion,
                userText = userTextToSave,
                moodEmoji = state.selectedMood,
                hasTranscript = !primaryTrack?.transcript.isNullOrEmpty(),
                tags = state.tags,
                images = state.attachedImages.mapIndexed { index, path ->
                    EntryImage(entryId = if (state.entryId > 0) state.entryId else 0L, imagePath = path, displayOrder = index)
                },
                audioTracks = updatedTracks,
                isArchived = false,
                isDraft = true,
                deletedAt = null
            )
    
            // Clear synchronously before coroutine to prevent onLeaveScreen race
            pendingFileAdditions.clear()

            val savedId = saveEntryUseCase(entryToSave)
            audioPlayerManager.stop()

            pendingFileDeletions.forEach { path ->
                deletePhysicalFile(path)
            }
            pendingFileDeletions.clear()

            _uiState.value = _uiState.value.copy(entryId = savedId, isDraft = true, hasUnsavedChanges = false)
            onSuccess()
        }
    }

    fun deleteEntry(onSuccess: () -> Unit) {
        val id = _uiState.value.entryId
        viewModelScope.launch {
            if (id > 0) {
                deleteEntryUseCase(id)
            }
            onSuccess()
        }
    }

    fun isEntryEmpty(): Boolean {
        val state = _uiState.value
        val plainText = RichTextHtmlSerializer.toPlainText(richTextState.document)
        return state.titleText.isBlank() && plainText.isBlank() && state.audioTracks.isEmpty() && state.attachedImages.isEmpty()
    }

    fun archiveCurrentNote(onSuccess: () -> Unit) {
        if (isEntryEmpty() || _uiState.value.isDraft) return
        val state = _uiState.value

        // Clear synchronously before coroutine to prevent onLeaveScreen race
        pendingFileAdditions.clear()

        viewModelScope.launch {
            if (state.entryId > 0) {
                val existing = state.entry
                if (existing != null) {
                    saveEntryUseCase(existing.copy(isArchived = true))
                }
            } else {
                var updatedTracks = state.audioTracks
                if (state.micFabState != MicFabState.IDLE) {
                    val resultFile = audioRecorderManager.stopRecording()
                    val totalDur = state.recordingDurationMs
                    if (resultFile != null && resultFile.exists() && resultFile.length() > 0L && updatedTracks.size < 3) {
                        val newTrack = AudioTrack(path = resultFile.absolutePath, durationMs = totalDur)
                        updatedTracks = (updatedTracks + newTrack).take(3)
                    }
                }

                val primaryTrack = updatedTracks.firstOrNull()
                val userTextToSave = RichTextHtmlSerializer.toHtml(richTextState.document)
                val entryToSave = JournalEntry(
                    id = 0L,
                    createdAt = state.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    title = state.titleText.ifBlank { null },
                    audioPath = primaryTrack?.path ?: "",
                    audioFormat = state.audioFormat,
                    duration = primaryTrack?.durationMs ?: 0L,
                    transcript = primaryTrack?.transcript,
                    userText = userTextToSave,
                    moodEmoji = state.selectedMood,
                    hasTranscript = !primaryTrack?.transcript.isNullOrEmpty(),
                    tags = state.tags,
                    images = state.attachedImages.mapIndexed { index, path ->
                        EntryImage(entryId = 0L, imagePath = path, displayOrder = index)
                    },
                    audioTracks = updatedTracks,
                    isArchived = true
                )
                saveEntryUseCase(entryToSave)
            }
            pendingFileDeletions.forEach { path ->
                deletePhysicalFile(path)
            }
            pendingFileDeletions.clear()
            onSuccess()
        }
    }
}
