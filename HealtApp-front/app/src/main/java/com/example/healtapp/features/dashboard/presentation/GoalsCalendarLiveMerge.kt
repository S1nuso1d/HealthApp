package com.example.healtapp.features.dashboard.presentation

import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDayDto
import kotlin.math.min

/**
 * Подмешивает актуальные шаги и сожжённые ккал за сегодня в день календаря
 * (как [com.example.healtapp.features.activity.presentation.ActivityViewModel] для недельного графика).
 */
object GoalsCalendarLiveMerge {

    fun patchToday(
        days: List<GoalsCalendarDayDto>,
        todayKey: String,
        stepsToday: Int,
        stepsGoal: Int,
        caloriesBurnedToday: Float,
        burnGoal: Float,
    ): List<GoalsCalendarDayDto> {
        if (stepsToday <= 0 && caloriesBurnedToday <= 0f) return days
        return days.map { day ->
            if (day.date != todayKey) day
            else patchDay(day, stepsToday, stepsGoal, caloriesBurnedToday, burnGoal)
        }
    }

    private fun patchDay(
        day: GoalsCalendarDayDto,
        stepsToday: Int,
        stepsGoal: Int,
        caloriesBurnedToday: Float,
        burnGoal: Float,
    ): GoalsCalendarDayDto {
        val steps = maxOf(day.steps, stepsToday)
        val burned = maxOf(day.calories_burned, caloriesBurnedToday)

        val hasSteps = steps > 0
        val hasBurned = burned > 0f
        val hasSleep = day.sleep_hours > 0f
        val hasWater = day.water_ml > 0
        val hasAny = hasSleep || hasWater || hasSteps || hasBurned ||
            day.calories_consumed > 0f || day.calories > 0f

        val stepsProg = if (hasSteps && stepsGoal > 0) {
            min(1f, steps.toFloat() / stepsGoal)
        } else {
            0f
        }
        val burnedProg = if (hasBurned && burnGoal > 0f) {
            min(1f, burned / burnGoal)
        } else {
            0f
        }

        val sleepOk = day.sleep_met
        val waterOk = day.hydration_met
        val stepsOk = stepsGoal > 0 && steps >= stepsGoal
        val nutritionOk = burnGoal > 0f && burned >= burnGoal * 0.7f
        val allOk = hasAny && sleepOk && waterOk && stepsOk && nutritionOk

        return day.copy(
            steps = steps,
            activity_progress = stepsProg,
            activity_met = stepsOk,
            calories_burned = burned,
            nutrition_progress = burnedProg,
            nutrition_met = nutritionOk,
            has_any_data = hasAny,
            all_goals_met = allOk,
        )
    }
}
