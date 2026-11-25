package dev.voicejournal.ui.insight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import dev.voicejournal.domain.usecase.TagMetric
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.theme.tabularNumbers
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing

@Composable
fun TopTagsCard(
    topTags: List<TagMetric>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val displayedTags = topTags.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.RadiusLg))
            .background(colors.surface)
            .padding(Spacing.SpaceLg)
    ) {
        Text(
            text = "Top Tags",
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.SpaceMd))

        if (displayedTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tags recorded yet",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.SpaceXs)
            ) {
                displayedTags.forEach { tag ->
                    val plainName = tag.name.removePrefix("#")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(Radius.RadiusSm))
                            .clickable { onTagClick(plainName) }
                            .padding(vertical = 6.dp, horizontal = Spacing.SpaceX2s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plainName,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(0.35f, fill = false)
                                .widthIn(max = 108.dp)
                        )

                        Spacer(modifier = Modifier.width(Spacing.SpaceXs))

                        // Progress fill bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(Spacing.SpaceXs)
                                .clip(RoundedCornerShape(Radius.RadiusXs))
                                .background(colors.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (tag.percentage / 100f).coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(Radius.RadiusXs))
                                    .background(colors.primary)
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.SpaceMd))

                        Text(
                            text = "${tag.count} ${if (tag.count == 1) "entry" else "entries"}",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
