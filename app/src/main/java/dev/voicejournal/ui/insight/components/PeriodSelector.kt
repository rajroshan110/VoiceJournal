package dev.voicejournal.ui.insight.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.usecase.TimePeriod
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelect: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimePeriod.values().forEach { period ->
            val isSelected = period == selectedPeriod

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else Color.Transparent,
                animationSpec = tween(durationMillis = 250),
                label = "PeriodBgColor"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.textSecondary,
                animationSpec = tween(durationMillis = 250),
                label = "PeriodTextColor"
            )

            val label = when (period) {
                TimePeriod.WEEK -> "Week"
                TimePeriod.MONTH -> "Month"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onPeriodSelect(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
