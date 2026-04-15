package com.example.healtapp.mock

data class MockActivityRecord(
    val title: String,
    val duration: String
)

val mockActivities = listOf(
    MockActivityRecord("Прогулка", "45 мин"),
    MockActivityRecord("Тренировка", "50 мин"),
    MockActivityRecord("Пробежка", "35 мин")
)