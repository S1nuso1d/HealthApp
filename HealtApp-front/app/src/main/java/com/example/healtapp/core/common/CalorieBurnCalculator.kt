package com.example.healtapp.core.common

import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import com.example.healtapp.features.activity.presentation.isWalkLikeApi
import java.time.LocalDate

/**
 * Цель и факт сожжённых ккал: согласовано с календарём целей на бэкенде (шаги × 0.05, мин. 400).
 */
object CalorieBurnCalculator {

    /** ~0.04 ккал на шаг при отсутствии записи с ккал в «ходьбе». */
    fun estimateKcalFromSteps(steps: Int): Int =
        (steps.coerceAtLeast(0) * 0.04f).toInt()

    fun dailyBurnGoal(
        targetSteps: Int,
        goal: String? = null,
    ): Int {
        val steps = targetSteps.coerceAtLeast(1000)
        var base = maxOf(400, (steps * 0.05f).toInt())
        when (goal) {
            Constants.Goals.LOSE_WEIGHT -> base = (base * 1.2f).toInt()
            Constants.Goals.GAIN_MUSCLE -> base = (base * 1.05f).toInt()
            else -> Unit
        }
        return base.coerceIn(250, 2000)
    }

    /**
     * Сумма за сегодня: тренировки (не walk) + ходьба/шаги (из записи или оценка).
     */
    fun totalBurnedToday(
        activities: List<ActivityDto>,
        stepsToday: Int,
    ): Int {
        val todayKey = LocalDate.now().toString()
        val todayActs = activities.filter {
            ActivityStepsHelper.activityDateKey(it.start_time) == todayKey
        }
        val trainingKcal = todayActs
            .filter { !isWalkLikeApi(it.activity_type) }
            .sumOf { (it.calories_burned ?: 0f).toDouble() }
            .toInt()
        val walkRecord = ActivityStepsHelper.findTodayWalkRecord(activities)
        val walkKcal = when {
            (walkRecord?.calories_burned ?: 0f) > 0f ->
                walkRecord!!.calories_burned!!.toInt()
            else -> estimateKcalFromSteps(stepsToday)
        }
        return (trainingKcal + walkKcal).coerceAtLeast(0)
    }
}
