package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.designsystem.components.dialogs.AppModalBottomSheet
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing

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
    val maxListHeight = (AppTheme.windowInfo.normalizedHeightDp * 0.42f).dp.coerceIn(200.dp, 360.dp)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val distinctTags = remember(availableTags) {
        availableTags.distinctBy { it.name.trim().trimStart('#', '@').trim().lowercase() }
    }

    // Capture initial selection on sheet opening so selected tags sort to the top,
    // but don't jump around under the user's finger during active multi-selection
    val initialSelected = remember(availableTags) { selectedTagNames }

    val sortedTags = remember(distinctTags) {
        distinctTags.sortedByDescending { tag ->
            val cleanName = tag.name.trim().trimStart('#', '@').trim()
            initialSelected.any { it.equals(cleanName, ignoreCase = true) }
        }
    }

    // Filter by search query
    val filteredTags = remember(sortedTags, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedTags
        } else {
            val query = searchQuery.trim().trimStart('#', '@').trim().lowercase()
            sortedTags.filter { it.name.trim().trimStart('#', '@').trim().lowercase().contains(query) }
        }
    }

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.SpaceXl, vertical = Spacing.SpaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Topics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (selectedTagNames.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceMd))

            // Search field for fast filtering when topic count > 4
            if (availableTags.size > 4) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search topics...",
                    requestFocusOnLaunch = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.SpaceMd)
                )
            }

            if (availableTags.isEmpty()) {
                Text(
                    text = "No topics found in notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = Spacing.SpaceLg)
                )
            } else if (filteredTags.isEmpty()) {
                Text(
                    text = "No topics matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = Spacing.SpaceLg)
                )
            } else {
                // High-density FlowRow layout with compact packing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
                    ) {
                        filteredTags.forEach { tag ->
                            val cleanName = tag.name.trim().trimStart('#', '@').trim()
                            val isSelected = selectedTagNames.any { it.equals(cleanName, ignoreCase = true) }
                            FilterOptionPill(
                                label = cleanName,
                                isSelected = isSelected,
                                onClick = { onTagToggle(cleanName) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.SpaceXl))
        }
    }
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
    val maxListHeight = (AppTheme.windowInfo.normalizedHeightDp * 0.42f).dp.coerceIn(200.dp, 360.dp)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val distinctPeople = remember(availablePeople) {
        availablePeople
            .map { it.trim().trimStart('#', '@').trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    // Capture initial selection on sheet opening so selected people sort to the top,
    // but don't jump around under the user's finger during active multi-selection
    val initialSelected = remember(availablePeople) { selectedPeople }

    val sortedPeople = remember(distinctPeople) {
        distinctPeople.sortedByDescending { person ->
            initialSelected.any { it.equals(person, ignoreCase = true) }
        }
    }

    // Filter by search query
    val filteredPeople = remember(sortedPeople, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedPeople
        } else {
            val query = searchQuery.trim().trimStart('#', '@').trim().lowercase()
            sortedPeople.filter { it.lowercase().contains(query) }
        }
    }

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.SpaceXl, vertical = Spacing.SpaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by People",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (selectedPeople.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceMd))

            // Search field for fast filtering when people count > 4
            if (availablePeople.size > 4) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search people...",
                    requestFocusOnLaunch = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.SpaceMd)
                )
            }

            if (availablePeople.isEmpty()) {
                Text(
                    text = "No people found in notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = Spacing.SpaceLg)
                )
            } else if (filteredPeople.isEmpty()) {
                Text(
                    text = "No people matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = Spacing.SpaceLg)
                )
            } else {
                // High-density FlowRow layout with compact packing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
                    ) {
                        filteredPeople.forEach { person ->
                            val cleanPerson = person.trim().trimStart('#', '@').trim()
                            val isSelected = selectedPeople.any { it.equals(cleanPerson, ignoreCase = true) }
                            FilterOptionPill(
                                label = cleanPerson,
                                isSelected = isSelected,
                                onClick = { onPersonToggle(cleanPerson) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.SpaceXl))
        }
    }
}

@Composable
private fun FilterOptionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.RadiusPill),
        color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant,
        border = if (isSelected) BorderStroke(Border.WidthThin, colors.primary) else null,
        modifier = Modifier.defaultMinSize(minHeight = ComponentDimens.ChipMinHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(IconSize.IconXs)
                )
                Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) colors.primary else colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
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

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.SpaceXl, vertical = Spacing.SpaceMd)
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
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceLg))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.SpaceXs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
            ) {
                availableMoods.take(5).forEach { mood ->
                    val isSelected = selectedMoods.contains(mood)
                    Surface(
                        onClick = { onMoodToggle(mood) },
                        shape = RoundedCornerShape(Radius.RadiusMd),
                        color = if (isSelected) colors.primary.copy(alpha = 0.3f) else colors.surfaceVariant,
                        border = if (isSelected) BorderStroke(Border.WidthThin, colors.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = ComponentDimens.ButtonMinHeight)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = Spacing.SpaceMd)
                        ) {
                            Text(
                                text = mood,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.SpaceXl))
        }
    }
}
