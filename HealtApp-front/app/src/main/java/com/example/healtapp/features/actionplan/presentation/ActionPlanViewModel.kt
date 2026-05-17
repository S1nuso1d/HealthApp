package com.example.healtapp.features.actionplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActionPlanUiState(
    val items: List<ActionPlanItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
)

@HiltViewModel
class ActionPlanViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionPlanUiState())
    val uiState: StateFlow<ActionPlanUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            AppRefreshBus.events.collect { load() }
        }
    }

    fun clearSnack() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            wellnessRepository.listActionPlan()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = list.map { dto ->
                                ActionPlanItemUi(
                                    id = dto.id,
                                    title = dto.title,
                                    description = dto.description,
                                    category = dto.category,
                                    status = dto.status,
                                    priority = dto.priority,
                                )
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            wellnessRepository.generateActionPlan()
                .onSuccess { count ->
                    _uiState.update { it.copy(isGenerating = false, snackMessage = "Добавлено задач: $count") }
                    AppRefreshBus.notifyDataChanged()
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isGenerating = false, error = e.message) }
                }
        }
    }

    fun toggle(item: ActionPlanItemUi) {
        val next = if (item.status == "done") "pending" else "done"
        viewModelScope.launch {
            wellnessRepository.updateActionPlanStatus(item.id, next).onSuccess {
                AppRefreshBus.notifyDataChanged()
                load()
            }
        }
    }
}
