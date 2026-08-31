package dev.voicejournal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.settings.BackupResultDialog

/**
 * Reusable confirmation dialog for restoring a backup archive.
 * Used consistently across Settings (Sync & Backup) and Journal Empty State.
 */
@Composable
fun BackupRestoreConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Restore",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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

/**
 * Reusable alert dialog for reporting backup and restore results (success / failure).
 */
@Composable
fun BackupResultAlertDialog(
    dialogInfo: BackupResultDialog,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogInfo.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = dialogInfo.message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        containerColor = colors.surface
    )
}

/**
 * Reusable progress overlay dialog while a restore operation is underway.
 */
@Composable
fun BackupRestoreProgressDialog(
    progressText: String?
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "Restoring Backup",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = progressText ?: "Restoring data…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {},
        containerColor = colors.surface
    )
}
