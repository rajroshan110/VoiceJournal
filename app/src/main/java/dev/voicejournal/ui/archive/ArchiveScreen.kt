package dev.voicejournal.ui.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.ui.components.getArchiveIcon
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.folders.components.FolderNoteCard
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    navController: NavController,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    var isSearchVisible by remember { mutableStateOf(false) }
    var showUnarchiveConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionState.isSelectionMode || isSearchVisible) {
        if (selectionState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (isSearchVisible) {
            isSearchVisible = false
            viewModel.setSearchQuery("")
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
                if (selectionState.isSelectionMode) {
                    // Selection Mode Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Selection",
                                    tint = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectionState.selectedCount} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Unarchive Icon Button
                            IconButton(onClick = { showUnarchiveConfirmDialog = true }) {
                                Icon(
                                    imageVector = getArchiveIcon(colors.primary),
                                    contentDescription = "Unarchive Selected Notes",
                                    tint = colors.primary
                                )
                            }

                            // Delete Icon Button (Move to Trash)
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Move Selected Notes to Trash",
                                    tint = colors.error
                                )
                            }
                        }
                    }
                } else {
                    // Standard Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchVisible) {
                            SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                placeholder = "Search archived notes...",
                                requestFocusOnLaunch = true,
                                onClose = {
                                    isSearchVisible = false
                                    viewModel.setSearchQuery("")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Archive",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            IconButton(onClick = { isSearchVisible = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Archived Notes",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (uiState.entries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Archived Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes you archive from Note Detail will appear here, keeping your main feed clean.",
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
                                    imageVector = getArchiveIcon(colors.primary),
                                    contentDescription = null,
                                    tint = colors.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${uiState.entries.size} ${if (uiState.entries.size == 1) "note" else "notes"} in Archive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    items(uiState.entries, key = { it.id }) { entry ->
                        val isSelected = entry.id in selectionState.selectedIds
                        FolderNoteCard(
                            entry = entry,
                            isSelected = isSelected,
                            onClick = {
                                if (selectionState.isSelectionMode) {
                                    viewModel.toggleSelection(entry.id)
                                } else {
                                    navController.navigate(Screen.NoteDetail.createRoute(entry.id))
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelectionMode(entry.id)
                            },
                            timeFormat = uiState.timeFormat
                        )
                    }
                }
            }
        }
    }

    // Unarchive Confirmation Dialog
    if (showUnarchiveConfirmDialog) {
        val count = selectionState.selectedCount
        AlertDialog(
            onDismissRequest = { showUnarchiveConfirmDialog = false },
            title = { Text("Unarchive notes?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to unarchive $count selected note(s)? They will be restored to your Journal and Calendar tabs.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnarchiveConfirmDialog = false
                        viewModel.unarchiveSelectedEntries()
                    }
                ) {
                    Text("Unarchive", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog (Move to Trash)
    if (showDeleteConfirmDialog) {
        val count = selectionState.selectedCount
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Trash?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to move $count selected note(s) to Trash?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSelectedEntries()
                    }
                ) {
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
