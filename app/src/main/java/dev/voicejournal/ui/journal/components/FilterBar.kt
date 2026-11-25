package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.tokens.physicalMinTouchTarget
import dev.voicejournal.ui.designsystem.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    selectedTagsCount: Int,
    selectedPeopleCount: Int,
    selectedMoodsCount: Int,
    isTopicsEnabled: Boolean = true,
    isPeopleEnabled: Boolean = true,
    isMoodEnabled: Boolean = true,
    onAllClick: () -> Unit,
    onTagsClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onMoodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isAllActive = (!isTopicsEnabled || selectedTagsCount == 0) &&
            (!isPeopleEnabled || selectedPeopleCount == 0) &&
            (!isMoodEnabled || selectedMoodsCount == 0)

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.SpaceX3s),
        contentPadding = PaddingValues(horizontal = Spacing.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [All] Chip
        item {
            FilterChip(
                selected = isAllActive,
                onClick = onAllClick,
                shape = RoundedCornerShape(Radius.RadiusPill),
                modifier = Modifier.physicalMinTouchTarget(),
                label = {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.textPrimary
                )
            )
        }

        // [Tags / Topics] Chip with trailing arrow and numeric badge counter
        if (isTopicsEnabled) {
            item {
                FilterChip(
                    selected = selectedTagsCount > 0,
                    onClick = onTagsClick,
                    shape = RoundedCornerShape(Radius.RadiusPill),
                    modifier = Modifier.physicalMinTouchTarget(),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                        ) {
                            Text(
                                text = "Topics",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (selectedTagsCount > 0) {
                                Badge(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.primary
                                ) {
                                    Text(
                                        text = "$selectedTagsCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
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
        }

        // [People] Chip with trailing arrow and numeric badge counter
        if (isPeopleEnabled) {
            item {
                FilterChip(
                    selected = selectedPeopleCount > 0,
                    onClick = onPeopleClick,
                    shape = RoundedCornerShape(Radius.RadiusPill),
                    modifier = Modifier.physicalMinTouchTarget(),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                        ) {
                            Text(
                                text = "People",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (selectedPeopleCount > 0) {
                                Badge(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.primary
                                ) {
                                    Text(
                                        text = "$selectedPeopleCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
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
        }

        // [Mood] Chip with trailing arrow and numeric badge counter
        if (isMoodEnabled) {
            item {
                FilterChip(
                    selected = selectedMoodsCount > 0,
                    onClick = onMoodClick,
                    shape = RoundedCornerShape(Radius.RadiusPill),
                    modifier = Modifier.physicalMinTouchTarget(),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                        ) {
                            Text(
                                text = "Mood",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (selectedMoodsCount > 0) {
                                Badge(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.primary
                                ) {
                                    Text(
                                        text = "$selectedMoodsCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
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
}
