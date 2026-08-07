package dev.voicejournal.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = AppTheme.colors


    SettingsHomeScreen(
        onBackClick = { navController.popBackStack() },
        viewModel = viewModel,
        uiState = uiState
    )

    // Modal Progress Overlay during Database Wipe
    if (uiState.isWipingData) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = colors.error, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Wiping Journal Data", color = colors.textPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Wiping database and local media files...",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {}
        )
    }

    // Set Custom PIN Dialog
    if (uiState.showPinSetupDialog) {
        var pinInput by rememberSaveable { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissPinSetupDialog() },
            containerColor = colors.surface,
            title = { Text("Set Custom PIN", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-digit security PIN for locking your journal.", color = colors.textSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("Enter 4-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmInput = it },
                        label = { Text("Confirm 4-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage!!, color = colors.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length < 4) {
                            errorMessage = "PIN must be 4 digits"
                        } else if (pinInput != confirmInput) {
                            errorMessage = "PINs do not match"
                        } else {
                            viewModel.saveCustomPin(pinInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPinSetupDialog() }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Two-Step Explicit Text Confirmation Dialog ("DELETE") for Delete All Journals
    if (uiState.showDeleteConfirmationDialog) {
        var inputCode by remember { mutableStateOf("") }
        val isConfirmEnabled = inputCode.trim() == "DELETE"

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Delete All Journal Entries?",
                    color = colors.error,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This action will permanently erase all journal entries, tags, cross-references, and local audio recordings from storage.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Type \"DELETE\" below to confirm:",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        singleLine = true,
                        placeholder = { Text("DELETE", color = colors.textSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.error,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteAllJournals() },
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        disabledContainerColor = colors.error.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Permanently Wipe All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
