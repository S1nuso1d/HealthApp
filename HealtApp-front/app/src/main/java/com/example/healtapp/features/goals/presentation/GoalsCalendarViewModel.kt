package com.example.healtapp.features.goals.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.api.DashboardApi
import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDayDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsCalendarUiState(
    val isLoading: Boolean = true,
    val days: List<GoalsCalendarDayDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class GoalsCalendarViewModel @Inject constructor(
    private val dashboardApi: DashboardApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsCalendarUiState())
    val uiState: StateFlow<GoalsCalendarUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { dashboardApi.getGoalsCalendar(35) }
                .onSuccess { res ->
                    _uiState.update { it.copy(isLoading = false, days = res.days) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Не удалось загрузить календарь")
                    }
                }
        }
    }
}
