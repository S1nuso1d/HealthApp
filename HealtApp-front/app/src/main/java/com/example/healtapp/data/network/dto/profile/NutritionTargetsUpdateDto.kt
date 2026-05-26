package com.example.healtapp.data.network.dto.profile

/** Только поля питания — без null-полей остального профиля. */
data class NutritionTargetsUpdateDto(
    val target_daily_calories: Int,
    val target_protein_g: Float,
    val target_fat_g: Float,
    val target_carbs_g: Float,
)
