package dev.voicejournal.ui.insight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import dev.voicejournal.domain.usecase.PersonMetric
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.theme.tabularNumbers
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing

@Composable
fun PeopleMentionedCard(
    topPeople: List<PersonMetric>,
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val displayedPeople = topPeople.take(10)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.RadiusLg))
            .background(colors.surface)
            .padding(Spacing.SpaceLg)
    ) {
        Text(
            text = "People Mentioned",
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.SpaceMd))

        if (displayedPeople.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No people tagged yet",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = Spacing.SpaceX2s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceLg)
            ) {
                displayedPeople.forEach { person ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(min = 64.dp, max = 80.dp)
                            .clip(RoundedCornerShape(Radius.RadiusMd))
                            .clickable { onPersonClick(person.name) }
                            .padding(vertical = Spacing.SpaceX2s, horizontal = Spacing.SpaceX2s)
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(Spacing.Space3Xl)
                                .background(colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                color = colors.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "@${person.name}",
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${person.count} ${if (person.count == 1) "time" else "times"}",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
