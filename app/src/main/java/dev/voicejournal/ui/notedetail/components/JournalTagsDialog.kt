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
    onTagsChanged: (List<Tag>) -> Unit,
    onDismiss: () -> Unit
) {
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
    
    var tagInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("", selection = TextRange(0)))
    }
    
    val colors = AppTheme.colors

    // Compute suggestion list matching current user input
    val suggestions by remember(tagInput.text, allAvailableTags, tags, activeCategory, isFolderEnabled, isTopicsEnabled, isPeopleEnabled) {
        derivedStateOf {
            val query = tagInput.text.trim().removePrefix("#").removePrefix("@")

            allAvailableTags.filter { tag ->
                if (!isTopicsEnabled && tag.type == TagType.TOPIC) return@filter false
                if (!isPeopleEnabled && tag.type == TagType.PERSON) return@filter false
                if (!isFolderEnabled && (tag.type == TagType.FOLDER || tag.type == TagType.THING)) return@filter false

                val isCategoryMatch = when (activeCategory) {
                    TagType.FOLDER -> tag.type == TagType.FOLDER || tag.type == TagType.THING
                    else -> tag.type == activeCategory
                }
                val isQueryMatch = query.isEmpty() || tag.name.contains(query, ignoreCase = true)
                val isNotAlreadyAdded = tags.none { it.name.equals(tag.name, ignoreCase = true) }
                isCategoryMatch && isQueryMatch && isNotAlreadyAdded
            }.take(10)
        }
    }

    val handleAddTag = {
        val cleanName = tagInput.text.trim().removePrefix("#").removePrefix("@")
        if (cleanName.isNotEmpty()) {
            if (tags.none { it.name.equals(cleanName, ignoreCase = true) }) {
                onTagsChanged(tags + Tag(name = cleanName, type = activeCategory))
            }
            tagInput = TextFieldValue("", selection = TextRange(0))
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
                    actions = {},
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
                                        if (tags.none { it.name.equals(suggestionTag.name, ignoreCase = true) }) {
                                            onTagsChanged(tags + suggestionTag)
                                        }
                                        tagInput = TextFieldValue("", selection = TextRange(0))
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
                                    tagInput = newValue
                                },
                                enabled = isInputAllowed,
                                placeholder = {
                                    Text(
                                        when {
                                            !isInputAllowed -> "Tag categories disabled in settings"
                                            isTopicsEnabled && activeCategory == TagType.TOPIC -> "Add topic name..."
                                            isPeopleEnabled && activeCategory == TagType.PERSON -> "Add person name..."
                                            isFolderEnabled && activeCategory == TagType.FOLDER -> "Add folder name..."
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
                                enabled = isInputAllowed,
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
                                    tint = if (isInputAllowed) colors.onPrimary else colors.textSecondary
                                )
                                Text(
                                    "Add", 
                                    color = if (isInputAllowed) colors.onPrimary else colors.textSecondary
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
                    if (isTopicsEnabled) add(TagType.TOPIC to "Topics")
                    if (isPeopleEnabled) add(TagType.PERSON to "People")
                    if (isFolderEnabled) add(TagType.FOLDER to "Folders")
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
                                        tagInput = TextFieldValue("", selection = TextRange(0))
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
                        title = "Topics",
                        tags = tags.filter { it.type == TagType.TOPIC },
                        onRemoveTag = { tag -> onTagsChanged(tags - tag) }
                    )
                }

                if (isPeopleEnabled) {
                    TagCategoryCard(
                        title = "People",
                        tags = tags.filter { it.type == TagType.PERSON },
                        onRemoveTag = { tag -> onTagsChanged(tags - tag) }
                    )
                }

                if (isFolderEnabled) {
                    TagCategoryCard(
                        title = "Folders",
                        tags = tags.filter { it.type == TagType.FOLDER || it.type == TagType.THING },
                        onRemoveTag = { tag -> onTagsChanged(tags - tag) }
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
