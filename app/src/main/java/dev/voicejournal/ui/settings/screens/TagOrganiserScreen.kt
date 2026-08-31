package dev.voicejournal.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog

@Composable
fun TagOrganiserScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showChooseTagsDialog by rememberSaveable { mutableStateOf(false) }

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
            // 1. Choose Tags Card (Interactive configuration)
            val activeTagsSummary = remember(uiState.isTopicsEnabled, uiState.isPeopleEnabled, uiState.isMoodEnabled) {
                listOfNotNull(
                    if (uiState.isTopicsEnabled) "Topics" else null,
                    if (uiState.isPeopleEnabled) "People" else null,
                    if (uiState.isMoodEnabled) "Mood" else null
                ).joinToString(", ").ifEmpty { "None" }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showChooseTagsDialog = true }
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
                            imageVector = getTagIcon(colors.textPrimary),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose Tags",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Active: $activeTagsSummary",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Configure Tags",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Enable Folder Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setFolderEnabled(!uiState.isFolderEnabled) }
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
                            imageVector = getFolderIcon(colors.textPrimary),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Folder",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Show folders in sidebar and notes. Existing folders remain safely preserved when turned off.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = uiState.isFolderEnabled,
                        onCheckedChange = { viewModel.setFolderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }

            // 3. Notes Organisation Card
            val isOrganisationAvailable = uiState.isFolderEnabled || uiState.isTopicsEnabled || uiState.isPeopleEnabled
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOrganisationAvailable) colors.surface else colors.surface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isOrganisationAvailable) { 
                        viewModel.setNotesOrganisationEnabled(!uiState.isNotesOrganisationEnabled) 
                    }
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
                            .background(
                                if (isOrganisationAvailable) colors.secondaryBackground else colors.secondaryBackground.copy(alpha = 0.5f), 
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTagIcon(if (isOrganisationAvailable) colors.textPrimary else colors.textSecondary),
                            contentDescription = null,
                            tint = if (isOrganisationAvailable) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notes Organisation",
                            color = if (isOrganisationAvailable) colors.textPrimary else colors.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isOrganisationAvailable) {
                                "Show organise action button when selecting notes in the journal screen"
                            } else {
                                "Enable at least one tag category (Folders, Topics, or People) to use notes organisation"
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = uiState.isNotesOrganisationEnabled && isOrganisationAvailable,
                        enabled = isOrganisationAvailable,
                        onCheckedChange = { viewModel.setNotesOrganisationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }

            // Flexible Spacer to push context note to footer
            Spacer(modifier = Modifier.weight(1f))

            // Footer Context Note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tag Organiser",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Control how your journal entries are categorised. Toggle folder support and organisation tools to match your workflow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showChooseTagsDialog) {
        ChooseTagsDialog(
            isTopicsEnabled = uiState.isTopicsEnabled,
            isPeopleEnabled = uiState.isPeopleEnabled,
            isMoodEnabled = uiState.isMoodEnabled,
            onToggleTopics = { viewModel.setTopicsEnabled(it) },
            onTogglePeople = { viewModel.setPeopleEnabled(it) },
            onToggleMood = { viewModel.setMoodEnabled(it) },
            onDismiss = { showChooseTagsDialog = false }
        )
    }
}

@Composable
private fun ChooseTagsDialog(
    isTopicsEnabled: Boolean,
    isPeopleEnabled: Boolean,
    isMoodEnabled: Boolean,
    onToggleTopics: (Boolean) -> Unit,
    onTogglePeople: (Boolean) -> Unit,
    onToggleMood: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column {
                    Text(
                        text = "Choose Tags",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select which tag categories and indicators appear across your journals, filters, and insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                // 1. Topics Row
                TagCategoryToggleRow(
                    iconText = "#",
                    title = "Topics",
                    subtitle = "Organise notes with topic hashtags (#work, #ideas)",
                    checked = isTopicsEnabled,
                    onCheckedChange = onToggleTopics
                )

                // 2. People Row
                TagCategoryToggleRow(
                    iconText = "@",
                    title = "People",
                    subtitle = "Mention and link people in notes (@john, @sarah)",
                    checked = isPeopleEnabled,
                    onCheckedChange = onTogglePeople
                )

                // 3. Mood Row
                TagCategoryToggleRow(
                    iconText = "😊",
                    title = "Mood",
                    subtitle = "Track emotional context and mood trends with emojis",
                    checked = isMoodEnabled,
                    onCheckedChange = onToggleMood
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        color = colors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TagCategoryToggleRow(
    iconText: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.secondaryBackground, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                fontSize = if (iconText.length == 1 && !iconText.startsWith("#") && !iconText.startsWith("@")) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceVariant
            )
        )
    }
}
