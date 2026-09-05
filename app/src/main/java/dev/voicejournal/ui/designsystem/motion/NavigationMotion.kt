package dev.voicejournal.ui.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * VoiceJournal Standard Navigation Motion Specs
 */
object NavigationMotion {

    /**
     * Screen Push (Forward Navigation to Detail Screen)
     */
    val ScreenPushEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = MotionSpecs.tweenStandard()
    ) + fadeIn(animationSpec = MotionSpecs.tweenStandard())

    val ScreenPushExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
        animationSpec = MotionSpecs.tweenExit()
    ) + fadeOut(animationSpec = MotionSpecs.tweenExit())

    /**
     * Screen Pop (Back Navigation from Detail Screen)
     */
    val ScreenPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
        animationSpec = MotionSpecs.tweenStandard()
    ) + fadeIn(animationSpec = MotionSpecs.tweenStandard())

    val ScreenPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = MotionSpecs.tweenExit()
    ) + fadeOut(animationSpec = MotionSpecs.tweenExit())

    /**
     * Root Tab Switch Forward (Left-to-Right)
     */
    val TabSwitchForwardEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = tween(260, easing = MotionSpecs.EasingStandard)
    ) + fadeIn(animationSpec = tween(260, easing = MotionSpecs.EasingStandard))

    val TabSwitchForwardExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
        animationSpec = tween(220, easing = MotionSpecs.EasingExit)
    ) + fadeOut(animationSpec = tween(220, easing = MotionSpecs.EasingExit))

    /**
     * Root Tab Switch Backward (Right-to-Left)
     */
    val TabSwitchBackwardEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
        animationSpec = tween(260, easing = MotionSpecs.EasingStandard)
    ) + fadeIn(animationSpec = tween(260, easing = MotionSpecs.EasingStandard))

    val TabSwitchBackwardExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = tween(220, easing = MotionSpecs.EasingExit)
    ) + fadeOut(animationSpec = tween(220, easing = MotionSpecs.EasingExit))

    /**
     * Default Fallback Screen Slide
     */
    val DefaultEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
        animationSpec = MotionSpecs.tweenFast()
    ) + fadeIn(animationSpec = MotionSpecs.tweenFast())

    val DefaultExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (-fullWidth * 0.10f).toInt() },
        animationSpec = MotionSpecs.tweenExit()
    ) + fadeOut(animationSpec = MotionSpecs.tweenExit())

    /**
     * Check whether system reduced motion or animation disable setting is active
     */
    fun isReducedMotion(context: android.content.Context): Boolean {
        return try {
            val resolver = context.contentResolver
            val durationScale = android.provider.Settings.Global.getFloat(
                resolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            val transitionScale = android.provider.Settings.Global.getFloat(
                resolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}
