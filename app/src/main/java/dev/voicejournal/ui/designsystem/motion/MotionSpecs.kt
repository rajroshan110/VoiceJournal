package dev.voicejournal.ui.designsystem.motion

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * VoiceJournal Motion Specification Tokens
 * Single source of truth for all animation durations, easings, and spring physics.
 */
object MotionSpecs {
    // Duration Tiers (ms)
    const val DurationInstant: Int = 100
    const val DurationFast: Int = 200
    const val DurationStandard: Int = 300
    const val DurationExpressive: Int = 375
    const val DurationShimmer: Int = 1200

    // Easing Curves
    val EasingStandard = FastOutSlowInEasing
    val EasingExit = FastOutLinearInEasing
    val EasingEnter = LinearOutSlowInEasing
    val EasingLinear = LinearEasing

    // Spring Physics Specs
    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Standard Tween Specs
    fun <T> tweenFast() = tween<T>(durationMillis = DurationFast, easing = EasingStandard)
    fun <T> tweenStandard() = tween<T>(durationMillis = DurationStandard, easing = EasingStandard)
    fun <T> tweenExit() = tween<T>(durationMillis = DurationFast, easing = EasingExit)
}
