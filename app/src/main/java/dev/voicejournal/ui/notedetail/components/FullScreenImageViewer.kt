package dev.voicejournal.ui.notedetail.components

import androidx.compose.runtime.Composable
import dev.voicejournal.ui.journal.components.LightboxDialog

@Composable
fun FullScreenImageViewer(
    imagePath: String,
    onClose: () -> Unit
) {
    LightboxDialog(
        imageUri = imagePath,
        onDismiss = onClose
    )
}
