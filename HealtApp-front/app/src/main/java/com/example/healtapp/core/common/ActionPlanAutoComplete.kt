package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi

/**
 * Локальное отображение «выполнено», если цели дня уже достигнуты (сервер делает то же при /dashboard/home).
 */
object ActionPlanAutoComplete {

    fun apply(
        items: List<ActionPlanItemUi>,
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
        mealCount: Int = if (caloriesToday > 0) 1 else 0,
        activityMinutesToday: Int = 0,
        moodSavedToday: Boolean = false,
    ): List<ActionPlanItemUi> = items.map { item ->
        if (item.status == "done" || item.status == "skipped") return@map item
        if (isCategoryGoalMet(
                category = item.category,
                waterMl = waterMl,
                waterTargetMl = waterTargetMl,
                stepsToday = stepsToday,
                stepsGoal = stepsGoal,
                caloriesBurnedToday = caloriesBurnedToday,
                caloriesBurnGoal = caloriesBurnGoal,
                sleepHours = sleepHours,
                sleepTargetHours = sleepTargetHours,
                caloriesToday = caloriesToday,
                caloriesTarget = caloriesTarget,
                mealCount = mealCount,
                activityMinutesToday = activityMinutesToday,
                moodSavedToday = moodSavedToday,
            )
        ) {
            item.copy(status = "done")
        } else {
            item
        }
    }

    private fun isCategoryGoalMet(
        category: String,
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
        mealCount: Int,
        activityMinutesToday: Int,
        moodSavedToday: Boolean,
    ): Boolean = when (category.lowercase()) {
        "hydration" -> waterTargetMl > 0 && waterMl >= waterTargetMl
        "activity" -> stepsGoal > 0 && stepsToday >= stepsGoal ||
            caloriesBurnGoal > 0 && caloriesBurnedToday >= caloriesBurnGoal ||
            activityMinutesToday >= 30
        "sleep" -> sleepTargetHours > 0f && sleepHours >= sleepTargetHours
        "meals", "nutrition", "correlation" -> {
            if (caloriesTarget <= 0) mealCount >= 2
            else {
                mealCount >= 2 && caloriesToday >= (caloriesTarget * 0.85).toInt() &&
                    caloriesToday <= (caloriesTarget * 1.15).toInt()
            }
        }
        "state" -> moodSavedToday
        else -> false
    }
}
