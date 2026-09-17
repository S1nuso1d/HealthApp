package com.example.healtapp.data.network.dto.export

import com.google.gson.annotations.SerializedName

data class ExportReportDto(
    val meta: ExportMetaDto? = null,
    val profile: ExportProfileDto? = null,
    val scores: Map<String, Int>? = null,
    val daily: List<ExportDayDto> = emptyList(),
)

data class ExportMetaDto(
    @SerializedName("generated_at") val generatedAt: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("period_days") val periodDays: Int? = null,
)

data class ExportProfileDto(
    val age: Int? = null,
    val sex: String? = null,
    @SerializedName("height_cm") val heightCm: Float? = null,
    @SerializedName("weight_kg") val weightKg: Float? = null,
    val goal: String? = null,
    @SerializedName("target_sleep_hours") val targetSleepHours: Float? = null,
    @SerializedName("target_water_ml") val targetWaterMl: Float? = null,
    @SerializedName("target_steps") val targetSteps: Int? = null,
    @SerializedName("target_daily_calories") val targetDailyCalories: Float? = null,
)

data class ExportDayDto(
    val date: String? = null,
    @SerializedName("sleep_hours") val sleepHours: Float? = null,
    @SerializedName("water_ml") val waterMl: Float? = null,
    val calories: Float? = null,
    @SerializedName("caffeine_mg") val caffeineMg: Float? = null,
    val steps: Int? = null,
    @SerializedName("active_minutes") val activeMinutes: Int? = null,
    val workouts: Int? = null,
    @SerializedName("state_score") val stateScore: Float? = null,
)
