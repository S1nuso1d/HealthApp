package com.example.healtapp.data.network.dto.cycle

import com.google.gson.annotations.SerializedName

data class CycleInsightsDto(
    @SerializedName("average_cycle_length") val averageCycleLength: Int = 28,
    @SerializedName("average_period_length") val averagePeriodLength: Int = 5,
    @SerializedName("tracked_cycles") val trackedCycles: Int = 0,
    @SerializedName("current_phase") val currentPhase: String? = null,
    @SerializedName("current_phase_title") val currentPhaseTitle: String? = null,
    @SerializedName("cycle_day") val cycleDay: Int? = null,
    @SerializedName("recovery_tip") val recoveryTip: String? = null,
    @SerializedName("phase_stats") val phaseStats: List<CyclePhaseStatDto> = emptyList(),
    val observations: List<CycleObservationDto> = emptyList(),
    @SerializedName("has_enough_data") val hasEnoughData: Boolean = false,
    val message: String? = null,
)

data class CyclePhaseStatDto(
    val phase: String? = null,
    @SerializedName("phase_title") val phaseTitle: String? = null,
    val days: Int = 0,
    @SerializedName("sleep_hours") val sleepHours: Double? = null,
    @SerializedName("state_score") val stateScore: Double? = null,
    val steps: Double? = null,
    @SerializedName("water_ml") val waterMl: Double? = null,
    @SerializedName("activity_minutes") val activityMinutes: Double? = null,
    val workouts: Double? = null,
)

data class CycleObservationDto(
    val phase: String? = null,
    @SerializedName("phase_title") val phaseTitle: String? = null,
    val metric: String? = null,
    val delta: Double? = null,
    val impact: String? = null,
    val text: String? = null,
)
