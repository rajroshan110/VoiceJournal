package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.components.TagChip
import dev.voicejournal.ui.designsystem.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalTagsDialog(
    tags: List<Tag>,
    allAvailableTags: List<Tag> = emptyList(),
    isFolderEnabled: Boolean = true,
    isTopicsEnabled: Boolean = true,
    isPeopleEnabled: Boolean = true,
    onSaveTags: (List<Tag>) -> Unit,
    onDismiss: () -> Unit
) {
    var localTags by remember { mutableStateOf(tags) }
    val defaultCategory = remember(isTopicsEnabled, isPeopleEnabled, isFolderEnabled) {
        when {
            isTopicsEnabled -> TagType.TOPIC
            isPeopleEnabled -> TagType.PERSON
            isFolderEnabled -> TagType.FOLDER
            else -> TagType.TOPIC
        }
    }
    var activeCategory by rememberSaveable(isTopicsEnabled, isPeopleEnabled, isFolderEnabled) {
        mutableStateOf(defaultCategory)
    }
    val defaultPrefix = when (defaultCategory) {
        TagType.TOPIC -> "#"
        TagType.PERSON -> "@"
        else -> ""
    }
    var tagInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(defaultPrefix, selection = TextRange(defaultPrefix.length)))
    }
    var pendingNewFolderTagName by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = AppTheme.colors

    // Compute suggestion list matching current user input prefix and text
    val suggestions by remember(tagInput.text, allAvailableTags, localTags, activeCategory, isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        derivedStateOf {
            val inputTrimmed = tagInput.text.trim()
            val (prefixCategory, query) = when {
                inputTrimmed.startsWith("#") && isTopicsEnabled -> TagType.TOPIC to inputTrimmed.removePrefix("#").trim()
                inputTrimmed.startsWith("@") && isPeopleEnabled -> TagType.PERSON to inputTrimmed.removePrefix("@").trim()
                isFolderEnabled -> TagType.FOLDER to inputTrimmed.removePrefix("#").removePrefix("@").trim()
                isTopicsEnabled -> TagType.TOPIC to inputTrimmed.removePrefix("#").removePrefix("@").trim()
                isPeopleEnabled -> TagType.PERSON to inputTrimmed.removePrefix("#").removePrefix("@").trim()
                else -> TagType.TOPIC to inputTrimmed
            }

            allAvailableTags.filter { tag ->
                if (!isTopicsEnabled && tag.type == TagType.TOPIC) return@filter false
                if (!isPeopleEnabled && tag.type == TagType.PERSON) return@filter false
                if (!isFolderEnabled && (tag.type == TagType.FOLDER || tag.type == TagType.THING)) return@filter false

                val isCategoryMatch = when (prefixCategory) {
                    TagType.FOLDER -> tag.type == TagType.FOLDER || tag.type == TagType.THING
                    else -> tag.type == prefixCategory
                }
                val isQueryMatch = query.isEmpty() || tag.name.contains(query, ignoreCase = true)
                val isNotAlreadyAdded = localTags.none { it.name.equals(tag.name, ignoreCase = true) }
                isCategoryMatch && isQueryMatch && isNotAlreadyAdded
            }.take(10)
        }
    }

    val handleAddTag = {
        val trimmed = tagInput.text.trim()
        if (trimmed.isNotEmpty()) {
            when {
                trimmed.startsWith("#") -> {
                    val cleanName = trimmed.removePrefix("#").trim()
                    if (isTopicsEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.TOPIC)
                        }
                        tagInput = TextFieldValue("#", selection = TextRange(1))
                    } else if (isPeopleEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.PERSON)
                        }
                        tagInput = TextFieldValue("@", selection = TextRange(1))
                    }
                }
                trimmed.startsWith("@") -> {
                    val cleanName = trimmed.removePrefix("@").trim()
                    if (isPeopleEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.PERSON)
                        }
                        tagInput = TextFieldValue("@", selection = TextRange(1))
                    } else if (isTopicsEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.TOPIC)
                        }
                        tagInput = TextFieldValue("#", selection = TextRange(1))
                    }
                }
                else -> {
                    val cleanName = trimmed
                    if (isFolderEnabled) {
                        // Strictly check if a FOLDER tag already exists in allAvailableTags or localTags
                        val folderExistsInAll = allAvailableTags.any { 
                            it.name.equals(cleanName, ignoreCase = true) && 
                            (it.type == TagType.FOLDER || it.type == TagType.THING) 
                        }
                        val folderExistsInLocal = localTags.any { 
                            it.name.equals(cleanName, ignoreCase = true) && 
                            (it.type == TagType.FOLDER || it.type == TagType.THING) 
                        }

                        if (folderExistsInAll || folderExistsInLocal) {
                            // Already exists as a Folder tag: add as Folder tag directly without popup dialog
                            val tagAlreadyInNote = localTags.any { it.name.equals(cleanName, ignoreCase = true) && it.type == TagType.FOLDER }
                            if (!tagAlreadyInNote) {
                                localTags = localTags + Tag(name = cleanName, type = TagType.FOLDER)
                            }
                            tagInput = TextFieldValue("", selection = TextRange(0))
                        } else {
                            // Folder tag does NOT exist yet: prompt dialog!
                            pendingNewFolderTagName = cleanName
                            tagInput = TextFieldValue("", selection = TextRange(0))
                        }
                    } else if (isTopicsEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.TOPIC)
                        }
                        tagInput = TextFieldValue("#", selection = TextRange(1))
                    } else if (isPeopleEnabled) {
                        if (cleanName.isNotEmpty() && localTags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            localTags = localTags + Tag(name = cleanName, type = TagType.PERSON)
                        }
                        tagInput = TextFieldValue("@", selection = TextRange(1))
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Manage Tags",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                onSaveTags(localTags)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", color = colors.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Static Suggestion Bar (above footer add bar)
                    if (suggestions.isNotEmpty()) {
                        Text(
                            text = "Suggestions",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            items(suggestions) { suggestionTag ->
                                TagChip(
                                    tag = suggestionTag,
                                    onClick = {
                                        if (localTags.none { it.name.equals(suggestionTag.name, ignoreCase = true) }) {
                                            localTags = localTags + suggestionTag
                                        }
                                        tagInput = when {
                                            activeCategory == TagType.TOPIC -> TextFieldValue("#", selection = TextRange(1))
                                            activeCategory == TagType.PERSON -> TextFieldValue("@", selection = TextRange(1))
                                            isFolderEnabled && activeCategory == TagType.FOLDER -> TextFieldValue("", selection = TextRange(0))
                                            else -> TextFieldValue("#", selection = TextRange(1))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Add Tags Input Card (Footer anchored)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isInputAllowed = isTopicsEnabled || isPeopleEnabled || isFolderEnabled

                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { newValue ->
                                    val newText = newValue.text
                                    // Handle Category Auto-Sync & Cursor position after prefix (#| / @|)
                                    if (newText.startsWith("#") && isTopicsEnabled) {
                                        activeCategory = TagType.TOPIC
                                        tagInput = newValue
                                    } else if (newText.startsWith("@") && isPeopleEnabled) {
                                        activeCategory = TagType.PERSON
                                        tagInput = newValue
                                    } else {
                                        activeCategory = when {
                                            isFolderEnabled -> TagType.FOLDER
                                            isTopicsEnabled -> TagType.TOPIC
                                            isPeopleEnabled -> TagType.PERSON
                                            else -> TagType.TOPIC
                                        }
                                        tagInput = newValue
                                    }
                                },
                                enabled = isInputAllowed,
                                placeholder = {
                                    Text(
                                        when {
                                            !isInputAllowed -> "Tag categories disabled in settings"
                                            isTopicsEnabled && activeCategory == TagType.TOPIC -> "Add #topic tag..."
                                            isPeopleEnabled && activeCategory == TagType.PERSON -> "Add @person tag..."
                                            isFolderEnabled && activeCategory == TagType.FOLDER -> "Add folder tag..."
                                            isTopicsEnabled -> "Add #topic tag..."
                                            isPeopleEnabled -> "Add @person tag..."
                                            isFolderEnabled -> "Add folder tag..."
                                            else -> "Add tag..."
                                        },
                                        color = colors.textSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedTextColor = colors.textPrimary,
                                    disabledTextColor = colors.textSecondary
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = handleAddTag,
                                enabled = isInputAllowed && tagInput.text.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    disabledContainerColor = colors.surfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = null, 
                                    tint = if (isInputAllowed && tagInput.text.trim().isNotEmpty()) colors.onPrimary else colors.textSecondary
                                )
                                Text(
                                    "Add", 
                                    color = if (isInputAllowed && tagInput.text.trim().isNotEmpty()) colors.onPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Category Tabs Selector
                val categoryList = buildList {
                    if (isTopicsEnabled) add(TagType.TOPIC to "# Topics")
                    if (isPeopleEnabled) add(TagType.PERSON to "@ People")
                    if (isFolderEnabled) add(TagType.FOLDER to "📁 Folder")
                }

                if (categoryList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryList.forEach { (type, label) ->
                            val isSelected = activeCategory == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) colors.primary else colors.surfaceVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        activeCategory = type
                                        // Automatic input prefix with cursor positioned cleanly AFTER prefix (#| or @|)
                                        tagInput = when (type) {
                                            TagType.TOPIC -> {
                                                 val text = if (tagInput.text.startsWith("#")) tagInput.text else "#" + tagInput.text.removePrefix("@")
                                                TextFieldValue(text, selection = TextRange(text.length))
                                            }
                                            TagType.PERSON -> {
                                                val text = if (tagInput.text.startsWith("@")) tagInput.text else "@" + tagInput.text.removePrefix("#")
                                                TextFieldValue(text, selection = TextRange(text.length))
                                            }
                                            TagType.FOLDER -> {
                                                val text = tagInput.text.removePrefix("#").removePrefix("@")
                                                TextFieldValue(text, selection = TextRange(text.length))
                                            }
                                            else -> tagInput
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) colors.onPrimary else colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Active Tags List by Category Cards
                if (isTopicsEnabled) {
                    TagCategoryCard(
                        title = "Topics (#)",
                        tags = localTags.filter { it.type == TagType.TOPIC },
                        onRemoveTag = { tag -> localTags = localTags - tag }
                    )
                }

                if (isPeopleEnabled) {
                    TagCategoryCard(
                        title = "People (@)",
                        tags = localTags.filter { it.type == TagType.PERSON },
                        onRemoveTag = { tag -> localTags = localTags - tag }
                    )
                }

                if (isFolderEnabled) {
                    TagCategoryCard(
                        title = "Folders (📁)",
                        tags = localTags.filter { it.type == TagType.FOLDER || it.type == TagType.THING },
                        onRemoveTag = { tag -> localTags = localTags - tag }
                    )
                }

                if (!isTopicsEnabled && !isPeopleEnabled && !isFolderEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tag categories are currently enabled.\nEnable them in Settings → Tag Organiser → Choose Tags.",
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Popup choice dialog for plain text brand new folder tags
    pendingNewFolderTagName?.let { newTagName ->
        AlertDialog(
            onDismissRequest = { pendingNewFolderTagName = null },
            title = {
                Text(
                    text = if (isTopicsEnabled) "Add Tag Category" else "Create Folder Tag",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isTopicsEnabled) {
                        "How would you like to add \"$newTagName\"?"
                    } else {
                        "Add \"$newTagName\" as a new folder tag?"
                    },
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        localTags = localTags + Tag(name = newTagName, type = TagType.FOLDER)
                        pendingNewFolderTagName = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Create New Folder", color = colors.onPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (isTopicsEnabled) {
                            localTags = localTags + Tag(name = newTagName, type = TagType.TOPIC)
                        }
                        pendingNewFolderTagName = null
                    }
                ) {
                    Text(
                        text = if (isTopicsEnabled) "Add as Topic" else "Discard",
                        color = if (isTopicsEnabled) colors.primary else colors.textSecondary
                    )
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TagCategoryCard(
    title: String,
    tags: List<Tag>,
    onRemoveTag: (Tag) -> Unit
) {
    val colors = AppTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (tags.isEmpty()) {
                Text("No tags in this category yet", color = colors.textSecondary, fontSize = 13.sp)
            } else {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag, onDelete = { onRemoveTag(tag) })
                    }
                }
            }
        }
    }
}
