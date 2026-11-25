package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.designsystem.components.buttons.AppPrimaryButton
import dev.voicejournal.ui.designsystem.components.dialogs.AppModalBottomSheet
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.tokens.physicalMinTouchTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchCategorizeSheet(
    selectedCount: Int,
    isFolderEnabled: Boolean = true,
    isTopicsEnabled: Boolean = true,
    isPeopleEnabled: Boolean = true,
    availableFolders: List<String> = listOf("Personal", "Work", "Ideas", "Journal"),
    availableTags: List<Tag>,
    availablePeople: List<String>,
    appliedFolder: String? = null,
    appliedTagNames: Set<String> = emptySet(),
    onApply: (targetFolder: String?, tagsToAssign: List<Tag>, tagsToRemove: List<Tag>, folderToRemove: String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors
    val maxListHeight = (AppTheme.windowInfo.normalizedHeightDp * 0.40f).dp.coerceIn(200.dp, 340.dp)

    val enabledTabs = remember(isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        buildList {
            if (isFolderEnabled) add(0 to "Folders")
            if (isTopicsEnabled) add(1 to "Topics")
            if (isPeopleEnabled) add(2 to "People")
        }
    }

    var activeTab by rememberSaveable(isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        mutableIntStateOf(enabledTabs.firstOrNull()?.first ?: -1)
    }

    var selectedFolder by remember(appliedFolder) { mutableStateOf<String?>(appliedFolder) }
    var customFolderInput by rememberSaveable { mutableStateOf("") }

    val cleanAppliedTags = remember(appliedTagNames) {
        appliedTagNames.map { it.trim().trimStart('#', '@').trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .toSet()
    }

    val selectedTopics = remember(appliedTagNames) {
        mutableStateListOf<String>().apply {
            addAll(cleanAppliedTags.filter { tag ->
                availableTags.any {
                    it.name.trim().trimStart('#', '@').trim().equals(tag, ignoreCase = true) && it.type == TagType.TOPIC
                }
            })
        }
    }
    var newTopicInput by rememberSaveable { mutableStateOf("") }

    val selectedPeople = remember(appliedTagNames) {
        mutableStateListOf<String>().apply {
            addAll(cleanAppliedTags.filter { person ->
                availablePeople.any {
                    it.trim().trimStart('#', '@').trim().equals(person, ignoreCase = true)
                }
            })
        }
    }
    var newPersonInput by rememberSaveable { mutableStateOf("") }

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.secondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.SpaceLg)
                .padding(bottom = Spacing.SpaceXl)
        ) {
            // Header Title & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.SpaceMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Organise Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$selectedCount note${if (selectedCount > 1) "s" else ""} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.physicalMinTouchTarget()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            // Segmented Category Tabs (Folders / Topics / People)
            if (enabledTabs.isNotEmpty()) {
                val selectedTabIndex = enabledTabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.primary,
                    modifier = Modifier.padding(bottom = Spacing.SpaceLg)
                ) {
                    enabledTabs.forEach { (tabIndex, title) ->
                        val isSelected = activeTab == tabIndex
                        Tab(
                            selected = isSelected,
                            onClick = { activeTab = tabIndex },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = {
                                val iconColor = if (isSelected) colors.primary else colors.textSecondary
                                when (tabIndex) {
                                    0 -> Icon(getFolderIcon(iconColor), contentDescription = null, modifier = Modifier.size(IconSize.IconSm))
                                    1 -> Icon(getTagIcon(iconColor), contentDescription = null, modifier = Modifier.size(IconSize.IconSm))
                                    else -> Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(IconSize.IconSm))
                                }
                            }
                        )
                    }
                }
            }

            // Tab Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                if (enabledTabs.isEmpty() || activeTab == -1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.Space2Xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tag categories are currently enabled.\nEnable Folders, Topics, or People in Settings → Tag Organiser.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (activeTab) {
                        // TAB 0: FOLDERS
                        0 -> {
                            Text(
                                text = "Assign to Folder",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = Spacing.SpaceXs)
                            )

                            // Folder List
                            availableFolders.distinct().forEach { folderName ->
                                val isSelected = selectedFolder == folderName
                                Surface(
                                    onClick = {
                                        selectedFolder = if (isSelected) null else folderName
                                    },
                                    shape = RoundedCornerShape(Radius.RadiusMd),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                                    border = BorderStroke(Border.WidthThin, if (isSelected) colors.primary else colors.divider),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.SpaceX2s)
                                        .defaultMinSize(minHeight = ComponentDimens.ButtonMinHeight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceMd),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                getFolderIcon(if (isSelected) colors.primary else colors.textSecondary),
                                                contentDescription = null,
                                                modifier = Modifier.size(IconSize.IconMd)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.SpaceMd))
                                            Text(
                                                text = folderName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                                        }
                                    }
                                }
                            }

                            // Add Custom Folder Input
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.SpaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customFolderInput,
                                    onValueChange = { customFolderInput = it },
                                    placeholder = {
                                        Text(
                                            text = "New folder name...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.divider
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                                AppPrimaryButton(
                                    onClick = {
                                        if (customFolderInput.isNotBlank()) {
                                            selectedFolder = customFolderInput.trim()
                                            customFolderInput = ""
                                        }
                                    },
                                    enabled = customFolderInput.isNotBlank()
                                ) {
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // TAB 1: TOPICS (#tags)
                        1 -> {
                            Text(
                                text = "Add Topic Tags",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = Spacing.SpaceXs)
                            )

                            // Existing Topic Chips (Compact Fit)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableTags.filter { it.type == TagType.TOPIC }
                                    .distinctBy { it.name.trim().trimStart('#', '@').trim().lowercase() }
                                    .forEach { tag ->
                                        val cleanName = tag.name.trim().trimStart('#', '@').trim()
                                        val isSelected = selectedTopics.any { it.equals(cleanName, ignoreCase = true) }

                                        BatchTagSelectionChip(
                                            label = cleanName,
                                            isSelected = isSelected,
                                            onClick = {
                                                val existing = selectedTopics.firstOrNull { it.equals(cleanName, ignoreCase = true) }
                                                if (existing != null) {
                                                    selectedTopics.removeAll { it.equals(cleanName, ignoreCase = true) }
                                                } else {
                                                    selectedTopics.add(cleanName)
                                                }
                                            }
                                        )
                                    }
                            }

                            // Add Custom Topic Tag Input
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.SpaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTopicInput,
                                    onValueChange = { newTopicInput = it },
                                    placeholder = {
                                        Text(
                                            text = "New topic...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.divider
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                                AppPrimaryButton(
                                    onClick = {
                                        val clean = newTopicInput.trim().trimStart('#', '@').trim()
                                        if (clean.isNotBlank()) {
                                            val existing = selectedTopics.firstOrNull { it.equals(clean, ignoreCase = true) }
                                            if (existing == null) selectedTopics.add(clean)
                                            newTopicInput = ""
                                        }
                                    },
                                    enabled = newTopicInput.isNotBlank()
                                ) {
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // TAB 2: PEOPLE (@mentions)
                        2 -> {
                            Text(
                                text = "Tag People",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = Spacing.SpaceXs)
                            )

                            // Existing People Chips (Compact Fit)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availablePeople
                                    .map { it.trim().trimStart('#', '@').trim() }
                                    .filter { it.isNotBlank() }
                                    .distinctBy { it.lowercase() }
                                    .forEach { cleanName ->
                                        val isSelected = selectedPeople.any { it.equals(cleanName, ignoreCase = true) }

                                        BatchTagSelectionChip(
                                            label = cleanName,
                                            isSelected = isSelected,
                                            onClick = {
                                                val existing = selectedPeople.firstOrNull { it.equals(cleanName, ignoreCase = true) }
                                                if (existing != null) {
                                                    selectedPeople.removeAll { it.equals(cleanName, ignoreCase = true) }
                                                } else {
                                                    selectedPeople.add(cleanName)
                                                }
                                            }
                                        )
                                    }
                            }

                            // Add Custom Person Input
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.SpaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPersonInput,
                                    onValueChange = { newPersonInput = it },
                                    placeholder = {
                                        Text(
                                            text = "New person...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.divider
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                                AppPrimaryButton(
                                    onClick = {
                                        val clean = newPersonInput.trim().trimStart('#', '@').trim()
                                        if (clean.isNotBlank()) {
                                            val existing = selectedPeople.firstOrNull { it.equals(clean, ignoreCase = true) }
                                            if (existing == null) selectedPeople.add(clean)
                                            newPersonInput = ""
                                        }
                                    },
                                    enabled = newPersonInput.isNotBlank()
                                ) {
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceLg))

            // Action Apply Button
            AppPrimaryButton(
                onClick = {
                    val topicTagsToAssign = if (isTopicsEnabled) selectedTopics.map { Tag(name = it, type = TagType.TOPIC) } else emptyList()
                    val personTagsToAssign = if (isPeopleEnabled) selectedPeople.map { Tag(name = it, type = TagType.PERSON) } else emptyList()

                    val removedTopicNames = if (isTopicsEnabled) {
                        cleanAppliedTags.filter { tag ->
                            availableTags.any {
                                it.name.trim().trimStart('#', '@').trim().equals(tag, ignoreCase = true) && it.type == TagType.TOPIC
                            }
                        }.filter { tag -> selectedTopics.none { it.equals(tag, ignoreCase = true) } }
                    } else emptyList()
                    val removedPersonNames = if (isPeopleEnabled) {
                        cleanAppliedTags.filter { person ->
                            availablePeople.any {
                                it.trim().trimStart('#', '@').trim().equals(person, ignoreCase = true)
                            }
                        }.filter { person -> selectedPeople.none { it.equals(person, ignoreCase = true) } }
                    } else emptyList()

                    val topicTagsToRemove = removedTopicNames.map { Tag(name = it, type = TagType.TOPIC) }
                    val personTagsToRemove = removedPersonNames.map { Tag(name = it, type = TagType.PERSON) }

                    val finalSelectedFolder = if (isFolderEnabled) selectedFolder else null
                    val folderToRemove = if (isFolderEnabled && appliedFolder != null && appliedFolder != finalSelectedFolder) appliedFolder else null

                    onApply(
                        finalSelectedFolder,
                        topicTagsToAssign + personTagsToAssign,
                        topicTagsToRemove + personTagsToRemove,
                        folderToRemove
                    )
                },
                enabled = enabledTabs.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apply Changes to $selectedCount Note${if (selectedCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BatchTagSelectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.RadiusPill),
        color = if (isSelected) colors.primary else colors.surface,
        border = BorderStroke(Border.WidthThin, if (isSelected) colors.primary else colors.divider),
        modifier = Modifier.defaultMinSize(minHeight = ComponentDimens.ChipMinHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(IconSize.IconXs)
                )
                Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.onPrimary else colors.textPrimary
            )
        }
    }
}
