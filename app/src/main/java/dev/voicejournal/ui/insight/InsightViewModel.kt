package dev.voicejournal.ui.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.usecase.GetInsightsSummaryUseCase
import dev.voicejournal.domain.usecase.InsightsSummary
import dev.voicejournal.domain.usecase.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InsightsContentState {
    object Loading : InsightsContentState()
    data class Success(val summary: InsightsSummary) : InsightsContentState()
    data class Error(val message: String) : InsightsContentState()
}

data class InsightUiState(
    val period: TimePeriod = TimePeriod.WEEK,
    val dateRangeMode: InsightDateRangeMode = InsightDateRangeMode.LAST_DAYS,
    val isTopicsEnabled: Boolean = true,
    val isPeopleEnabled: Boolean = true,
    val isMoodEnabled: Boolean = true,
    val contentState: InsightsContentState = InsightsContentState.Loading
)

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val getInsightsSummaryUseCase: GetInsightsSummaryUseCase,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightUiState())
    val uiState: StateFlow<InsightUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferencesManager.insightDateRangeMode.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(dateRangeMode = mode)
                loadData(_uiState.value.period, mode)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isTopicsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isTopicsEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isPeopleEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isPeopleEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesManager.isMoodEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isMoodEnabled = enabled)
            }
        }
    }

    fun setPeriod(period: TimePeriod) {
        if (_uiState.value.period != period) {
            _uiState.value = _uiState.value.copy(period = period)
            loadData(period, _uiState.value.dateRangeMode)
        }
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(contentState = InsightsContentState.Loading)
        loadData(_uiState.value.period, _uiState.value.dateRangeMode)
    }

    private fun loadData(period: TimePeriod, dateRangeMode: InsightDateRangeMode) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getInsightsSummaryUseCase(period, dateRangeMode)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        contentState = InsightsContentState.Error(e.message ?: "Failed to load insights data")
                    )
                }
                .collect { summary ->
                    _uiState.value = _uiState.value.copy(
                        contentState = InsightsContentState.Success(summary)
                    )
                }
        }
    }
}
