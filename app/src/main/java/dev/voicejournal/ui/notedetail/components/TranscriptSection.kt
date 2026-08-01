package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun TranscriptSection(
    transcript: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isTranscribing: Boolean = false
) {
    val colors = AppTheme.colors

    if (isTranscribing) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Transcribing audio locally...", color = colors.textSecondary, fontSize = 13.sp)
        }
        return
    }

    if (transcript.isNullOrEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Capsule Button: [ ∨ Tap to expand transcript ]
        Box(
            modifier = Modifier
                .background(colors.surfaceVariant, RoundedCornerShape(20.dp))
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isExpanded) "Collapse transcript" else "Tap to expand transcript",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = transcript,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
