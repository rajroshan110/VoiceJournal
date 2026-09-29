package dev.voicejournal.ui.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.usecase.ExtractUnifiedTagsUseCase
import dev.voicejournal.domain.usecase.GetAllEntriesUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import dev.voicejournal.ui.journal.FilterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@Stable
data class CalendarDayItem(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isFuture: Boolean,
    val entries: List<JournalEntry>,
    val categories: List<String>
)

data class CalendarUiState(
    val isLoading: Boolean = false,
    val currentYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val gridDays: List<CalendarDayItem> = emptyList(),
    val entriesByDate: Map<LocalDate, List<JournalEntry>> = emptyMap(),
    val selectedDateEntries: List<JournalEntry> = emptyList(),
    val monthEntries: List<JournalEntry> = emptyList(),
    val filterState: FilterState = FilterState(),
    val availableTags: List<Tag> = emptyList(),
    val availablePeople: List<String> = emptyList(),
    val startOfWeek: StartOfWeek = StartOfWeek.SYSTEM_DEFAULT,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true,
    val errorMessage: String? = null
)

private data class CalendarPrefConfig(
    val startOfWeek: StartOfWeek = StartOfWeek.SYSTEM_DEFAULT,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true
)

@HiltViewModel
class CalendarViewModel internal constructor(
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val userPreferencesManager: UserPreferencesManager,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
    private val extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase = ExtractUnifiedTagsUseCase()
) : ViewModel() {

    @Inject
    constructor(
        getAllEntriesUseCase: GetAllEntriesUseCase,
        getAllTagsUseCase: GetAllTagsUseCase,
        userPreferencesManager: UserPreferencesManager,
        extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase
    ) : this(getAllEntriesUseCase, getAllTagsUseCase, userPreferencesManager, Dispatchers.Default, extractUnifiedTagsUseCase)

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _filterState = MutableStateFlow(FilterState())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val rawEntriesFlow = getAllEntriesUseCase().catch { e -> _errorMessage.value = e.message ?: "Failed to read database" }
            val rawTagsFlow = getAllTagsUseCase().catch { }

            val extractedDataFlow = combine(rawEntriesFlow, rawTagsFlow) { rawEntries, rawTags ->
                val extracted = extractUnifiedTagsUseCase(rawEntries, rawTags)
                Triple(rawEntries, extracted.allTags, extracted.allPeople)
            }.distinctUntilChanged().flowOn(defaultDispatcher)

            val preferencesFlow = combine(
                combine(
                    userPreferencesManager.startOfWeek,
                    userPreferencesManager.timeFormat
                ) { sow, tf -> sow to tf },
                combine(
                    userPreferencesManager.isTopicsEnabled,
                    userPreferencesManager.isPeopleEnabled,
                    userPreferencesManager.isMoodEnabled
                ) { te, pe, me -> Triple(te, pe, me) }
            ) { p1, p2 ->
                CalendarPrefConfig(
                    startOfWeek = p1.first,
                    timeFormat = p1.second,
                    isTopicsEnabled = p2.first,
                    isPeopleEnabled = p2.second,
                    isMoodEnabled = p2.third
                )
            }

            val filteredDataFlow = combine(
                extractedDataFlow,
                _filterState,
                _yearMonth,
                _selectedDate,
                preferencesFlow
            ) { data, filters, ym, selDate, prefs ->
                val startOfWeekSetting = prefs.startOfWeek
                val timeFormatPref = prefs.timeFormat
                val rawEntries = data.first
                val allTags = data.second
                val extractedPeople = data.third

                val filteredEntries = rawEntries.filter { entry ->
                    val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"

                    val matchesTags = !prefs.isTopicsEnabled || filters.selectedTags.isEmpty() ||
                            entry.tags.any { tag ->
                                tag.type == TagType.TOPIC &&
                                filters.selectedTags.any { it.equals(tag.name.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            }

                    val matchesPeople = !prefs.isPeopleEnabled || filters.selectedPeople.isEmpty() ||
                            entry.tags.any { tag ->
                                tag.type == TagType.PERSON &&
                                filters.selectedPeople.any { it.equals(tag.name.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            } ||
                            entry.people.any { p ->
                                filters.selectedPeople.any { it.equals(p.trim().trimStart('#', '@').trim(), ignoreCase = true) }
                            }

                    val matchesMoods = !prefs.isMoodEnabled || filters.selectedMoods.isEmpty() || (entry.mood in filters.selectedMoods)

                    matchesTags && matchesPeople && matchesMoods
                }

                val entriesByLocalDate: Map<LocalDate, List<JournalEntry>> = filteredEntries.groupBy { entry ->
                    java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }

                val todayYM = YearMonth.now()
                val isCurrentMonth = ym == todayYM
                val hasExplicitSelectionInMonth = selDate != null && selDate.year == ym.year && selDate.month == ym.month
                val effectiveSelectedDate = when {
                    hasExplicitSelectionInMonth -> selDate
                    isCurrentMonth -> LocalDate.now()
                    else -> null
                }

                val gridDays = CalendarUtils.buildGridDays(
                    yearMonth = ym,
                    selectedDate = effectiveSelectedDate,
                    startOfWeek = startOfWeekSetting,
                    entriesByDate = entriesByLocalDate
                )

                val selectedEntries = if (effectiveSelectedDate != null) {
                    entriesByLocalDate[effectiveSelectedDate] ?: emptyList()
                } else {
                    emptyList()
                }

                CalendarUiState(
                    isLoading = false,
                    currentYearMonth = ym,
                    selectedDate = effectiveSelectedDate,
                    gridDays = gridDays,
                    entriesByDate = entriesByLocalDate,
                    selectedDateEntries = selectedEntries,
                    monthEntries = filteredEntries.filter { entry ->
                        val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        entryDate.month == ym.month && entryDate.year == ym.year
                    },
                    filterState = filters,
                    availableTags = allTags,
                    availablePeople = extractedPeople,
                    startOfWeek = startOfWeekSetting,
                    timeFormat = timeFormatPref,
                    isTopicsEnabled = prefs.isTopicsEnabled,
                    isPeopleEnabled = prefs.isPeopleEnabled,
                    isMoodEnabled = prefs.isMoodEnabled,
                    errorMessage = null
                )
            }.distinctUntilChanged().flowOn(defaultDispatcher)

            combine(
                filteredDataFlow,
                _isLoading,
                _errorMessage
            ) { baseState, loading, err ->
                baseState.copy(
                    isLoading = loading,
                    errorMessage = err
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setYearMonth(yearMonth: YearMonth) {
        val todayYM = YearMonth.now()
        if (!yearMonth.isAfter(todayYM)) {
            _yearMonth.value = yearMonth
        }
    }

    fun nextMonth() {
        setYearMonth(_yearMonth.value.plusMonths(1))
    }

    fun previousMonth() {
        setYearMonth(_yearMonth.value.minusMonths(1))
    }

    fun selectDate(date: LocalDate) {
        val todayYM = YearMonth.now()
        val targetYM = YearMonth.of(date.year, date.month)
        if (targetYM.isAfter(todayYM)) {
            return
        }
        _selectedDate.value = date
        if (targetYM != _yearMonth.value) {
            setYearMonth(targetYM)
        }
    }

    fun jumpToDate(year: Int, month: Int, dayOfMonth: Int) {
        val date = LocalDate.of(year, month, dayOfMonth)
        selectDate(date)
    }


    fun toggleTagFilter(tagName: String) {
        val cleanTag = tagName.trim().trimStart('#', '@').trim()
        val current = _filterState.value.selectedTags
        val existing = current.firstOrNull { it.equals(cleanTag, ignoreCase = true) }
        val next = if (existing != null) {
            current.filterNot { it.equals(cleanTag, ignoreCase = true) }.toSet()
        } else {
            current + cleanTag
        }
        _filterState.value = _filterState.value.copy(selectedTags = next)
    }

    fun togglePersonFilter(person: String) {
        val cleanPerson = person.trim().trimStart('#', '@').trim()
        val current = _filterState.value.selectedPeople
        val existing = current.firstOrNull { it.equals(cleanPerson, ignoreCase = true) }
        val next = if (existing != null) {
            current.filterNot { it.equals(cleanPerson, ignoreCase = true) }.toSet()
        } else {
            current + cleanPerson
        }
        _filterState.value = _filterState.value.copy(selectedPeople = next)
    }

    fun toggleMoodFilter(mood: String) {
        val current = _filterState.value.selectedMoods
        val next = if (mood in current) current - mood else current + mood
        _filterState.value = _filterState.value.copy(selectedMoods = next)
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
    }

    fun retryLoad() {
        _errorMessage.value = null
    }
}
