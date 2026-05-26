package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi
import com.example.healtapp.features.dashboard.presentation.DailyBriefUi
import com.example.healtapp.features.dashboard.presentation.DashboardUiState
import com.example.healtapp.features.dashboard.presentation.ScoreBreakdownUi
import com.example.healtapp.features.dashboard.presentation.WeeklyMetricUi
import com.example.healtapp.features.dashboard.presentation.WeeklySummaryUi
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem

/** Статические данные для режима «без сервера» (демо). */
object LocalDemoData {

    fun dashboardUiState(): DashboardUiState = DashboardUiState(
            isLoading = false,
            error = null,
            isGuestMode = true,
            greetingText = "Демо-режим",
            headerSubtitle = "Пример сводки без сервера — войди в аккаунт для синхронизации",
            sleepHours = 7.5f,
            sleepTargetHours = 8f,
            sleepQuality = "78",
            waterMl = 1200,
            waterTargetMl = 2500,
            caloriesToday = 1450,
            caloriesTarget = 2200,
            caffeineToday = 50f,
            stepsToday = 8240,
            stepsGoal = 10_000,
        activityMinutesToday = 48,
        caloriesBurnedToday = 320,
        caloriesBurnGoal = 500,
        scores = ScoreBreakdownUi(
            healthScore = 72,
            sleepScore = 78,
            hydrationScore = 48,
            activityScore = 82,
            nutritionScore = 75,
            stateScore = 68,
        ),
        dailyBrief = DailyBriefUi(
            title = "Демо-сводка дня",
            summary = "Пример AI-брифа: сон близок к цели, воды пока мало — добавьте стакан.",
            keyPoints = listOf("Сон 7,5 ч", "Вода ~48% цели", "Шаги на хорошем уровне"),
        ),
        actionPlanItems = listOf(
            ActionPlanItemUi(1, "Выпить воды", "Добавьте 500 мл до обеда", "hydration", "pending", "high"),
            ActionPlanItemUi(2, "Прогулка 15 мин", "После обеда для шагов", "activity", "pending", "medium"),
        ),
        weeklySummary = WeeklySummaryUi(
            periodLabel = "12 — 18 мая",
            metrics = listOf(
                WeeklyMetricUi("sleep", "Сон", "7,4 ч / ночь", 3, 5, "Среднее за 3 дня с данными"),
                WeeklyMetricUi("water", "Вода", "1 800 мл / день", 4, 5, "Среднее за 4 дня с данными"),
                WeeklyMetricUi("steps", "Шаги", "8 200 / день", 5, 5, "Среднее за 5 дней с данными"),
                WeeklyMetricUi("calories", "Калории", "2 100 ккал / день", 3, 5, "Среднее за 3 дня с данными"),
            ),
        ),
        waterStreakDays = 2,
        stepsStreakDays = 3,
    )

    fun recommendationItems(): List<RecommendationUiItem> = listOf(
        RecommendationUiItem(
            category = "hydration",
            title = "Выпей ещё воды",
            description = "В демо-режиме запросы к серверу не выполняются — это пример рекомендации.",
            priority = "medium",
            status = "active",
            confidence = 0.82f,
            action = "add_water",
            personalizedTip = "Поставь бутылку на стол — так проще не забыть.",
            progressLabel = "≈ 48% от дневной цели",
        ),
        RecommendationUiItem(
            category = "sleep",
            title = "Ложись чуть раньше",
            description = "Стабильный отход ко сну помогает восстановлению.",
            priority = "low",
            status = "active",
            confidence = 0.71f,
            action = "sleep_window",
            personalizedTip = "Попробуй за 30 минут до сна приглушить экраны.",
            progressLabel = "Цель: 8 ч",
        ),
        RecommendationUiItem(
            category = "activity",
            title = "Прогулка в обед",
            description = "Короткая активность улучшает концентрацию во второй половине дня.",
            priority = "medium",
            status = "active",
            confidence = 0.65f,
            action = "walk",
            personalizedTip = "15–20 минут пешком уже заметны для шагов.",
            progressLabel = "Шаги: демо-значение",
        ),
    )
}
