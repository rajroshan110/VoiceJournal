package dev.voicejournal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun RenameItemDialog(
    itemTitle: String,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = AppTheme.colors
    var nameInput by rememberSaveable { mutableStateOf(currentName.removePrefix("#").removePrefix("@")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = itemTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "All associated entries will be updated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Enter new name...", color = colors.textSecondary) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput.trim())
                    }
                },
                enabled = nameInput.isNotBlank() && nameInput.trim() != currentName.removePrefix("#").removePrefix("@"),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeItemDialog(
    title: String,
    sourceTag: Tag,
    targetCandidates: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (targetTag: Tag) -> Unit
) {
    val colors = AppTheme.colors
    var selectedTarget by remember { mutableStateOf<Tag?>(targetCandidates.firstOrNull()) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Merge \"${sourceTag.name}\" into another item. All entries will be moved to the selected target, and duplicate associations will be consolidated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (targetCandidates.isEmpty()) {
                    Text(
                        text = "No other target items available to merge into.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTarget?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target", color = colors.textSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            targetCandidates.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.name, color = colors.textPrimary) },
                                    onClick = {
                                        selectedTarget = candidate
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedTarget
                    if (target != null) {
                        onConfirm(target)
                    }
                },
                enabled = selectedTarget != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
