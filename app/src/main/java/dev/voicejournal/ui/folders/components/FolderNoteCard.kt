package dev.voicejournal.ui.folders.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.util.TimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderNoteCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    val formattedDateTimeText = remember(entry.createdAt, timeFormat, context) {
        TimeFormatter.formatDateWithTime(entry.createdAt, timeFormat, context)
    }

    val noteTitle = remember(entry.title) {
        if (!entry.title.isNullOrBlank()) entry.title else "Untitled Note"
    }

    val snippetText = remember(entry.plainUserText, entry.transcript) {
        val text = entry.plainUserText ?: entry.transcript
        if (!text.isNullOrBlank()) text.trim() else "No text preview"
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp,
        border = AppTheme.getSelectionBorder(isSelected, colors.primary, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Line: Date & Time of creation + Selection Checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDateTimeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Line 1: Note Title / Header
            Text(
                text = noteTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Line 2: Note text preview with ellipsis
            Text(
                text = snippetText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
