package com.example.healtapp.features.pills.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.health.PillDto
import com.example.healtapp.data.preferences.PillDoseLogStore
import com.example.healtapp.data.preferences.PillDoseStatus
import com.example.healtapp.domain.repository.HealthRepository
import com.example.healtapp.notifications.PillReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PillsUiState(
    val isLoading: Boolean = false,
    val pills: List<PillDto> = emptyList(),
    val todayStatusByPillId: Map<Int, PillDoseStatus> = emptyMap(),
    val adherencePercent: Int? = null,
    val error: String? = null,
)

@HiltViewModel
class PillsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val healthRepository: HealthRepository,
    private val doseLogStore: PillDoseLogStore,
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
                    rescheduleAll(pills)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pills = pills,
                            todayStatusByPillId = buildTodayMap(pills),
                            adherencePercent = doseLogStore.adherencePercent(7),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun logDose(pillId: Int, status: PillDoseStatus) {
        doseLogStore.logToday(pillId, status)
        _uiState.update {
            it.copy(
                todayStatusByPillId = it.todayStatusByPillId + (pillId to status),
                adherencePercent = doseLogStore.adherencePercent(7),
            )
        }
    }

    fun addPill(name: String, dosage: String, timeOfDay: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pill = PillDto(name = name, dosage = dosage, timeOfDay = timeOfDay)
            healthRepository.createPillReminder(pill)
                .onSuccess { newPill ->
                    newPill.id?.let { id ->
                        PillReminderScheduler.schedulePillReminder(
                            context = appContext,
                            pillId = id,
                            name = newPill.name,
                            dosage = newPill.dosage,
                            timeOfDay = newPill.timeOfDay,
                            active = newPill.isActive,
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills + newPill,
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
                    PillReminderScheduler.schedulePillReminder(
                        context = appContext,
                        pillId = id,
                        name = updatedPill.name,
                        dosage = updatedPill.dosage,
                        timeOfDay = updatedPill.timeOfDay,
                        active = updatedPill.isActive,
                    )
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills.map { if (it.id == id) updatedPill else it },
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
                    PillReminderScheduler.cancelPillReminder(appContext, id)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pills = state.pills.filter { it.id != id },
                            todayStatusByPillId = state.todayStatusByPillId - id,
                            adherencePercent = doseLogStore.adherencePercent(7),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun rescheduleAll(pills: List<PillDto>) {
        pills.forEach { pill ->
            val id = pill.id ?: return@forEach
            PillReminderScheduler.schedulePillReminder(
                context = appContext,
                pillId = id,
                name = pill.name,
                dosage = pill.dosage,
                timeOfDay = pill.timeOfDay,
                active = pill.isActive,
            )
        }
    }

    private fun buildTodayMap(pills: List<PillDto>): Map<Int, PillDoseStatus> =
        pills.mapNotNull { pill ->
            val id = pill.id ?: return@mapNotNull null
            doseLogStore.todayStatus(id)?.let { id to it }
        }.toMap()
}
