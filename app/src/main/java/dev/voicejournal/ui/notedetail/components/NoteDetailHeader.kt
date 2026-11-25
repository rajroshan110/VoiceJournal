package dev.voicejournal.ui.notedetail.components

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.designsystem.components.buttons.AppCircleIconButton
import dev.voicejournal.ui.designsystem.components.buttons.AppCompactPillButton
import dev.voicejournal.ui.designsystem.components.menus.AppDropdownMenu
import dev.voicejournal.ui.designsystem.components.menus.AppDropdownMenuItem
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
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
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    isMoodEnabled: Boolean = true
) {
    val colors = AppTheme.colors
    val windowInfo = AppTheme.windowInfo
    val actionVisualSize = if (windowInfo.isNarrowPhone) 36.dp else ComponentDimens.CircleActionVisualSize
    val context = LocalContext.current
    var moodMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var moreMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val moreMenuTransitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(moreMenuExpanded) {
        moreMenuTransitionState.targetState = moreMenuExpanded
    }

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
            .semantics { isTraversalGroup = true }
    ) {
        if (isSelectionMode) {
            // Selection Mode Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceXs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close / Clear Selection Button (Circle X)
                AppCircleIconButton(
                    onClick = onClearSelection,
                    visualSize = actionVisualSize,
                    contentDescription = "Clear Selection"
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconMd)
                    )
                }

                // Selection Count Title
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Delete Selected Tracks Button (Circle 🗑)
                AppCircleIconButton(
                    onClick = onDeleteSelectedTracks,
                    visualSize = actionVisualSize,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentDescription = "Delete Selected Tracks"
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconMd)
                    )
                }
            }
        } else {
        // Standard Note Detail Header Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Cluster: Close (X) + Mood Selector + Archive + Add Attachment (+)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (windowInfo.isNarrowPhone) Spacing.SpaceX3s else Spacing.SpaceX2s)
            ) {
                // Minimalist Close Button (Circle X)
                AppCircleIconButton(
                    onClick = onClose,
                    visualSize = actionVisualSize,
                    contentDescription = "Close"
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconMd)
                    )
                }

                // Minimalist Emoji Selector Button (Circle Emoji)
                if (isMoodEnabled) {
                    Box {
                        AppCircleIconButton(
                            onClick = { moodMenuExpanded = true },
                            visualSize = actionVisualSize,
                            contentDescription = "Select Mood"
                        ) {
                            Text(
                                text = currentMood,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        AppDropdownMenu(
                            expanded = moodMenuExpanded,
                            onDismissRequest = { moodMenuExpanded = false }
                        ) {
                            EMOTION_EMOJIS.forEach { (emoji, label) ->
                                AppDropdownMenuItem(
                                    text = "$emoji  $label",
                                    isSelected = emoji == currentMood,
                                    onClick = {
                                        onMoodSelect(emoji)
                                        moodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Minimalist Archive Button (Circle Archive)
                AppCircleIconButton(
                    onClick = onArchiveClick,
                    visualSize = actionVisualSize,
                    contentDescription = "Archive Note"
                ) {
                    Icon(
                        imageVector = ArchiveIcon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconMd)
                    )
                }

                // Minimalist Attachment Button (Circle +)
                AppCircleIconButton(
                    onClick = onAddPhoto,
                    visualSize = actionVisualSize,
                    contentDescription = "Add Item"
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconMd)
                    )
                }
            }

            // Flexible separator pinning Left Cluster to start and Right Cluster to end
            Spacer(modifier = Modifier.weight(1f))

            // Right Cluster: Save + Options (⋮)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
            ) {
                // Save Pill Button (Pure alpha fade in / fade out)
                AnimatedVisibility(
                    visible = showSaveButton,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    AppCompactPillButton(
                        onClick = onSave,
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Minimalist 3-Dot Options Button (Circle ⋮) with smooth animated popup
                Box {
                    val density = LocalDensity.current
                    AppCircleIconButton(
                        onClick = { moreMenuExpanded = !moreMenuExpanded },
                        visualSize = actionVisualSize,
                        containerColor = if (moreMenuExpanded) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant,
                        contentColor = if (moreMenuExpanded) colors.primary else colors.textPrimary,
                        contentDescription = "More Options"
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.IconMd)
                        )
                    }

                    if (moreMenuTransitionState.currentState || moreMenuTransitionState.targetState) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(x = 0, y = with(density) { 50.dp.roundToPx() }),
                            onDismissRequest = { moreMenuExpanded = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Box {
                                androidx.compose.animation.AnimatedVisibility(
                                    visibleState = moreMenuTransitionState,
                                    enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        transformOrigin = TransformOrigin(1f, 0f),
                                        initialScale = 0.75f
                                    ),
                                    exit = fadeOut(animationSpec = tween(140)) + scaleOut(
                                        animationSpec = tween(140, easing = FastOutSlowInEasing),
                                        transformOrigin = TransformOrigin(1f, 0f),
                                        targetScale = 0.75f
                                    )
                                ) {
                                    Surface(
                                        color = colors.surface,
                                        shape = RoundedCornerShape(ComponentDimens.MenuCornerRadius),
                                        shadowElevation = 12.dp,
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(Border.WidthThin, colors.textSecondary.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .widthIn(
                                                min = 210.dp,
                                                max = (windowInfo.normalizedWidthDp * 0.72f).dp.coerceAtMost(280.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = Spacing.SpaceXs)
                                        ) {
                                            // Created & Modified Time Info Header
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceX2s)
                                            ) {
                                                Text(
                                                    text = "Created",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textSecondary
                                                )
                                                Text(
                                                    text = formattedCreated,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.textPrimary
                                                )
                                                Spacer(modifier = Modifier.height(Spacing.SpaceX2s))
                                                Text(
                                                    text = "Last Modified",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textSecondary
                                                )
                                                Text(
                                                    text = formattedUpdated,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.textPrimary
                                                )
                                            }

                                            if (onDeleteNote != null) {
                                                HorizontalDivider(
                                                    color = colors.divider.copy(alpha = 0.5f),
                                                    modifier = Modifier.padding(vertical = Spacing.SpaceX2s)
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .defaultMinSize(minHeight = ComponentDimens.DropdownItemMinHeight)
                                                        .clip(RoundedCornerShape(Radius.RadiusSm))
                                                        .clickable {
                                                            moreMenuExpanded = false
                                                            onDeleteNote()
                                                        }
                                                        .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceXs),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Note",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(IconSize.IconSm)
                                                    )
                                                    Text(
                                                        text = "Delete Note",
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}


