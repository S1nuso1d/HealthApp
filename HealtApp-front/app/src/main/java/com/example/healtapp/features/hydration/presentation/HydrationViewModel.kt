package com.example.healtapp.features.hydration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.sync.PendingSyncFlusher
import com.example.healtapp.domain.repository.HydrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val repository: HydrationRepository,
    private val pendingSyncStore: PendingSyncStore,
    private val pendingSyncFlusher: PendingSyncFlusher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HydrationUiState())
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppRefreshBus.events.collect { load() }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            pendingSyncFlusher.flush()
            val pendingCount = pendingSyncStore.load().hydration.size + pendingSyncStore.load().meals.size
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                pendingSyncCount = pendingCount,
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
                _uiState.update {
                    it.copy(progressCelebrateToken = it.progressCelebrateToken + 1)
                }
                load()
                AppRefreshBus.notifyDataChanged()
            }.onFailure { _ ->
                pendingSyncStore.enqueueHydration(amount)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Нет сети — ${amount} мл сохранены и отправятся позже",
                        pendingSyncCount = run {
                            val q = pendingSyncStore.load()
                            q.hydration.size + q.meals.size
                        },
                    )
                }
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
