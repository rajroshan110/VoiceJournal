package dev.voicejournal.ui.theme

import androidx.compose.runtime.Composable
import dev.voicejournal.ui.designsystem.theme.VoiceTheme as DesignVoiceTheme

@Composable
fun VoiceTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    DesignVoiceTheme(
        themeMode = themeMode,
        content = content
    )
}
