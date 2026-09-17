package com.example.healtapp.features.dashboard.presentation

import com.example.healtapp.core.common.ActionPlanProgressHint
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem
import java.time.LocalTime

data class TodayFocusItemUi(
    val id: String,
    val category: String,
    val title: String,
    val detail: String,
    val progress: String?,
    val progressFraction: Float? = null,
    val primary: Boolean = false,
)

object TodayFocusBuilder {

    fun build(state: DashboardUiState, isEvening: Boolean, recovery: Boolean): List<TodayFocusItemUi> {
        val pendingPlan = state.actionPlanItems.filter { it.status != "done" && it.status != "skipped" }
        val items = mutableListOf<TodayFocusItemUi>()

        if (recovery) {
            items += TodayFocusItemUi(
                id = "recovery",
                category = "state",
                title = "День без силовой",
                detail = "Цели воды и шагов мягче. Силовую лучше оставить — сегодня восстановление.",
                progress = null,
            )
        }

        if (isEvening) {
            state.circadian?.bedtimeNudge?.let { nudge ->
                items += TodayFocusItemUi(
                    id = "nudge-bedtime",
                    category = "sleep",
                    title = "Окно ко сну",
                    detail = nudge,
                    progress = state.circadian.usualBedtime?.let { "Обычно в $it" }
                        ?: progressSleep(state),
                    progressFraction = fractionFor("sleep", state),
                )
            }
            if (state.caffeineToday >= 80f) {
                items += TodayFocusItemUi(
                    id = "nudge-caffeine",
                    category = "nutrition",
                    title = "Кофеин уже в дневнике",
                    detail = "${state.caffeineToday.toInt()} мг. Вечером лучше вода маленькими порциями, без догона цели.",
                    progress = null,
                )
            }
            if (state.caloriesToday > 0) {
                items += TodayFocusItemUi(
                    id = "nudge-late-meal",
                    category = "nutrition",
                    title = "Последний приём — не позже окна сна",
                    detail = "Тяжёлый ужин ближе к отбою часто укорачивает ночь. Если ещё не ели — что-то лёгкое.",
                    progress = progressNutrition(state),
                    progressFraction = fractionFor("nutrition", state),
                )
            }
            if (!recovery) {
                items += TodayFocusItemUi(
                    id = "nudge-skip-workout",
                    category = "activity",
                    title = "Тренировку лучше оставить на завтра",
                    detail = "Поздняя нагрузка бьёт по засыпанию. Прогулка — да, силовая и интервалы — нет.",
                    progress = progressActivity(state),
                    progressFraction = fractionFor("activity", state),
                )
            }
        } else {
            if (state.waterMl < state.waterTargetMl * 0.3f && state.waterTargetMl > 0) {
                items += TodayFocusItemUi(
                    id = "nudge-water",
                    category = "hydration",
                    title = "Допить воду",
                    detail = "Сейчас ${state.waterMl} мл из ${state.waterTargetMl} мл. Добавляйте воду в разделе гидратации.",
                    progress = progressHydration(state),
                    progressFraction = fractionFor("hydration", state),
                )
            }
            if (state.caloriesToday <= 0 && LocalTime.now().hour < 12) {
                items += TodayFocusItemUi(
                    id = "nudge-breakfast",
                    category = "nutrition",
                    title = "Завтрак ещё не записан",
                    detail = "В дневнике питания пока нет калорий за сегодня. Отметьте завтрак в разделе еды.",
                    progress = progressNutrition(state),
                    progressFraction = fractionFor("nutrition", state),
                )
            }
            DayInsightBuilder.trainingWindowLabel(state)?.let { until ->
                if (!recovery) {
                    items += TodayFocusItemUi(
                        id = "nudge-window",
                        category = "activity",
                        title = "Окно тренировки до $until",
                        detail = "Обычный отбой минус три часа. Позже нагрузка чаще мешает сну.",
                        progress = progressActivity(state),
                        progressFraction = fractionFor("activity", state),
                    )
                }
            }
        }

        pendingPlan.forEach { plan ->
            if (isGoalMet(plan.category, state)) return@forEach
            val category = plan.category.lowercase()
            if (recovery && category == "activity") return@forEach
            if (isEvening && category == "activity") return@forEach
            if (items.any { it.title.equals(plan.title, ignoreCase = true) }) return@forEach
            items += TodayFocusItemUi(
                id = "plan-${plan.id}",
                category = category,
                title = plan.title,
                detail = plan.description,
                progress = progressFor(plan, state),
                progressFraction = fractionFor(category, state),
            )
        }

        state.recommendations.forEach { rec ->
            if (!isRecommendationActive(rec, state)) return@forEach
            if (items.any { it.title.equals(rec.title, ignoreCase = true) }) return@forEach
            val category = rec.category.lowercase()
            if (recovery && category == "activity") return@forEach
            if (isEvening && category == "activity") return@forEach
            val tip = rec.personalizedTip?.takeIf { it.isNotBlank() }
            items += TodayFocusItemUi(
                id = "rec-$category-${rec.title}",
                category = category,
                title = rec.title,
                detail = listOfNotNull(rec.description.takeIf { it.isNotBlank() }, tip)
                    .distinct()
                    .joinToString("\n"),
                progress = rec.progressLabel ?: remainingLabel(category, state),
                progressFraction = fractionFor(category, state),
            )
        }

        val ranked = items.distinctBy { it.id }.take(6)
        return ranked.mapIndexed { index, item -> item.copy(primary = index == 0) }
    }

