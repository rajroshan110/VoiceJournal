package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.components.dialogs.AppModalBottomSheet
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing

private fun getCameraIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(9f, 2f)
            lineToRelative(-1.83f, 2f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(6f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineToRelative(-3.17f)
            lineTo(15f, 2f)
            horizontalLineTo(9f)
            close()
            moveTo(12f, 17f)
            curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
            reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
            reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
            reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
            close()
        }
    }.build()
}

private fun getPhotoLibraryIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "PhotoLibrary",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(22f, 16f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineTo(8f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            close()
            moveTo(11f, 12f)
            lineToRelative(2.03f, 2.71f)
            lineToRelative(2.97f, -3.97f)
            lineTo(20f, 15f)
            horizontalLineTo(9f)
            lineToRelative(2f, -3f)
            close()
            moveTo(2f, 6f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            horizontalLineTo(2f)
            close()
        }
    }.build()
}

private fun getAudioFileIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "AudioFile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(12f, 3f)
            verticalLineToRelative(10.55f)
            curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2f, -0.55f)
            curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
            reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
            reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
            verticalLineTo(7f)
            horizontalLineToRelative(4f)
            verticalLineTo(3f)
            horizontalLineToRelative(-6f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddVoiceRecordingClick: () -> Unit
) {
    val colors = AppTheme.colors

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Spacing.SpaceXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Attachment",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Spacing.SpaceXs, bottom = Spacing.SpaceLg)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.SpaceLg)
                    .clip(RoundedCornerShape(Radius.RadiusLg)),
                verticalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
            ) {
                AddItemRow(
                    icon = getCameraIcon(colors.primary),
                    text = "Take Photo",
                    onClick = { onTakePhotoClick(); onDismiss() }
                )
                AddItemRow(
                    icon = getPhotoLibraryIcon(colors.primary),
                    text = "Add Photo",
                    onClick = { onAddPhotoClick(); onDismiss() }
                )
                AddItemRow(
                    icon = getAudioFileIcon(colors.primary),
                    text = "Add Voice Recording",
                    onClick = { onAddVoiceRecordingClick(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun AddItemRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.RadiusMd))
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(IconSize.IconLg)
        )

        Spacer(modifier = Modifier.width(Spacing.SpaceLg))

        Text(
            text = text,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
