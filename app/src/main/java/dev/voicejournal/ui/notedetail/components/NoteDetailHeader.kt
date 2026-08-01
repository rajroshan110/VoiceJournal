package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.theme.AppTheme
import dev.voicejournal.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ArchiveIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Outlined.Archive",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(20.54f, 5.23f)
            lineTo(19.15f, 3.55f)
            curveTo(18.88f, 3.21f, 18.47f, 3f, 18f, 3f)
            horizontalLineTo(6f)
            curveTo(5.53f, 3f, 5.12f, 3.21f, 4.84f, 3.55f)
            lineTo(3.46f, 5.23f)
            curveTo(3.17f, 5.57f, 3f, 6.02f, 3f, 6.5f)
            verticalLineTo(19f)
            curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
            verticalLineTo(6.5f)
            curveTo(21f, 6.02f, 20.83f, 5.57f, 20.54f, 5.23f)
            close()
            moveTo(5.12f, 5f)
            horizontalLineTo(18.87f)
            lineTo(19.66f, 6f)
            horizontalLineTo(4.34f)
            lineTo(5.12f, 5f)
            close()
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(8f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            close()
            moveTo(12f, 9.5f)
            lineTo(8f, 13.5f)
            horizontalLineTo(10.5f)
            verticalLineTo(17f)
            horizontalLineTo(13.5f)
            verticalLineTo(13.5f)
            horizontalLineTo(16f)
            lineTo(12f, 9.5f)
            close()
        }
    }.build()
}

val EMOTION_EMOJIS = listOf(
    "😊" to "Happy",
    "😌" to "Peaceful",
    "😔" to "Sad / Sorrow",
    "😤" to "Frustrated",
    "😡" to "Angry"
)

@Composable
fun NoteDetailHeader(
    onClose: () -> Unit,
    onMoodSelect: (String) -> Unit,
    currentMood: String,
    onSave: () -> Unit,
    onAddPhoto: () -> Unit,
    createdAt: Long = 0L,
    updatedAt: Long = 0L,
    onDeleteNote: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    showSaveButton: Boolean = false,
    onClearSelection: () -> Unit = {},
    onDeleteSelectedTracks: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    var moodMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val formattedCreated = remember(createdAt, timeFormat, context) {
        if (createdAt > 0) {
            TimeFormatter.formatHeaderDateTime(createdAt, timeFormat, context)
        } else "Just now"
    }

    val formattedUpdated = remember(updatedAt, createdAt, timeFormat, context) {
        val targetTime = if (updatedAt > 0) updatedAt else createdAt
        if (targetTime > 0) {
            TimeFormatter.formatHeaderDateTime(targetTime, timeFormat, context)
        } else "Just now"
    }

    Surface(
        color = colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        if (isSelectionMode) {
            // Selection Mode Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close / Clear Selection Button (Circle X)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceVariant, CircleShape)
                    .clickable { onClearSelection() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = colors.textPrimary)
            }

            // Selection Count Title
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            // Delete Selected Tracks Button (Circle 🗑)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    .clickable { onDeleteSelectedTracks() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Selected Tracks",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    } else {
        // Standard Note Detail Header Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Cluster: Close (X) + Mood Selector + Add Attachment (+)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minimalist Close Button (Circle X)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceVariant, CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary)
                }

                // Minimalist Emoji Selector Button (Circle Emoji)
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surfaceVariant, CircleShape)
                            .clickable { moodMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentMood, fontSize = 20.sp)
                    }

                    DropdownMenu(
                        expanded = moodMenuExpanded,
                        onDismissRequest = { moodMenuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        EMOTION_EMOJIS.forEach { (emoji, label) ->
                            DropdownMenuItem(
                                text = { Text("$emoji  $label", color = colors.textPrimary, fontSize = 14.sp) },
                                onClick = {
                                    onMoodSelect(emoji)
                                    moodMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Minimalist Archive Button (Circle Archive)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceVariant, CircleShape)
                        .clickable { onArchiveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ArchiveIcon,
                        contentDescription = "Archive Note",
                        tint = colors.textPrimary
                    )
                }

                // Minimalist Attachment Button (Circle +)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceVariant, CircleShape)
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = colors.textPrimary)
                }
            }

            // Right Cluster: Save + Options (⋮)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save Pill Button (Pure alpha fade in / fade out)
                AnimatedVisibility(
                    visible = showSaveButton,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(17.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Save", color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Minimalist 3-Dot Options Button (Circle ⋮)
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surfaceVariant, CircleShape)
                            .clickable { moreMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = colors.textPrimary)
                    }

                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 220.dp)
                            .background(colors.surface)
                    ) {
                        // Created & Modified Time Info Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Created",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = formattedCreated,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Last Modified",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = formattedUpdated,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                        }

                        if (onDeleteNote != null) {
                            HorizontalDivider(color = colors.divider)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete Note",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Note",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onDeleteNote()
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
