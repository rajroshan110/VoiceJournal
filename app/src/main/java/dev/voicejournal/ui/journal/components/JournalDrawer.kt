package dev.voicejournal.ui.journal.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.designsystem.theme.AppTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun JournalDrawerContent(
    onNavigateToArchive: () -> Unit = {},
    onNavigateToDraft: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.textPrimary,
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Surface(
                    color = colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🎙️",
                            fontSize = 20.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = "Voice Journal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Organize & Explore",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.border.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FEATURES",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Archive Navigation Item (Top of FEATURES)
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = dev.voicejournal.ui.components.getArchiveIcon(colors.primary),
                        contentDescription = "Archive",
                        tint = colors.primary
                    )
                },
                label = {
                    Text(
                        text = "Archive",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToArchive()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                    unselectedIconColor = colors.primary,
                    unselectedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 2. Draft Navigation Item (Below Archive)
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = dev.voicejournal.ui.components.getDraftIcon(colors.primary),
                        contentDescription = "Draft",
                        tint = colors.primary
                    )
                },
                label = {
                    Text(
                        text = "Draft",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToDraft()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                    unselectedIconColor = colors.primary,
                    unselectedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 3. Folder Navigation Item (Below Draft)
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = getFolderIcon(colors.primary),
                        contentDescription = "Folders",
                        tint = colors.primary
                    )
                },
                label = {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToFolders()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                    unselectedIconColor = colors.primary,
                    unselectedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 4. Tags Navigation Item (Below Folders)
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = getTagIcon(colors.primary),
                        contentDescription = "Tags",
                        tint = colors.primary
                    )
                },
                label = {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToTags()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                    unselectedIconColor = colors.primary,
                    unselectedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 5. Trash Navigation Item (Below Tags)
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash",
                        tint = colors.error
                    )
                },
                label = {
                    Text(
                        text = "Trash",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToTrash()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                    unselectedIconColor = colors.error,
                    unselectedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(color = colors.border.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer / App Info
            Text(
                text = "VoiceJournal v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
