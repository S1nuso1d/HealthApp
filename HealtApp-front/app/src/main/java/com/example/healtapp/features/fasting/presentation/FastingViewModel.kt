package com.example.healtapp.features.fasting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.FastingPlan
import com.example.healtapp.data.preferences.FastingPrefs
import com.example.healtapp.data.preferences.FastingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FastingUiState(
    val fasting: FastingState = FastingState(),
    val nowMs: Long = System.currentTimeMillis(),
    val message: String? = null,
)

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val fastingPrefs: FastingPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FastingUiState())
    val uiState: StateFlow<FastingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fastingPrefs.state.collect { state ->
                _uiState.update { it.copy(fasting = state) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(nowMs = System.currentTimeMillis()) }
            }
        }
    }

    fun selectPlan(plan: FastingPlan) {
        viewModelScope.launch {
            fastingPrefs.setPlan(plan)
            _uiState.update { it.copy(message = "План ${plan.label} выбран") }
        }
    }

    fun startFast() {
        viewModelScope.launch {
            fastingPrefs.startFast()
            _uiState.update { it.copy(message = "Голодание началось. Удачи!") }
        }
    }

    fun stopFast() {
        viewModelScope.launch {
            fastingPrefs.stopFast()
            _uiState.update { it.copy(message = "Голодание завершено") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
