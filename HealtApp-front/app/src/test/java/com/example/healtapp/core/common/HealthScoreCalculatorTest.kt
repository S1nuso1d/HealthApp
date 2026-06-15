package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ScoreBreakdownUi
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthScoreCalculatorTest {

    @Test
    fun `normalizeMood returns double value for mood 6 and above`() {
        assertEquals(6.0, HealthScoreCalculator.normalizeMood(6), 0.01)
        assertEquals(10.0, HealthScoreCalculator.normalizeMood(10), 0.01)
    }

    @Test
    fun `normalizeMood multiplies by 2 for mood 5 and below`() {
        assertEquals(10.0, HealthScoreCalculator.normalizeMood(5), 0.01)
        assertEquals(4.0, HealthScoreCalculator.normalizeMood(2), 0.01)
    }

    @Test
    fun `computeStateScore calculates correct score`() {
        // Mood 10 -> norm 10.0
        // Energy 10
        // Stress 1 -> inverted 10.0
        // Avg = 10.0 -> Score 100
        assertEquals(100, HealthScoreCalculator.computeStateScore(mood = 10, energy = 10, stress = 1))

        // Mood 5 -> norm 10.0
        // Energy 5
        // Stress 5 -> inverted 6.0
        // Avg = 7.0 -> Score 70
        assertEquals(70, HealthScoreCalculator.computeStateScore(mood = 5, energy = 5, stress = 5))
    }

    @Test
    fun `recomputeHealthScore computes combined health score`() {
        val baseScore = ScoreBreakdownUi(
            healthScore = 0,
            sleepScore = 0,
            hydrationScore = 0,
            activityScore = 0,
            nutritionScore = 0,
            stateScore = 0
        )
        
        val newScore = HealthScoreCalculator.recomputeHealthScore(
            scores = baseScore,
            stateScore = 80,
            sleepHours = 8f, // 100
            waterMl = 2500, // 100 target 2500
            waterTargetMl = 2500,
            stepsToday = 5000, // 50 target 10000
            stepsGoal = 10000,
            caloriesToday = 2000,
            caloriesTarget = 2000,
            moodSavedToday = true
        )

        assertEquals(100, newScore.sleepScore)
        assertEquals(100, newScore.hydrationScore)
        assertEquals(50, newScore.activityScore)
        assertEquals(80, newScore.stateScore)
        
        // Activity(50) + Sleep(100) + Hydration(100) + State(80) = 330 / 4 = 82
        assertEquals(82, newScore.healthScore)
    }
}
