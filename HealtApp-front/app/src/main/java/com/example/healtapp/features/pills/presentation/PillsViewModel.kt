package com.example.healtapp.features.pills.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.health.PillDto
import com.example.healtapp.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PillsUiState(
    val isLoading: Boolean = false,
    val pills: List<PillDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PillsViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PillsUiState())
    val uiState: StateFlow<PillsUiState> = _uiState.asStateFlow()

    init {
        loadPills()
    }

    fun loadPills() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            healthRepository.getPillReminders()
                .onSuccess { pills ->
                    _uiState.update { it.copy(isLoading = false, pills = pills) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun addPill(name: String, dosage: String, timeOfDay: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pill = PillDto(name = name, dosage = dosage, timeOfDay = timeOfDay)
            healthRepository.createPillReminder(pill)
                .onSuccess { newPill ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills + newPill
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun updatePill(id: Int, name: String, dosage: String, timeOfDay: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pill = PillDto(id = id, name = name, dosage = dosage, timeOfDay = timeOfDay, isActive = isActive)
            healthRepository.updatePillReminder(id, pill)
                .onSuccess { updatedPill ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills.map { if (it.id == id) updatedPill else it }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun deletePill(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            healthRepository.deletePillReminder(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills.filter { it.id != id }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
