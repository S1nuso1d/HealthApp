package com.example.healtapp.data.network.dto.profile

import com.google.gson.annotations.SerializedName

data class ProfileDto(
    val id: Int,
    val user_id: Int,
    val first_name: String? = null,
    val last_name: String? = null,
    val nickname: String? = null,
    val display_name: String? = null,
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
    val is_vegetarian: Boolean? = null,
    val has_allergies: Boolean? = null,
    val allergies_text: String? = null,
    val onboarding_completed: Boolean = false,
    val current_streak: Int = 0,
    @SerializedName("has_avatar")
    val hasAvatar: Boolean = false,
)