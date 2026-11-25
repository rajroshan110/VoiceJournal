package dev.voicejournal.ui.notedetail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.components.buttons.AppCircleIconButton
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.model.SpanType

/**
 * Validates that [rawUrl] uses an explicit http:// or https:// scheme (case-insensitive)
 * with a non-blank authority and no control characters.
 */
internal fun isAllowedExternalUrl(rawUrl: String): Boolean {
    if (rawUrl.any { it.isISOControl() }) return false
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return false
    val afterScheme = when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
        else -> return false
    }
    val authorityEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        .let { if (it == -1) afterScheme.length else it }
    val authority = afterScheme.substring(0, authorityEnd)
    return authority.isNotBlank() && authority.none { it.isWhitespace() }
}

private fun openExternalLink(context: Context, rawUrl: String) {
    if (!isAllowedExternalUrl(rawUrl)) return
    try {
        val uri = Uri.parse(rawUrl.trim())
        val scheme = uri.scheme?.lowercase()
        if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    } catch (_: Exception) {}
}

@Composable
fun LinkActionPopup(
    richTextState: RichTextState,
    onEditLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLink = richTextState.getLinkAtCursor()
    val context = LocalContext.current
    val colors = AppTheme.colors

    AnimatedVisibility(
        visible = activeLink != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (activeLink != null) {
            val url = (activeLink.type as? SpanType.Link)?.url ?: ""

            Surface(
                color = colors.surfaceVariant,
                shape = RoundedCornerShape(Radius.RadiusMd),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceX2s)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = url,
                        color = colors.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openExternalLink(context, url) }
                    )

                    Spacer(modifier = Modifier.width(Spacing.SpaceXs))

                    AppCircleIconButton(
                        onClick = { openExternalLink(context, url) },
                        visualSize = 32.dp,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = colors.primary,
                        contentDescription = "Open Link"
                    ) {
                        Text(
                            text = "↗",
                            color = colors.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AppCircleIconButton(
                        onClick = onEditLink,
                        visualSize = 32.dp,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = colors.textPrimary,
                        contentDescription = "Edit Link"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.IconSm)
                        )
                    }
                }
            }
        }
    }
}

