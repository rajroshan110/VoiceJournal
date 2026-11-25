package dev.voicejournal.ui.designsystem.tokens

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.responsive.LocalAppWindowInfo

/**
 * VoiceJournal Accessibility Touch Target Specification
 */
object TouchTarget {
    /** 48.dp — Mandatory minimum hardware touch target size */
    val MinTouchTargetSize: Dp = 48.dp
}

/**
 * Ensures the composable maintains at least 48dp of physical touch area even when
 * proportional UI scaling shrinks logical dp on narrower phones.
 */
fun Modifier.physicalMinTouchTarget(): Modifier = composed {
    val scale = LocalAppWindowInfo.current.uiScaleFactor
    val target = if (scale < 1f) (48f / scale).dp else TouchTarget.MinTouchTargetSize
    this.defaultMinSize(minWidth = target, minHeight = target)
}
