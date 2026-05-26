package com.example.healtapp.notifications

import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import java.time.LocalDate

object ReminderDataChecker {

    suspend fun hasMealTypeToday(
        mealRepository: MealRepository,
        apiType: String,
    ): Boolean {
        val today = LocalDate.now().toString()
        val meals = mealRepository.getMealHistory().getOrNull().orEmpty()
        return meals.any {
            it.meal_time.take(10) == today &&
                it.meal_type.equals(apiType, ignoreCase = true)
        }
    }

    suspend fun todayWaterMl(hydrationRepository: HydrationRepository): Int {
        val summary = hydrationRepository.getTodayHydrationSummary().getOrNull()
        return summary?.total_ml ?: 0
    }

    suspend fun todaySteps(
        activityRepository: ActivityRepository,
        healthConnectReader: HealthConnectReader? = null,
    ): Int {
        val today = LocalDate.now().toString()
        val history = activityRepository.getActivityHistory().getOrNull().orEmpty()
        val fromDb = ActivityStepsHelper.sumStepsForDate(history, today)
        val fromHc = healthConnectReader?.let { reader ->
            runCatching { reader.readTodaySteps() }.getOrNull()
        }
        return fromHc?.takeIf { it > 0 } ?: fromDb
    }
}
