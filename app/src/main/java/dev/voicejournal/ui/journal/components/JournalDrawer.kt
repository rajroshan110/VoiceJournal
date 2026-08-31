package dev.voicejournal.ui.journal.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.components.getFolderIcon
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.designsystem.theme.AppTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import dev.voicejournal.BuildConfig

@Composable
fun JournalDrawerContent(
    isFolderEnabled: Boolean = true,
    onNavigateToArchive: () -> Unit = {},
    onNavigateToDraft: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    val appIconBitmap = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 108
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }

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
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "Voice Journal",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
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

            // Navigation Items
            Text(
                text = "FEATURES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            // 3. Folder Navigation Item (Below Draft, shown only when isFolderEnabled)
            if (isFolderEnabled) {
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
            }

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

            // Footer / App Info with dynamic Version Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "App Version",
                                "VoiceJournal v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                            )
                        )
                        Toast.makeText(context, "Version info copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VoiceJournal v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = colors.surfaceVariant
                ) {
                    Text(
                        text = "Build ${BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
