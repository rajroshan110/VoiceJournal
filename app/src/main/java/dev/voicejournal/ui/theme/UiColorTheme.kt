package dev.voicejournal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AppColors(
    val background: Color,
    val secondaryBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val border: Color,
    val error: Color = Color(0xFFE53935),
    val isLight: Boolean
)

val DarkColors = AppColors(
    background = Color(0xFF121212),
    secondaryBackground = Color(0xFF181818),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF242424),
    primary = Color(0xFF8B9A46),
    primaryContainer = Color(0xFF262C18),
    onPrimary = Color(0xFF121212),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0A0A0),
    divider = Color(0xFF2E2E2E),
    border = Color(0xFF333333),
    error = Color(0xFFCF6679),
    isLight = false
)

val LightPremiumColors = AppColors(
    background = Color(0xFFF9F2E9),
    secondaryBackground = Color(0xFFFFF8F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFDFBF8),
    primary = Color(0xFF6B1100),
    primaryContainer = Color(0xFFF5E6E3),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1C1B1F),
    textSecondary = Color(0xFF796E65),
    divider = Color(0xFFE8DED2),
    border = Color(0xFFEFE7DD),
    error = Color(0xFFD32F2F),
    isLight = true
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    fun getSelectionBorder(isSelected: Boolean, primaryColor: Color, borderColor: Color): androidx.compose.foundation.BorderStroke {
        return androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) primaryColor else borderColor.copy(alpha = 0.5f)
        )
    }
}
