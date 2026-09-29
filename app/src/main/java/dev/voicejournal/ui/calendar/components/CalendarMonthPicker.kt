package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.calendar.CalendarUtils
import dev.voicejournal.ui.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Shared swipeable monthly calendar picker container used across VoiceJournal.
 * Supports horizontal swipe gesture between months, year jumping, and responsive portrait/landscape sizing.
 */
@Composable
fun CalendarMonthPicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    maxDate: LocalDate = LocalDate.now(),
    isYearPickerVisibleState: MutableState<Boolean>? = null
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        dev.voicejournal.ui.designsystem.motion.NavigationMotion.isReducedMotion(context)
    }
    val coroutineScope = rememberCoroutineScope()

    val internalYearPickerState = rememberSaveable { mutableStateOf(false) }
    val yearPickerVisibleState = isYearPickerVisibleState ?: internalYearPickerState
    var isYearPickerVisible by yearPickerVisibleState

    val initialPage = remember(selectedDate) {
        CalendarUtils.yearMonthToPage(YearMonth.of(selectedDate.year, selectedDate.monthValue))
            .coerceIn(0, CalendarUtils.getMaxPage())
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { CalendarUtils.getPageCount() }
    )

    val displayedYearMonth = remember(pagerState.currentPage) {
        CalendarUtils.pageToYearMonth(pagerState.currentPage)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isYearPickerVisible) {
            YearPickerGrid(
                currentYear = maxDate.year,
                selectedYear = selectedDate.year,
                onYearSelected = { y ->
                    val nowMonth = YearMonth.now()
                    val candidateYearMonth = YearMonth.of(y, displayedYearMonth.monthValue)
                    val targetYearMonth = if (candidateYearMonth > nowMonth) nowMonth else candidateYearMonth
                    val adjustedDay = selectedDate.dayOfMonth.coerceAtMost(targetYearMonth.lengthOfMonth())
                    val candidateDate = targetYearMonth.atDay(adjustedDay)
                    val finalDate = if (candidateDate > maxDate) maxDate else candidateDate
                    onDateSelected(finalDate)
                    val targetPage = CalendarUtils.yearMonthToPage(targetYearMonth).coerceIn(0, CalendarUtils.getMaxPage())
                    coroutineScope.launch {
                        pagerState.scrollToPage(targetPage)
                    }
                    isYearPickerVisible = false
                },
                isLandscape = isLandscape
            )
        } else {
            MonthNavigationHeader(
                displayedYearMonth = displayedYearMonth,
                canGoNext = pagerState.currentPage < CalendarUtils.getMaxPage(),
                onPrevious = {
                    if (pagerState.currentPage > 0) {
                        val target = pagerState.currentPage - 1
                        coroutineScope.launch {
                            if (isReducedMotion) pagerState.scrollToPage(target)
                            else pagerState.animateScrollToPage(target)
                        }
                    }
                },
                onNext = {
                    if (pagerState.currentPage < CalendarUtils.getMaxPage()) {
                        val target = pagerState.currentPage + 1
                        coroutineScope.launch {
                            if (isReducedMotion) pagerState.scrollToPage(target)
                            else pagerState.animateScrollToPage(target)
                        }
                    }
                },
                onTitleClick = { isYearPickerVisible = true }
            )
            Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 6.dp))
            WeekdayHeaderRow()
            Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 4.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                key = { page -> page },
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val pageYearMonth = remember(page) {
                    CalendarUtils.pageToYearMonth(page)
                }
                CalendarGridDays(
                    displayedYearMonth = pageYearMonth,
                    selectedDate = selectedDate,
                    today = maxDate,
                    onDateClick = { date -> onDateSelected(date) },
                    isLandscape = isLandscape
                )
            }
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    displayedYearMonth: YearMonth,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = colors.textPrimary
            )
        }
        Text(
            text = displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTitleClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = if (canGoNext) colors.textPrimary else colors.textSecondary.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val colors = AppTheme.colors
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        weekdays.forEach { dayName ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayName,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarGridDays(
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    isLandscape: Boolean
) {
    val colors = AppTheme.colors
    val daysInMonth = displayedYearMonth.lengthOfMonth()
    val firstOfMonth = displayedYearMonth.atDay(1)
    val startDayOfWeek = firstOfMonth.dayOfWeek.value % 7 // 0 for Sunday .. 6 for Saturday

    val rowHeight = if (isLandscape) 32.dp else 38.dp
    val circleSize = if (isLandscape) 28.dp else 34.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        for (r in 0..5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 0..6) {
                    val cellIndex = r * 7 + c
                    val dayNumber = cellIndex - startDayOfWeek + 1
                    if (dayNumber in 1..daysInMonth) {
                        val cellDate = displayedYearMonth.atDay(dayNumber)
                        val isSelectable = cellDate <= today
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(circleSize)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(colors.primary, CircleShape)
                                        } else if (isToday) {
                                            Modifier.border(1.dp, colors.primary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .then(
                                        if (isSelectable) {
                                            Modifier
                                                .clip(CircleShape)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true, radius = circleSize / 2)
                                                ) {
                                                    onDateClick(cellDate)
                                                }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = when {
                                        isSelected -> colors.onPrimary
                                        !isSelectable -> colors.textSecondary.copy(alpha = 0.38f)
                                        isToday -> colors.primary
                                        else -> colors.textPrimary
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearPickerGrid(
    currentYear: Int,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    isLandscape: Boolean
) {
    val colors = AppTheme.colors
    val years = remember(currentYear) { (1970..currentYear).toList().reversed() }
    val initialIndex = remember(selectedYear) {
        (years.indexOf(selectedYear) - 3).coerceAtLeast(0)
    }
    val listState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 215.dp else 260.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(years) { y ->
            val isCurrentSel = y == selectedYear
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isCurrentSel) colors.primary else Color.Transparent)
                    .clickable { onYearSelected(y) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = y.toString(),
                    color = if (isCurrentSel) colors.onPrimary else colors.textPrimary,
                    fontWeight = if (isCurrentSel) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }
    }
}
