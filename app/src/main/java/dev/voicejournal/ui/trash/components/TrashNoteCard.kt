package dev.voicejournal.ui.trash.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun TrashNoteCard(
    entry: JournalEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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

    val remainingDays = entry.daysUntilPermanentDeletion
    val isExpiringSoon = entry.isExpiringSoon

    val badgeColor = if (isExpiringSoon) colors.error else colors.primary
    val badgeBg = if (isExpiringSoon) colors.error.copy(alpha = 0.12f) else colors.primary.copy(alpha = 0.12f)
    val badgeText = if (remainingDays <= 1) "Deletes in 1 day" else "Deletes in $remainingDays days"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) colors.primary else colors.border.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date & Time
                Text(
                    text = formattedDateTimeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )

                // Right Row: Expiration Badge + Checkmark Icon when selected
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Title
            Text(
                text = noteTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Text preview
            Text(
                text = snippetText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
