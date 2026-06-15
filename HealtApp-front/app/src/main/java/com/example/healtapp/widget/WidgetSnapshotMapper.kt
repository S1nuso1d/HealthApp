package com.example.healtapp.widget

import com.example.healtapp.data.preferences.WidgetSnapshot
import com.example.healtapp.features.dashboard.presentation.DashboardUiState

fun DashboardUiState.toWidgetSnapshot(): WidgetSnapshot = WidgetSnapshot(
    stepsToday = stepsToday,
    stepsGoal = stepsGoal,
    waterMl = waterMl,
    waterGoalMl = waterTargetMl,
    healthScore = scores.healthScore,
    sleepHours = sleepHours,
    sleepTargetHours = sleepTargetHours,
    caloriesToday = caloriesToday,
    caloriesTarget = caloriesTarget,
    briefTitle = dailyBrief?.title.orEmpty(),
)
