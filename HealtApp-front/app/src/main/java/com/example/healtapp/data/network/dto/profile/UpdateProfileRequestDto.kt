package com.example.healtapp.data.network.dto.profile

data class UpdateProfileRequestDto(
    val age: Int? = null,
    val sex: String? = null,
    val height_cm: Float? = null,
    val weight_kg: Float? = null,
    val goal: String? = null,
    val activity_level: String? = null,
    val target_sleep_hours: Float? = null,
    val target_water_ml: Float? = null,
    val target_daily_calories: Int? = null,
    val target_protein_g: Float? = null,
    val target_fat_g: Float? = null,
    val target_carbs_g: Float? = null,
    val target_steps: Int? = null,
)