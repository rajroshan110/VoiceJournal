package dev.voicejournal.ui.notedetail

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.voicejournal.ui.notedetail.components.AddItemSheet
import dev.voicejournal.data.storage.MediaStorageManager
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.util.findActivity
import dev.voicejournal.ui.designsystem.components.audio.UnifiedAudioPlayerBar
import dev.voicejournal.ui.notedetail.components.TranscriptionButton
import dev.voicejournal.ui.notedetail.components.TranscriptSection
import dev.voicejournal.ui.notedetail.components.EditorToolbar
import dev.voicejournal.ui.notedetail.components.ImageMosaic
import dev.voicejournal.ui.notedetail.components.JournalTagsDialog
import dev.voicejournal.ui.notedetail.components.JuneDateTimePicker
import dev.voicejournal.ui.notedetail.components.MicFabState
import dev.voicejournal.ui.notedetail.components.MicRecordingPill
import dev.voicejournal.ui.notedetail.components.NoteDetailHeader
import dev.voicejournal.ui.notedetail.components.UserTextInput
import dev.voicejournal.ui.designsystem.theme.AppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.voicejournal.util.TimeFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    entryId: Long,
    initialFolder: String? = null,
    initialTag: String? = null,
    initialTagType: String? = null,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trackSelectionState by viewModel.trackSelectionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTagsDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var datePickerTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddItemSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelectedTracksDialog by rememberSaveable { mutableStateOf(false) }
    var showArchiveConfirmDialog by rememberSaveable { mutableStateOf(false) }

    var selectedLightboxImage by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    var lastBackPressTime by rememberSaveable { mutableLongStateOf(0L) }
    var showUnsavedPromptDialog by rememberSaveable { mutableStateOf(false) }
    var showDraftOrDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardRecordingConfirmDialog by rememberSaveable { mutableStateOf(false) }

    fun handleExit() {
        if (trackSelectionState.isSelectionMode) {
            viewModel.clearTrackSelection()
            return
        }
        if (uiState.micFabState != MicFabState.IDLE) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                viewModel.discardRecordingAndReset()
                navController.popBackStack()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Recording in progress. Press back again to discard and exit.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val isNewOrDraft = uiState.isDraft || uiState.entryId <= 0
        if (isNewOrDraft) {
            if (viewModel.hasContentModifications()) {
                showDraftOrDiscardDialog = true
            } else {
                navController.popBackStack()
            }
        } else {
            if (!viewModel.isEntryEmpty() && uiState.hasUnsavedChanges) {
                showUnsavedPromptDialog = true
            } else {
                navController.popBackStack()
            }
        }
    }

    BackHandler(enabled = true) {
        handleExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findActivity()
            if (activity?.isChangingConfigurations != true) {
                viewModel.onLeaveScreen()
            }
        }
    }

    LaunchedEffect(entryId, initialFolder, initialTag, initialTagType) {
        if (entryId > 0) {
            viewModel.loadEntry(entryId)
        } else {
            viewModel.resetNewEntryState(
                initialFolder = initialFolder,
                initialTag = initialTag,
                initialTagType = initialTagType
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addImage(uri.toString())
        }
    }

    var currentPhotoFile by rememberSaveable { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = currentPhotoFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            viewModel.addImage(file.absolutePath)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (uiState.audioTracks.size < 3) {
                viewModel.addAudioFile(it.toString())
            } else {
                Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingTrack {
                Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleStartRecordingPermission() {
        if (uiState.audioTracks.size >= 3) {
            Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
            return
        }
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.startRecordingTrack {
                Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val formattedDate = remember(uiState.createdAt) {
        TimeFormatter.formatHeaderDate(uiState.createdAt)
    }
    val formattedTime = remember(uiState.createdAt, uiState.timeFormat, context) {
        TimeFormatter.formatTime(uiState.createdAt, uiState.timeFormat, context)
    }

    val colors = AppTheme.colors

    val density = LocalDensity.current
    val currentImeBottom by rememberUpdatedState(WindowInsets.ime.getBottom(density))
    val noteEditorBringIntoViewSpec = remember(density) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                // When keyboard is closed (not active), NEVER scroll the note container!
                // This completely prevents Bug 1: clicking the blank space / bottom margin from
                // prematurely sucking down the note to the top/bottom before the keyboard opens.
                val isKeyboardActive = currentImeBottom > 0
                if (!isKeyboardActive) {
                    Log.d("VoiceJournalDebug", "calculateScrollDistance: keyboard closed -> 0f (offset=$offset, size=$size)")
                    return 0f
                }

                // If the item requesting to be brought into view is large (like the entire BasicTextField itself
                // on initial focus gain), NEVER scroll the container to its bottom or top!
                // Full containers should not jump the viewport; only targeted cursor rects should scroll.
                if (size > containerSize * 0.5f) {
                    Log.d("VoiceJournalDebug", "calculateScrollDistance: Ignored large child (size=$size > 0.5*container=$containerSize, offset=$offset)")
                    return 0f
                }

                // Graceful breathing room (+1 line height above where mic/toolbar sits: 56dp mic + 8dp bottom padding + 24dp text line = 88dp):
                val gracefulBreathingPx = with(density) { 88.dp.toPx() }
                val targetBottom = containerSize - gracefulBreathingPx

                // 1. If cursor is ALREADY visible above the target bottom and below top, DO NOT JUMP:
                if (offset >= 0f && offset + size <= targetBottom) {
                    Log.d("VoiceJournalDebug", "calculateScrollDistance: cursor already visible (offset=$offset, size=$size, targetBottom=$targetBottom) -> 0f")
                    return 0f
                }

                // 2. Only if the cursor is obscured below the target bottom:
                if (offset + size > targetBottom) {
                    val result = (offset + size) - targetBottom
                    Log.d("VoiceJournalDebug", "calculateScrollDistance: cursor obscured -> result=$result, offset=$offset, targetBottom=$targetBottom")
                    return result
                }

                // 3. If cursor is above the visible viewport (e.g. typing or arrowing up):
                if (offset < 0f) {
                    Log.d("VoiceJournalDebug", "calculateScrollDistance: cursor above viewport -> offset=$offset")
                    return offset
                }

                return 0f
            }
        }
    }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            NoteDetailHeader(
                onClose = { handleExit() },
                onMoodSelect = { viewModel.setMood(it) },
                currentMood = uiState.selectedMood,
                onSave = {
                    viewModel.saveEntryWithFeedback { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onAddPhoto = { showAddItemSheet = true },
                createdAt = uiState.createdAt,
                updatedAt = uiState.updatedAt,
                onDeleteNote = if (uiState.entryId > 0 && !viewModel.isEntryEmpty()) { { showDeleteConfirmDialog = true } } else null,
                isSelectionMode = trackSelectionState.isSelectionMode,
                selectedCount = trackSelectionState.totalSelectedCount,
                showSaveButton = !viewModel.isEntryEmpty() && uiState.hasUnsavedChanges,
                onClearSelection = { viewModel.clearTrackSelection() },
                onDeleteSelectedTracks = { showDeleteSelectedTracksDialog = true },
                onArchiveClick = {
                    if (viewModel.isEntryEmpty()) {
                        Toast.makeText(context, "Cannot archive an empty note!", Toast.LENGTH_SHORT).show()
                    } else if (uiState.isDraft || uiState.entryId <= 0) {
                        Toast.makeText(context, "Please save the draft before archiving!", Toast.LENGTH_SHORT).show()
                    } else {
                        showArchiveConfirmDialog = true
                    }
                },
                timeFormat = uiState.timeFormat,
                isMoodEnabled = uiState.isMoodEnabled
            )
        },
        bottomBar = {
            if (uiState.isMarkdownEnabled) {
                EditorToolbar(
                    richTextState = viewModel.richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLandscape) Modifier else Modifier.imePadding())
                )
            }
        },
        floatingActionButton = {
            MicRecordingPill(
                state = uiState.micFabState,
                durationMs = uiState.recordingDurationMs,
                onStartRecording = { handleStartRecordingPermission() },
                onPauseRecording = { viewModel.pauseRecordingTrack() },
                onResumeRecording = { viewModel.resumeRecordingTrack() },
                onSaveTrack = {
                    viewModel.saveCurrentRecordingTrack {
                        Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancelRecording = {
                    if (uiState.micFabState == MicFabState.RECORDING) {
                        viewModel.pauseRecordingTrack()
                    }
                    showDiscardRecordingConfirmDialog = true
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(paddingValues)
                .then(if (uiState.isMarkdownEnabled) Modifier else Modifier.imePadding())
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides noteEditorBringIntoViewSpec) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Photo Mosaic Section
            if (uiState.attachedImages.isNotEmpty()) {
                ImageMosaic(
                    images = uiState.attachedImages,
                    isSelectionMode = trackSelectionState.isSelectionMode,
                    selectedImages = trackSelectionState.selectedImagePaths,
                    onImageClick = { idx ->
                        if (idx in uiState.attachedImages.indices) {
                            selectedLightboxImage = uiState.attachedImages[idx]
                        }
                    },
                    onImageLongClick = { idx ->
                        if (idx in uiState.attachedImages.indices) {
                            viewModel.toggleImageSelectionMode(uiState.attachedImages[idx])
                        }
                    }
                )
            }

            // Title Field
            OutlinedTextField(
                value = uiState.titleText,
                onValueChange = { viewModel.setTitleText(it) },
                placeholder = {
                    Text(
                        "Add title",
                        color = colors.textSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Date, Time & Tag Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable {
                        datePickerTab = 0
                        showDatePicker = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable {
                        datePickerTab = 1
                        showDatePicker = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                val hasAnyTagsEnabled = uiState.isFolderEnabled || uiState.isTopicsEnabled || uiState.isPeopleEnabled
                if (hasAnyTagsEnabled) {
                    val visibleTags = remember(uiState.tags, uiState.isFolderEnabled, uiState.isTopicsEnabled, uiState.isPeopleEnabled, uiState.isMoodEnabled) {
                        uiState.tags.filter { tag ->
                            when (tag.type) {
                                TagType.TOPIC -> uiState.isTopicsEnabled
                                TagType.PERSON -> uiState.isPeopleEnabled
                                TagType.FOLDER, TagType.THING -> uiState.isFolderEnabled
                                TagType.MOOD -> uiState.isMoodEnabled
                            }
                        }
                    }

                    Surface(
                        color = if (visibleTags.isNotEmpty()) colors.primaryContainer else colors.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { showTagsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏷️ ", fontSize = 12.sp)
                            Text(
                                text = if (visibleTags.isNotEmpty()) "Tags (${visibleTags.size})" else "Tags",
                                color = if (visibleTags.isNotEmpty()) colors.primary else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Multi-Track Audio Players with Long-Press & Selection State (Max 3 tracks)
            if (uiState.audioTracks.isNotEmpty()) {
                uiState.audioTracks.forEachIndexed { index, track ->
                    val isTrackSelected = track.id in trackSelectionState.selectedTrackIds
                    val isTrackActive = uiState.playingTrackId == track.id
                    val isPlayingThisTrack = isTrackActive && uiState.isPlaying
                    val isExpanded = uiState.expandedTrackId == track.id
                    val isTrackTranscribing = uiState.isTranscribing && uiState.transcribingTrackId == track.id

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isTrackSelected) colors.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .border(
                                width = if (isTrackSelected) 2.dp else 0.dp,
                                color = if (isTrackSelected) colors.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (trackSelectionState.isSelectionMode) {
                                        viewModel.toggleTrackSelection(track.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleTrackSelectionMode(track.id)
                                }
                            )
                            .padding(4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Track label row ("Voice Note" / "Track N" + selection checkmark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (uiState.audioTracks.size > 1) "Track ${index + 1}" else "Voice Note",
                                    color = if (isTrackSelected) colors.primary else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isTrackSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(colors.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colors.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Player row: [UnifiedAudioPlayerBar ──────────] [Aa]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UnifiedAudioPlayerBar(
                                    isPlaying = isPlayingThisTrack,
                                    isBuffering = false,
                                    isAudioError = isTrackActive && !uiState.errorMessage.isNullOrBlank(),
                                    currentPositionMs = if (isTrackActive) uiState.currentPositionMs else 0L,
                                    durationMs = track.durationMs,
                                    waveformAmplitudes = track.waveformAmplitudes,
                                    onPlayPauseClick = {
                                        if (trackSelectionState.isSelectionMode) {
                                            viewModel.toggleTrackSelection(track.id)
                                        } else {
                                            viewModel.togglePlaybackForTrack(track)
                                        }
                                    },
                                    onSeekFraction = { frac -> viewModel.seekTrackToFraction(track, frac) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (uiState.isSpeechToTextEnabled) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TranscriptionButton(
                                        isTranscribing = isTrackTranscribing,
                                        onClick = {
                                            if (trackSelectionState.isSelectionMode) {
                                                viewModel.toggleTrackSelection(track.id)
                                            } else {
                                                viewModel.toggleAaTranscriptForTrack(track)
                                            }
                                        }
                                    )
                                }
                            }

                            // Expandable transcript panel (below the player row)
                            TranscriptSection(
                                transcript = track.transcript,
                                isExpanded = isExpanded,
                                isTranscribing = isTrackTranscribing,
                                isTranscriptionFailed = track.isTranscriptionFailed,
                                onRegenerateClick = {
                                    if (trackSelectionState.isSelectionMode) {
                                        viewModel.toggleTrackSelection(track.id)
                                    } else {
                                        viewModel.regenerateTranscriptForTrack(track)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Text Content Input Area
            UserTextInput(
                richTextState = viewModel.richTextState,
                focusRequester = focusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 350.dp)
            )

            // Standardized bottom breathing space:
            // Ensures the last lines of note text can be comfortably scrolled well above
            // the footer/keyboard with ample room to view and edit without feeling cramped.
            // Also forwards taps to focus so tapping the empty margin pops up the keyboard.
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val endPos = viewModel.richTextState.document.length
                        viewModel.richTextState.updateSelection(TextRange(endPos))
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            )
        }
    }
}
}

    // June Interactive Date & Time Picker Dialog
    if (showDatePicker) {
        JuneDateTimePicker(
            initialDateTimeMillis = uiState.createdAt,
            initialTab = datePickerTab,
            onDateTimeSelected = { selectedMillis ->
                viewModel.setDateTime(selectedMillis)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // June Add Attachment Bottom Sheet
    if (showAddItemSheet) {
        AddItemSheet(
            onDismiss = { showAddItemSheet = false },
            onTakePhotoClick = {
                try {
                    val photoFile = MediaStorageManager.generateImageFile(context)
                    currentPhotoFile = photoFile
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                    cameraLauncher.launch(photoUri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onAddPhotoClick = {
                dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onAddVoiceRecordingClick = {
                if (uiState.audioTracks.size < 3) {
                    dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                } else {
                    Toast.makeText(context, "Maximum 3 audio tracks per note", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // June Dedicated Manage Tags Modal Dialog
    if (showTagsDialog) {
        JournalTagsDialog(
            tags = uiState.tags,
            allAvailableTags = uiState.allAvailableTags,
            isFolderEnabled = uiState.isFolderEnabled,
            isTopicsEnabled = uiState.isTopicsEnabled,
            isPeopleEnabled = uiState.isPeopleEnabled,
            onTagsChanged = { newTags -> viewModel.setTags(newTags) },
            onDismiss = { showTagsDialog = false }
        )
    }

    // Delete Entire Note Confirmation Dialog
    if (showDeleteConfirmDialog) {
        dev.voicejournal.ui.components.MoveToTrashDialog(
            message = "Are you sure you want to move this note to Trash?",
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteEntry { navController.popBackStack() }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Delete Selected Items (Voice Tracks & Images) Confirmation Dialog
    if (showDeleteSelectedTracksDialog) {
        val count = trackSelectionState.totalSelectedCount
        AlertDialog(
            onDismissRequest = { showDeleteSelectedTracksDialog = false },
            title = { Text("Delete selected items?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the selected $count item(s)?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedTracksDialog = false
                        viewModel.deleteSelectedItems()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedTracksDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Archive Confirmation Dialog
    if (showArchiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            title = { Text("Archive note?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to archive this note? It will be hidden from your Journal and Calendar tabs and moved to Archive.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirmDialog = false
                        viewModel.archiveCurrentNote {
                            Toast.makeText(context, "Note archived", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Archive", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // Lightbox Dialog
    selectedLightboxImage?.let { imageUri ->
        dev.voicejournal.ui.journal.components.LightboxDialog(
            imageUri = imageUri,
            onDismiss = { selectedLightboxImage = null },
            onDelete = {
                viewModel.deleteSingleImage(imageUri)
                selectedLightboxImage = null
            }
        )
    }

    // Error Message Dialog
    val currentErrorMessage = uiState.errorMessage
    if (!currentErrorMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = {
                Text(
                    text = "Playback Error",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = currentErrorMessage,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("OK", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Generate Transcript Confirmation Dialog
    if (uiState.showTranscriptConfirmTrack != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTranscriptConfirmDialog() },
            title = { Text("Generate transcript?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will transcribe the audio locally on your device. No internet connection is required.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmGenerateTranscript() }) {
                    Text("Generate", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTranscriptConfirmDialog() }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // Whisper Model Download Prompt Dialog
    if (uiState.showModelDownloadPromptTrack != null) {
        AlertDialog(
            onDismissRequest = {
                if (uiState.modelDownloadProgress == null) viewModel.dismissModelDownloadPrompt()
            },
            title = { Text("Download Whisper Model", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Whisper Base Q5 model (~59 MB - English & Hindi) is required for offline audio transcription. Would you like to download it now?",
                        color = colors.textSecondary
                    )
                    if (uiState.modelDownloadProgress != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { uiState.modelDownloadProgress ?: 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloading: ${(uiState.modelDownloadProgress!! * 100).toInt()}%",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (uiState.modelDownloadProgress == null) {
                    TextButton(onClick = { viewModel.confirmDownloadModel() }) {
                        Text("Download", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (uiState.modelDownloadProgress == null) {
                    TextButton(onClick = { viewModel.dismissModelDownloadPrompt() }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            },
            containerColor = colors.surface
        )
    }

    // Draft Note and Discard Confirmation Dialog for New Notes & Drafts
    if (showDraftOrDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDraftOrDiscardDialog = false },
            title = { Text("Save to Drafts?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Would you like to save this note as a draft or discard it?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDraftOrDiscardDialog = false
                        viewModel.saveDraftOnExit {
                            Toast.makeText(context, "Saved to Drafts", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Draft Note", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDraftOrDiscardDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            showDraftOrDiscardDialog = false
                            viewModel.discardRecordingAndReset()
                            navController.popBackStack()
                        }
                    ) {
                        Text("Discard", color = colors.error, fontWeight = FontWeight.Medium)
                    }
                }
            },
            containerColor = colors.surface
        )
    }

    // Unsaved Progress Confirmation Dialog for Existing Notes
    if (showUnsavedPromptDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedPromptDialog = false },
            title = { Text("Save Changes?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to save your progress before leaving?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedPromptDialog = false
                        viewModel.saveEntryWithFeedback { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Save", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedPromptDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            showUnsavedPromptDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Discard", color = colors.error, fontWeight = FontWeight.Medium)
                    }
                }
            },
            containerColor = colors.surface
        )
    }

    // Discard Active Recording Confirmation Dialog
    if (showDiscardRecordingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardRecordingConfirmDialog = false },
            title = { Text("Discard recording?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to discard this voice recording?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardRecordingConfirmDialog = false
                        viewModel.discardRecordingAndReset()
                    }
                ) {
                    Text("Discard", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardRecordingConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}
