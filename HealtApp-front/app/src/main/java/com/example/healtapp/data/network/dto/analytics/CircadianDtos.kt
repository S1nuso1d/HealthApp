package com.example.healtapp.data.network.dto.analytics

import com.google.gson.annotations.SerializedName

data class CircadianProfileDto(
    @SerializedName("sample_nights") val sampleNights: Int = 0,
    @SerializedName("usual_bedtime") val usualBedtime: String? = null,
    @SerializedName("usual_bedtime_hour") val usualBedtimeHour: Int? = null,
    @SerializedName("usual_bedtime_minute") val usualBedtimeMinute: Int? = null,
    @SerializedName("consistency_minutes") val consistencyMinutes: Int? = null,
    @SerializedName("notify_hour") val notifyHour: Int? = null,
    @SerializedName("notify_minute") val notifyMinute: Int? = null,
    @SerializedName("bedtime_nudge") val bedtimeNudge: String? = null,
    @SerializedName("recent_workout_warning") val recentWorkoutWarning: String? = null,
    @SerializedName("hours_before_sleep") val hoursBeforeSleep: List<HoursBeforeSleepBucketDto> = emptyList(),
)

data class HoursBeforeSleepBucketDto(
    val bucket: String? = null,
    val label: String? = null,
    val nights: Int = 0,
    @SerializedName("sleep_hours") val sleepHours: Double? = null,
)
