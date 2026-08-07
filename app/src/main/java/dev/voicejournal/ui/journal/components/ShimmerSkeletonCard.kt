package dev.voicejournal.ui.journal.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun ShimmerSkeletonCard(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        colors.surfaceVariant.copy(alpha = 0.6f),
        colors.divider.copy(alpha = 0.8f),
        colors.surfaceVariant.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceX2s),
        shape = RoundedCornerShape(Radius.RadiusMd),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.SpaceLg)) {
            // Header Shimmer (Date & Emoji placeholder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(Radius.RadiusXs))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(Radius.RadiusXs))
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceMd))

            // Body Text Preview Shimmer (3 lines)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(Radius.RadiusXs))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceXs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(Radius.RadiusXs))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(Spacing.SpaceLg))

            // Waveform Shimmer Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(Radius.RadiusSm))
                    .background(brush)
            ) {}

            Spacer(modifier = Modifier.height(Spacing.SpaceMd))

            // Tags Shimmer Row
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(Radius.RadiusMd))
                            .background(brush)
                    )
                }
            }
        }
    }
}
