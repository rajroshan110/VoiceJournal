package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import dev.voicejournal.ui.components.getTagIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.theme.AppTheme

enum class SortOption(val label: String) {
    CREATED_DESC("Created Date (Newest First)"),
    CREATED_ASC("Created Date (Oldest First)"),
    MODIFIED_DESC("Modified Date (Newest First)"),
    MODIFIED_ASC("Modified Date (Oldest First)")
}

private fun getSortIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Sort",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(3f, 18f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 6f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(18f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 13f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    currentSortOption: SortOption,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelect: (SortOption) -> Unit,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onCategorizeSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        color = colors.background,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search notes...",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                onSearchQueryChange("")
                            } else {
                                onSearchToggle()
                                keyboardController?.hide()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", tint = colors.textSecondary)
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = colors.textPrimary
                        )
                    }

                    Text(
                        text = if (isSelectionMode) "$selectedCount selected" else "Journal",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelectionMode) colors.primary else colors.textPrimary,
                        fontWeight = if (isSelectionMode) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedCount > 0) {
                            IconButton(
                                onClick = onCategorizeSelected,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = getTagIcon(colors.primary),
                                    contentDescription = "Organize selected notes"
                                )
                            }

                            IconButton(
                                onClick = onDeleteSelected,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected notes",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection mode",
                                tint = colors.textSecondary
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Search Button
                        IconButton(
                            onClick = onSearchToggle,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search notes and transcriptions",
                                tint = colors.textSecondary
                            )
                        }

                        // Sort Button
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = getSortIcon(colors.primary),
                                    contentDescription = "Sort notes",
                                    tint = colors.primary
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                color = if (option == currentSortOption) colors.primary else colors.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            onSortOptionSelect(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Settings Button
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open settings",
                                tint = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
