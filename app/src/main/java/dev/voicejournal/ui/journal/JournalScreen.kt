package dev.voicejournal.ui.journal

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Share
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.journal.components.*
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.designsystem.components.buttons.AppCompactPillButton
import dev.voicejournal.ui.designsystem.components.buttons.AppPrimaryButton
import dev.voicejournal.ui.designsystem.components.buttons.AppSecondaryOutlinedButton
import dev.voicejournal.ui.designsystem.components.dialogs.AppAlertDialog
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.util.findActivity
import kotlinx.coroutines.launch

private val MicIcon: ImageVector = ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 14f)
            curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
            reflectiveCurveTo(9f, 3.34f, 9f, 5f)
            verticalLineToRelative(6f)
            curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
            close()
            moveTo(17f, 11f)
            curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
            reflectiveCurveToRelative(-5f, -2.24f, -5f, -5f)
            horizontalLineTo(5f)
            curveToRelative(0f, 3.53f, 2.61f, 6.43f, 6f, 6.92f)
            verticalLineTo(21f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-3.08f)
            curveToRelative(3.39f, -0.49f, 6f, -3.39f, 6f, -6.92f)
            horizontalLineToRelative(-2f)
            close()
        }
    }.build()

private val FilterOffIcon: ImageVector = ImageVector.Builder(
        name = "FilterOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 18f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 6f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(18f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
            moveTo(6f, 13f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(-2f)
            horizontalLineTo(6f)
            verticalLineToRelative(2f)
            close()
        }
    }.build()

