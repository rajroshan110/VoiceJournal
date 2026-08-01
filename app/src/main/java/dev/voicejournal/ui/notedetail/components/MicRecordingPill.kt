package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.theme.AppTheme
import java.util.Locale

enum class MicFabState {
    IDLE, RECORDING, PAUSED
}

private fun getMicIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(12f, 14f)
            curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
            reflectiveCurveTo(9f, 3.34f, 9f, 5f)
            verticalLineToRelative(6f)
            curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
            close()
            moveTo(17f, 11f)
            curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
            reflectiveCurveToRelative(-5f, -2.24f, -5f, -5f)
            horizontalLineTo(5f)
            curveToRelative(0f, 3.53f, 2.61f, 6.43f, 6f, 6.92f)
            verticalLineTo(21f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-3.08f)
            curveToRelative(3.39f, -0.49f, 6f, -3.39f, 6f, -6.92f)
            horizontalLineToRelative(-2f)
            close()
        }
    }.build()
}

private fun getPauseIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }.build()
}

@Composable
fun MicRecordingPill(
    state: MicFabState,
    durationMs: Long,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onSaveTrack: () -> Unit,
    onCancelRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val formattedDuration = remember(durationMs) {
        val totalSecs = (durationMs / 1000L).coerceAtLeast(0L)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    val cornerRadius by animateDpAsState(
        targetValue = if (state == MicFabState.IDLE) 16.dp else 24.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillCornerRadius"
    )

    Surface(
        modifier = modifier
            .padding(bottom = 8.dp)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(cornerRadius),
        color = colors.primary,
        shadowElevation = 6.dp
    ) {
        AnimatedContent(
            targetState = state == MicFabState.IDLE,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f)) togetherWith
                (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f))
            },
            label = "micPillContentTransition"
        ) { isIdle ->
            if (isIdle) {
                // 56dp Floating Mic FAB
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onStartRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMicIcon(colors.onPrimary),
                        contentDescription = "Record Voice Note",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                // Fixed Minimalist Horizontal Control Pill with Generous Spacing
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live / Frozen Duration Timer (Monospace digits with fixed min width prevents jitter)
                    Box(
                        modifier = Modifier.widthIn(min = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formattedDuration,
                            color = colors.onPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Pause / Resume Control Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.onPrimary.copy(alpha = 0.2f), CircleShape)
                            .clickable {
                                if (state == MicFabState.RECORDING) {
                                    onPauseRecording()
                                } else {
                                    onResumeRecording()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state == MicFabState.RECORDING) getPauseIcon(colors.onPrimary) else Icons.Default.PlayArrow,
                            contentDescription = if (state == MicFabState.RECORDING) "Pause Recording" else "Resume Recording",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save Track Checkmark Button (✓)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.onPrimary, CircleShape)
                            .clickable { onSaveTrack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Audio Track",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Cancel Recording Button (X)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.onPrimary.copy(alpha = 0.2f), CircleShape)
                            .clickable { onCancelRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Recording",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
