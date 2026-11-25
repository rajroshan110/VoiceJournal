package dev.voicejournal.ui.tags.components

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.designsystem.components.menus.AppDropdownMenu
import dev.voicejournal.ui.designsystem.components.menus.AppDropdownMenuItem
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.physicalMinTouchTarget
import dev.voicejournal.ui.tags.TagItem

@Composable
fun CategoryFolderCard(
    title: String,
    tagType: TagType,
    tagCount: Int,
    iconEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = iconEmoji, style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$tagCount ${if (tagCount == 1) "tag" else "tags"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Explore all $title tags",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun TagCardGrid(
    tagItem: TagItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onMerge: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val iconEmoji = when (tagItem.tag.type) {
        TagType.TOPIC -> "🏷️"
        TagType.PERSON -> "👤"
        else -> "🏷️"
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = iconEmoji, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${tagItem.noteCount} ${if (tagItem.noteCount == 1) "note" else "notes"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (onRename != null || onMerge != null) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.physicalMinTouchTarget()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AppDropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (onRename != null) {
                                    AppDropdownMenuItem(
                                        text = "Rename Tag",
                                        onClick = {
                                            menuExpanded = false
                                            onRename()
                                        }
                                    )
                                }
                                if (onMerge != null) {
                                    AppDropdownMenuItem(
                                        text = "Merge Tag",
                                        onClick = {
                                            menuExpanded = false
                                            onMerge()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = tagItem.tag.name.trim().trimStart('#', '@').trim(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!tagItem.previewNoteTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tagItem.previewNoteTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Empty tag",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TagCardList(
    tagItem: TagItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onMerge: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val iconEmoji = when (tagItem.tag.type) {
        TagType.TOPIC -> "🏷️"
        TagType.PERSON -> "👤"
        else -> "🏷️"
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                color = colors.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconEmoji, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tagItem.tag.name.trim().trimStart('#', '@').trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!tagItem.previewNoteTitle.isNullOrBlank()) {
                    Text(
                        text = tagItem.previewNoteTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${tagItem.noteCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (onRename != null || onMerge != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.physicalMinTouchTarget()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AppDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (onRename != null) {
                                AppDropdownMenuItem(
                                    text = "Rename Tag",
                                    onClick = {
                                        menuExpanded = false
                                        onRename()
                                    }
                                )
                            }
                            if (onMerge != null) {
                                AppDropdownMenuItem(
                                    text = "Merge Tag",
                                    onClick = {
                                        menuExpanded = false
                                        onMerge()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