    private fun isRecommendationActive(rec: RecommendationUiItem, state: DashboardUiState): Boolean {
        if (!rec.status.equals("active", ignoreCase = true) && rec.status.isNotBlank()) return false
        return !isGoalMet(rec.category, state)
    }

    private fun isGoalMet(category: String, state: DashboardUiState): Boolean = when (category.lowercase()) {
        "hydration" -> state.waterMl >= state.waterTargetMl && state.waterTargetMl > 0
        "activity" -> state.stepsToday >= state.stepsGoal && state.stepsGoal > 0
        "sleep" -> state.sleepHours >= state.sleepTargetHours - 0.05f && state.sleepHours > 0f
        "meals", "nutrition" -> state.caloriesToday >= (state.caloriesTarget * 0.98f).toInt() && state.caloriesTarget > 0
        "state" -> state.moodCheckIn.savedToday
        else -> false
    }

    private fun fractionFor(category: String, state: DashboardUiState): Float? = when (category.lowercase()) {
        "hydration" -> state.waterTargetMl.takeIf { it > 0 }?.let { state.waterMl.toFloat() / it }
        "activity" -> state.stepsGoal.takeIf { it > 0 }?.let { state.stepsToday.toFloat() / it }
        "sleep" -> state.sleepTargetHours.takeIf { it > 0f }?.let { state.sleepHours / it }
        "meals", "nutrition" -> state.caloriesTarget.takeIf { it > 0 }?.let { state.caloriesToday.toFloat() / it }
        "state" -> if (state.moodCheckIn.savedToday) 1f else 0f
        else -> null
    }?.coerceIn(0f, 1f)

    private fun remainingLabel(category: String, state: DashboardUiState): String? = when (category.lowercase()) {
        "hydration" -> progressHydration(state)
        "activity" -> progressActivity(state)
        "sleep" -> progressSleep(state)
        "meals", "nutrition" -> progressNutrition(state)
        "state" -> if (state.moodCheckIn.savedToday) "отмечено" else "ещё не отмечено"
        else -> null
    }

    private fun progressHydration(state: DashboardUiState): String? {
        if (state.waterTargetMl <= 0) return null
        val left = (state.waterTargetMl - state.waterMl).coerceAtLeast(0)
        return if (left == 0) "вода закрыта" else "ещё $left мл · ${state.waterMl} / ${state.waterTargetMl}"
    }

    private fun progressActivity(state: DashboardUiState): String? {
        if (state.stepsGoal <= 0) return null
        val left = (state.stepsGoal - state.stepsToday).coerceAtLeast(0)
        return if (left == 0) {
            "шаги закрыты"
        } else {
            "ещё $left шагов · ${state.stepsToday} / ${state.stepsGoal}"
        }
    }

    private fun progressSleep(state: DashboardUiState): String? {
        if (state.sleepTargetHours <= 0f) return null
        if (state.sleepHours <= 0f) return "сон ещё не записан · цель ${formatHours(state.sleepTargetHours)}"
        val left = (state.sleepTargetHours - state.sleepHours).coerceAtLeast(0f)
        return if (left <= 0.05f) {
            "сон закрыт"
        } else {
            "ещё ${formatHours(left)} · ${formatHours(state.sleepHours)} / ${formatHours(state.sleepTargetHours)}"
        }
    }

    private fun progressNutrition(state: DashboardUiState): String? {
        if (state.caloriesTarget <= 0) return null
        val left = (state.caloriesTarget - state.caloriesToday).coerceAtLeast(0)
        return if (left == 0) {
            "калории закрыты"
        } else {
            "ещё $left ккал · ${state.caloriesToday} / ${state.caloriesTarget}"
        }
    }

    private fun formatHours(value: Float): String {
        val rounded = (value * 10).toInt() / 10f
        return if (rounded == rounded.toInt().toFloat()) {
            "${rounded.toInt()} ч"
        } else {
            "$rounded ч"
        }
    }

    private fun progressFor(item: ActionPlanItemUi, state: DashboardUiState): String? =
        ActionPlanProgressHint.label(
            item = item,
            waterMl = state.waterMl,
            waterTargetMl = state.waterTargetMl,
            stepsToday = state.stepsToday,
            stepsGoal = state.stepsGoal,
            caloriesBurnedToday = state.caloriesBurnedToday,
            caloriesBurnGoal = state.caloriesBurnGoal,
            sleepHours = state.sleepHours,
            sleepTargetHours = state.sleepTargetHours,
            caloriesToday = state.caloriesToday,
            caloriesTarget = state.caloriesTarget,
            activityMinutesToday = state.activityMinutesToday,
            moodSavedToday = state.moodCheckIn.savedToday,
        ) ?: remainingLabel(item.category, state)
}
