package dev.voicejournal.ui.folders.components

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
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.folders.FolderItem
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun FolderCardGrid(
    folderItem: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onMerge: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

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
                // Folder Icon Box
                Surface(
                    color = colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getFolderIcon(colors.primary),
                            contentDescription = folderItem.tag.name,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Note count badge
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${folderItem.noteCount} ${if (folderItem.noteCount == 1) "note" else "notes"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (onRename != null || onMerge != null) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (onRename != null) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Folder") },
                                        onClick = {
                                            menuExpanded = false
                                            onRename()
                                        }
                                    )
                                }
                                if (onMerge != null) {
                                    DropdownMenuItem(
                                        text = { Text("Merge Folder") },
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
                text = folderItem.tag.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!folderItem.previewNoteTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folderItem.previewNoteTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Empty folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun FolderCardList(
    folderItem: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onMerge: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

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
                    Icon(
                        imageVector = getFolderIcon(colors.primary),
                        contentDescription = folderItem.tag.name,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderItem.tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!folderItem.previewNoteTitle.isNullOrBlank()) {
                    Text(
                        text = folderItem.previewNoteTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
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
                        text = "${folderItem.noteCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (onRename != null || onMerge != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (onRename != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename Folder") },
                                    onClick = {
                                        menuExpanded = false
                                        onRename()
                                    }
                                )
                            }
                            if (onMerge != null) {
                                DropdownMenuItem(
                                    text = { Text("Merge Folder") },
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
