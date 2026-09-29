package dev.voicejournal.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import dev.voicejournal.ui.calendar.components.*
import dev.voicejournal.ui.journal.ActiveSheet
import dev.voicejournal.ui.journal.components.FilterBar
import dev.voicejournal.ui.journal.components.MoodFilterBottomSheet
import dev.voicejournal.ui.journal.components.PeopleFilterBottomSheet
import dev.voicejournal.ui.journal.components.TopicFilterBottomSheet
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    navController: NavController,
    isExpandedLayout: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        dev.voicejournal.ui.designsystem.motion.NavigationMotion.isReducedMotion(context)
    }

    var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val initialPage = remember {
        CalendarUtils.yearMonthToPage(uiState.currentYearMonth)
            .coerceIn(0, CalendarUtils.getMaxPage())
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { CalendarUtils.getPageCount() }
    )

    // Sync ViewModel when pager settles on a new month page
    LaunchedEffect(pagerState.settledPage) {
        val settledMonth = CalendarUtils.pageToYearMonth(pagerState.settledPage)
        if (settledMonth != uiState.currentYearMonth) {
            viewModel.setYearMonth(settledMonth)
        }
    }

    // Sync pager when ViewModel currentYearMonth changes externally
    LaunchedEffect(uiState.currentYearMonth) {
        val targetPage = CalendarUtils.yearMonthToPage(uiState.currentYearMonth)
            .coerceIn(0, CalendarUtils.getMaxPage())
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            val diff = kotlin.math.abs(pagerState.currentPage - targetPage)
            if (isReducedMotion || diff > 1) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val displayYearMonth = remember(pagerState.currentPage) {
        CalendarUtils.pageToYearMonth(pagerState.currentPage)
    }


    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || configuration.screenHeightDp < 480

    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    if (isDatePickerVisible) {
        val selected = uiState.selectedDate ?: LocalDate.now()
        CalendarDatePickerDialog(
            initialDate = selected,
            onDateSelected = { year, month, day ->
                viewModel.jumpToDate(year, month, day)
                isDatePickerVisible = false
            },
            onDismiss = { isDatePickerVisible = false }
        )
    }

    val hasActiveFilters = uiState.filterState.selectedTags.isNotEmpty() ||
            uiState.filterState.selectedPeople.isNotEmpty() ||
            uiState.filterState.selectedMoods.isNotEmpty()

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNavBar(navController = navController) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.background)
            ) {
                MonthHeader(
                    yearMonth = displayYearMonth,
                    onPrevious = {
                        val targetPage = (pagerState.targetPage - 1).coerceAtLeast(0)
                        if (targetPage != pagerState.targetPage) {
                            coroutineScope.launch {
                                if (isReducedMotion) {
                                    pagerState.scrollToPage(targetPage)
                                } else {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        }
                    },
                    onNext = {
                        val maxPage = CalendarUtils.getMaxPage()
                        val targetPage = (pagerState.targetPage + 1).coerceAtMost(maxPage)
                        if (targetPage != pagerState.targetPage) {
                            coroutineScope.launch {
                                if (isReducedMotion) {
                                    pagerState.scrollToPage(targetPage)
                                } else {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        }
                    },
                    onDatePickerClick = { isDatePickerVisible = true },
                    onTitleClick = {
                        val now = LocalDate.now()
                        viewModel.jumpToDate(now.year, now.monthValue, now.dayOfMonth)
                    },
                    filterContent = if (isCompactLandscape && (uiState.isTopicsEnabled || uiState.isPeopleEnabled || uiState.isMoodEnabled)) {
                        {
                            FilterBar(
                                selectedTagsCount = uiState.filterState.selectedTags.size,
                                selectedPeopleCount = uiState.filterState.selectedPeople.size,
                                selectedMoodsCount = uiState.filterState.selectedMoods.size,
                                isTopicsEnabled = uiState.isTopicsEnabled,
                                isPeopleEnabled = uiState.isPeopleEnabled,
                                isMoodEnabled = uiState.isMoodEnabled,
                                onAllClick = { viewModel.clearAllFilters() },
                                onTagsClick = { activeSheet = ActiveSheet.TAGS },
                                onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                                onMoodClick = { activeSheet = ActiveSheet.MOOD }
                            )
                        }
                    } else null
                )
                if (!isCompactLandscape && (uiState.isTopicsEnabled || uiState.isPeopleEnabled || uiState.isMoodEnabled)) {
                    FilterBar(
                        selectedTagsCount = uiState.filterState.selectedTags.size,
                        selectedPeopleCount = uiState.filterState.selectedPeople.size,
                        selectedMoodsCount = uiState.filterState.selectedMoods.size,
                        isTopicsEnabled = uiState.isTopicsEnabled,
                        isPeopleEnabled = uiState.isPeopleEnabled,
                        isMoodEnabled = uiState.isMoodEnabled,
                        onAllClick = { viewModel.clearAllFilters() },
                        onTagsClick = { activeSheet = ActiveSheet.TAGS },
                        onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                        onMoodClick = { activeSheet = ActiveSheet.MOOD }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.background)
        ) {
            if (isExpandedLayout) {
                // Expanded / Medium Layout (Row - 50/50 Split)
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left Pane: CalendarMonthPager
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CalendarMonthPager(
                            pagerState = pagerState,
                            selectedDate = uiState.selectedDate,
                            currentYearMonth = uiState.currentYearMonth,
                            currentGridDays = uiState.gridDays,
                            entriesByDate = uiState.entriesByDate,
                            startOfWeek = uiState.startOfWeek,
                            onDateSelect = { dayItem -> viewModel.selectDate(dayItem.date) },
                            isCompact = isCompactLandscape
                        )
                    }

                    VerticalDivider(color = AppTheme.colors.divider)

                    // Right Pane: DayEntryList
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        DayEntryList(
                            selectedDate = uiState.selectedDate,
                            entries = uiState.selectedDateEntries,
                            hasActiveFilters = hasActiveFilters,
                            isLoading = uiState.isLoading,
                            onEntryClick = { entryId ->
                                navController.navigate(Screen.NoteDetail.createRoute(entryId))
                            },
                            onClearFiltersClick = { viewModel.clearAllFilters() },
                            timeFormat = uiState.timeFormat
                        )
                    }
                }
            } else {
                DayEntryList(
                    selectedDate = uiState.selectedDate,
                    entries = uiState.selectedDateEntries,
                    hasActiveFilters = hasActiveFilters,
                    isLoading = uiState.isLoading,
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.NoteDetail.createRoute(entryId))
                    },
                    onClearFiltersClick = { viewModel.clearAllFilters() },
                    timeFormat = uiState.timeFormat,
                    headerContent = {
                        CalendarMonthPager(
                            pagerState = pagerState,
                            selectedDate = uiState.selectedDate,
                            currentYearMonth = uiState.currentYearMonth,
                            currentGridDays = uiState.gridDays,
                            entriesByDate = uiState.entriesByDate,
                            startOfWeek = uiState.startOfWeek,
                            onDateSelect = { dayItem -> viewModel.selectDate(dayItem.date) },
                            isCompact = isCompactLandscape
                        )
                    }
                )
            }
        }
    }

    // Modal Bottom Sheets for Filter Controls
    when (activeSheet) {
        ActiveSheet.TAGS -> {
            TopicFilterBottomSheet(
                availableTags = uiState.availableTags.filter { it.type == TagType.TOPIC },
                selectedTagNames = uiState.filterState.selectedTags,
                onTagToggle = { tagName -> viewModel.toggleTagFilter(tagName) },
                onClearAll = { viewModel.clearAllFilters() },
                onDismissRequest = { activeSheet = ActiveSheet.NONE }
            )
        }
        ActiveSheet.PEOPLE -> {
            PeopleFilterBottomSheet(
                availablePeople = uiState.availablePeople,
                selectedPeople = uiState.filterState.selectedPeople,
                onPersonToggle = { person -> viewModel.togglePersonFilter(person) },
                onClearAll = { viewModel.clearAllFilters() },
                onDismissRequest = { activeSheet = ActiveSheet.NONE }
            )
        }
        ActiveSheet.MOOD -> {
            MoodFilterBottomSheet(
                selectedMoods = uiState.filterState.selectedMoods,
                onMoodToggle = { mood -> viewModel.toggleMoodFilter(mood) },
                onClearAll = { viewModel.clearAllFilters() },
                onDismissRequest = { activeSheet = ActiveSheet.NONE }
            )
        }
        ActiveSheet.NONE -> {}
    }
}

