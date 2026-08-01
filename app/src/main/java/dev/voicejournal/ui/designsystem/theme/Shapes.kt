package dev.voicejournal.ui.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import dev.voicejournal.ui.designsystem.tokens.Radius

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.RadiusXs),
    small = RoundedCornerShape(Radius.RadiusSm),
    medium = RoundedCornerShape(Radius.RadiusMd),
    large = RoundedCornerShape(Radius.RadiusLg),
    extraLarge = RoundedCornerShape(Radius.RadiusPill)
)
