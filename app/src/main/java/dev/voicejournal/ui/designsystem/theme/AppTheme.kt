package dev.voicejournal.ui.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.voicejournal.ui.theme.AppThemeMode

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

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
            primary = appColors.primary,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onPrimary = appColors.onPrimary,
            outline = appColors.divider
        )
    } else {
        darkColorScheme(
            background = appColors.background,
            surface = appColors.surface,
            primary = appColors.primary,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onPrimary = appColors.onPrimary,
            outline = appColors.divider
        )
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = appColors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = appColors.background.toArgb()

            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = appColors.isLight
            insets.isAppearanceLightNavigationBars = appColors.isLight
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
