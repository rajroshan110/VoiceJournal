package dev.voicejournal.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import dev.voicejournal.ui.designsystem.responsive.AppWindowInfo
import dev.voicejournal.ui.designsystem.responsive.LocalAppWindowInfo
import dev.voicejournal.ui.designsystem.responsive.rememberAppWindowInfo
import dev.voicejournal.ui.theme.AppThemeMode
import dev.voicejournal.ui.util.findActivity

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val windowInfo: AppWindowInfo
        @Composable
        @ReadOnlyComposable
        get() = LocalAppWindowInfo.current

    fun getSelectionBorder(isSelected: Boolean, primaryColor: androidx.compose.ui.graphics.Color, borderColor: androidx.compose.ui.graphics.Color): androidx.compose.foundation.BorderStroke {
        return dev.voicejournal.ui.designsystem.tokens.Border.getSelectionBorder(isSelected, primaryColor, borderColor)
    }
}

@Composable
fun VoiceTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val appColors = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) DarkColors else LightPremiumColors
        AppThemeMode.DARK -> DarkColors
        AppThemeMode.LIGHT_PREMIUM -> LightPremiumColors
    }

    val colorScheme = if (appColors.isLight) {
        lightColorScheme(
            background = appColors.background,
            surface = appColors.surface,
            surfaceVariant = appColors.surfaceVariant,
            primary = appColors.primary,
            primaryContainer = appColors.primaryContainer,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onPrimary = appColors.onPrimary,
            onPrimaryContainer = appColors.primary,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.divider,
            error = appColors.error
        )
    } else {
        darkColorScheme(
            background = appColors.background,
            surface = appColors.surface,
            surfaceVariant = appColors.surfaceVariant,
            primary = appColors.primary,
            primaryContainer = appColors.primaryContainer,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onPrimary = appColors.onPrimary,
            onPrimaryContainer = appColors.primary,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.divider,
            error = appColors.error
        )
    }

    val windowInfo = rememberAppWindowInfo()
    val rawDensity = LocalDensity.current
    val normalizedDensity = remember(
        rawDensity.density,
        windowInfo.uiScaleFactor,
        windowInfo.effectiveFontScale
    ) {
        Density(
            density = rawDensity.density * windowInfo.uiScaleFactor,
            fontScale = windowInfo.effectiveFontScale
        )
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = appColors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = appColors.background.toArgb()

            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = appColors.isLight
            insets.isAppearanceLightNavigationBars = appColors.isLight
        }
    }

    CompositionLocalProvider(
        LocalDensity provides normalizedDensity,
        LocalAppColors provides appColors,
        LocalAppWindowInfo provides windowInfo
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
