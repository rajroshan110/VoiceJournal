package dev.voicejournal.ui.calendar

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.voicejournal.ui.calendar.components.*
import dev.voicejournal.ui.journal.ActiveSheet
import dev.voicejournal.ui.journal.components.FilterBar
import dev.voicejournal.ui.journal.components.MoodFilterBottomSheet
import dev.voicejournal.ui.journal.components.PeopleFilterBottomSheet
import dev.voicejournal.ui.journal.components.TopicFilterBottomSheet
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun CalendarScreen(
    navController: NavController,
    isExpandedLayout: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cardPlaybackState by viewModel.cardPlaybackState.collectAsState()
    val context = LocalContext.current

    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingNavEntryId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(pendingNavEntryId) {
        onDispose {
            viewModel.stopAudioOnLeave(pendingNavEntryId)
        }
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
                    yearMonth = uiState.currentYearMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() },
                    onDatePickerClick = { launchDatePicker() },
                    onTitleClick = {
                        val now = LocalDate.now()
                        viewModel.jumpToDate(now.year, now.monthValue, now.dayOfMonth)
                    }
                )
                FilterBar(
                    selectedTagsCount = uiState.filterState.selectedTags.size,
                    selectedPeopleCount = uiState.filterState.selectedPeople.size,
                    selectedMoodsCount = uiState.filterState.selectedMoods.size,
                    onAllClick = { viewModel.clearAllFilters() },
                    onTagsClick = { activeSheet = ActiveSheet.TAGS },
                    onPeopleClick = { activeSheet = ActiveSheet.PEOPLE },
                    onMoodClick = { activeSheet = ActiveSheet.MOOD }
                )
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
                    // Left Pane: CalendarGrid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AnimatedContent(
                            targetState = uiState.currentYearMonth,
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally { width -> width } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(220)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(220)))
                                }
                            },
                            label = "CalendarMonthTransitionWide"
                        ) { _ ->
                            CalendarGrid(
                                gridDays = uiState.gridDays,
                                startOfWeek = uiState.startOfWeek,
                                onDateSelect = { dayItem -> viewModel.selectDate(dayItem.date) },
                                modifier = Modifier.pointerInput(Unit) {
                                    var hasScrolled = false
                                    detectHorizontalDragGestures(
                                        onDragStart = { hasScrolled = false },
                                        onDragEnd = { hasScrolled = false },
                                        onDragCancel = { hasScrolled = false },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (!hasScrolled) {
                                                if (dragAmount > 30) {
                                                    viewModel.previousMonth()
                                                    hasScrolled = true
                                                } else if (dragAmount < -30) {
                                                    viewModel.nextMonth()
                                                    hasScrolled = true
                                                }
                                            }
                                        }
                                    )
                                }
                            )
                        }
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
                            playbackState = cardPlaybackState,
                            onPlayPauseClick = { entry -> viewModel.playAudio(entry) },
                            onEntryClick = { entryId ->
                                pendingNavEntryId = entryId
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
                    playbackState = cardPlaybackState,
                    onPlayPauseClick = { entry -> viewModel.playAudio(entry) },
                    onEntryClick = { entryId ->
                        pendingNavEntryId = entryId
                        navController.navigate(Screen.NoteDetail.createRoute(entryId))
                    },
                    onClearFiltersClick = { viewModel.clearAllFilters() },
                    timeFormat = uiState.timeFormat,
                    headerContent = {
                        AnimatedContent(
                            targetState = uiState.currentYearMonth,
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally { width -> width } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(220)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(220)))
                                }
                            },
                            label = "CalendarMonthTransition"
                        ) { _ ->
                            CalendarGrid(
                                gridDays = uiState.gridDays,
                                startOfWeek = uiState.startOfWeek,
                                onDateSelect = { dayItem -> viewModel.selectDate(dayItem.date) },
                                modifier = Modifier.pointerInput(Unit) {
                                    var hasScrolled = false
                                    detectHorizontalDragGestures(
                                        onDragStart = { hasScrolled = false },
                                        onDragEnd = { hasScrolled = false },
                                        onDragCancel = { hasScrolled = false },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (!hasScrolled) {
                                                if (dragAmount > 30) {
                                                    viewModel.previousMonth()
                                                    hasScrolled = true
                                                } else if (dragAmount < -30) {
                                                    viewModel.nextMonth()
                                                    hasScrolled = true
                                                }
                                            }
                                        }
                                    )
                                }
                            )
                        }
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
