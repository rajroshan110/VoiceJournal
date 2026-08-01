package dev.voicejournal.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

fun getFolderIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(10f, 4f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(2f, 18f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(8f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineToRelative(-8f)
            lineToRelative(-2f, -2f)
            close()
        }
    }.build()
}

fun getCreateNewFolderIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "CreateNewFolder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(20f, 6f)
            horizontalLineToRelative(-8f)
            lineToRelative(-2f, -2f)
            horizontalLineTo(4f)
            curveToRelative(-1.11f, 0f, -1.99f, 0.89f, -1.99f, 2f)
            lineTo(2f, 18f)
            curveToRelative(0f, 1.11f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.11f, 0f, 2f, -0.89f, 2f, -2f)
            verticalLineTo(8f)
            curveToRelative(0f, -1.11f, -0.89f, -2f, -2f, -2f)
            close()
            moveTo(19f, 14f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-3f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(3f)
            verticalLineTo(9f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            close()
        }
    }.build()
}

fun getGridViewIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "GridView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(3f, 3f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineTo(3f)
            horizontalLineTo(3f)
            close()
            moveTo(13f, 3f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineTo(3f)
            horizontalLineToRelative(-8f)
            close()
            moveTo(3f, 13f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-8f)
            horizontalLineTo(3f)
            close()
            moveTo(13f, 13f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(-8f)
            close()
        }
    }.build()
}

fun getListViewIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "ListView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(3f, 13f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 17f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 9f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 13f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 17f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 7f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(14f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            close()
        }
    }.build()
}

fun getTagIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "Tag",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(21.41f, 11.58f)
            lineToRelative(-9f, -9f)
            curveTo(12.05f, 2.22f, 11.55f, 2f, 11f, 2f)
            horizontalLineTo(4f)
            curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
            verticalLineToRelative(7f)
            curveToRelative(0f, 0.55f, 0.22f, 1.05f, 0.59f, 1.42f)
            lineToRelative(9f, 9f)
            curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
            lineToRelative(7.41f, -7.41f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.04f, 0f, -1.43f)
            close()
            moveTo(5.5f, 7f)
            curveTo(4.67f, 7f, 4f, 6.33f, 4f, 5.5f)
            reflectiveCurveTo(4.67f, 4f, 5.5f, 4f)
            reflectiveCurveTo(7f, 4.67f, 7f, 5.5f)
            reflectiveCurveTo(6.33f, 7f, 5.5f, 7f)
            close()
        }
    }.build()
}

fun getArchiveIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "Archive",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
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

fun getDraftIcon(tintColor: Color = Color.Unspecified): ImageVector {
    return ImageVector.Builder(
        name = "Draft",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(21.99f, 8f)
            curveToRelative(0f, -0.72f, -0.37f, -1.35f, -0.94f, -1.7f)
            lineTo(13f, 1.5f)
            curveToRelative(-0.6f, -0.36f, -1.4f, -0.36f, -2f, 0f)
            lineTo(2.95f, 6.3f)
            curveTo(2.38f, 6.65f, 2f, 7.28f, 2f, 8f)
            verticalLineToRelative(8f)
            curveToRelative(0f, 0.72f, 0.37f, 1.35f, 0.94f, 1.7f)
            lineToRelative(8.05f, 4.8f)
            curveToRelative(0.6f, 0.36f, 1.4f, 0.36f, 2f, 0f)
            lineToRelative(8.05f, -4.8f)
            curveToRelative(0.57f, -0.35f, 0.95f, -0.98f, 0.95f, -1.7f)
            verticalLineTo(8f)
            close()
            moveTo(12f, 3.5f)
            lineToRelative(7f, 4.18f)
            lineTo(12f, 11.87f)
            lineTo(5f, 7.68f)
            lineToRelative(7f, -4.18f)
            close()
        }
    }.build()
}
