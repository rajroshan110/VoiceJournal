package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.designsystem.theme.AppTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val enabledTabs = remember(isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        buildList {
            if (isFolderEnabled) add(0 to "Folders")
            if (isTopicsEnabled) add(1 to "Topics (#)")
            if (isPeopleEnabled) add(2 to "People (@)")
        }
    }

    var activeTab by rememberSaveable(isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        mutableIntStateOf(enabledTabs.firstOrNull()?.first ?: -1)
    }

    var selectedFolder by remember(appliedFolder) { mutableStateOf<String?>(appliedFolder) }
    var customFolderInput by rememberSaveable { mutableStateOf("") }

    val cleanAppliedTags = remember(appliedTagNames) {
        appliedTagNames.map { it.trim().removePrefix("#").removePrefix("@") }.toSet()
    }

    val selectedTopics = remember(appliedTagNames) {
        mutableStateListOf<String>().apply {
            addAll(cleanAppliedTags.filter { tag -> availableTags.any { it.name.trim().removePrefix("#") == tag && it.type != TagType.PERSON && it.type != TagType.FOLDER } })
        }
    }
    var newTopicInput by rememberSaveable { mutableStateOf("") }

    val selectedPeople = remember(appliedTagNames) {
        mutableStateListOf<String>().apply {
            addAll(cleanAppliedTags.filter { person -> availablePeople.any { it.trim().removePrefix("@") == person } })
        }
    }
    var newPersonInput by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.secondaryBackground,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Title & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            // Segmented Category Tabs (Folders / Topics / People)
            if (enabledTabs.isNotEmpty()) {
                val selectedTabIndex = enabledTabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    enabledTabs.forEach { (tabIndex, title) ->
                        val isSelected = activeTab == tabIndex
                        Tab(
                            selected = isSelected,
                            onClick = { activeTab = tabIndex },
                            text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            icon = {
                                val iconColor = if (isSelected) colors.primary else colors.textSecondary
                                when (tabIndex) {
                                    0 -> Icon(getFolderIcon(iconColor), contentDescription = null, modifier = Modifier.size(18.dp))
                                    1 -> Icon(getTagIcon(iconColor), contentDescription = null, modifier = Modifier.size(18.dp))
                                    else -> Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (enabledTabs.isEmpty() || activeTab == -1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tag categories are currently enabled.\nEnable Folders, Topics, or People in Settings → Tag Organiser.",
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp
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
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Folder List
                        availableFolders.distinct().forEach { folderName ->
                            val isSelected = selectedFolder == folderName
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                                border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.divider),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedFolder = if (isSelected) null else folderName
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            getFolderIcon(if (isSelected) colors.primary else colors.textSecondary),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = folderName,
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
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
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customFolderInput,
                                onValueChange = { customFolderInput = it },
                                placeholder = { Text("New folder name...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.divider
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customFolderInput.isNotBlank()) {
                                        selectedFolder = customFolderInput.trim()
                                        customFolderInput = ""
                                    }
                                },
                                enabled = customFolderInput.isNotBlank()
                            ) {
                                Text("Add")
                            }
                        }
                    }

                    // TAB 1: TOPICS (#tags)
                    1 -> {
                        Text(
                            text = "Add Topic Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Existing Topic Chips (Compact Fit)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableTags.filter { it.type == TagType.TOPIC || it.type == TagType.THING }.forEach { tag ->
                                val cleanName = tag.name.trim().removePrefix("#").removePrefix("@")
                                val isSelected = selectedTopics.contains(cleanName)

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) colors.primary else colors.surface,
                                    border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.divider),
                                    modifier = Modifier
                                        .clickable {
                                            if (isSelected) selectedTopics.remove(cleanName) else selectedTopics.add(cleanName)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "#$cleanName",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onPrimary else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Add Custom Topic Tag Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTopicInput,
                                onValueChange = { newTopicInput = it.trim().removePrefix("#") },
                                placeholder = { Text("New topic #tag...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.divider
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTopicInput.isNotBlank()) {
                                        val clean = newTopicInput.trim()
                                        if (!selectedTopics.contains(clean)) selectedTopics.add(clean)
                                        newTopicInput = ""
                                    }
                                },
                                enabled = newTopicInput.isNotBlank()
                            ) {
                                Text("Add")
                            }
                        }
                    }

                    // TAB 2: PEOPLE (@mentions)
                    2 -> {
                        Text(
                            text = "Tag People",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Existing People Chips (Compact Fit)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availablePeople.forEach { person ->
                                val cleanName = person.trim().removePrefix("@")
                                val isSelected = selectedPeople.contains(cleanName)

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) colors.primary else colors.surface,
                                    border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.divider),
                                    modifier = Modifier
                                        .clickable {
                                            if (isSelected) selectedPeople.remove(cleanName) else selectedPeople.add(cleanName)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "@$cleanName",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onPrimary else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Add Custom Person Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPersonInput,
                                onValueChange = { newPersonInput = it.trim().removePrefix("@") },
                                placeholder = { Text("New @person...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.divider
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPersonInput.isNotBlank()) {
                                        val clean = newPersonInput.trim()
                                        if (!selectedPeople.contains(clean)) selectedPeople.add(clean)
                                        newPersonInput = ""
                                    }
                                },
                                enabled = newPersonInput.isNotBlank()
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Apply Button
            Button(
                onClick = {
                    val topicTagsToAssign = if (isTopicsEnabled) selectedTopics.map { Tag(name = "#$it", type = TagType.TOPIC) } else emptyList()
                    val personTagsToAssign = if (isPeopleEnabled) selectedPeople.map { Tag(name = "@$it", type = TagType.PERSON) } else emptyList()

                    val removedTopicNames = if (isTopicsEnabled) {
                        cleanAppliedTags.filter { tag -> availableTags.any { it.name.trim().removePrefix("#") == tag && it.type != TagType.PERSON && it.type != TagType.FOLDER } } - selectedTopics.toSet()
                    } else emptySet()
                    val removedPersonNames = if (isPeopleEnabled) {
                        cleanAppliedTags.filter { person -> availablePeople.any { it.trim().removePrefix("@") == person } } - selectedPeople.toSet()
                    } else emptySet()

                    val topicTagsToRemove = removedTopicNames.map { Tag(name = "#$it", type = TagType.TOPIC) }
                    val personTagsToRemove = removedPersonNames.map { Tag(name = "@$it", type = TagType.PERSON) }

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
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apply Changes to $selectedCount Note${if (selectedCount > 1) "s" else ""}",
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
