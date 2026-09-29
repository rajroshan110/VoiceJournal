package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.voicejournal.ui.calendar.components.CalendarMonthPicker
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    initialDateTimeMillis: Long,
    initialTab: Int = 0,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val now = remember { System.currentTimeMillis() }
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = if (initialDateTimeMillis in 1..now) initialDateTimeMillis else now
        }
    }

    val initialLocalDate = remember {
        LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    var selectedDateEpochDay by rememberSaveable { mutableLongStateOf(initialLocalDate.toEpochDay()) }
    val selectedDate = remember(selectedDateEpochDay) { LocalDate.ofEpochDay(selectedDateEpochDay) }

    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    var isMinuteMode by rememberSaveable { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    if (!isInitialized) {
        if (isMinuteMode) {
            timePickerState.selection = TimePickerSelectionMode.Minute
        }
        isInitialized = true
    }

    LaunchedEffect(timePickerState.selection) {
        isMinuteMode = (timePickerState.selection == TimePickerSelectionMode.Minute)
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val onConfirm = {
        val currentNow = System.currentTimeMillis()
        val selectedCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.year)
            set(Calendar.MONTH, selectedDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            set(Calendar.MINUTE, timePickerState.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val finalMillis = minOf(selectedCal.timeInMillis, currentNow)
        onDateTimeSelected(finalMillis)
    }

    val tabSwitcher = @Composable {
        Row(
            modifier = Modifier
                .background(colors.surfaceVariant, CircleShape)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Date", "Time").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(72.dp)
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
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            color = if (isSelected) colors.onPrimary else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        if (isLandscape) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                modifier = Modifier
                    .widthIn(min = 390.dp, max = 420.dp)
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // Top Bar: Tabs on the left, Cancel/OK on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabSwitcher()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onConfirm,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("OK", color = colors.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Content Area: Calendar or Time Picker matching length/width
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            CalendarMonthPicker(
                                selectedDate = selectedDate,
                                onDateSelected = { date -> selectedDateEpochDay = date.toEpochDay() },
                                isLandscape = true
                            )
                        } else {
                            LandscapeTimePicker(timePickerState = timePickerState)
                        }
                    }
                }
            }
        } else {
            // Portrait mode
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    tabSwitcher()
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            CalendarMonthPicker(
                                selectedDate = selectedDate,
                                onDateSelected = { date -> selectedDateEpochDay = date.toEpochDay() },
                                isLandscape = false
                            )
                        } else {
                            TimePicker(
                                state = timePickerState,
                                layoutType = TimePickerLayoutType.Vertical,
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
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("OK", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backwards-compatibility alias for [DateTimePicker].
 */
@Deprecated("Use DateTimePicker instead", ReplaceWith("DateTimePicker(initialDateTimeMillis, initialTab, onDateTimeSelected, onDismiss)"))
@Composable
fun JuneDateTimePicker(
    initialDateTimeMillis: Long,
    initialTab: Int = 0,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    DateTimePicker(
        initialDateTimeMillis = initialDateTimeMillis,
        initialTab = initialTab,
        onDateTimeSelected = onDateTimeSelected,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeTimePicker(
    timePickerState: TimePickerState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isMinute = timePickerState.selection == TimePickerSelectionMode.Minute
    val isAfternoon = timePickerState.hour >= 12
    val displayHour = remember(timePickerState.hour) {
        val h = timePickerState.hour % 12
        if (h == 0) 12 else h
    }
    val displayMinute = timePickerState.minute

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Time Digits: 01 / 30 / AM / PM
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            // Hour Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (!isMinute) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .size(width = 58.dp, height = 46.dp)
                    .clickable { timePickerState.selection = TimePickerSelectionMode.Hour }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayHour.toString().padStart(2, '0'),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isMinute) colors.onPrimary else colors.textPrimary
                    )
                }
            }

            Text(
                text = ":",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Minute Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isMinute) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .size(width = 58.dp, height = 46.dp)
                    .clickable { timePickerState.selection = TimePickerSelectionMode.Minute }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayMinute.toString().padStart(2, '0'),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMinute) colors.onPrimary else colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AM / PM Toggle Stack
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.divider),
                modifier = Modifier.width(58.dp)
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = if (!isAfternoon) colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (isAfternoon) {
                                    timePickerState.hour = (timePickerState.hour - 12).coerceAtLeast(0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isAfternoon) colors.primary else colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = if (isAfternoon) colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (!isAfternoon) {
                                    timePickerState.hour = (timePickerState.hour + 12).coerceAtMost(23)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAfternoon) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Analog Clock Face (Round, fits inside dial with no clipping)
        val dialSize = 232.dp
        val density = LocalDensity.current
        val dialSizePx = with(density) { dialSize.toPx() }
        val centerOffsetPx = dialSizePx / 2f
        val numbersRadiusPx = with(density) { ((dialSize / 2) - 24.dp).toPx() }
        val selectorRadiusPx = with(density) { 17.dp.toPx() }
        val centerDotRadiusPx = with(density) { 4.dp.toPx() }
        val handStrokeWidthPx = with(density) { 2.dp.toPx() }
        val smallDotRadiusPx = with(density) { 2.5f.dp.toPx() }
        val dialFaceColor = if (colors.isLight) Color(0xFFE6E1E8) else Color(0xFF49454E)

        val selectorAngleDeg = remember(isMinute, timePickerState.hour, timePickerState.minute) {
            if (!isMinute) {
                val h12 = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                (h12 * 30 - 90).toDouble()
            } else {
                (timePickerState.minute * 6 - 90).toDouble()
            }
        }
        val selectorAngleRad = Math.toRadians(selectorAngleDeg)
        val selectorX = centerOffsetPx + numbersRadiusPx * cos(selectorAngleRad).toFloat()
        val selectorY = centerOffsetPx + numbersRadiusPx * sin(selectorAngleRad).toFloat()

        fun updateFromPosition(pos: Offset) {
            val dx = pos.x - centerOffsetPx
            val dy = pos.y - centerOffsetPx
            val angleRad = kotlin.math.atan2(dy, dx)
            var degrees = Math.toDegrees(angleRad.toDouble()) + 90.0
            if (degrees < 0) degrees += 360.0

            if (!isMinute) {
                val raw = (Math.round(degrees / 30.0).toInt()) % 12
                val hour12 = if (raw == 0) 12 else raw
                val isPm = timePickerState.hour >= 12
                val newHour24 = if (isPm) {
                    if (hour12 == 12) 12 else hour12 + 12
                } else {
                    if (hour12 == 12) 0 else hour12
                }
                timePickerState.hour = newHour24
            } else {
                val min = (Math.round(degrees / 6.0).toInt()) % 60
                timePickerState.minute = min
            }
        }

        Box(
            modifier = Modifier
                .size(dialSize)
                .pointerInput(isMinute) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateFromPosition(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (!isMinute) {
                                    timePickerState.selection = TimePickerSelectionMode.Minute
                                }
                                break
                            }
                            updateFromPosition(change.position)
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dial Face Background (true circle, never clipped)
                drawCircle(
                    color = dialFaceColor,
                    radius = size.minDimension / 2f,
                    center = center
                )

                // Hand line
                drawLine(
                    color = colors.primary,
                    start = center,
                    end = Offset(selectorX, selectorY),
                    strokeWidth = handStrokeWidthPx,
                    cap = StrokeCap.Round
                )

                // Center pivot dot
                drawCircle(
                    color = colors.primary,
                    radius = centerDotRadiusPx,
                    center = center
                )

                // Selector circle at selected number
                drawCircle(
                    color = colors.primary,
                    radius = selectorRadiusPx,
                    center = Offset(selectorX, selectorY)
                )

                // If minute is not a multiple of 5, draw a small white dot at hand tip
                if (isMinute && timePickerState.minute % 5 != 0) {
                    drawCircle(
                        color = colors.onPrimary,
                        radius = smallDotRadiusPx,
                        center = Offset(selectorX, selectorY)
                    )
                }
            }

            // Draw Clock Numbers
            if (!isMinute) {
                val curHour12 = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                (1..12).forEach { h ->
                    val angleRad = Math.toRadians((h * 30 - 90).toDouble())
                    val nx = centerOffsetPx + numbersRadiusPx * cos(angleRad).toFloat()
                    val ny = centerOffsetPx + numbersRadiusPx * sin(angleRad).toFloat()
                    val isSelected = h == curHour12

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .offset {
                                IntOffset(
                                    x = (nx - with(density) { 17.dp.toPx() }).roundToInt(),
                                    y = (ny - with(density) { 17.dp.toPx() }).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = h.toString(),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.onPrimary else colors.textPrimary
                        )
                    }
                }
            } else {
                (0..11).forEach { idx ->
                    val m = idx * 5
                    val angleRad = Math.toRadians((m * 6 - 90).toDouble())
                    val nx = centerOffsetPx + numbersRadiusPx * cos(angleRad).toFloat()
                    val ny = centerOffsetPx + numbersRadiusPx * sin(angleRad).toFloat()
                    val isSelected = m == timePickerState.minute

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .offset {
                                IntOffset(
                                    x = (nx - with(density) { 17.dp.toPx() }).roundToInt(),
                                    y = (ny - with(density) { 17.dp.toPx() }).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.toString().padStart(2, '0'),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.onPrimary else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
