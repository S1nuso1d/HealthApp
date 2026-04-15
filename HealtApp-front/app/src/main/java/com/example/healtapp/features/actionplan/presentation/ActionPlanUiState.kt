package com.example.healtapp.features.actionplan.presentation

data class ActionPlanUiState(
    val tasks: List<String> = listOf(
        "Не пить кофе после 16:00",
        "Добавить 20 минут прогулки вечером",
        "Добрать 500 мл воды до 18:00"
    )
)