package com.example.healtapp.data.network.dto.meal

data class MealSummaryDto(
    val total_calories: Int = 0,
    val total_protein: Float = 0f,
    val total_fat: Float = 0f,
    val total_carbs: Float = 0f,
    val total_caffeine_mg: Float = 0f
)