package dev.voicejournal.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel
import dev.voicejournal.ui.settings.components.DestructiveActionRow
import dev.voicejournal.ui.settings.components.settingHighlight
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun PrivacySecurityScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    if (uiState.isViewingAppLockDetail) {
        AppLockDetailScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBackClick = { viewModel.setViewingAppLockDetail(false) },
            modifier = modifier
        )
    } else {
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
                // App Lock Card - Navigates to App Lock Detail Screen
                val appLockSubtitle = when (uiState.appLockMode) {
                    AppLockMode.NONE -> "No lock"
                    AppLockMode.BIOMETRIC -> "Same as screen lock"
                    AppLockMode.CUSTOM_PIN -> "Custom PIN"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingHighlight(uiState.highlightedSettingKey == "app_lock_mode")
                        .clickable { viewModel.setViewingAppLockDetail(true) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.secondaryBackground, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Lock",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = appLockSubtitle,
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Screen Privacy Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingHighlight(uiState.highlightedSettingKey == "is_screen_privacy_enabled")
                        .clickable { viewModel.setScreenPrivacyEnabled(!uiState.isScreenPrivacyEnabled) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.secondaryBackground, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Privacy",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Prevent screenshots and hide app content in recents",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = uiState.isScreenPrivacyEnabled,
                            onCheckedChange = { viewModel.setScreenPrivacyEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceVariant
                            )
                        )
                    }
                }

                // Data Management - Delete All Journals (positioned directly below Screen Privacy)
                DestructiveActionRow(
                    title = "Delete All Journals",
                    subtitle = "Permanently wipe database tables and local media files",
                    onClick = { viewModel.initiateDeleteAllJournals() },
                    modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "delete_all_journals")
                )

                // Flexible Spacer to push privacy context note to footer
                Spacer(modifier = Modifier.weight(1f))

                // On-Device Privacy Note (Footer Context)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "On-Device Privacy",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VoiceJournal is built with a local-first design. Your recordings, transcriptions, and journal entries are processed and stored on your device, keeping your data under your control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
