package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.motion.MotionSpecs
import dev.voicejournal.ui.designsystem.theme.AppTheme

/**
 * Circular "Aa" button that triggers transcription for a single audio track.
 *
 * Responsibility: render the transcription affordance only.
 * It is placed by the parent screen in a Row beside [UnifiedAudioPlayerBar];
 * it has no knowledge of playback state.
 */
@Composable
fun TranscriptionButton(
    isTranscribing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .size(34.dp)
            .background(
                if (isTranscribing) colors.primary.copy(alpha = 0.5f) else colors.primary,
                CircleShape
            )
            .clickable(enabled = !isTranscribing) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isTranscribing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Aa",
                color = colors.onPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Expandable transcript panel that appears below an audio track's player row.
 *
 * Responsibility: display transcript text, generation progress, failure state,
 * and the regenerate action. Visibility is driven entirely by the caller via
 * [isExpanded] and [isTranscribing] — no internal expand/collapse toggle.
 */
@Composable
fun TranscriptSection(
    transcript: String?,
    isExpanded: Boolean,
    isTranscribing: Boolean,
    isTranscriptionFailed: Boolean,
    onRegenerateClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors

    val shouldShowContainer = isTranscribing ||
            (isExpanded && (!transcript.isNullOrEmpty() || isTranscriptionFailed))

    AnimatedVisibility(
        visible = shouldShowContainer,
        enter = expandVertically(animationSpec = MotionSpecs.tweenFast()) +
                fadeIn(animationSpec = MotionSpecs.tweenFast()),
        exit = shrinkVertically(animationSpec = MotionSpecs.tweenExit()) +
                fadeOut(animationSpec = MotionSpecs.tweenExit())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Top action bar: "Failed in between" (left) & "Regenerate transcript" (right)
            if (onRegenerateClick != null || isTranscriptionFailed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTranscriptionFailed) {
                        Text(
                            text = "Failed in between",
                            color = colors.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (onRegenerateClick != null) {
                        TextButton(
                            onClick = onRegenerateClick,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Regenerate transcript",
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Generating progress row
            if (isTranscribing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating transcript...",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Transcript text
            if (!transcript.isNullOrEmpty()) {
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
