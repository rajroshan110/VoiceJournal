package dev.voicejournal.ui.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.components.getGridViewIcon
import dev.voicejournal.ui.components.getListViewIcon
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.folders.components.FolderNoteCard
import dev.voicejournal.ui.journal.components.LightboxDialog
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.tags.components.CategoryFolderCard
import dev.voicejournal.ui.tags.components.TagCardGrid
import dev.voicejournal.ui.tags.components.TagCardList
import dev.voicejournal.ui.designsystem.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    navController: NavController,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    var selectedLightboxImage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    // System Back Press handling across 3 hierarchy levels
    BackHandler(enabled = uiState.selectedTag != null || uiState.selectedCategory != null || isSearchVisible) {
        if (isSearchVisible) {
            isSearchVisible = false
            viewModel.setSearchQuery("")
        } else if (uiState.selectedTag != null) {
            viewModel.selectTag(null)
        } else if (uiState.selectedCategory != null) {
            viewModel.selectCategory(null)
        }
    }

    val selectedCategory = uiState.selectedCategory
    val selectedTag = uiState.selectedTag

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNavBar(navController = navController) },
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isSearchVisible && selectedCategory != null && selectedTag == null) {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search tags...",
                            requestFocusOnLaunch = true,
                            onClose = {
                                isSearchVisible = false
                                viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (selectedTag != null) {
                                    viewModel.selectTag(null)
                                } else if (selectedCategory != null) {
                                    viewModel.selectCategory(null)
                                } else {
                                    navController.popBackStack()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            val headerTitle = when {
                                selectedTag != null -> selectedTag.name
                                selectedCategory == TagType.TOPIC -> "Topics"
                                selectedCategory == TagType.PERSON -> "People"
                                else -> "Tags Manager"
                            }

                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedCategory != null && selectedTag == null) {
                                IconButton(onClick = { isSearchVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Tags",
                                        tint = colors.textSecondary
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleViewMode() }) {
                                    Icon(
                                        imageVector = if (uiState.isGridView) getListViewIcon(colors.textSecondary) else getGridViewIcon(colors.textSecondary),
                                        contentDescription = "Toggle view mode",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedCategory != null) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTag != null) {
                            navController.navigate(
                                Screen.NoteDetail.createRoute(
                                    entryId = -1L,
                                    initialTag = selectedTag.name,
                                    initialTagType = selectedTag.type.name
                                )
                            )
                        } else {
                            viewModel.setCreateTagDialogOpen(true)
                        }
                    },
                    containerColor = colors.primary,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (selectedTag == null) "New Tag" else "New Note"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // LEVEL 3: Notes in Selected Tag View
                selectedTag != null -> {
                    val notes = uiState.notesInSelectedTag

                    if (notes.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(32.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedTag.type == TagType.PERSON) "👤" else "🏷️",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No notes tagged with '${selectedTag.name}'",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap '+' to create a new note pre-tagged with '${selectedTag.name}'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Surface(
                                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = if (selectedTag.type == TagType.PERSON) "👤" else "🏷️",
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${notes.size} ${if (notes.size == 1) "note" else "notes"} in ${selectedTag.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }

                            items(notes, key = { it.id }) { entry ->
                                FolderNoteCard(
                                    entry = entry,
                                    onClick = {
                                        navController.navigate(Screen.NoteDetail.createRoute(entry.id))
                                    },
                                    timeFormat = uiState.timeFormat
                                )
                            }
                        }
                    }
                }

                // LEVEL 2: Tags List within Category (Topics or People)
                selectedCategory != null -> {
                    val rawTagsList = if (selectedCategory == TagType.TOPIC) uiState.topicTags else uiState.personTags
                    val filteredTags = remember(rawTagsList, uiState.searchQuery) {
                        if (uiState.searchQuery.isBlank()) {
                            rawTagsList
                        } else {
                            rawTagsList.filter {
                                it.tag.name.contains(uiState.searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredTags.isEmpty()) {
                        val categoryName = if (selectedCategory == TagType.TOPIC) "topics" else "people"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(32.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedCategory == TagType.PERSON) "👤" else "🏷️",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (uiState.searchQuery.isBlank()) "No $categoryName created yet" else "No matching $categoryName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (uiState.searchQuery.isBlank())
                                        "Tap '+' to create a new $categoryName tag!"
                                    else "Try a different search keyword.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.searchQuery.isBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.setCreateTagDialogOpen(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create New ${if (selectedCategory == TagType.TOPIC) "Topic" else "Person"}")
                                    }
                                }
                            }
                        }
                    } else if (uiState.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTags, key = { it.tag.id }) { item ->
                                TagCardGrid(
                                    tagItem = item,
                                    onClick = { viewModel.selectTag(item.tag) },
                                    onRename = { viewModel.setTagToRename(item.tag) },
                                    onMerge = { viewModel.setTagToMerge(item.tag) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTags, key = { it.tag.id }) { item ->
                                TagCardList(
                                    tagItem = item,
                                    onClick = { viewModel.selectTag(item.tag) },
                                    onRename = { viewModel.setTagToRename(item.tag) },
                                    onMerge = { viewModel.setTagToMerge(item.tag) }
                                )
                            }
                        }
                    }
                }

                // LEVEL 1: Root Tags Manager (Topics & People Root Folders)
                else -> {
                    if (!uiState.isTopicsEnabled && !uiState.isPeopleEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tag categories are currently enabled.\nEnable them in Settings → Tag Organiser → Choose Tags.",
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.isTopicsEnabled) {
                                item {
                                    CategoryFolderCard(
                                        title = "Topics",
                                        tagType = TagType.TOPIC,
                                        tagCount = uiState.topicTags.size,
                                        iconEmoji = "🏷️",
                                        onClick = { viewModel.selectCategory(TagType.TOPIC) }
                                    )
                                }
                            }

                            if (uiState.isPeopleEnabled) {
                                item {
                                    CategoryFolderCard(
                                        title = "People",
                                        tagType = TagType.PERSON,
                                        tagCount = uiState.personTags.size,
                                        iconEmoji = "👤",
                                        onClick = { viewModel.selectCategory(TagType.PERSON) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Tag Dialog
    if (uiState.isCreateTagDialogOpen && selectedCategory != null) {
        var tagNameInput by rememberSaveable { mutableStateOf("") }
        val categoryLabel = if (selectedCategory == TagType.TOPIC) "Topic" else "Person"

        AlertDialog(
            onDismissRequest = { viewModel.setCreateTagDialogOpen(false) },
            title = {
                Text(
                    text = "New $categoryLabel Tag",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tagNameInput,
                    onValueChange = { tagNameInput = it },
                    placeholder = { Text("Enter $categoryLabel name...", color = colors.textSecondary) },
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
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createTag(tagNameInput, selectedCategory) },
                    enabled = tagNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setCreateTagDialogOpen(false) }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rename Tag Dialog
    val tagToRename = uiState.tagToRename
    if (tagToRename != null) {
        dev.voicejournal.ui.components.RenameItemDialog(
            itemTitle = "Rename Tag",
            currentName = tagToRename.name,
            onDismiss = { viewModel.setTagToRename(null) },
            onConfirm = { newName -> viewModel.renameTag(tagToRename, newName) }
        )
    }

    // Merge Tag Dialog
    val tagToMerge = uiState.tagToMerge
    if (tagToMerge != null) {
        val allCategoryTags = if (tagToMerge.type == TagType.TOPIC) uiState.topicTags else uiState.personTags
        val targetCandidates = allCategoryTags.map { it.tag }.filter { it.id != tagToMerge.id }
        dev.voicejournal.ui.components.MergeItemDialog(
            title = "Merge Tag",
            sourceTag = tagToMerge,
            targetCandidates = targetCandidates,
            onDismiss = { viewModel.setTagToMerge(null) },
            onConfirm = { targetTag -> viewModel.mergeTags(tagToMerge, targetTag) }
        )
    }

    // Lightbox for note images
    if (selectedLightboxImage != null) {
        LightboxDialog(
            imageUri = selectedLightboxImage!!,
            onDismiss = { selectedLightboxImage = null }
        )
    }
}
