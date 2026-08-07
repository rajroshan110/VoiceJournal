package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.ui.calendar.CalendarDayItem
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarGrid(
    gridDays: List<CalendarDayItem>,
    startOfWeek: StartOfWeek = StartOfWeek.SYSTEM_DEFAULT,
    onDateSelect: (CalendarDayItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val dayLabels = remember(startOfWeek) {
        val firstDayOfWeek = when (startOfWeek) {
            StartOfWeek.SYSTEM_DEFAULT -> WeekFields.of(Locale.getDefault()).firstDayOfWeek
            StartOfWeek.MONDAY -> DayOfWeek.MONDAY
            StartOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
        (0 until 7).map { i ->
            firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Header Row: Days of Week (Mon, Tue, Wed...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Column Grid
        Column(modifier = Modifier.fillMaxWidth()) {
            gridDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayItem ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            DayCell(
                                dayItem = dayItem,
                                onClick = { onDateSelect(dayItem) }
                            )
                        }
                    }
                }
            }
        }
    }
}
