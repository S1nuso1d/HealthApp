package com.example.healtapp.data.network.dto.meal

data class MealDto(
    val id: Int,
    val user_id: Int,
    val meal_type: String,
    val name: String,
    val calories: Float? = null,
    val protein_g: Float? = null,
    val fat_g: Float? = null,
    val carbs_g: Float? = null,
    val fiber_g: Float? = null,
    val sugar_g: Float? = null,
    val caffeine_mg: Float? = null,
    val water_ml: Float? = null,
    val portion_g: Float? = null,
    val glycemic_load: Float? = null,
    val meal_category: String? = null,
    val minutes_before_sleep: Int? = null,
    val is_late_meal: Boolean? = null,
    val meal_time: String,
    val notes: String? = null,
    val source: String,
    val created_at: String? = null
)