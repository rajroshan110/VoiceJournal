package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.voicejournal.ui.theme.AppTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneDateTimePicker(
    initialDateTimeMillis: Long,
    initialTab: Int = 0,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val now = remember { System.currentTimeMillis() }
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = if (initialDateTimeMillis in 1..now) initialDateTimeMillis else now
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented Tab Switcher (Date / Time)
                Row(
                    modifier = Modifier
                        .background(colors.surfaceVariant, CircleShape)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Date", "Time").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .weight(1f)
                                .background(
                                    color = if (isSelected) colors.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.DateRange else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.onPrimary else colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) colors.onPrimary else colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Date Picker Component
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = colors.surface,
                            selectedDayContainerColor = colors.primary,
                            selectedDayContentColor = colors.onPrimary,
                            todayDateBorderColor = colors.primary,
                            dayContentColor = colors.textPrimary,
                            weekdayContentColor = colors.textSecondary,
                            subheadContentColor = colors.textSecondary
                        ),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                } else {
                    // Time Picker Component
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                selectorColor = colors.primary,
                                timeSelectorSelectedContainerColor = colors.primary,
                                timeSelectorSelectedContentColor = colors.onPrimary,
                                timeSelectorUnselectedContainerColor = colors.surfaceVariant,
                                timeSelectorUnselectedContentColor = colors.textPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm / Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val currentNow = System.currentTimeMillis()
                            val selectedCal = Calendar.getInstance()
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedCal.timeInMillis = millis
                            }
                            selectedCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            selectedCal.set(Calendar.MINUTE, timePickerState.minute)
                            val finalMillis = minOf(selectedCal.timeInMillis, currentNow)
                            onDateTimeSelected(finalMillis)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("OK", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
