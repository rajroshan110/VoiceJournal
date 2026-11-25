package dev.voicejournal.ui.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.tokens.physicalMinTouchTarget

@Composable
fun AppPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(Radius.RadiusMd),
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.SpaceLg,
        vertical = Spacing.SpaceXs
    ),
    content: @Composable RowScope.() -> Unit
) {
    val appColors = AppTheme.colors
    val resolvedColors = colors ?: ButtonDefaults.buttonColors(
        containerColor = appColors.primary,
        contentColor = appColors.onPrimary,
        disabledContainerColor = appColors.primary.copy(alpha = 0.38f),
        disabledContentColor = appColors.onPrimary.copy(alpha = 0.38f)
    )

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ComponentDimens.ButtonMinHeight),
        enabled = enabled,
        shape = shape,
        colors = resolvedColors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun AppSecondaryOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(Radius.RadiusMd),
    border: BorderStroke? = BorderStroke(Border.WidthThin, AppTheme.colors.border),
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.SpaceLg,
        vertical = Spacing.SpaceXs
    ),
    content: @Composable RowScope.() -> Unit
) {
    val appColors = AppTheme.colors
    val resolvedColors = colors ?: ButtonDefaults.outlinedButtonColors(
        contentColor = appColors.textPrimary,
        disabledContentColor = appColors.textSecondary.copy(alpha = 0.5f)
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ComponentDimens.ButtonMinHeight),
        enabled = enabled,
        shape = shape,
        border = border,
        colors = resolvedColors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun AppCompactPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colors.primary,
    contentColor: Color = AppTheme.colors.onPrimary,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.SpaceMd,
        vertical = Spacing.SpaceX2s
    ),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.RadiusPill)
    val effectiveContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.38f)
    val effectiveContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = ComponentDimens.ButtonCompactMinHeight)
            .clip(shape)
            .background(color = effectiveContainerColor, shape = shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides effectiveContentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                content()
            }
        }
    }
}

@Composable
fun AppCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualSize: Dp = ComponentDimens.CircleActionVisualSize,
    containerColor: Color = AppTheme.colors.surfaceVariant,
    contentColor: Color = AppTheme.colors.textPrimary,
    border: BorderStroke? = null,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val effectiveContainer = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    val effectiveContent = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .physicalMinTouchTarget()
            .clip(CircleShape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .background(color = effectiveContainer, shape = CircleShape)
                .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides effectiveContent) {
                content()
            }
        }
    }
}

@Composable
fun AppFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.primary,
    contentColor: Color = AppTheme.colors.onPrimary,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(ComponentDimens.FabSize),
        shape = RoundedCornerShape(16.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )
}
