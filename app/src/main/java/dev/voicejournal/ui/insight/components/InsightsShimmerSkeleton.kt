package dev.voicejournal.ui.insight.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun InsightsShimmerSkeleton(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val shimmerColors = listOf(
        colors.surfaceVariant.copy(alpha = 0.6f),
        colors.divider.copy(alpha = 0.8f),
        colors.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.SpaceLg)
    ) {
        // Period selector skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.Space3Xl)
                .clip(RoundedCornerShape(Radius.RadiusPill))
                .background(brush)
        )

        // Recording Activity Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(Radius.RadiusLg))
                .background(brush)
        )

        // Mood Trends Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(Radius.RadiusLg))
                .background(brush)
        )

        // Top Tags Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(Radius.RadiusLg))
                .background(brush)
        )

        // People Mentioned Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(Radius.RadiusLg))
                .background(brush)
        )
    }
}
