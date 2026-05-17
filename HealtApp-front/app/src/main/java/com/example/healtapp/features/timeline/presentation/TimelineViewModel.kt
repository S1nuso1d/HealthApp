package com.example.healtapp.features.timeline.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.domain.repository.WellnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val timeFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru", "RU"))

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val insightsDeferred = async { wellnessRepository.getInsights() }
                    val runsDeferred = async { wellnessRepository.getAnalysisRuns(15) }
                    val statesDeferred = async { wellnessRepository.listUserStates() }

                    val insights = insightsDeferred.await().getOrThrow()
                    val runs = runsDeferred.await().getOrThrow()
                    val states = statesDeferred.await().getOrThrow()

                    val events = mutableListOf<TimelineEventUi>()

                    insights.forEach { insight ->
                        events.add(
                            TimelineEventUi(
                                id = "insight_${insight.title}",
                                timeLabel = "Инсайт",
                                title = insight.title,
                                subtitle = insight.description,
                                category = insight.category,
                            ),
                        )
                    }

                    runs.forEach { run ->
                        val label = runCatching {
                            LocalDateTime.parse(run.createdAt.take(19)).format(timeFmt)
                        }.getOrElse { run.createdAt.take(16) }
                        events.add(
                            TimelineEventUi(
                                id = "run_${run.id}",
                                timeLabel = label,
                                title = "Аналитика за ${run.periodDays} дн.",
                                subtitle = "Health score: ${run.healthScore?.toInt() ?: "—"} · ${run.status}",
                                category = "analytics",
                            ),
                        )
                    }

                    states.take(10).forEach { state ->
                        val label = runCatching {
                            LocalDateTime.parse(state.recordTime.take(19)).format(timeFmt)
                        }.getOrElse { state.recordTime.take(16) }
                        events.add(
                            TimelineEventUi(
                                id = "state_${state.id}",
                                timeLabel = label,
                                title = "Отметка состояния",
                                subtitle = buildString {
                                    state.mood?.let { append("настроение $it ") }
                                    state.energy?.let { append("энергия $it ") }
                                    state.stress?.let { append("стресс $it") }
                                }.trim(),
                                category = "state",
                            ),
                        )
                    }

                    val summary = if (runs.isNotEmpty()) {
                        "Последний индекс: ${runs.first().healthScore?.toInt() ?: "—"} · ${runs.size} пересчётов"
                    } else {
                        "Добавляйте данные — здесь появятся инсайты и отметки"
                    }

                    val highlight = insights.firstOrNull()?.title ?: "Пока без выделенного инсайта"

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            events = events,
                            summaryText = summary,
                            insightHighlight = highlight,
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
