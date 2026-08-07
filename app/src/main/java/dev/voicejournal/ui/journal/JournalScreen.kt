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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.journal.components.*
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.theme.AppTheme
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
    var selectedLightboxImage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCategorizeSheet by rememberSaveable { mutableStateOf(false) }

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

    var pendingNavEntryId by remember { mutableStateOf<Long?>(null) }

    // Handle back button presses for navigation drawer, selection mode and search bar
    BackHandler(enabled = drawerState.isOpen || selectionState.isSelectionMode || uiState.isSearchActive) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (selectionState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (uiState.isSearchActive) {
            viewModel.toggleSearch()
        }
    }

    // Stop audio, clear selection & close search when leaving screen
    DisposableEffect(pendingNavEntryId) {
        onDispose {
            viewModel.stopAudioOnLeave(pendingNavEntryId)
            if (uiState.isSearchActive) {
                viewModel.toggleSearch()
            }
            if (selectionState.isSelectionMode) {
                viewModel.clearSelection()
            }
        }
    }

    // Trigger Toast on playback error state
    LaunchedEffect(cardPlaybackState.status) {
        if (cardPlaybackState.status == PlaybackStatus.Error && cardPlaybackState.errorMessage != null) {
            Toast.makeText(context, cardPlaybackState.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selectionState.isSelectionMode,
        drawerContent = {
            JournalDrawerContent(
                onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                onNavigateToDraft = { navController.navigate(Screen.Draft.route) },
                onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                onNavigateToTags = { navController.navigate(Screen.Tags.route) },
                onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
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
                        onCategorizeSelected = { showCategorizeSheet = true },
                        onDeleteSelected = { showDeleteDialog = true },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                    FilterBar(
                        selectedTagsCount = uiState.filterState.selectedTags.size,
                        selectedPeopleCount = uiState.filterState.selectedPeople.size,
                        selectedMoodsCount = uiState.filterState.selectedMoods.size,
                        onAllClick = { viewModel.clearAllFilters() },
                        onTagsClick = { activeSheet = ActiveSheet.TAGS },
                        onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                        onMoodClick = { activeSheet = ActiveSheet.MOOD }
                    )
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
                        modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp)
                    ) {
                        repeat(3) {
                            ShimmerSkeletonCard()
                        }
                    }
                }
                is FeedState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Button(
                                    onClick = { viewModel.retryFeedLoad() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                is FeedState.EmptyGlobal -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = MicIcon,
                            contentDescription = "No journal entries",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No journal entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Capture your thoughts and voice notes to start your journal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate(Screen.NoteDetail.createRoute(-1L)) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create new entry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Your First Note")
                        }
                    }
                }
                is FeedState.EmptyFiltered -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = FilterOffIcon,
                            contentDescription = "No matching entries",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No matching entries found",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your filters or search terms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearAllFilters() },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Clear Filters")
                        }
                    }
                }
                is FeedState.Success -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!uiState.permissionGranted) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2B2618),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Storage permission required to play voice notes and view media previews",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFFD54F)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.setPermissionGranted(true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black)
                                        ) {
                                            Text("Grant Permission", fontSize = 12.sp)
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

            // Confirmation Delete Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = "Delete ${selectionState.selectedEntryIds.size} notes?") },
                    text = { Text(text = "Are you sure you want to delete the selected notes? This action cannot be undone and will permanently free up storage space.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelectedEntries()
                                showDeleteDialog = false
                            }
                        ) {
                            Text(text = "Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(text = "Cancel", color = AppTheme.colors.textSecondary)
                        }
                    },
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary,
                    textContentColor = AppTheme.colors.textSecondary
                )
            }

            // Batch Categorize Modal Sheet
            if (showCategorizeSheet) {
                val selectedEntries = remember(selectionState.selectedEntryIds, uiState.entries) {
                    uiState.entries.filter { it.id in selectionState.selectedEntryIds }
                }
                val appliedFolder = remember(selectedEntries) {
                    if (selectedEntries.isNotEmpty()) {
                        val folderSets = selectedEntries.map { entry ->
                            entry.tags.filter { it.type == TagType.FOLDER }.map { it.name }.toSet()
                        }
                        folderSets.reduce { acc, set -> acc.intersect(set) }.firstOrNull()
                            ?: selectedEntries.flatMap { it.tags }.find { it.type == TagType.FOLDER }?.name
                    } else null
                }
                val appliedTagNames = remember(selectedEntries) {
                    if (selectedEntries.isNotEmpty()) {
                        val firstTags = selectedEntries.first().tags.map { it.name.trim().removePrefix("#").removePrefix("@") }.toSet()
                        selectedEntries.fold(firstTags) { acc, entry ->
                            acc.intersect(entry.tags.map { it.name.trim().removePrefix("#").removePrefix("@") }.toSet())
                        }
                    } else emptySet()
                }

                BatchCategorizeSheet(
                    selectedCount = selectionState.selectedEntryIds.size,
                    availableFolders = (listOf("Personal", "Work", "Ideas", "Journal") + uiState.availableTags.filter { it.type == TagType.FOLDER }.map { it.name }).distinct(),
                    availableTags = uiState.availableTags,
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
                        availableTags = uiState.availableTags,
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
