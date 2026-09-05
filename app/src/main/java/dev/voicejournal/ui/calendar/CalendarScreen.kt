package dev.voicejournal.ui.calendar

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import dev.voicejournal.ui.designsystem.theme.AppTheme
import java.time.LocalDate

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


    fun launchDatePicker() {
        val selected = uiState.selectedDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.jumpToDate(year, month + 1, dayOfMonth)
            },
            selected.year,
            selected.monthValue - 1,
            selected.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    val hasActiveFilters = uiState.filterState.selectedTags.isNotEmpty() ||
            uiState.filterState.selectedPeople.isNotEmpty() ||
            uiState.filterState.selectedMoods.isNotEmpty()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || configuration.screenHeightDp < 480

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
                    onDatePickerClick = { launchDatePicker() },
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
                availableTags = uiState.availableTags,
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
