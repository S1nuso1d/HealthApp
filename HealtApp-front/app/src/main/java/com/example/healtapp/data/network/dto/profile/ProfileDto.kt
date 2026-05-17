package com.example.healtapp.data.network.dto.profile

import com.google.gson.annotations.SerializedName

data class ProfileDto(
    val id: Int,
    val user_id: Int,
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
    @SerializedName("has_avatar")
    val hasAvatar: Boolean = false,
)