enum class ActiveSheet {
    NONE, TAGS, PEOPLE, MOOD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    navController: NavController,
    initialPerson: String? = null,
    initialTag: String? = null,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardPlaybackState by viewModel.cardPlaybackState.collectAsStateWithLifecycle()
    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.NONE) }
    var selectedLightboxImage by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCategorizeSheet by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
        }
    }

    pendingImportUri?.let { uri ->
        dev.voicejournal.ui.components.BackupRestoreConfirmationDialog(
            onConfirm = {
                pendingImportUri = null
                viewModel.importBackupFromUri(uri)
            },
            onDismiss = { pendingImportUri = null }
        )
    }

    uiState.backupResultDialog?.let { dialogInfo ->
        dev.voicejournal.ui.components.BackupResultAlertDialog(
            dialogInfo = dialogInfo,
            onDismiss = { viewModel.dismissBackupResultDialog() }
        )
    }

    if (uiState.isImporting) {
        dev.voicejournal.ui.components.BackupRestoreProgressDialog(
            progressText = uiState.backupProgressText
        )
    }

    val listState = rememberLazyListState()

    LaunchedEffect(initialPerson, initialTag) {
        if (!initialPerson.isNullOrEmpty()) {
            viewModel.setPersonFilter(initialPerson)
        } else if (!initialTag.isNullOrEmpty()) {
            viewModel.setTagFilter(initialTag)
        }
    }

    // Smooth sort repositioning with fluid animation ONLY when user changes sort option
    var prevSortOption by remember { mutableStateOf<SortOption?>(null) }
    LaunchedEffect(uiState.sortOption) {
        if (prevSortOption != null && prevSortOption != uiState.sortOption) {
            listState.animateScrollToItem(0)
        }
        prevSortOption = uiState.sortOption
    }

    // Auto-close search if list is scrolled and search query is empty
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            if (uiState.isSearchActive && uiState.searchQuery.isEmpty()) {
                viewModel.toggleSearch()
            }
            keyboardController?.hide()
        }
    }

    var pendingNavEntryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val hasFilters = uiState.filterState.selectedTags.isNotEmpty() ||
                     uiState.filterState.selectedPeople.isNotEmpty() ||
                     uiState.filterState.selectedMoods.isNotEmpty()

    // Handle back button presses for navigation drawer, selection mode, search bar, and filters
    BackHandler(enabled = drawerState.isOpen || selectionState.isSelectionMode || uiState.isSearchActive || hasFilters) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (selectionState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (uiState.isSearchActive) {
            viewModel.toggleSearch()
        } else if (hasFilters) {
            viewModel.clearAllFilters()
        }
    }

    // Stop audio, clear selection & close search when leaving screen (only on true navigation away, not configuration change)
    DisposableEffect(pendingNavEntryId) {
        onDispose {
            val activity = context.findActivity()
            if (activity?.isChangingConfigurations != true) {
                viewModel.stopAudioOnLeave(pendingNavEntryId)
                if (uiState.isSearchActive) {
                    viewModel.toggleSearch()
                }
                if (selectionState.isSelectionMode) {
                    viewModel.clearSelection()
                }
                activeSheet = ActiveSheet.NONE
                showCategorizeSheet = false
                selectedLightboxImage = null
            }
        }
    }

    // Playback Error Dialog state
    var audioErrorDialogMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(cardPlaybackState.status, cardPlaybackState.errorMessage) {
        if (cardPlaybackState.status == PlaybackStatus.Error && cardPlaybackState.errorMessage != null) {
            audioErrorDialogMessage = cardPlaybackState.errorMessage
            viewModel.clearPlaybackError()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioErrorDialogMessage = null
            viewModel.clearPlaybackError()
        }
    }

    if (!audioErrorDialogMessage.isNullOrBlank()) {
        AppAlertDialog(
            onDismissRequest = { audioErrorDialogMessage = null },
            title = {
                Text(
                    text = "Playback Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = audioErrorDialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { audioErrorDialogMessage = null }) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    val windowInfo = AppTheme.windowInfo
    val isCompactLandscape = windowInfo.isCompactLandscape
    val isNarrowPhone = windowInfo.isNarrowPhone

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selectionState.isSelectionMode,
        drawerContent = {
            JournalDrawerContent(
                isFolderEnabled = uiState.isFolderEnabled,
                onNavigateToArchive = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Archive.route)
                },
                onNavigateToDraft = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Draft.route)
                },
                onNavigateToFolders = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Folders.route)
                },
                onNavigateToTags = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Tags.route)
                },
                onNavigateToTrash = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Trash.route)
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            containerColor = AppTheme.colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.background)
                ) {
                    JournalHeader(
                        isSearchActive = uiState.isSearchActive,
                        searchQuery = uiState.searchQuery,
                        currentSortOption = uiState.sortOption,
                        onSearchToggle = { viewModel.toggleSearch() },
                        onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                        onSortOptionSelect = { option -> viewModel.setSortOption(option) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        isSelectionMode = selectionState.isSelectionMode,
                        selectedCount = selectionState.selectedEntryIds.size,
                        isNotesOrganisationEnabled = uiState.isNotesOrganisationEnabled,
                        isFolderEnabled = uiState.isFolderEnabled,
                        isTopicsEnabled = uiState.isTopicsEnabled,
                        isPeopleEnabled = uiState.isPeopleEnabled,
                        onCategorizeSelected = { showCategorizeSheet = true },
                        onDeleteSelected = { showDeleteDialog = true },
                        onClearSelection = { viewModel.clearSelection() },
                        filterContent = if (isCompactLandscape && !uiState.isSearchActive && (uiState.isTopicsEnabled || uiState.isPeopleEnabled || uiState.isMoodEnabled)) {
                            {
                                FilterBar(
                                    selectedTagsCount = uiState.filterState.selectedTags.size,
                                    selectedPeopleCount = uiState.filterState.selectedPeople.size,
                                    selectedMoodsCount = uiState.filterState.selectedMoods.size,
                                    isTopicsEnabled = uiState.isTopicsEnabled,
                                    isPeopleEnabled = uiState.isPeopleEnabled,
                                    isMoodEnabled = uiState.isMoodEnabled,
                                    onAllClick = { viewModel.clearAllFilters() },
                                    onTagsClick = { activeSheet = ActiveSheet.TAGS },
                                    onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                                    onMoodClick = { activeSheet = ActiveSheet.MOOD }
                                )
                            }
                        } else null
                    )
                    if (!isCompactLandscape && (uiState.isTopicsEnabled || uiState.isPeopleEnabled || uiState.isMoodEnabled)) {
                        FilterBar(
                            selectedTagsCount = uiState.filterState.selectedTags.size,
                            selectedPeopleCount = uiState.filterState.selectedPeople.size,
                            selectedMoodsCount = uiState.filterState.selectedMoods.size,
                            isTopicsEnabled = uiState.isTopicsEnabled,
                            isPeopleEnabled = uiState.isPeopleEnabled,
                            isMoodEnabled = uiState.isMoodEnabled,
                            onAllClick = { viewModel.clearAllFilters() },
                            onTagsClick = { activeSheet = ActiveSheet.TAGS },
                            onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                            onMoodClick = { activeSheet = ActiveSheet.MOOD }
                        )
                    }
                }
            },
            bottomBar = {
                BottomNavBar(
                    navController = navController,
                    onJournalReselected = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )
            },
            floatingActionButton = {
                if (uiState.feedState !is FeedState.EmptyGlobal) {
                    MicFab(
                        onClick = { navController.navigate(Screen.NoteDetail.createRoute(-1L)) },
                        modifier = Modifier.padding(bottom = Spacing.SpaceXs, end = Spacing.SpaceXs)
                    )
                }
            }
        ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.background)
        ) {
            // Render State Matrix
            when (val state = uiState.feedState) {
                is FeedState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.background)
                    )
                }
                is FeedState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.SpaceLg),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Radius.RadiusMd),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isNarrowPhone) {
                                Column(
                                    modifier = Modifier.padding(Spacing.SpaceLg),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.SpaceMd))
                                        Text(
                                            text = state.message,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    AppCompactPillButton(
                                        onClick = { viewModel.retryFeedLoad() },
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Retry", maxLines = 1, softWrap = false)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.padding(Spacing.SpaceLg),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.SpaceMd))
                                        Text(
                                            text = state.message,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    AppCompactPillButton(
                                        onClick = { viewModel.retryFeedLoad() },
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("Retry", maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
                is FeedState.EmptyGlobal -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.Space2Xl),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = MicIcon,
                            contentDescription = "No journal entries",
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(IconSize.IconHero)
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceLg))
                        Text(
                            text = "No journal entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceXs))
                        Text(
                            text = "Capture your thoughts and voice notes to start your journal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceXl))
                        AppPrimaryButton(
                            onClick = { navController.navigate(Screen.NoteDetail.createRoute(-1L)) },
                            shape = RoundedCornerShape(Radius.RadiusPill)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create new entry",
                                modifier = Modifier.size(IconSize.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                            Text(
                                text = "Record Your First Note",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.SpaceMd))

                        AppSecondaryOutlinedButton(
                            onClick = {
                                dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                                importLauncher.launch(arrayOf("application/zip", "*/*"))
                            },
                            shape = RoundedCornerShape(Radius.RadiusPill)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Import Backup",
                                modifier = Modifier.size(IconSize.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                            Text(
                                text = "Import Backup",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                is FeedState.EmptyFiltered -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.Space2Xl),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = FilterOffIcon,
                            contentDescription = "No matching entries",
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(IconSize.IconHero)
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceLg))
                        Text(
                            text = "No matching entries found",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceXs))
                        Text(
                            text = "Try adjusting your filters or search terms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.SpaceXl))
                        AppSecondaryOutlinedButton(
                            onClick = { viewModel.clearAllFilters() },
                            shape = RoundedCornerShape(Radius.RadiusPill)
                        ) {
                            Text(
                                text = "Clear Filters",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                is FeedState.Success -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = Spacing.SpaceX2s),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!uiState.permissionGranted) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(Radius.RadiusMd),
                                    color = Color(0xFF2B2618),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceXs)
                                ) {
                                    if (isNarrowPhone) {
                                        Column(
                                            modifier = Modifier.padding(Spacing.SpaceLg),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                                        ) {
                                            Text(
                                                text = "Storage permission required to play voice notes and view media previews",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFFD54F)
                                            )
                                            AppCompactPillButton(
                                                onClick = { viewModel.setPermissionGranted(true) },
                                                containerColor = Color(0xFFFFD54F),
                                                contentColor = Color.Black,
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text(
                                                    text = "Grant Permission",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.padding(Spacing.SpaceLg),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
                                        ) {
                                            Text(
                                                text = "Storage permission required to play voice notes and view media previews",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFFD54F),
                                                modifier = Modifier.weight(1f)
                                            )
                                            AppCompactPillButton(
                                                onClick = { viewModel.setPermissionGranted(true) },
                                                containerColor = Color(0xFFFFD54F),
                                                contentColor = Color.Black
                                            ) {
                                                Text(
                                                    text = "Grant Permission",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = state.entries,
                            key = { it.id },
                            contentType = { "journal_entry" }
                        ) { entry ->
                            val isPlaying by remember(entry.id) {
                                derivedStateOf {
                                    cardPlaybackState.activeEntryId == entry.id && cardPlaybackState.status == PlaybackStatus.Playing
                                }
                            }
                            val isBuffering by remember(entry.id) {
                                derivedStateOf {
                                    cardPlaybackState.activeEntryId == entry.id && cardPlaybackState.status == PlaybackStatus.Buffering
                                }
                            }
                            val isAudioError by remember(entry.id) {
                                derivedStateOf {
                                    cardPlaybackState.activeEntryId == entry.id && cardPlaybackState.status == PlaybackStatus.Error
                                }
                            }
                            val currentPos by remember(entry.id) {
                                derivedStateOf {
                                    if (cardPlaybackState.activeEntryId == entry.id) cardPlaybackState.currentPositionMs else 0L
                                }
                            }

                            val isSelected = entry.id in selectionState.selectedEntryIds

                            EntryCard(
                                entry = entry,
                                activeTrackId = cardPlaybackState.activeTrackId,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                isAudioError = isAudioError,
                                currentPositionMs = currentPos,
                                timeFormat = uiState.timeFormat,
                                isMoodEnabled = uiState.isMoodEnabled,
                                onPlayPauseTrackClick = { track ->
                                    if (selectionState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        viewModel.playTrack(entry, track)
                                    }
                                },
                                onSeekTrackFraction = { track, fraction -> viewModel.seekTrackToFraction(entry, track, fraction) },
                                onCardClick = {
                                    if (selectionState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        pendingNavEntryId = entry.id
                                        navController.navigate(Screen.NoteDetail.createRoute(entry.id))
                                    }
                                },
                                onImageClick = { imgUri ->
                                    if (selectionState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        selectedLightboxImage = imgUri
                                    }
                                },
                                onTagClick = { tag ->
                                    if (selectionState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        if (tag.type == TagType.PERSON) {
                                            viewModel.togglePersonFilter(tag.name)
                                        } else {
                                            viewModel.toggleTagFilter(tag.name)
                                        }
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = selectionState.isSelectionMode,
                                onLongClick = { viewModel.toggleEntrySelection(entry.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            // Lightbox Modal Image Viewer
            selectedLightboxImage?.let { imageUri ->
                LightboxDialog(
                    imageUri = imageUri,
                    onDismiss = { selectedLightboxImage = null }
                )
            }

            // Confirmation Move to Trash Dialog
            if (showDeleteDialog) {
                val count = selectionState.selectedEntryIds.size
                dev.voicejournal.ui.components.MoveToTrashDialog(
                    message = "Are you sure you want to move $count selected note(s) to Trash?",
                    onConfirm = {
                        viewModel.deleteSelectedEntries()
                        showDeleteDialog = false
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }

            // Batch Categorize Modal Sheet
            if (showCategorizeSheet) {
                val selectedEntries = remember(selectionState.selectedEntryIds, uiState.entries) {
                    uiState.entries.filter { it.id in selectionState.selectedEntryIds }
                }
                val appliedFolder = remember(selectedEntries) {
                    if (selectedEntries.isNotEmpty()) {
                        val firstFolders = selectedEntries.first().tags
                            .filter { it.type == TagType.FOLDER || it.type == TagType.THING }
                            .map { it.name.trim().trimStart('#', '@').trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                        val commonFolders = selectedEntries.drop(1).fold(firstFolders) { acc, entry ->
                            val entryFolders = entry.tags
                                .filter { it.type == TagType.FOLDER || it.type == TagType.THING }
                                .map { it.name.trim().trimStart('#', '@').trim() }
                            acc.filter { folder -> entryFolders.any { it.equals(folder, ignoreCase = true) } }
                        }
                        commonFolders.firstOrNull()
                            ?: selectedEntries.flatMap { it.tags }
                                .firstOrNull { it.type == TagType.FOLDER || it.type == TagType.THING }
                                ?.name?.trim()?.trimStart('#', '@')?.trim()
                    } else null
                }
                val appliedTagNames = remember(selectedEntries) {
                    if (selectedEntries.isNotEmpty()) {
                        val firstTags = selectedEntries.first().tags
                            .map { it.name.trim().trimStart('#', '@').trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                        selectedEntries.drop(1).fold(firstTags) { acc, entry ->
                            val entryCleanTags = entry.tags
                                .map { it.name.trim().trimStart('#', '@').trim() }
                                .filter { it.isNotBlank() }
                            acc.filter { tag -> entryCleanTags.any { it.equals(tag, ignoreCase = true) } }
                        }.toSet()
                    } else emptySet()
                }

                BatchCategorizeSheet(
                    selectedCount = selectionState.selectedEntryIds.size,
                    isFolderEnabled = uiState.isFolderEnabled,
                    isTopicsEnabled = uiState.isTopicsEnabled,
                    isPeopleEnabled = uiState.isPeopleEnabled,
                    availableFolders = uiState.availableTags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }.map { it.name.trim().trimStart('#', '@').trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() },
                    availableTags = uiState.availableTags.filter { it.type == TagType.TOPIC },
                    availablePeople = uiState.availablePeople,
                    appliedFolder = appliedFolder,
                    appliedTagNames = appliedTagNames,
                    onApply = { targetFolder, tagsToAssign, tagsToRemove, folderToRemove ->
                        val count = selectionState.selectedEntryIds.size
                        viewModel.batchCategorizeSelectedEntries(targetFolder, tagsToAssign, tagsToRemove, folderToRemove)
                        showCategorizeSheet = false
                        Toast.makeText(context, "Updated $count notes", Toast.LENGTH_SHORT).show()
                    },
                    onDismissRequest = { showCategorizeSheet = false }
                )
            }

            // Bottom Sheets for Filter Options
            when (activeSheet) {
                ActiveSheet.TAGS -> {
                    TopicFilterBottomSheet(
                        availableTags = uiState.availableTags.filter { it.type == TagType.TOPIC },
                        selectedTagNames = uiState.filterState.selectedTags,
                        onTagToggle = { tagName -> viewModel.toggleTagFilter(tagName) },
                        onClearAll = { viewModel.clearAllFilters() },
                        onDismissRequest = { activeSheet = ActiveSheet.NONE }
                    )
                }
                ActiveSheet.PEOPLE -> {
                    PeopleFilterBottomSheet(
                        availablePeople = uiState.availablePeople,
                        selectedPeople = uiState.filterState.selectedPeople,
                        onPersonToggle = { person -> viewModel.togglePersonFilter(person) },
                        onClearAll = { viewModel.clearAllFilters() },
                        onDismissRequest = { activeSheet = ActiveSheet.NONE }
                    )
                }
                ActiveSheet.MOOD -> {
                    MoodFilterBottomSheet(
                        selectedMoods = uiState.filterState.selectedMoods,
                        onMoodToggle = { mood -> viewModel.toggleMoodFilter(mood) },
                        onClearAll = { viewModel.clearAllFilters() },
                        onDismissRequest = { activeSheet = ActiveSheet.NONE }
                    )
                }
                ActiveSheet.NONE -> {}
            }
        }
    }
}
}
