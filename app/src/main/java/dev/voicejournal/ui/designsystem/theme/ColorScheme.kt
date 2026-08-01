package dev.voicejournal.ui.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * VoiceJournal Semantic Color Scheme Data Model
 */
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
    val error: Color = Color(0xFFCF6679),
    val isLight: Boolean
)

/**
 * Dark Obsidian Theme Color Palette (Default)
 */
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

/**
 * Light Premium Paper Theme Color Palette
 */
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
