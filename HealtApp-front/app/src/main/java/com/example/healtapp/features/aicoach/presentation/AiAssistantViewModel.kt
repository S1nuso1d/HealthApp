package com.example.healtapp.features.aicoach.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChatMessageUi(
    val id: Long,
    val isUser: Boolean,
    val text: String,
)

data class AiMetricChipUi(
    val label: String,
    val value: String,
    val progress: Float?,
)

data class AiAssistantUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val metricChips: List<AiMetricChipUi> = emptyList(),
    val contextHint: String? = null,
    val isGuestMode: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val wellnessRepository: WellnessRepository,
    private val tokenStorage: TokenStorage,
    private val profileRepository: ProfileRepository,
    private val sleepRepository: SleepRepository,
    private val hydrationRepository: HydrationRepository,
    private val activityRepository: ActivityRepository,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    private var messageId = 0L
    private var todayContextLine: String? = null

    init {
        loadContextAndWelcome()
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
    }

    fun sendMessage() {
        val question = _uiState.value.input.trim()
        if (question.isBlank() || _uiState.value.isLoading || _uiState.value.isGuestMode) return
        addUserMessage(question)
        _uiState.update { it.copy(input = "", isLoading = true, error = null) }
        val payload = buildQuestionWithContext(question)
        viewModelScope.launch {
            wellnessRepository.aiChat(payload).onSuccess { response ->
                addBotMessage(response.answer.trim())
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось получить ответ. Проверьте сервер и LLM.",
                    )
                }
            }
        }
    }

    private fun loadContextAndWelcome() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(
                        isGuestMode = true,
                        contextHint = "Демо-режим: войдите в аккаунт, чтобы советник видел ваш дневник.",
                    )
                }
                addBotMessage(
                    "Войдите в аккаунт и добавьте записи сна, воды, питания и шагов — тогда смогу давать персональные советы по вашим данным.",
                )
                return@launch
            }
            val ctx = runCatching { buildTodayContext() }.getOrNull()
            if (ctx != null) {
                todayContextLine = ctx.contextLine
                _uiState.update {
                    it.copy(
                        metricChips = ctx.chips,
                        contextHint = ctx.hint,
                    )
                }
                addBotMessage(ctx.welcome)
            } else {
                addBotMessage(
                    "Задайте вопрос о сне, питании, воде или активности — отвечу на основе данных из вашего дневника HealthApp.",
                )
            }
        }
    }

    private data class TodayContext(
        val contextLine: String,
        val chips: List<AiMetricChipUi>,
        val hint: String?,
        val welcome: String,
    )

    private suspend fun buildTodayContext(): TodayContext = coroutineScope {
        val today = LocalDate.now().toString()
        val profile = profileRepository.getMyProfile().getOrNull()
        val sleep = async { sleepRepository.getSleepHistory().getOrNull().orEmpty() }
        val water = async { hydrationRepository.getTodayHydrationSummary().getOrNull() }
        val activity = async { activityRepository.getActivityHistory().getOrNull().orEmpty() }
        val meals = async { mealRepository.getMealHistory().getOrNull().orEmpty() }

        val sleepTarget = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f
        val waterTarget = profile?.target_water_ml?.toInt()?.takeIf { it > 0 } ?: 2500
        val stepsTarget = profile?.target_steps?.takeIf { it > 0 } ?: 10_000
        val calTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200

        val sleepH = sleep.await()
            .filter { it.sleep_end.take(10) == today }
            .sumOf { it.duration_hours.toDouble() }
            .toFloat()
        val waterMl = water.await()?.total_ml ?: 0
        val stepsDb = ActivityStepsHelper.sumStepsForDate(activity.await(), today)
        val stepsHc = HealthConnectReader(appContext)
            .takeIf { it.canRequestPermissions() }
            ?.let { runCatching { it.readTodaySteps() }.getOrNull() }
        val steps = stepsHc?.takeIf { it > 0 } ?: stepsDb
        val kcal = meals.await()
            .filter { it.meal_time.take(10) == today }
            .sumOf { (it.calories ?: 0f).toDouble() }
            .toInt()

        val chips = listOf(
            AiMetricChipUi(
                label = "Сон",
                value = if (sleepH > 0f) "%.1f ч".format(sleepH) else "—",
                progress = if (sleepTarget > 0f) (sleepH / sleepTarget).coerceIn(0f, 1f) else null,
            ),
            AiMetricChipUi(
                label = "Вода",
                value = "$waterMl мл",
                progress = if (waterTarget > 0) waterMl.toFloat() / waterTarget else null,
            ),
            AiMetricChipUi(
                label = "Шаги",
                value = "%,d".format(steps).replace(',', ' '),
                progress = if (stepsTarget > 0) steps.toFloat() / stepsTarget else null,
            ),
            AiMetricChipUi(
                label = "Ккал",
                value = "$kcal",
                progress = if (calTarget > 0) kcal.toFloat() / calTarget else null,
            ),
        )

        val gaps = buildList {
            if (sleepH < sleepTarget * 0.85f) add("сон")
            if (waterMl < waterTarget * 0.7f) add("вода")
            if (steps < stepsTarget * 0.6f) add("шаги")
            if (kcal < calTarget * 0.5f) add("питание")
        }
        val hint = when {
            gaps.isEmpty() -> "Сегодня основные цели близки к норме — можно уточнить детали."
            gaps.size == 1 -> "Сфокусируйтесь на: ${gaps.first()}."
            else -> "Сегодня отстаём: ${gaps.joinToString(", ")}."
        }

        val line = buildString {
            append("Контекст на сегодня ($today): ")
            append("сон ${"%.1f".format(sleepH)} / $sleepTarget ч, ")
            append("вода $waterMl / $waterTarget мл, ")
            append("шаги $steps / $stepsTarget, ")
            append("калории $kcal / $calTarget ккал.")
        }

        val welcome = if (gaps.isNotEmpty()) {
            "Вижу ваш дневник за сегодня: ${gaps.joinToString(" и ")} пока ниже цели. " +
                "Спросите, что сделать до вечера — отвечу с опорой на ваши цифры."
        } else {
            "По сегодняшним записям цели выглядят хорошо. Спросите, что улучшить дальше или как удержать ритм."
        }

        TodayContext(line, chips, hint, welcome)
    }

    private fun buildQuestionWithContext(question: String): String {
        val ctx = todayContextLine ?: return question
        return "$ctx\n\nВопрос пользователя: $question"
    }

    private fun addUserMessage(text: String) {
        messageId++
        _uiState.update { it.copy(messages = it.messages + ChatMessageUi(messageId, true, text)) }
    }

    private fun addBotMessage(text: String) {
        messageId++
        _uiState.update { it.copy(messages = it.messages + ChatMessageUi(messageId, false, text)) }
    }
}
