package dev.voicejournal.ui.designsystem.components.menus

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.Spacing

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val windowInfo = AppTheme.windowInfo
    val maxWidth = (windowInfo.normalizedWidthDp * 0.72f).dp
        .coerceAtLeast(ComponentDimens.DropdownMinWidth)
        .coerceAtMost(ComponentDimens.DropdownMaxWidth)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(
            min = ComponentDimens.DropdownMinWidth,
            max = maxWidth
        ),
        offset = offset,
        properties = properties,
        shape = RoundedCornerShape(ComponentDimens.MenuCornerRadius),
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        content = content
    )
}

@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isDestructive: Boolean = false
) {
    val colors = AppTheme.colors
    val resolvedTextColor = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.5f)
        isDestructive -> MaterialTheme.colorScheme.error
        isSelected -> colors.primary
        else -> colors.textPrimary
    }
    val resolvedIconColor = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.5f)
        isDestructive -> MaterialTheme.colorScheme.error
        isSelected -> colors.primary
        else -> colors.textSecondary
    }

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ComponentDimens.DropdownItemMinHeight),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = resolvedTextColor,
            leadingIconColor = resolvedIconColor,
            trailingIconColor = resolvedIconColor,
            disabledTextColor = colors.textSecondary.copy(alpha = 0.5f),
            disabledLeadingIconColor = colors.textSecondary.copy(alpha = 0.5f),
            disabledTrailingIconColor = colors.textSecondary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(
            horizontal = Spacing.SpaceLg,
            vertical = Spacing.SpaceXs
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.AppExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(ComponentDimens.MenuCornerRadius),
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        content = content
    )
}
