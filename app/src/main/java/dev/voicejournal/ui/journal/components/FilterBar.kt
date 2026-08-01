package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    selectedTagsCount: Int,
    selectedPeopleCount: Int,
    selectedMoodsCount: Int,
    onAllClick: () -> Unit,
    onTagsClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onMoodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isAllActive = selectedTagsCount == 0 && selectedPeopleCount == 0 && selectedMoodsCount == 0

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.SpaceX2s),
        contentPadding = PaddingValues(horizontal = Spacing.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [All] Chip
        item {
            FilterChip(
                selected = isAllActive,
                onClick = onAllClick,
                label = { Text("All", style = MaterialTheme.typography.labelLarge, fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.textPrimary
                )
            )
        }

        // [Tags] Chip with trailing arrow and numeric badge counter
        item {
            FilterChip(
                selected = selectedTagsCount > 0,
                onClick = onTagsClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                    ) {
                        Text("Topics", style = MaterialTheme.typography.labelLarge)
                        if (selectedTagsCount > 0) {
                            Badge(
                                containerColor = colors.primaryContainer,
                                contentColor = colors.primary
                            ) {
                                Text("$selectedTagsCount", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Topics Filter",
                        modifier = Modifier.size(IconSize.IconSm)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.25f),
                    selectedLabelColor = colors.primary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.textPrimary
                )
            )
        }

        // [People] Chip with trailing arrow and numeric badge counter
        item {
            FilterChip(
                selected = selectedPeopleCount > 0,
                onClick = onPeopleClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                    ) {
                        Text("People", style = MaterialTheme.typography.labelLarge)
                        if (selectedPeopleCount > 0) {
                            Badge(
                                containerColor = colors.primaryContainer,
                                contentColor = colors.primary
                            ) {
                                Text("$selectedPeopleCount", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select People Filter",
                        modifier = Modifier.size(IconSize.IconSm)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.25f),
                    selectedLabelColor = colors.primary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.textPrimary
                )
            )
        }

        // [Mood] Chip with trailing arrow and numeric badge counter
        item {
            FilterChip(
                selected = selectedMoodsCount > 0,
                onClick = onMoodClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                    ) {
                        Text("Mood", style = MaterialTheme.typography.labelLarge)
                        if (selectedMoodsCount > 0) {
                            Badge(
                                containerColor = colors.primaryContainer,
                                contentColor = colors.primary
                            ) {
                                Text("$selectedMoodsCount", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Mood Filter",
                        modifier = Modifier.size(IconSize.IconSm)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.25f),
                    selectedLabelColor = colors.primary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.textPrimary
                )
            )
        }
    }
}
