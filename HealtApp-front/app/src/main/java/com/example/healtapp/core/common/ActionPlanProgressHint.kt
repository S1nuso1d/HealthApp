package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi

object ActionPlanProgressHint {

    fun label(
        item: ActionPlanItemUi,
        waterMl: Int,
        waterTargetMl: Int,
        stepsToday: Int,
        stepsGoal: Int,
        caloriesBurnedToday: Int,
        caloriesBurnGoal: Int,
        sleepHours: Float,
        sleepTargetHours: Float,
        caloriesToday: Int,
        caloriesTarget: Int,
        activityMinutesToday: Int,
        moodSavedToday: Boolean,
    ): String? {
        if (item.status == "done" || item.status == "skipped") return null
        return when (item.category.lowercase()) {
            "hydration" -> {
                if (waterTargetMl <= 0) return null
                val left = (waterTargetMl - waterMl).coerceAtLeast(0)
                if (left <= 0) "Норма воды выполнена" else "Осталось $left мл воды"
            }
            "activity" -> when {
                stepsGoal > 0 && stepsToday < stepsGoal ->
                    "Шаги: ${formatNum(stepsToday)} / ${formatNum(stepsGoal)}"
                caloriesBurnGoal > 0 && caloriesBurnedToday < caloriesBurnGoal ->
                    "Сожжено: $caloriesBurnedToday / $caloriesBurnGoal ккал"
                activityMinutesToday < 30 ->
                    "Активность: $activityMinutesToday мин (цель 30+)"
                else -> null
            }
            "sleep" -> {
                if (sleepTargetHours <= 0f) return null
                val left = (sleepTargetHours - sleepHours).coerceAtLeast(0f)
                if (left <= 0.1f) "Сон в норме" else "До цели сна: ${"%.1f".format(left).replace('.', ',')} ч"
            }
            "meals", "nutrition", "correlation" -> {
                if (caloriesTarget <= 0) {
                    if (caloriesToday > 0) "Еда записана" else "Добавьте приём пищи"
                } else {
                    "Калории: $caloriesToday / $caloriesTarget ккал"
                }
            }
            "state" -> if (moodSavedToday) "Настроение отмечено" else "Отметьте настроение на главной"
            else -> null
        }
    }

    private fun formatNum(n: Int): String =
        "%,d".format(n).replace(',', '\u00A0')
}
