package com.example.healtapp.features.hydration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.domain.repository.HydrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val repository: HydrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HydrationUiState())
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = repository.getTodayHydrationSummary()

            result.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    waterToday = summary.total_ml,
                    todayRecords = summary.records,
                    target = 2500
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось загрузить данные по воде"
                )
            }
        }
    }

    fun addWater(amount: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = repository.addHydration(amount)

            result.onSuccess {
                load()
                AppRefreshBus.notifyDataChanged()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось добавить воду"
                )
            }
        }
    }

    fun updateRecord(id: Int, amountMl: Int, recordTimeIso: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.updateHydration(id, amountMl, recordTimeIso)
                .onSuccess {
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось обновить запись",
                    )
                }
        }
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.deleteHydration(id)
                .onSuccess {
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось удалить запись",
                    )
                }
        }
    }
}
