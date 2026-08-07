package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicFilterBottomSheet(
    availableTags: List<Tag>,
    selectedTagNames: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Sort so selected tags appear first
    val sortedTags = remember(availableTags, selectedTagNames) {
        availableTags.sortedByDescending { selectedTagNames.contains(it.name) }
    }

    // Filter by search query
    val filteredTags = remember(sortedTags, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedTags
        } else {
            val query = searchQuery.trim().lowercase().removePrefix("#")
            sortedTags.filter { it.name.removePrefix("#").lowercase().contains(query) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Topics (#)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (selectedTagNames.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", color = colors.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search field for fast filtering when topic count > 4
            if (availableTags.size > 4) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search topics...",
                    requestFocusOnLaunch = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            if (availableTags.isEmpty()) {
                Text(
                    text = "No topics (#) found in notes",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (filteredTags.isEmpty()) {
                Text(
                    text = "No topics matching \"$searchQuery\"",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // High-density FlowRow layout with compact packing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredTags.forEach { tag ->
                            val isSelected = selectedTagNames.contains(tag.name)
                            val displayName = "#${tag.name.removePrefix("#")}"
                            
                            Surface(
                                onClick = { onTagToggle(tag.name) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, colors.primary) else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = displayName,
                                        fontSize = 14.sp,
                                        color = if (isSelected) colors.primary else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Backward compatibility alias for TopicFilterBottomSheet
 */
@Deprecated(
    message = "Use TopicFilterBottomSheet instead",
    replaceWith = ReplaceWith("TopicFilterBottomSheet(availableTags, selectedTagNames, onTagToggle, onClearAll, onDismissRequest)")
)
@Composable
fun TagsFilterBottomSheet(
    availableTags: List<Tag>,
    selectedTagNames: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    TopicFilterBottomSheet(
        availableTags = availableTags,
        selectedTagNames = selectedTagNames,
        onTagToggle = onTagToggle,
        onClearAll = onClearAll,
        onDismissRequest = onDismissRequest
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PeopleFilterBottomSheet(
    availablePeople: List<String>,
    selectedPeople: Set<String>,
    onPersonToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Sort so selected people appear first
    val sortedPeople = remember(availablePeople, selectedPeople) {
        availablePeople.sortedByDescending { selectedPeople.contains(it) }
    }

    // Filter by search query
    val filteredPeople = remember(sortedPeople, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedPeople
        } else {
            val query = searchQuery.trim().lowercase().removePrefix("@")
            sortedPeople.filter { it.removePrefix("@").lowercase().contains(query) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by People (@)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (selectedPeople.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", color = colors.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search field for fast filtering when people count > 4
            if (availablePeople.size > 4) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search people...",
                    requestFocusOnLaunch = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            if (availablePeople.isEmpty()) {
                Text(
                    text = "No people (@mentions) found in notes",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (filteredPeople.isEmpty()) {
                Text(
                    text = "No people matching \"$searchQuery\"",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // High-density FlowRow layout with compact packing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredPeople.forEach { person ->
                            val isSelected = selectedPeople.contains(person)
                            val displayName = "@${person.removePrefix("@")}"

                            Surface(
                                onClick = { onPersonToggle(person) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, colors.primary) else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = displayName,
                                        fontSize = 14.sp,
                                        color = if (isSelected) colors.primary else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodFilterBottomSheet(
    availableMoods: List<String> = listOf("😊", "😌", "😔", "😤", "😡"),
    selectedMoods: Set<String>,
    onMoodToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Mood",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (selectedMoods.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", color = colors.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                availableMoods.take(5).forEach { mood ->
                    val isSelected = selectedMoods.contains(mood)
                    Surface(
                        onClick = { onMoodToggle(mood) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) colors.primary.copy(alpha = 0.3f) else colors.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.dp, colors.primary) else null
                    ) {
                        Text(
                            text = mood,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
