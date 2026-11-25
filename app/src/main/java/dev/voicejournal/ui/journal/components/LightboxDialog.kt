package dev.voicejournal.ui.journal.components

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.voicejournal.ui.designsystem.components.buttons.AppCircleIconButton
import dev.voicejournal.ui.designsystem.components.dialogs.AppAlertDialog
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Spacing
import java.io.File

@Composable
fun LightboxDialog(
    imageUri: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (newScale <= 1.01f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            val maxOffsetX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
            val maxOffsetY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
            val newX = (offset.x + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            val newY = (offset.y + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            offset = Offset(newX, newY)
        }
    }

    val modelObj: Any = remember(imageUri) {
        if (imageUri.startsWith("/")) File(imageUri) else imageUri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() }, // Tapping anywhere outside closes image
            contentAlignment = Alignment.Center
        ) {
            // Padded container for image to enforce safe margins around display edges
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 64.dp)
                    .clipToBounds()
                    .onGloballyPositioned { containerSize = it.size }
                    .clickable(enabled = false) {} // Consume clicks inside image container
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = modelObj,
                    contentDescription = "Interactive full-screen media image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            // Top Action Bar (Delete + Close)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = Spacing.SpaceMd, start = Spacing.SpaceLg, end = Spacing.SpaceLg),
                horizontalArrangement = if (onDelete != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    AppCircleIconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        contentDescription = "Delete image"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.IconLg)
                        )
                    }
                }

                AppCircleIconButton(
                    onClick = onDismiss,
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    contentDescription = "Close full-screen image viewer"
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.IconLg)
                    )
                }
            }
        }
    }

    // Confirmation dialog before deleting individual image
    if (showDeleteConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete image?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this image? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}
