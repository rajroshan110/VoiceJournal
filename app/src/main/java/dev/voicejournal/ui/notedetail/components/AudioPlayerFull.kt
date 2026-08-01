package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.AnimatedVisibility
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
import dev.voicejournal.ui.components.UnifiedAudioPlayerBar
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun AudioPlayerFull(
    isPlaying: Boolean,
    progress: Float = 0f,
    waveformAmplitudes: List<Byte> = emptyList(),
    durationMs: Long = 0L,
    currentPosMs: Long = 0L,
    transcript: String? = null,
    isTranscriptExpanded: Boolean = false,
    isTranscribing: Boolean = false,
    isTranscriptionFailed: Boolean = false,
    isSpeechToTextEnabled: Boolean = true,
    onPlayToggle: () -> Unit,
    onSeek: ((Float) -> Unit)? = null,
    onAaClick: () -> Unit,
    onRegenerateClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        UnifiedAudioPlayerBar(
            isPlaying = isPlaying,
            currentPositionMs = currentPosMs,
            durationMs = durationMs,
            waveformAmplitudes = waveformAmplitudes,
            onPlayPauseClick = onPlayToggle,
            onSeekFraction = onSeek,
            trailingContent = if (isSpeechToTextEnabled) {
                {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isTranscribing) colors.primary.copy(alpha = 0.5f) else colors.primary,
                                CircleShape
                            )
                            .clickable(enabled = !isTranscribing) { onAaClick() },
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
            } else null
        )

        // Expandable Transcript Container with Actions at TOP of the Box
        val shouldShowContainer = isSpeechToTextEnabled && (isTranscribing || (isTranscriptExpanded && (!transcript.isNullOrEmpty() || isTranscriptionFailed)))

        if (shouldShowContainer) {
            AnimatedVisibility(visible = true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Top Action Bar inside the box: "Failed in between" (left) & "Regenerate transcript" (right)
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
                            Text("Generating transcript...", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }

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
    }
}
