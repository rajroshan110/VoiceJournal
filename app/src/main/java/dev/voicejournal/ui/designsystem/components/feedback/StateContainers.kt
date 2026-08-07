package dev.voicejournal.ui.designsystem.components.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun LoadingStateContainer(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.SpaceLg)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = message
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = colors.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceMd))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateContainer(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Info,
    actionButton: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.SpaceXl)
            .semantics {
                contentDescription = if (subtitle.isNullOrEmpty()) title else "$title. $subtitle"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(IconSize.IconHero)
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceMd))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.SpaceXs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(Spacing.SpaceLg))
                actionButton()
            }
        }
    }
}

@Composable
fun ErrorStateContainer(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.SpaceXl)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "Error: $errorMessage"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(IconSize.IconHero)
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceMd))
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceXs))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.SpaceLg))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier.defaultMinSize(minWidth = dev.voicejournal.ui.designsystem.tokens.TouchTarget.MinTouchTargetSize, minHeight = dev.voicejournal.ui.designsystem.tokens.TouchTarget.MinTouchTargetSize)
            ) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
