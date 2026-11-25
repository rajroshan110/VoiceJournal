package dev.voicejournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.designsystem.tokens.physicalMinTouchTarget

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
    val chipShape = RoundedCornerShape(Radius.RadiusPill)

    Box(
        modifier = Modifier
            .physicalMinTouchTarget()
            .semantics {
                if (onClick != null) {
                    role = Role.Button
                }
                contentDescription = if (selected) "Tag ${tag.name}, selected" else "Tag ${tag.name}"
            }
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(chipShape)
                        .clickable { onClick.invoke() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = ComponentDimens.ChipMinHeight)
                .background(
                    color = if (selected) colors.primary else colors.surfaceVariant,
                    shape = chipShape
                )
                .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
        ) {
            val cleanName = tag.name.trim().trimStart('#', '@').trim()
            Text(
                text = "$icon$cleanName",
                color = if (selected) colors.onPrimary else colors.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
                Box(
                    modifier = Modifier
                        .size(IconSize.IconMd)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Remove tag ${tag.name}"
                        }
                        .clickable { onDelete.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = if (selected) colors.onPrimary else colors.textSecondary,
                        modifier = Modifier.size(IconSize.IconXs)
                    )
                }
            }
        }
    }
}
