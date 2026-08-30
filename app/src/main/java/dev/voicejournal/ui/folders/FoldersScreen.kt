package dev.voicejournal.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.ui.components.getCreateNewFolderIcon
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getGridViewIcon
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.folders.components.FolderCardGrid
import dev.voicejournal.ui.folders.components.FolderCardList
import dev.voicejournal.ui.components.getListViewIcon
import dev.voicejournal.ui.journal.PlaybackStatus
import dev.voicejournal.ui.journal.components.EntryCard
import dev.voicejournal.ui.journal.components.LightboxDialog
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.util.findActivity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    navController: NavController,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardPlaybackState by viewModel.cardPlaybackState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    var selectedLightboxImage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    // Handle system back button to exit selected folder first, or navigate back
    BackHandler(enabled = uiState.selectedFolder != null || isSearchVisible) {
        if (isSearchVisible) {
            isSearchVisible = false
            viewModel.setSearchQuery("")
        } else if (uiState.selectedFolder != null) {
            viewModel.selectFolder(null)
        }
    }

    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findActivity()
            if (activity?.isChangingConfigurations != true) {
                viewModel.stopAudioOnLeave()
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNavBar(navController = navController) },
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isSearchVisible && uiState.selectedFolder == null) {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search folders...",
                            requestFocusOnLaunch = true,
                            onClose = {
                                isSearchVisible = false
                                viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (uiState.selectedFolder != null) {
                                    viewModel.selectFolder(null)
                                } else {
                                    navController.popBackStack()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = uiState.selectedFolder?.name ?: "Folder Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.selectedFolder == null) {
                                IconButton(onClick = { isSearchVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Folders",
                                        tint = colors.textSecondary
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleViewMode() }) {
                                    Icon(
                                        imageVector = if (uiState.isGridView) getListViewIcon(colors.textSecondary) else getGridViewIcon(colors.textSecondary),
                                        contentDescription = "Toggle view mode",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val selectedFolder = uiState.selectedFolder
            FloatingActionButton(
                onClick = {
                    if (selectedFolder == null) {
                        viewModel.setCreateFolderDialogOpen(true)
                    } else {
                        navController.navigate(Screen.NoteDetail.createRoute(-1L, initialFolder = selectedFolder.name))
                    }
                },
                containerColor = colors.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = if (selectedFolder == null) "New Folder" else "New Note")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val selectedFolder = uiState.selectedFolder

            if (selectedFolder != null) {
                // Folder Notes View
                val notes = uiState.notesInSelectedFolder

                if (notes.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📁", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notes in '${selectedFolder.name}'",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add this folder tag to your journal entries or tap '+' to create a new note here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Surface(
                                color = colors.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = getFolderIcon(colors.primary),
                                        contentDescription = null,
                                        tint = colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${notes.size} ${if (notes.size == 1) "note" else "notes"} in ${selectedFolder.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }

                        items(notes, key = { it.id }) { entry ->
                            dev.voicejournal.ui.folders.components.FolderNoteCard(
                                entry = entry,
                                onClick = {
                                    navController.navigate(Screen.NoteDetail.createRoute(entry.id))
                                },
                                timeFormat = uiState.timeFormat
                            )
                        }
                    }
                }
            } else {
                // All Folders View
                val filteredFolders = remember(uiState.folders, uiState.searchQuery) {
                    if (uiState.searchQuery.isBlank()) {
                        uiState.folders
                    } else {
                        uiState.folders.filter {
                            it.tag.name.contains(uiState.searchQuery, ignoreCase = true)
                        }
                    }
                }

                if (filteredFolders.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📁", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isBlank()) "No folders created yet" else "No matching folders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.searchQuery.isBlank())
                                    "Tap '+' to create a new folder, or add folder tags to your journal notes!"
                                else "Try a different search keyword.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.setCreateFolderDialogOpen(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create New Folder")
                                }
                            }
                        }
                    }
                } else if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFolders, key = { it.tag.id }) { item ->
                            FolderCardGrid(
                                folderItem = item,
                                onClick = { viewModel.selectFolder(item.tag) },
                                onRename = { viewModel.setFolderToRename(item.tag) },
                                onMerge = { viewModel.setFolderToMerge(item.tag) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFolders, key = { it.tag.id }) { item ->
                            FolderCardList(
                                folderItem = item,
                                onClick = { viewModel.selectFolder(item.tag) },
                                onRename = { viewModel.setFolderToRename(item.tag) },
                                onMerge = { viewModel.setFolderToMerge(item.tag) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (uiState.isCreateFolderDialogOpen) {
        var folderNameInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.setCreateFolderDialogOpen(false) },
            title = {
                Text(
                    text = "New Folder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    placeholder = { Text("Enter folder name...", color = colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createFolder(folderNameInput) },
                    enabled = folderNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setCreateFolderDialogOpen(false) }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rename Folder Dialog
    val folderToRename = uiState.folderToRename
    if (folderToRename != null) {
        dev.voicejournal.ui.components.RenameItemDialog(
            itemTitle = "Rename Folder",
            currentName = folderToRename.name,
            onDismiss = { viewModel.setFolderToRename(null) },
            onConfirm = { newName -> viewModel.renameFolder(folderToRename, newName) }
        )
    }

    // Merge Folder Dialog
    val folderToMerge = uiState.folderToMerge
    if (folderToMerge != null) {
        val targetCandidates = uiState.folders.map { it.tag }.filter { it.id != folderToMerge.id }
        dev.voicejournal.ui.components.MergeItemDialog(
            title = "Merge Folder",
            sourceTag = folderToMerge,
            targetCandidates = targetCandidates,
            onDismiss = { viewModel.setFolderToMerge(null) },
            onConfirm = { targetTag -> viewModel.mergeFolder(folderToMerge, targetTag) }
        )
    }

    // Lightbox for note images
    if (selectedLightboxImage != null) {
        LightboxDialog(
            imageUri = selectedLightboxImage!!,
            onDismiss = { selectedLightboxImage = null }
        )
    }
}
