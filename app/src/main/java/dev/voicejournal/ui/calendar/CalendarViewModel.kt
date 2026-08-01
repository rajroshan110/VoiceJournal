package dev.voicejournal.ui.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.PlayerState
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.usecase.GetAllEntriesUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import dev.voicejournal.ui.journal.CardPlaybackState
import dev.voicejournal.ui.journal.FilterState
import dev.voicejournal.ui.journal.PlaybackStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
    val selectedDateEntries: List<JournalEntry> = emptyList(),
    val monthEntries: List<JournalEntry> = emptyList(),
    val filterState: FilterState = FilterState(),
    val availableTags: List<Tag> = emptyList(),
    val availablePeople: List<String> = emptyList(),
    val startOfWeek: StartOfWeek = StartOfWeek.SYSTEM_DEFAULT,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    val errorMessage: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _filterState = MutableStateFlow(FilterState())
    private val _cardPlaybackState = MutableStateFlow(CardPlaybackState())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val cardPlaybackState = _cardPlaybackState.asStateFlow()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private fun findTrackId(entryId: Long?, audioPath: String?): String? {
        if (audioPath.isNullOrBlank()) return null
        val targetEntry = _uiState.value.selectedDateEntries.firstOrNull { it.id == entryId }
            ?: _uiState.value.monthEntries.firstOrNull { it.id == entryId }
        return targetEntry?.allAudioTracks?.firstOrNull { it.path == audioPath || it.id == audioPath }?.id
            ?: targetEntry?.allAudioTracks?.firstOrNull()?.id
    }

    init {
        // Observe player state for audio playback in Calendar tab
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                val current = _cardPlaybackState.value
                when (state) {
                    is PlayerState.Playing -> {
                        val entryId = state.entryId ?: current.activeEntryId
                        val trackId = findTrackId(entryId, state.audioPath) ?: current.activeTrackId
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = entryId,
                            activeTrackId = trackId,
                            status = PlaybackStatus.Playing,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Paused -> {
                        val entryId = state.entryId ?: current.activeEntryId
                        val trackId = findTrackId(entryId, state.audioPath) ?: current.activeTrackId
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = entryId,
                            activeTrackId = trackId,
                            status = PlaybackStatus.Paused,
                            currentPositionMs = state.currentPosition
                        )
                    }
                    is PlayerState.Ended -> {
                        _cardPlaybackState.value = current.copy(
                            activeEntryId = null,
                            activeTrackId = null,
                            status = PlaybackStatus.Idle,
                            currentPositionMs = 0L
                        )
                    }
                    is PlayerState.Idle -> {
                        if (current.status != PlaybackStatus.Error) {
                            _cardPlaybackState.value = current.copy(
                                activeEntryId = null,
                                activeTrackId = null,
                                status = PlaybackStatus.Idle,
                                currentPositionMs = 0L
                            )
                        }
                    }
                    is PlayerState.Error -> {
                        _cardPlaybackState.value = current.copy(
                            status = PlaybackStatus.Error,
                            currentPositionMs = 0L
                        )
                        _errorMessage.value = state.message
                    }
                }
            }
        }

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
            }.distinctUntilChanged().flowOn(Dispatchers.Default)

            val preferencesFlow = combine(userPreferencesManager.startOfWeek, userPreferencesManager.timeFormat) { sow, tf ->
                sow to tf
            }

            val filteredDataFlow = combine(
                extractedDataFlow,
                _filterState,
                _yearMonth,
                _selectedDate,
                preferencesFlow
            ) { data, filters, ym, selDate, prefs ->
                val startOfWeekSetting = prefs.first
                val timeFormatPref = prefs.second
                val rawEntries = data.first
                val allTags = data.second
                val extractedPeople = data.third

                val filteredEntries = rawEntries.filter { entry ->
                    val textContent = "${entry.title ?: ""} ${entry.userText ?: ""} ${entry.transcript ?: ""}"

                    val matchesTags = filters.selectedTags.isEmpty() ||
                            entry.tags.any { (it.type == TagType.TOPIC || it.type == TagType.THING) && it.name.removePrefix("#") in filters.selectedTags } ||
                            filters.selectedTags.any { tag -> textContent.contains("#$tag", ignoreCase = true) }

                    val matchesPeople = filters.selectedPeople.isEmpty() ||
                            entry.tags.any { it.type == TagType.PERSON && (it.name.removePrefix("@") in filters.selectedPeople || it.name in filters.selectedPeople) } ||
                            entry.people.any { it.removePrefix("@") in filters.selectedPeople } ||
                            filters.selectedPeople.any { person ->
                                textContent.contains("@$person", ignoreCase = true)
                            }

                    val matchesMoods = filters.selectedMoods.isEmpty() || (entry.mood in filters.selectedMoods)

                    matchesTags && matchesPeople && matchesMoods
                }

                val entriesByLocalDate: Map<LocalDate, List<JournalEntry>> = filteredEntries.groupBy { entry ->
                    java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }

                val today = LocalDate.now()
                val firstDayOfWeek = when (startOfWeekSetting) {
                    StartOfWeek.SYSTEM_DEFAULT -> WeekFields.of(Locale.getDefault()).firstDayOfWeek
                    StartOfWeek.MONDAY -> DayOfWeek.MONDAY
                    StartOfWeek.SUNDAY -> DayOfWeek.SUNDAY
                }
                val firstOfMonth = ym.atDay(1)

                var startLocalDate = firstOfMonth
                while (startLocalDate.dayOfWeek != firstDayOfWeek) {
                    startLocalDate = startLocalDate.minusDays(1)
                }

                val gridDays = (0 until 42).map { dayIndex ->
                    val date = startLocalDate.plusDays(dayIndex.toLong())
                    val dayEntries = entriesByLocalDate[date] ?: emptyList()
                    val categories = dayEntries.flatMap { entry ->
                        val list = mutableListOf<String>()
                        if (entry.tags.isNotEmpty()) list.add("Work")
                        if (entry.people.isNotEmpty()) list.add("Personal")
                        if (entry.moodEmoji != null) list.add("Mood")
                        list
                    }.distinct()

                    CalendarDayItem(
                        date = date,
                        isCurrentMonth = date.month == ym.month && date.year == ym.year,
                        isToday = date == today,
                        isSelected = date == selDate,
                        isFuture = date.isAfter(today),
                        entries = dayEntries,
                        categories = categories
                    )
                }

                val selectedEntries = entriesByLocalDate[selDate] ?: emptyList()

                CalendarUiState(
                    isLoading = false,
                    currentYearMonth = ym,
                    selectedDate = selDate,
                    gridDays = gridDays,
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
                    errorMessage = null
                )
            }.distinctUntilChanged().flowOn(Dispatchers.Default)

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

    fun nextMonth() {
        val current = _yearMonth.value
        val todayYM = YearMonth.now()
        if (current.isBefore(todayYM)) {
            _yearMonth.value = current.plusMonths(1)
        }
    }

    fun previousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (date.month != _yearMonth.value.month || date.year != _yearMonth.value.year) {
            _yearMonth.value = YearMonth.of(date.year, date.month)
        }
    }

    fun jumpToDate(year: Int, month: Int, dayOfMonth: Int) {
        val date = LocalDate.of(year, month, dayOfMonth)
        selectDate(date)
    }

    fun playAudio(entry: JournalEntry) {
        val currentPlayback = _cardPlaybackState.value

        if (currentPlayback.activeEntryId == entry.id) {
            if (currentPlayback.status == PlaybackStatus.Playing) {
                audioPlayerManager.pause()
            } else if (currentPlayback.status == PlaybackStatus.Paused) {
                audioPlayerManager.resume()
            } else {
                startPlaybackFor(entry)
            }
        } else {
            audioPlayerManager.stop()
            startPlaybackFor(entry)
        }
    }

    private fun startPlaybackFor(entry: JournalEntry) {
        val path = entry.audioUri
        if (path.isNullOrEmpty() || !File(path).exists()) {
            _cardPlaybackState.value = CardPlaybackState(
                activeEntryId = entry.id,
                status = PlaybackStatus.Error,
                errorMessage = "Audio file unavailable"
            )
            return
        }

        _cardPlaybackState.value = CardPlaybackState(
            activeEntryId = entry.id,
            status = PlaybackStatus.Buffering,
            currentPositionMs = 0L
        )

        audioPlayerManager.play(path, entry.title ?: "Voice Note Playback", entry.id)
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

    fun stopAudioOnLeave(navigatingToEntryId: Long? = null) {
        val activeId = _cardPlaybackState.value.activeEntryId
        if (navigatingToEntryId == null || activeId == null || navigatingToEntryId != activeId) {
            audioPlayerManager.stop()
            _cardPlaybackState.value = CardPlaybackState()
        }
    }
}
