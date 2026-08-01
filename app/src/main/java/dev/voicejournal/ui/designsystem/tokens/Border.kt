package dev.voicejournal.ui.designsystem.tokens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VoiceJournal Border Stroke Tokens & Helpers
 */
object Border {
    /** 1.dp — Standard unselected card border width */
    val WidthThin: Dp = 1.dp

    /** 2.dp — Active selection card border width */
    val WidthThick: Dp = 2.dp

    /**
     * Selection border factory method
     */
    fun getSelectionBorder(isSelected: Boolean, primaryColor: Color, borderColor: Color): BorderStroke {
        return BorderStroke(
            width = if (isSelected) WidthThick else WidthThin,
            color = if (isSelected) primaryColor else borderColor.copy(alpha = 0.5f)
        )
    }
}
