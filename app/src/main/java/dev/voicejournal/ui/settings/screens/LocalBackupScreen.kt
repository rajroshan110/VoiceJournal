package dev.voicejournal.ui.settings.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel
import dev.voicejournal.ui.theme.AppTheme
import java.io.File

@Composable
fun LocalBackupScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportBackupToUri(it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
        }
    }

    if (pendingImportUri != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = {
                Text(
                    text = "Restore Backup",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Restoring a backup will replace all current Voice Journal data on this device, including notes, media, tags, and settings.\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val uriToImport = pendingImportUri
                        pendingImportUri = null
                        uriToImport?.let { viewModel.importBackupFromUri(it) }
                    }
                ) {
                    Text(
                        text = "Restore",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { pendingImportUri = null }
                ) {
                    Text(
                        text = "Cancel",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            containerColor = colors.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cloud Sync Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cloud Sync",
                    color = colors.primary,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Local First Architecture (Active)",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "All voice recordings and transcripts remain stored exclusively on your device for absolute privacy.",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Local Export & Import Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Local Backup & Restore",
                    color = colors.primary,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export or import your journal entries, tags, and audio recordings as a zip archive via Storage Access Framework.",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isExporting || uiState.isImporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isExporting) "Exporting backup ZIP..." else "Importing backup ZIP...",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("VoiceJournal_Backup_${System.currentTimeMillis()}.vjbackup.zip") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Backup", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip")) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Backup", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
