package dev.voicejournal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.designsystem.components.buttons.AppPrimaryButton
import dev.voicejournal.ui.designsystem.components.dialogs.AppAlertDialog
import dev.voicejournal.ui.designsystem.components.menus.AppDropdownMenuItem
import dev.voicejournal.ui.designsystem.components.menus.AppExposedDropdownMenu
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing

@Composable
fun RenameItemDialog(
    itemTitle: String,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = AppTheme.colors
    var nameInput by rememberSaveable { mutableStateOf(currentName.removePrefix("#").removePrefix("@")) }

    AppAlertDialog(
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
                    modifier = Modifier.padding(bottom = Spacing.SpaceMd)
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = {
                        Text(
                            text = "Enter new name...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(Radius.RadiusMd),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput.trim())
                    }
                },
                enabled = nameInput.isNotBlank() && nameInput.trim() != currentName.removePrefix("#").removePrefix("@")
            ) {
                Text("Rename", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }
        }
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

    AppAlertDialog(
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
                    modifier = Modifier.padding(bottom = Spacing.SpaceLg)
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            label = {
                                Text(
                                    text = "Select Target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(Radius.RadiusMd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        AppExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            targetCandidates.forEach { candidate ->
                                AppDropdownMenuItem(
                                    text = candidate.name,
                                    isSelected = candidate == selectedTarget,
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
            AppPrimaryButton(
                onClick = {
                    val target = selectedTarget
                    if (target != null) {
                        onConfirm(target)
                    }
                },
                enabled = selectedTarget != null
            ) {
                Text("Merge", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }
        }
    )
}
