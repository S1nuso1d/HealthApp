package com.example.healtapp.features.weekly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.data.network.dto.analytics.AnalyticsCompareDto
import com.example.healtapp.data.network.dto.wellness.AIBriefDto
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.dashboard.presentation.WeeklyMetricUi
import com.example.healtapp.features.dashboard.presentation.WeeklySummaryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyReviewUiState(
    val isLoading: Boolean = true,
    val brief: AIBriefDto? = null,
    val compare: AnalyticsCompareDto? = null,
    val compareUnavailable: String? = null,
    val weeklySummary: WeeklySummaryUi? = null,
    val error: String? = null,
)

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReviewUiState())
    val uiState: StateFlow<WeeklyReviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, compareUnavailable = null) }
            coroutineScope {
                val briefDeferred = async { wellnessRepository.weeklyBrief(7) }
                val compareDeferred = async { wellnessRepository.compareAnalytics() }
                val overviewDeferred = async { wellnessRepository.getAnalyticsOverview(7) }
                val brief = briefDeferred.await()
                val compare = compareDeferred.await()
                val overview = overviewDeferred.await().getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        brief = brief.getOrNull(),
                        compare = compare.getOrNull(),
                        weeklySummary = overview?.let { data ->
                            val trends = data.trends
                            WeeklySummaryUi(
                                periodLabel = "${data.meta.startDate} — ${data.meta.endDate}",
                                metrics = listOf(
                                    WeeklyMetricUi(
                                        key = "sleep",
                                        label = "Сон",
                                        averageDisplay = trends?.avgSleepHours?.let { h -> "%.1f ч".format(h) } ?: "—",
                                        daysLogged = trends?.daysWithData ?: 0,
                                        daysInPeriod = data.summary.periodDays,
                                        hint = "среднее за дни с записями",
                                    ),
                                    WeeklyMetricUi(
                                        key = "water",
                                        label = "Вода",
                                        averageDisplay = trends?.avgWaterMl?.let { ml -> "${ml.toInt()} мл" } ?: "—",
                                        daysLogged = trends?.daysWithData ?: 0,
                                        daysInPeriod = data.summary.periodDays,
                                        hint = "среднее за дни с записями",
                                    ),
                                    WeeklyMetricUi(
                                        key = "steps",
                                        label = "Шаги",
                                        averageDisplay = trends?.avgSteps?.let { s -> s.toInt().toString() } ?: "—",
                                        daysLogged = trends?.daysWithData ?: 0,
                                        daysInPeriod = data.summary.periodDays,
                                        hint = "среднее за дни с записями",
                                    ),
                                    WeeklyMetricUi(
                                        key = "calories",
                                        label = "Ккал",
                                        averageDisplay = trends?.avgCalories?.let { c -> c.toInt().toString() } ?: "—",
                                        daysLogged = trends?.daysWithData ?: 0,
                                        daysInPeriod = data.summary.periodDays,
                                        hint = "среднее за дни с записями",
                                    ),
                                ),
                            )
                        },
                        compareUnavailable = if (compare.isFailure) {
                            "Сравнение появится после двух пересчётов аналитики."
                        } else {
                            null
                        },
                        error = brief.exceptionOrNull()?.let { t ->
                            UserFacingMessages.fromThrowable(t, "Не удалось загрузить разбор недели")
                        },
                    )
                }
            }
        }
    }
}
