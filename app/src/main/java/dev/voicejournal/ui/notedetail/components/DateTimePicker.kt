package dev.voicejournal.ui.notedetail.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.voicejournal.ui.calendar.CalendarUtils
import dev.voicejournal.ui.calendar.components.CalendarMonthPicker
import dev.voicejournal.ui.designsystem.components.buttons.AppCompactPillButton
import dev.voicejournal.ui.designsystem.components.buttons.AppPrimaryButton
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import java.time.LocalDate
import java.time.YearMonth
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

    var isYearPickerVisible by rememberSaveable { mutableStateOf(false) }
    val yearGridState = rememberLazyGridState()

    val initialPage = remember {
        CalendarUtils.yearMonthToPage(YearMonth.of(initialLocalDate.year, initialLocalDate.monthValue))
            .coerceIn(0, CalendarUtils.getMaxPage())
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { CalendarUtils.getPageCount() }
    )

    var selectedHour by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var isMinuteMode by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val windowInfo = AppTheme.windowInfo
    val isLandscape = windowInfo.isCompactLandscape || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onConfirm = {
        val currentNow = System.currentTimeMillis()
        val selectedCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.year)
            set(Calendar.MONTH, selectedDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
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
                        .defaultMinSize(minWidth = 72.dp, minHeight = 32.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isSelected) colors.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.DateRange else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isSelected) colors.onPrimary else colors.textSecondary,
                            modifier = Modifier.size(IconSize.IconXs)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
                        Text(
                            text = label,
                            color = if (isSelected) colors.onPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        if (isLandscape) {
            Surface(
                shape = RoundedCornerShape(ComponentDimens.DialogCornerRadius),
                color = colors.surface,
                modifier = Modifier
                    .widthIn(max = (windowInfo.normalizedWidthDp * 0.85f).dp.coerceAtMost(460.dp))
                    .wrapContentHeight()
                    .padding(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceX2s)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = Spacing.SpaceXs, vertical = Spacing.SpaceXs)
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
                                contentPadding = PaddingValues(horizontal = Spacing.SpaceXs, vertical = Spacing.SpaceX2s)
                            ) {
                                Text("Cancel", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(modifier = Modifier.width(Spacing.SpaceX2s))
                            AppCompactPillButton(
                                onClick = onConfirm,
                                contentPadding = PaddingValues(horizontal = Spacing.SpaceMd, vertical = Spacing.SpaceX2s)
                            ) {
                                Text(
                                    text = "OK",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.SpaceX2s))

                    // Content Area: Calendar or Time Picker matching length/width
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (windowInfo.normalizedHeightDp * 0.76f).dp.coerceAtMost(280.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            CalendarMonthPicker(
                                selectedDate = selectedDate,
                                onDateSelected = { date -> selectedDateEpochDay = date.toEpochDay() },
                                isLandscape = true,
                                pagerState = pagerState,
                                isYearPickerVisible = isYearPickerVisible,
                                onYearPickerVisibleChange = { isYearPickerVisible = it },
                                yearGridState = yearGridState
                            )
                        } else {
                            LandscapeTimePicker(
                                selectedHour = selectedHour,
                                selectedMinute = selectedMinute,
                                isMinuteMode = isMinuteMode,
                                onHourChange = { newHour -> selectedHour = newHour },
                                onMinuteChange = { newMinute -> selectedMinute = newMinute },
                                onModeChange = { isMin -> isMinuteMode = isMin }
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait mode
            Surface(
                shape = RoundedCornerShape(ComponentDimens.DialogCornerRadius),
                color = colors.surface,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.SpaceLg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.SpaceLg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    tabSwitcher()
                    Spacer(modifier = Modifier.height(Spacing.SpaceLg))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            CalendarMonthPicker(
                                selectedDate = selectedDate,
                                onDateSelected = { date -> selectedDateEpochDay = date.toEpochDay() },
                                isLandscape = false,
                                pagerState = pagerState,
                                isYearPickerVisible = isYearPickerVisible,
                                onYearPickerVisibleChange = { isYearPickerVisible = it },
                                yearGridState = yearGridState
                            )
                        } else {
                            key(isLandscape) {
                                val portraitTimePickerState = rememberTimePickerState(
                                    initialHour = selectedHour,
                                    initialMinute = selectedMinute,
                                    is24Hour = false
                                )
                                LaunchedEffect(Unit) {
                                    if (isMinuteMode) {
                                        portraitTimePickerState.selection = TimePickerSelectionMode.Minute
                                    }
                                }
                                LaunchedEffect(portraitTimePickerState.hour, portraitTimePickerState.minute) {
                                    selectedHour = portraitTimePickerState.hour
                                    selectedMinute = portraitTimePickerState.minute
                                }
                                LaunchedEffect(portraitTimePickerState.selection) {
                                    isMinuteMode = (portraitTimePickerState.selection == TimePickerSelectionMode.Minute)
                                }
                                TimePicker(
                                    state = portraitTimePickerState,
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
                    }

                    Spacer(modifier = Modifier.height(Spacing.SpaceLg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(Spacing.SpaceXs))
                        AppPrimaryButton(
                            onClick = onConfirm,
                            contentPadding = PaddingValues(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceXs)
                        ) {
                            Text(
                                text = "OK",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
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

@Composable
private fun LandscapeTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    isMinuteMode: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val windowInfo = AppTheme.windowInfo
    val isMinute = isMinuteMode
    val isAfternoon = selectedHour >= 12
    val displayHour = remember(selectedHour) {
        val h = selectedHour % 12
        if (h == 0) 12 else h
    }
    val displayMinute = selectedMinute

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Time Digits: 01 / 30 / AM / PM
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(end = Spacing.SpaceLg)
        ) {
            // Hour Box
            Surface(
                shape = RoundedCornerShape(Radius.RadiusMd),
                color = if (!isMinute) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .size(width = 58.dp, height = 46.dp)
                    .clickable { onModeChange(false) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayHour.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!isMinute) colors.onPrimary else colors.textPrimary
                    )
                }
            }

            Text(
                text = ":",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = Spacing.SpaceX3s)
            )

            // Minute Box
            Surface(
                shape = RoundedCornerShape(Radius.RadiusMd),
                color = if (isMinute) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .size(width = 58.dp, height = 46.dp)
                    .clickable { onModeChange(true) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayMinute.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isMinute) colors.onPrimary else colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SpaceXs))

            // AM / PM Toggle Stack
            Surface(
                shape = RoundedCornerShape(Radius.RadiusSm),
                color = colors.surface,
                border = BorderStroke(Border.WidthThin, colors.divider),
                modifier = Modifier.width(58.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.SpaceX3s)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(Radius.RadiusXs))
                            .background(
                                color = if (!isAfternoon) colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(Radius.RadiusXs)
                            )
                            .clickable {
                                if (isAfternoon) {
                                    onHourChange((selectedHour - 12).coerceAtLeast(0))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!isAfternoon) colors.primary else colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.SpaceX3s))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(Radius.RadiusXs))
                            .background(
                                color = if (isAfternoon) colors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(Radius.RadiusXs)
                            )
                            .clickable {
                                if (!isAfternoon) {
                                    onHourChange((selectedHour + 12).coerceAtMost(23))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAfternoon) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Analog Clock Face (Round, proportional to screen height, never clipped)
        val dialSize = (windowInfo.normalizedHeightDp * 0.56f).dp.coerceIn(176.dp, 232.dp)
        val density = LocalDensity.current
        val dialSizePx = with(density) { dialSize.toPx() }
        val centerOffsetPx = dialSizePx / 2f
        val numbersRadiusPx = dialSizePx * 0.396f
        val selectorRadiusPx = dialSizePx * 0.073f
        val numberBoxSize = dialSize * 0.146f
        val numberBoxHalfPx = with(density) { (numberBoxSize / 2f).toPx() }
        val centerDotRadiusPx = with(density) { 4.dp.toPx() }
        val handStrokeWidthPx = with(density) { 2.dp.toPx() }
        val smallDotRadiusPx = with(density) { 2.5f.dp.toPx() }
        val dialFaceColor = if (colors.isLight) Color(0xFFE6E1E8) else Color(0xFF49454E)

        val selectorAngleDeg = remember(isMinute, selectedHour, selectedMinute) {
            if (!isMinute) {
                val h12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                (h12 * 30 - 90).toDouble()
            } else {
                (selectedMinute * 6 - 90).toDouble()
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
                val isPm = selectedHour >= 12
                val newHour24 = if (isPm) {
                    if (hour12 == 12) 12 else hour12 + 12
                } else {
                    if (hour12 == 12) 0 else hour12
                }
                onHourChange(newHour24)
            } else {
                val min = (Math.round(degrees / 6.0).toInt()) % 60
                onMinuteChange(min)
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
                                    onModeChange(true)
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
                if (isMinute && selectedMinute % 5 != 0) {
                    drawCircle(
                        color = colors.onPrimary,
                        radius = smallDotRadiusPx,
                        center = Offset(selectorX, selectorY)
                    )
                }
            }

            // Draw Clock Numbers
            if (!isMinute) {
                val curHour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                (1..12).forEach { h ->
                    val angleRad = Math.toRadians((h * 30 - 90).toDouble())
                    val nx = centerOffsetPx + numbersRadiusPx * cos(angleRad).toFloat()
                    val ny = centerOffsetPx + numbersRadiusPx * sin(angleRad).toFloat()
                    val isSelected = h == curHour12

                    Box(
                        modifier = Modifier
                            .size(numberBoxSize)
                            .offset {
                                IntOffset(
                                    x = (nx - numberBoxHalfPx).roundToInt(),
                                    y = (ny - numberBoxHalfPx).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = h.toString(),
                            style = MaterialTheme.typography.labelMedium,
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
                    val isSelected = m == selectedMinute

                    Box(
                        modifier = Modifier
                            .size(numberBoxSize)
                            .offset {
                                IntOffset(
                                    x = (nx - numberBoxHalfPx).roundToInt(),
                                    y = (ny - numberBoxHalfPx).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.onPrimary else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