@Composable
private fun CalendarDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    var displayedYear by rememberSaveable { mutableIntStateOf(initialDate.year) }
    var displayedMonth by rememberSaveable { mutableIntStateOf(initialDate.monthValue) }
    var isYearPickerVisible by rememberSaveable { mutableStateOf(false) }

    val selectedDate = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }
    val displayedYearMonth = remember(displayedYear, displayedMonth) { YearMonth.of(displayedYear, displayedMonth) }

    val onConfirm = {
        onDateSelected(
            selectedDate.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = if (isLandscape) {
                Modifier
                    .width(520.dp)
                    .height(316.dp)
                    .padding(vertical = 4.dp)
            } else {
                Modifier
                    .width(328.dp)
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            }
        ) {
            if (isLandscape) {
                // Landscape: Date/Day on LEFT, Calendar on RIGHT simultaneously
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left Column: Header (Year + Day/Date)
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight()
                            .background(
                                colors.surfaceVariant,
                                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = selectedDate.year.toString(),
                                color = if (isYearPickerVisible) colors.primary else colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { isYearPickerVisible = true }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE,\nd MMM")),
                                color = if (!isYearPickerVisible) colors.textPrimary else colors.textSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp,
                                modifier = Modifier.clickable { isYearPickerVisible = false }
                            )
                        }
                    }

                    // Right Column: Calendar Body + Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                colors.surface,
                                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isYearPickerVisible) {
                            YearPickerContent(
                                currentYear = today.year,
                                selectedYear = selectedDate.year,
                                onYearSelected = { y ->
                                    val newYearMonth = YearMonth.of(y, displayedMonth)
                                    val adjustedDay = selectedDate.dayOfMonth.coerceAtMost(newYearMonth.lengthOfMonth())
                                    val candidate = LocalDate.of(y, displayedMonth, adjustedDay)
                                    val finalDate = if (candidate > today) today else candidate
                                    selectedEpochDay = finalDate.toEpochDay()
                                    displayedYear = finalDate.year
                                    displayedMonth = finalDate.monthValue
                                    isYearPickerVisible = false
                                },
                                isLandscape = true
                            )
                        } else {
                            Column {
                                MonthNavigationHeader(
                                    displayedYearMonth = displayedYearMonth,
                                    canGoNext = displayedYearMonth.plusMonths(1).atDay(1) <= today,
                                    onPrevious = {
                                        val prev = displayedYearMonth.minusMonths(1)
                                        displayedYear = prev.year
                                        displayedMonth = prev.monthValue
                                    },
                                    onNext = {
                                        val next = displayedYearMonth.plusMonths(1)
                                        displayedYear = next.year
                                        displayedMonth = next.monthValue
                                    },
                                    onTitleClick = { isYearPickerVisible = true }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                WeekdayHeaderRow()
                                Spacer(modifier = Modifier.height(2.dp))
                                CalendarGridDays(
                                    displayedYearMonth = displayedYearMonth,
                                    selectedDate = selectedDate,
                                    today = today,
                                    onDateClick = { date -> selectedEpochDay = date.toEpochDay() },
                                    isLandscape = true
                                )
                            }
                        }

                        CalendarActionButtons(
                            onDismiss = onDismiss,
                            onConfirm = onConfirm
                        )
                    }
                }
            } else {
                // Portrait: Header on TOP, Calendar in middle, CANCEL / OK at bottom
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colors.surfaceVariant,
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedDate.year.toString(),
                                color = if (isYearPickerVisible) colors.primary else colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { isYearPickerVisible = true }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                                color = if (!isYearPickerVisible) colors.textPrimary else colors.textSecondary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isYearPickerVisible = false }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colors.surface,
                                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (isYearPickerVisible) {
                            YearPickerContent(
                                currentYear = today.year,
                                selectedYear = selectedDate.year,
                                onYearSelected = { y ->
                                    val newYearMonth = YearMonth.of(y, displayedMonth)
                                    val adjustedDay = selectedDate.dayOfMonth.coerceAtMost(newYearMonth.lengthOfMonth())
                                    val candidate = LocalDate.of(y, displayedMonth, adjustedDay)
                                    val finalDate = if (candidate > today) today else candidate
                                    selectedEpochDay = finalDate.toEpochDay()
                                    displayedYear = finalDate.year
                                    displayedMonth = finalDate.monthValue
                                    isYearPickerVisible = false
                                },
                                isLandscape = false
                            )
                        } else {
                            MonthNavigationHeader(
                                displayedYearMonth = displayedYearMonth,
                                canGoNext = displayedYearMonth.plusMonths(1).atDay(1) <= today,
                                onPrevious = {
                                    val prev = displayedYearMonth.minusMonths(1)
                                    displayedYear = prev.year
                                    displayedMonth = prev.monthValue
                                },
                                onNext = {
                                    val next = displayedYearMonth.plusMonths(1)
                                    displayedYear = next.year
                                    displayedMonth = next.monthValue
                                },
                                onTitleClick = { isYearPickerVisible = true }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            WeekdayHeaderRow()
                            Spacer(modifier = Modifier.height(4.dp))
                            CalendarGridDays(
                                displayedYearMonth = displayedYearMonth,
                                selectedDate = selectedDate,
                                today = today,
                                onDateClick = { date -> selectedEpochDay = date.toEpochDay() },
                                isLandscape = false
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        CalendarActionButtons(
                            onDismiss = onDismiss,
                            onConfirm = onConfirm
                        )
                    }
                }
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

    val rowHeight = if (isLandscape) 33.dp else 38.dp
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
private fun YearPickerContent(
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
            .height(if (isLandscape) 220.dp else 260.dp),
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

@Composable
private fun CalendarActionButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onDismiss,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "CANCEL",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = onConfirm,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "OK",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
