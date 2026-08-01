package dev.voicejournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun TagChip(
    tag: Tag,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val icon = when (tag.type) {
        TagType.TOPIC -> "🏷️ "
        TagType.PERSON -> "👤 "
        TagType.MOOD -> "😌 "
        TagType.THING, TagType.FOLDER -> "📁 "
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (selected) colors.primary else colors.surfaceVariant,
                shape = RoundedCornerShape(Radius.RadiusPill)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
    ) {
        Text(
            text = "$icon${tag.name}",
            color = if (selected) colors.onPrimary else colors.textPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove tag",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(IconSize.IconXs)
                    .clickable { onDelete.invoke() }
            )
        }
    }
}
