package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ScoreBreakdownUi
import kotlin.math.roundToInt

/**
 * Зеркалит логику [health_score_service] на бэкенде для мгновенного обновления UI.
 */
object HealthScoreCalculator {

    fun normalizeMood(mood: Int): Double =
        if (mood <= 5) mood * 2.0 else mood.toDouble()

    fun computeStateScore(mood: Int, energy: Int, stress: Int): Int {
        val moodNorm = normalizeMood(mood)
        val stressInverted = 11.0 - stress.coerceIn(1, 10)
        val avg = (moodNorm + energy.coerceIn(1, 10) + stressInverted) / 3.0
        return (avg / 10.0 * 100).roundToInt().coerceIn(0, 100)
    }

    fun recomputeHealthScore(
        scores: ScoreBreakdownUi,
        stateScore: Int,
        sleepHours: Float,
        waterMl: Int,
        waterTargetMl: Int,
        stepsToday: Int,
        stepsGoal: Int,
        caloriesToday: Int,
        caloriesTarget: Int,
        moodSavedToday: Boolean,
    ): ScoreBreakdownUi {
        val parts = mutableListOf<Int>()

        var sleepScore = scores.sleepScore
        if (sleepHours > 0f) {
            val target = 8f
            sleepScore = if (sleepHours >= target) {
                100
            } else {
                ((sleepHours / target) * 100f).roundToInt().coerceIn(0, 100)
            }
            parts.add(sleepScore)
        }

        var hydrationScore = scores.hydrationScore
        if (waterMl > 0) {
            val target = waterTargetMl.coerceAtLeast(1)
            hydrationScore = if (waterMl >= target) {
                100
            } else {
                ((waterMl.toFloat() / target) * 100f).roundToInt().coerceIn(0, 100)
            }
            parts.add(hydrationScore)
        }

        var activityScore = scores.activityScore
        if (stepsToday > 0) {
            val target = stepsGoal.coerceAtLeast(1)
            activityScore = if (stepsToday >= target) {
                100
            } else {
                ((stepsToday.toFloat() / target) * 100f).roundToInt().coerceIn(0, 100)
            }
            parts.add(activityScore)
        }

        var nutritionScore = scores.nutritionScore
        if (caloriesToday > 0) {
            val target = caloriesTarget.coerceAtLeast(1)
            val ratio = caloriesToday.toFloat() / target
            nutritionScore = when {
                ratio in 0.85f..1.15f -> 100
                ratio < 0.85f -> ((ratio / 0.85f) * 100f).roundToInt().coerceIn(0, 100)
                else -> (100f - (ratio - 1.15f) * 120f).roundToInt().coerceIn(40, 100)
            }
            parts.add(nutritionScore)
        }

        val effectiveStateScore = if (moodSavedToday) stateScore else scores.stateScore
        if (moodSavedToday && effectiveStateScore > 0) {
            parts.add(effectiveStateScore)
        }

        val healthScore = if (parts.isEmpty()) 0 else parts.average().roundToInt().coerceIn(0, 100)

        return scores.copy(
            healthScore = healthScore,
            sleepScore = sleepScore,
            hydrationScore = hydrationScore,
            activityScore = activityScore,
            nutritionScore = nutritionScore,
            stateScore = effectiveStateScore,
        )
    }
}
