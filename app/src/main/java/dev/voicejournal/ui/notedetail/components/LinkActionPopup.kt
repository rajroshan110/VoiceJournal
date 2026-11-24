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
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.theme.AppTheme
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
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = url,
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openExternalLink(context, url) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { openExternalLink(context, url) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "↗",
                            color = colors.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onEditLink,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Link",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

