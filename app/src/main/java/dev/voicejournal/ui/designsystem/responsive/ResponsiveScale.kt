package dev.voicejournal.ui.designsystem.responsive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

enum class AppWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class AppWindowInfo(
    val rawWidthDp: Int,
    val rawHeightDp: Int,
    val shortestWidthDp: Int,
    val normalizedWidthDp: Float,
    val normalizedHeightDp: Float,
    val uiScaleFactor: Float,
    val effectiveFontScale: Float,
    val aspectRatio: Float,
    val widthSizeClass: AppWidthSizeClass,
    val isNarrowPhone: Boolean,
    val isCompactHeight: Boolean,
    val isCompactLandscape: Boolean,
    val isWideLayout: Boolean
)

/**
 * Computes the proportional UI density scale factor relative to a 412dp reference phone.
 * Tablets / expanded screens (>= 600dp shortest width) keep native 1.0f density.
 */
fun calculateUiScaleFactor(shortestWidthDp: Int): Float {
    return if (shortestWidthDp >= 600) {
        1.0f
    } else {
        (shortestWidthDp.toFloat() / 412f).coerceIn(0.84f, 1.15f)
    }
}

/**
 * Clamps system font scale so extreme accessibility/OEM font settings don't break card/button geometry
 * while still honoring user legibility preferences.
 */
fun normalizeFontScale(rawFontScale: Float, shortestWidthDp: Int): Float {
    return if (shortestWidthDp >= 600) {
        rawFontScale.coerceIn(0.85f, 1.30f)
    } else {
        rawFontScale.coerceIn(0.95f, 1.15f)
    }
}

fun computeAppWindowInfo(
    screenWidthDp: Int,
    screenHeightDp: Int,
    isLandscape: Boolean,
    rawFontScale: Float
): AppWindowInfo {
    val safeWidth = screenWidthDp.coerceAtLeast(1)
    val safeHeight = screenHeightDp.coerceAtLeast(1)
    val shortestWidthDp = minOf(safeWidth, safeHeight)
    val uiScaleFactor = calculateUiScaleFactor(shortestWidthDp)
    val effectiveFontScale = normalizeFontScale(rawFontScale, shortestWidthDp)
    val normalizedWidthDp = safeWidth.toFloat() / uiScaleFactor
    val normalizedHeightDp = safeHeight.toFloat() / uiScaleFactor
    val aspectRatio = safeHeight.toFloat() / safeWidth.toFloat()

    val widthSizeClass = when {
        safeWidth < 600 -> AppWidthSizeClass.COMPACT
        safeWidth < 840 -> AppWidthSizeClass.MEDIUM
        else -> AppWidthSizeClass.EXPANDED
    }

    val isNarrowPhone = shortestWidthDp < 370
    val isCompactHeight = safeHeight < 640
    val isCompactLandscape = isLandscape && safeHeight < 480
    val isWideLayout = widthSizeClass != AppWidthSizeClass.COMPACT

    return AppWindowInfo(
        rawWidthDp = safeWidth,
        rawHeightDp = safeHeight,
        shortestWidthDp = shortestWidthDp,
        normalizedWidthDp = normalizedWidthDp,
        normalizedHeightDp = normalizedHeightDp,
        uiScaleFactor = uiScaleFactor,
        effectiveFontScale = effectiveFontScale,
        aspectRatio = aspectRatio,
        widthSizeClass = widthSizeClass,
        isNarrowPhone = isNarrowPhone,
        isCompactHeight = isCompactHeight,
        isCompactLandscape = isCompactLandscape,
        isWideLayout = isWideLayout
    )
}

val LocalAppWindowInfo = staticCompositionLocalOf {
    computeAppWindowInfo(
        screenWidthDp = 412,
        screenHeightDp = 915,
        isLandscape = false,
        rawFontScale = 1.0f
    )
}

@Composable
fun rememberAppWindowInfo(): AppWindowInfo {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val rawFontScale = density.fontScale

    return remember(screenWidthDp, screenHeightDp, isLandscape, rawFontScale) {
        computeAppWindowInfo(
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = isLandscape,
            rawFontScale = rawFontScale
        )
    }
}
