package com.example.healtapp.features.dashboard.presentation

import java.time.LocalTime

data class DayInsightUi(
    val title: String,
    val detail: String,
    val proof: String? = null,
)

object DayInsightBuilder {

    fun phrase(state: DashboardUiState, isEvening: Boolean, recovery: Boolean): String {
        if (recovery) return "Сегодня день восстановления — цели мягче, без силовой"
        if (isEvening) {
            val caffeine = state.caffeineToday
            return when {
                !state.circadian?.bedtimeNudge.isNullOrBlank() ->
                    state.circadian?.bedtimeNudge.orEmpty()
                caffeine >= 150f -> "Кофеин уже высокий · воду после 20:00 лучше не догонять"
                else -> "Окно ко сну: без тяжёлой тренировки и позднего ужина"
            }
        }
        val window = trainingWindowLabel(state)
        return when {
            state.waterMl < state.waterTargetMl * 0.25f && state.waterTargetMl > 0 ->
                "Сначала вода и завтрак" + window?.let { " · тренировка до $it" }.orEmpty()
            state.caloriesToday <= 0 ->
                "Завтрак ещё не записан" + window?.let { " · окно нагрузки до $it" }.orEmpty()
            else -> window?.let { "Окно для тренировки до $it" }
                ?: "Четыре опоры дня — вода, еда, шаги и сон"
        }
    }

    fun insight(state: DashboardUiState, isEvening: Boolean): DayInsightUi? {
        state.circadian?.recentWorkoutWarning?.takeIf { it.isNotBlank() }?.let {
            return DayInsightUi(
                title = "Почему сон может быть короче",
                detail = it,
                proof = state.circadian.usualBedtime?.let { t -> "Обычный отбой около $t" },
            )
        }
        val lateMeal = state.circadian?.hoursBeforeSleep
            ?.filter { (it.sleepHours ?: 0.0) > 0 && it.nights >= 2 }
            ?.minByOrNull { it.sleepHours ?: Double.MAX_VALUE }
        if (lateMeal != null) {
            val best = state.circadian?.hoursBeforeSleep
                ?.filter { (it.sleepHours ?: 0.0) > 0 && it.nights >= 2 }
                ?.maxByOrNull { it.sleepHours ?: 0.0 }
            if (best != null && (best.sleepHours ?: 0.0) - (lateMeal.sleepHours ?: 0.0) >= 0.3) {
                return DayInsightUi(
                    title = "Ужин сдвигает сон",
                    detail = "Когда последний приём ближе к отбою (${lateMeal.label}), сон короче, чем при более ранней еде (${best.label}).",
                    proof = "${"%.1f".format(lateMeal.sleepHours)} ч против ${"%.1f".format(best.sleepHours)} ч · только ночи с записью",
                )
            }
        }
        state.topInsights.firstOrNull()?.let {
            return DayInsightUi(title = it.title, detail = it.description)
        }
        if (isEvening) {
            val lever = state.tonightRisk?.levers?.firstOrNull()
            if (lever != null) {
                return DayInsightUi(
                    title = lever.title?.takeIf { it.isNotBlank() } ?: "На сон сегодня влияет",
                    detail = lever.why ?: lever.action.orEmpty(),
                    proof = state.tonightRisk?.primaryAction,
                )
            }
        }
        if (state.sleepHours <= 0f && LocalTime.now().hour >= 10) {
            return DayInsightUi(
                title = "Ночь ещё не в дневнике",
                detail = "Без записи сна рекомендации опираются только на воду, еду и шаги. Добавьте ночь вручную или из Health Connect.",
            )
        }
        return null
    }

    fun trainingWindowLabel(state: DashboardUiState): String? {
        val hour = state.circadian?.usualBedtimeHour ?: return null
        val minute = state.circadian?.usualBedtimeMinute ?: 0
        val end = hour * 60 + minute - 180
        if (end <= 0) return null
        val h = ((end / 60) + 24) % 24
        val m = end % 60
        return "%02d:%02d".format(h, m)
    }
}
