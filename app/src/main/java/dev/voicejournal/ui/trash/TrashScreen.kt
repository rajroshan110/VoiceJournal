package dev.voicejournal.ui.trash

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.trash.components.TrashNoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    navController: NavController,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val context = LocalContext.current
    var lastToastTime by remember { mutableLongStateOf(0L) }

    // Handle system back button to exit selection mode first
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.clearSelection()
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
                                text = if (uiState.isSelectionMode) {
                                    "${uiState.selectedEntryIds.size} Selected"
                                } else {
                                    "Trash"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.allTrashEntries.isNotEmpty()) {
                                if (uiState.isSelectionMode) {
                                    // Select All button
                                    IconButton(onClick = { viewModel.selectAll() }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Select All",
                                            tint = colors.primary
                                        )
                                    }
                                } else {
                                    // Toggle Selection Mode button
                                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Select Notes",
                                            tint = colors.textSecondary
                                        )
                                    }

                                    // Clear Trash Header Button
                                    IconButton(onClick = { viewModel.setClearTrashDialogOpen(true) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear Trash",
                                            tint = colors.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedEntryIds.isNotEmpty()) {
                Surface(
                    color = colors.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Restore Action
                        Button(
                            onClick = { viewModel.setRestoreDialogOpen(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore (${uiState.selectedEntryIds.size})")
                        }

                        // Delete Permanently Action
                        Button(
                            onClick = { viewModel.setDeleteSelectedDialogOpen(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            } else if (uiState.allTrashEntries.isEmpty()) {
                // Empty Trash State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗑️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Trash is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes placed in trash are automatically deleted permanently after 7 days.",
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
                    // Section 1: Expiring Soon (1 day left)
                    if (uiState.expiringSoonEntries.isNotEmpty()) {
                        item {
                            Surface(
                                color = colors.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Expiring Soon (1 day left)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.error
                                    )
                                }
                            }
                        }

                        items(uiState.expiringSoonEntries, key = { it.id }) { entry ->
                            TrashNoteCard(
                                entry = entry,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = entry.id in uiState.selectedEntryIds,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (now - lastToastTime > 4000L) {
                                            Toast.makeText(context, "Restore to view the note", Toast.LENGTH_SHORT).show()
                                            lastToastTime = now
                                        }
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectionMode(entry.id)
                                },
                                timeFormat = uiState.timeFormat
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Section 2: Deleting Later (> 1 day left)
                    if (uiState.deletingLaterEntries.isNotEmpty()) {
                        item {
                            Surface(
                                color = colors.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "🕒 Deleting Later (> 1 day left)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }

                        items(uiState.deletingLaterEntries, key = { it.id }) { entry ->
                            TrashNoteCard(
                                entry = entry,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = entry.id in uiState.selectedEntryIds,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (now - lastToastTime > 4000L) {
                                            Toast.makeText(context, "Restore to view the note", Toast.LENGTH_SHORT).show()
                                            lastToastTime = now
                                        }
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
    }

    // Dialog for Clear Trash (Empty All)
    if (uiState.isClearTrashDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setClearTrashDialogOpen(false) },
            title = {
                Text(
                    text = "Empty Trash?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "All ${uiState.allTrashEntries.size} notes in trash will be permanently deleted. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.emptyTrash() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setClearTrashDialogOpen(false) }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog for Delete Selected Permanently
    if (uiState.isDeleteSelectedDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setDeleteSelectedDialogOpen(false) },
            title = {
                Text(
                    text = "Permanently Delete Notes?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "${uiState.selectedEntryIds.size} selected note(s) will be permanently deleted from device storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.permanentlyDeleteSelectedEntries() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDeleteSelectedDialogOpen(false) }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog for Restore Selected Notes Confirmation
    if (uiState.isRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setRestoreDialogOpen(false) },
            title = {
                Text(
                    text = "Restore Notes?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to restore ${uiState.selectedEntryIds.size} selected note(s) back to your journal entries?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.restoreSelectedEntries() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRestoreDialogOpen(false) }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
