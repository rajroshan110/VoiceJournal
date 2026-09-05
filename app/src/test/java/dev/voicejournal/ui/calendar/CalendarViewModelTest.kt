package dev.voicejournal.ui.calendar

import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.usecase.GetAllEntriesUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getAllEntriesUseCase: GetAllEntriesUseCase
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAllEntriesUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke() } returns flowOf(emptyList())
        }
        getAllTagsUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke() } returns flowOf(emptyList())
        }
        userPreferencesManager = mockk(relaxed = true) {
            every { startOfWeek } returns flowOf(StartOfWeek.SYSTEM_DEFAULT)
            every { timeFormat } returns flowOf(TimeFormat.SYSTEM_DEFAULT)
            every { isTopicsEnabled } returns flowOf(true)
            every { isPeopleEnabled } returns flowOf(true)
            every { isMoodEnabled } returns flowOf(true)
        }

        viewModel = CalendarViewModel(
            getAllEntriesUseCase = getAllEntriesUseCase,
            getAllTagsUseCase = getAllTagsUseCase,
            userPreferencesManager = userPreferencesManager,
            defaultDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSetYearMonthCannotExceedCurrentMonth() {
        val now = YearMonth.now()
        val futureMonth = now.plusMonths(2)

        viewModel.setYearMonth(futureMonth)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(now, viewModel.uiState.value.currentYearMonth)
    }

    @Test
    fun testSetYearMonthDoesNotAutoShiftSelectedDateAcrossMonths() {
        val now = YearMonth.now()
        val pastMonth = now.minusMonths(2)

        viewModel.setYearMonth(pastMonth)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pastMonth, viewModel.uiState.value.currentYearMonth)
        // In past month with no explicit date clicked, selectedDate is null (no auto-shift)
        org.junit.Assert.assertNull(viewModel.uiState.value.selectedDate)

        // Switching back to current month defaults to today
        viewModel.setYearMonth(now)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(now, viewModel.uiState.value.currentYearMonth)
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testExplicitDateSelectionInPastMonthHighlightsSelectedDate() {
        val pastMonth = YearMonth.now().minusMonths(2)
        val pastDate = pastMonth.atDay(10)

        viewModel.selectDate(pastDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pastMonth, viewModel.uiState.value.currentYearMonth)
        assertEquals(pastDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testSelectDateDisallowsFutureMonth() {
        val now = YearMonth.now()
        val nextMonthDate = now.plusMonths(1).atDay(1)

        viewModel.selectDate(nextMonthDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not have navigated to future month
        assertEquals(now, viewModel.uiState.value.currentYearMonth)
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testSelectDateInPastMonthNavigatesToThatMonth() {
        val pastDate = LocalDate.of(2024, 5, 15)

        viewModel.selectDate(pastDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(YearMonth.of(2024, 5), viewModel.uiState.value.currentYearMonth)
        assertEquals(pastDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testSelectDateInCurrentMonthUpdatesSelectionWithoutChangingMonth() {
        val now = YearMonth.now()
        val targetDate = now.atDay(1)

        viewModel.selectDate(targetDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(now, viewModel.uiState.value.currentYearMonth)
        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
    }
}
