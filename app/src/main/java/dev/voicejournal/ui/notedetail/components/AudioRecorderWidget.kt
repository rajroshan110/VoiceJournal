package dev.voicejournal.ui.notedetail.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.util.Locale

@Composable
fun AudioRecorderWidget(
    isRecording: Boolean,
    durationMs: Long = 0L,
    onRecordToggle: () -> Unit
) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(if (isRecording) pulseScale else 1.0f)
                .background(
                    color = if (isRecording) Color(0xFFCF6679) else colors.primary,
                    shape = CircleShape
                )
                .clickable { onRecordToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                tint = colors.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecording) {
            val totalSeconds = (durationMs / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val formattedTime = String.format(Locale.US, "%d:%02d", minutes, seconds)
            Text(
                text = "Recording... $formattedTime",
                color = Color(0xFFCF6679),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif
            )
        } else {
            Text(
                text = "Tap to start recording",
                color = colors.textSecondary,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif
            )
        }
    }
}
