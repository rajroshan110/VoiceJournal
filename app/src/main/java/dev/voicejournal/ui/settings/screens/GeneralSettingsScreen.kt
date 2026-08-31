package dev.voicejournal.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.ModelDownloadState
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel
import dev.voicejournal.ui.settings.components.SettingDayPickerRow
import dev.voicejournal.ui.settings.components.SettingSegmentedRow
import dev.voicejournal.ui.settings.components.SettingToggleRow
import dev.voicejournal.ui.settings.components.settingHighlight
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.theme.AppThemeMode

@Composable
fun GeneralSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App UI Theme
        val themeSubtitle = when (uiState.appThemeMode) {
            AppThemeMode.SYSTEM -> "System Default Theme"
            AppThemeMode.DARK -> "Sleek Dark Theme"
            AppThemeMode.LIGHT_PREMIUM -> "Warm Premium Journal Light Palette"
        }
        SettingSegmentedRow(
            title = "App UI Theme",
            subtitle = themeSubtitle,
            icon = Icons.Default.Settings,
            options = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT_PREMIUM, AppThemeMode.DARK),
            selectedOption = uiState.appThemeMode,
            onOptionSelected = { viewModel.setAppThemeMode(it) },
            optionLabel = { mode ->
                when (mode) {
                    AppThemeMode.SYSTEM -> "System"
                    AppThemeMode.LIGHT_PREMIUM -> "Light"
                    AppThemeMode.DARK -> "Dark"
                }
            },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "app_theme")
        )

        // Time Format
        val timeFormatSubtitle = when (uiState.timeFormat) {
            TimeFormat.SYSTEM_DEFAULT -> "System Default"
            TimeFormat.TWELVE_HOUR -> "12-Hour (4:32 PM)"
            TimeFormat.TWENTY_FOUR_HOUR -> "24-Hour (16:32)"
        }
        SettingSegmentedRow(
            title = "Time format",
            subtitle = timeFormatSubtitle,
            icon = Icons.Default.Info,
            options = listOf(TimeFormat.SYSTEM_DEFAULT, TimeFormat.TWELVE_HOUR, TimeFormat.TWENTY_FOUR_HOUR),
            selectedOption = uiState.timeFormat,
            onOptionSelected = { viewModel.setTimeFormat(it) },
            optionLabel = { format ->
                when (format) {
                    TimeFormat.SYSTEM_DEFAULT -> "System"
                    TimeFormat.TWELVE_HOUR -> "12 Hr"
                    TimeFormat.TWENTY_FOUR_HOUR -> "24 Hr"
                }
            },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "time_format")
        )

        // Markdown Editor Toggle
        SettingToggleRow(
            title = "Markdown editor",
            subtitle = "Use rich text formatting when writing journals",
            icon = Icons.Default.Edit,
            checked = uiState.isMarkdownEnabled,
            onCheckedChange = { viewModel.setMarkdownEnabled(it) },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "is_markdown_enabled")
        )

        // Start of the Week
        val startOfWeekSubtitle = when (uiState.startOfWeek) {
            StartOfWeek.SYSTEM_DEFAULT -> "System Default"
            StartOfWeek.MONDAY -> "Monday"
            StartOfWeek.SUNDAY -> "Sunday"
        }
        SettingDayPickerRow(
            title = "Start of the week",
            subtitle = startOfWeekSubtitle,
            icon = Icons.Default.DateRange,
            selectedDay = uiState.startOfWeek,
            onDaySelected = { viewModel.setStartOfWeek(it) },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "start_of_week")
        )

        // Insight Date Range Mode
        val dateRangeSubtitle = when (uiState.insightDateRangeMode) {
            InsightDateRangeMode.LAST_DAYS -> "Last 7 / 30 Days (Rolling window)"
            InsightDateRangeMode.CURRENT_CALENDAR -> "Current Calendar Week / Month"
        }
        SettingSegmentedRow(
            title = "Insight Date Range Mode",
            subtitle = dateRangeSubtitle,
            icon = Icons.Default.DateRange,
            options = listOf(InsightDateRangeMode.LAST_DAYS, InsightDateRangeMode.CURRENT_CALENDAR),
            selectedOption = uiState.insightDateRangeMode,
            onOptionSelected = { viewModel.setInsightDateRangeMode(it) },
            optionLabel = { mode ->
                when (mode) {
                    InsightDateRangeMode.LAST_DAYS -> "7 / 30 Days"
                    InsightDateRangeMode.CURRENT_CALENDAR -> "Month"
                }
            },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "insight_date_range")
        )

        // Audio Recording Quality
        val audioSubtitle = when (uiState.audioFormat) {
            AudioFormat.WAV_16KHZ -> "Uncompressed Lossless PCM · ~1.92 MB/min"
            AudioFormat.M4A_AAC_128KBPS -> "Compressed AAC 128kbps · ~0.96 MB/min"
        }
        SettingSegmentedRow(
            title = "Audio Recording Quality",
            subtitle = audioSubtitle,
            icon = Icons.Default.Share,
            options = listOf(AudioFormat.WAV_16KHZ, AudioFormat.M4A_AAC_128KBPS),
            selectedOption = uiState.audioFormat,
            onOptionSelected = { viewModel.setAudioFormat(it) },
            optionLabel = { format ->
                when (format) {
                    AudioFormat.WAV_16KHZ -> "WAV"
                    AudioFormat.M4A_AAC_128KBPS -> "M4A"
                }
            },
            modifier = Modifier.settingHighlight(uiState.highlightedSettingKey == "audio_format")
        )

        // Local Speech-to-Text Model Section (Redesigned Clean Layout)
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .settingHighlight(uiState.highlightedSettingKey == "whisper_model")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
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
                            text = "Speech-to-Text Model",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isSpeechToTextEnabled) "Offline Whisper Base Q5 model" else "Offline transcription disabled",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = uiState.isSpeechToTextEnabled,
                        onCheckedChange = { viewModel.setSpeechToTextEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                if (uiState.isSpeechToTextEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    when (uiState.sttModelDownloadState) {
                        is ModelDownloadState.Downloaded -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Model ready (59 MB)",
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.deleteWhisperModel() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Model", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is ModelDownloadState.Downloading -> {
                            Column {
                                LinearProgressIndicator(
                                    progress = { uiState.downloadProgress ?: 0f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Downloading... ${((uiState.downloadProgress ?: 0f) * 100).toInt()}%", color = colors.textSecondary, fontSize = 12.sp)
                                    TextButton(onClick = { viewModel.cancelModelDownload() }) {
                                        Text("Cancel", color = colors.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        is ModelDownloadState.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = colors.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(onClick = { viewModel.downloadWhisperModel() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry Download", color = colors.primary, fontSize = 12.sp)
                                }
                            }
                        }
                        is ModelDownloadState.Idle -> {
                            if (!uiState.isModelDownloaded) {
                                Button(
                                    onClick = { viewModel.downloadWhisperModel() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Whisper Base Q5 (59 MB)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
