package dev.voicejournal.ui.draft

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.voicejournal.ui.components.getDraftIcon
import dev.voicejournal.ui.folders.components.FolderNoteCard
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun DraftScreen(
    navController: NavController,
    viewModel: DraftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectionState by viewModel.selectionState.collectAsState()
    val colors = AppTheme.colors

    var isSearchVisible by remember { mutableStateOf(false) }
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

                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move Selected Drafts to Trash",
                                tint = colors.error
                            )
                        }
                    }
                } else {
                    // Standard Top Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    text = "Drafts",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Drafts",
                                    tint = if (isSearchVisible) colors.primary else colors.textSecondary
                                )
                            }
                        }

                        if (isSearchVisible) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search drafts...", color = colors.textSecondary, fontSize = 14.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border,
                                    focusedContainerColor = colors.surfaceVariant,
                                    unfocusedContainerColor = colors.surfaceVariant,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
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
                        Text("📝", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Draft Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unsaved notes and ongoing drafts will automatically appear here until you save them.",
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
                                    imageVector = getDraftIcon(colors.primary),
                                    contentDescription = null,
                                    tint = colors.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${uiState.entries.size} ${if (uiState.entries.size == 1) "draft" else "drafts"} in progress",
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

    // Delete Confirmation Dialog (Move to Trash)
    if (showDeleteConfirmDialog) {
        val count = selectionState.selectedCount
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Trash?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to move $count selected draft(s) to Trash?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSelectedDrafts()
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
