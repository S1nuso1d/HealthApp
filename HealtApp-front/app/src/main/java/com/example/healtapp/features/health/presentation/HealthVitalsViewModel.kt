package com.example.healtapp.features.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.api.HealthApi
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.data.network.dto.health.HealthSampleDto
import com.example.healtapp.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthVitalsUiState(
    val isLoading: Boolean = false,
    val chartDays: Int = 7,
    val samples: List<HealthSampleDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HealthVitalsViewModel @Inject constructor(
    private val healthApi: HealthApi,
    private val activityRepository: ActivityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthVitalsUiState())
    val uiState: StateFlow<HealthVitalsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setChartPeriod(days: Int) {
        val d = if (days >= 30) 30 else 7
        _uiState.update { if (it.chartDays == d) it else it.copy(chartDays = d) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val days = _uiState.value.chartDays
            _uiState.update { it.copy(isLoading = true, error = null) }
            coroutineScope {
                val samplesDef = async { runCatching { healthApi.listSamples(days = days, metrics = null) } }
                val actDef = async { activityRepository.getActivityHistory() }
                val samplesRes = samplesDef.await()
                val actRes = actDef.await()
                val windowStart = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
                val activitiesInWindow = actRes.getOrElse { emptyList() }
                    .filter { act ->
                        val t = runCatching { OffsetDateTime.parse(act.start_time).toInstant() }
                            .getOrElse { Instant.EPOCH }
                        !t.isBefore(windowStart)
                    }
                    .sortedByDescending { it.start_time }
                samplesRes
                    .onSuccess { list ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                samples = list,
                                activities = activitiesInWindow,
                                error = null,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activities = activitiesInWindow,
                                error = e.message ?: "Не удалось загрузить показатели",
                            )
                        }
                    }
            }
        }
    }
}
