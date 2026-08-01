package dev.voicejournal.ui.insight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.usecase.TimeOfDayMetric
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.theme.AppTheme
import java.util.Locale

@Composable
fun PeakReflectionCard(
    timeOfDayDistribution: List<TimeOfDayMetric>,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val peakPeriod = timeOfDayDistribution.maxByOrNull { it.count }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.RadiusLg))
            .background(colors.surface)
            .padding(Spacing.SpaceLg)
    ) {
        // Title Row
        Text(
            text = "Peak Reflection Hours",
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.SpaceMd))

        // 4 Time of Day Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
        ) {
            timeOfDayDistribution.forEach { item ->
                val pctStr = String.format(Locale.US, "%.0f%%", item.percentage)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clip(RoundedCornerShape(Radius.RadiusMd))
                        .background(colors.surfaceVariant)
                        .padding(vertical = Spacing.SpaceXs, horizontal = Spacing.SpaceX3s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = item.periodName,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pctStr,
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Peak Insight Badge placed below the cards, left-aligned
        peakPeriod?.let { peak ->
            if (peak.count > 0) {
                Spacer(modifier = Modifier.height(Spacing.SpaceMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.RadiusSm))
                            .background(colors.primaryContainer)
                            .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
                    ) {
                        Text(
                            text = "${peak.icon} ${peak.periodName}",
                            color = colors.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
