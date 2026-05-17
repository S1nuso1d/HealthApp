package com.example.healtapp.data.network.dto.meal

data class SavedDishDto(
    val id: Int,
    val user_id: Int,
    val name: String,
    val meal_type: String? = null,
    val calories: Float? = null,
    val protein_g: Float? = null,
    val fat_g: Float? = null,
    val carbs_g: Float? = null,
    val notes: String? = null,
    val created_at: String? = null,
)

data class SavedDishCreateRequestDto(
    val name: String,
    val meal_type: String? = null,
    val calories: Float? = null,
    val protein_g: Float? = null,
    val fat_g: Float? = null,
    val carbs_g: Float? = null,
    val notes: String? = null,
)

data class CopyDayRequestDto(
    val source_date: String,
    val target_date: String? = null,
)

data class CopyDayResponseDto(
    val copied: Int = 0,
    val message: String? = null,
    val target_date: String? = null,
)
