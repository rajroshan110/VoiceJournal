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
    val selectedDate: LocalDate = LocalDate.now(),
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
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    @Inject
    constructor(
        getAllEntriesUseCase: GetAllEntriesUseCase,
        getAllTagsUseCase: GetAllTagsUseCase,
        userPreferencesManager: UserPreferencesManager
    ) : this(getAllEntriesUseCase, getAllTagsUseCase, userPreferencesManager, Dispatchers.Default)

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
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
                val personTagNames = rawTags.filter { it.type == TagType.PERSON }.map { it.name.removePrefix("@") }
                val entryPersonTags = rawEntries.flatMap { entry ->
                    entry.tags.filter { it.type == TagType.PERSON }.map { it.name.removePrefix("@") }
                }
                val textMentions = rawEntries.flatMap { entry ->
                    val text = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
                    Regex("@\\w+").findAll(text).map { it.value.removePrefix("@") }.toList()
                }
                val extractedPeople = (personTagNames + entryPersonTags + textMentions)
                    .distinct()
                    .filter { it.isNotBlank() }
                    .sorted()

                val extractedHashtags = rawEntries.flatMap { entry ->
                    val text = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"
                    Regex("#\\w+").findAll(text).map { it.value.removePrefix("#") }.toList()
                }.distinct().filter { it.isNotBlank() }

                val topicTagNames = rawTags.filter { it.type == TagType.TOPIC }.map { it.name.removePrefix("#") }
                val allTagNames = (topicTagNames + extractedHashtags).distinct()
                val allTags = allTagNames.mapIndexed { i, name -> Tag(i.toLong(), name) }
                
                Triple(rawEntries, allTags, extractedPeople)
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
                            entry.tags.any { (it.type == TagType.TOPIC || it.type == TagType.THING) && it.name.removePrefix("#") in filters.selectedTags } ||
                            filters.selectedTags.any { tag -> textContent.contains("#$tag", ignoreCase = true) }

                    val matchesPeople = !prefs.isPeopleEnabled || filters.selectedPeople.isEmpty() ||
                            entry.tags.any { it.type == TagType.PERSON && (it.name.removePrefix("@") in filters.selectedPeople || it.name in filters.selectedPeople) } ||
                            entry.people.any { it.removePrefix("@") in filters.selectedPeople } ||
                            filters.selectedPeople.any { person ->
                                textContent.contains("@$person", ignoreCase = true)
                            }

                    val matchesMoods = !prefs.isMoodEnabled || filters.selectedMoods.isEmpty() || (entry.mood in filters.selectedMoods)

                    matchesTags && matchesPeople && matchesMoods
                }

                val entriesByLocalDate: Map<LocalDate, List<JournalEntry>> = filteredEntries.groupBy { entry ->
                    java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }

                val gridDays = CalendarUtils.buildGridDays(
                    yearMonth = ym,
                    selectedDate = selDate,
                    startOfWeek = startOfWeekSetting,
                    entriesByDate = entriesByLocalDate
                )

                val selectedEntries = entriesByLocalDate[selDate] ?: emptyList()

                CalendarUiState(
                    isLoading = false,
                    currentYearMonth = ym,
                    selectedDate = selDate,
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
            val currentSel = _selectedDate.value
            if (currentSel.year != yearMonth.year || currentSel.monthValue != yearMonth.monthValue) {
                val newDate = if (yearMonth == todayYM) {
                    LocalDate.now()
                } else {
                    val day = currentSel.dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
                    yearMonth.atDay(day)
                }
                _selectedDate.value = newDate
            }
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
        val cleanTag = tagName.removePrefix("#")
        val current = _filterState.value.selectedTags
        val next = if (cleanTag in current) current - cleanTag else current + cleanTag
        _filterState.value = _filterState.value.copy(selectedTags = next)
    }

    fun togglePersonFilter(person: String) {
        val cleanPerson = person.removePrefix("@")
        val current = _filterState.value.selectedPeople
        val next = if (cleanPerson in current) current - cleanPerson else current + cleanPerson
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
