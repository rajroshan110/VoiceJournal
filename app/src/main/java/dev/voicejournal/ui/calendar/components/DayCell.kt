package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.calendar.CalendarDayItem
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.format.DateTimeFormatter

@Composable
fun DayCell(
    dayItem: CalendarDayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dateStr = dayItem.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    val count = dayItem.entries.size
    val categoriesStr = dayItem.categories.joinToString(", ").ifEmpty { "No categories" }
    val accessibilityDescription = "$dateStr, $count entries recorded. $categoriesStr"

    val textColor = when {
        dayItem.isSelected -> colors.onPrimary
        !dayItem.isCurrentMonth -> colors.textSecondary.copy(alpha = 0.38f)
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                color = if (dayItem.isSelected) colors.primary else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = if (dayItem.isToday && !dayItem.isSelected) 1.5.dp else 0.dp,
                color = if (dayItem.isToday) colors.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
            .semantics {
                contentDescription = accessibilityDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayItem.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (dayItem.isToday || dayItem.isSelected) FontWeight.Bold else FontWeight.Normal
            )

            // Single Entry Indicator Dot
            if (count > 0 && !dayItem.isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.primary, CircleShape)
                )
            }
        }
    }
}
