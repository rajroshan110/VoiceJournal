package dev.voicejournal.ui.settings.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.saveable.rememberSaveable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel
import dev.voicejournal.ui.settings.components.settingHighlight
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.io.File

@Composable
fun LocalBackupScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    var pendingImportUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    uiState.backupResultDialog?.let { dialogInfo ->
        dev.voicejournal.ui.components.BackupResultAlertDialog(
            dialogInfo = dialogInfo,
            onDismiss = { viewModel.dismissBackupResultDialog() }
        )
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

    pendingImportUri?.let { uri ->
        dev.voicejournal.ui.components.BackupRestoreConfirmationDialog(
            onConfirm = {
                pendingImportUri = null
                viewModel.importBackupFromUri(uri)
            },
            onDismiss = { pendingImportUri = null }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Export & Import Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .settingHighlight(uiState.highlightedSettingKey in listOf("export_backup", "import_backup"))
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Security Warning
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backups are unencrypted. Store them securely.",
                            color = colors.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isExporting || uiState.isImporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = colors.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.backupProgressText ?: (if (uiState.isExporting) "Preparing backup…" else "Validating backup…"),
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
                                onClick = {
                                    dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                                    exportLauncher.launch("VoiceJournal_Backup_${System.currentTimeMillis()}.vjbackup.zip")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export\nBackup", textAlign = TextAlign.Center)
                            }

                            OutlinedButton(
                                onClick = {
                                    dev.voicejournal.ui.util.AppLockStateManager.notifySystemPickerLaunched()
                                    importLauncher.launch(arrayOf("application/zip"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Import\nBackup", textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Flexible Spacer to push privacy context note to footer
            Spacer(modifier = Modifier.weight(1f))

            // Local-First Architecture Note (Footer Context)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Local-First Architecture",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VoiceJournal operates without third-party cloud synchronization to protect your privacy and ensure complete data ownership. You can create full, portable backup archives anytime and transfer them securely to any device or storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
