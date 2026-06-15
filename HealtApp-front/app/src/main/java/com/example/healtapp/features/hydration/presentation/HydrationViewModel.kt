package com.example.healtapp.features.hydration.presentation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.preferences.HydrationPrefs
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.preferences.WidgetSnapshot
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.data.sync.PendingSyncFlusher
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.widget.refreshAllWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HydrationViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: HydrationRepository,
    private val profileRepository: ProfileRepository,
    private val pendingSyncStore: PendingSyncStore,
    private val pendingSyncFlusher: PendingSyncFlusher,
    private val widgetSnapshotStore: WidgetSnapshotStore,
    private val hydrationPrefs: HydrationPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HydrationUiState())
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hydrationPrefs.customQuickAmountsFlow.collect { custom ->
                _uiState.update { it.copy(customQuickAmounts = custom) }
            }
        }
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

            val waterTarget = profileRepository.getMyProfile()
                .getOrNull()
                ?.target_water_ml
                ?.toInt()
                ?.takeIf { it > 0 }
                ?: 2500

            val result = repository.getTodayHydrationSummary()

            result.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    waterToday = summary.total_ml,
                    todayRecords = summary.records,
                    target = waterTarget,
                )
                val snap = widgetSnapshotStore.load() ?: WidgetSnapshot()
                widgetSnapshotStore.save(snap.copy(waterMl = summary.total_ml))
                refreshAllWidgets(appContext)
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
                val snap = widgetSnapshotStore.load() ?: WidgetSnapshot()
                widgetSnapshotStore.save(snap.copy(waterMl = snap.waterMl + amount))
                refreshAllWidgets(appContext)
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

    fun addCustomQuickAmount(ml: Int) {
        viewModelScope.launch {
            val ok = hydrationPrefs.addCustomQuickAmount(ml)
            if (!ok) {
                _uiState.update {
                    it.copy(
                        error = when {
                            ml !in HydrationPrefs.MIN_ML..HydrationPrefs.MAX_ML ->
                                "Укажите объём от ${HydrationPrefs.MIN_ML} до ${HydrationPrefs.MAX_ML} мл"
                            it.customQuickAmounts.size >= HydrationPrefs.MAX_CUSTOM_BUTTONS ->
                                "Можно сохранить не больше ${HydrationPrefs.MAX_CUSTOM_BUTTONS} своих кнопок"
                            else -> "Такая кнопка уже есть в быстром добавлении"
                        },
                    )
                }
            }
        }
    }

    fun removeCustomQuickAmount(ml: Int) {
        viewModelScope.launch {
            hydrationPrefs.removeCustomQuickAmount(ml)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
