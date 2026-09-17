package com.example.healtapp.data.network.dto.analytics

import com.google.gson.annotations.SerializedName

data class TonightRiskDto(
    @SerializedName("generated_at") val generatedAt: String? = null,
    @SerializedName("local_hour") val localHour: Int = 0,
    @SerializedName("is_evening") val isEvening: Boolean = false,
    @SerializedName("sleep_target_hours") val sleepTargetHours: Double? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("risk_score") val riskScore: Int = 0,
    val headline: String? = null,
    @SerializedName("primary_action") val primaryAction: String? = null,
    val levers: List<TonightRiskLeverDto> = emptyList(),
    @SerializedName("suggested_experiment") val suggestedExperiment: TonightSuggestedExperimentDto? = null,
)

data class TonightRiskLeverDto(
    val id: String? = null,
    val title: String? = null,
    val why: String? = null,
    val action: String? = null,
)

data class TonightSuggestedExperimentDto(
    @SerializedName("factor_id") val factorId: String? = null,
    val title: String? = null,
    val action: String? = null,
    val metric: String? = null,
)

data class HabitExperimentsResponseDto(
    val active: HabitExperimentDto? = null,
    val items: List<HabitExperimentDto> = emptyList(),
)

data class HabitExperimentDto(
    val id: Int = 0,
    @SerializedName("factor_id") val factorId: String? = null,
    val title: String? = null,
    val action: String? = null,
    val metric: String? = null,
    val status: String? = null,
    @SerializedName("started_on") val startedOn: String? = null,
    @SerializedName("ends_on") val endsOn: String? = null,
    @SerializedName("baseline_value") val baselineValue: Double? = null,
    @SerializedName("result_value") val resultValue: Double? = null,
    @SerializedName("result_summary") val resultSummary: String? = null,
    @SerializedName("days_total") val daysTotal: Int = 7,
    @SerializedName("days_elapsed") val daysElapsed: Int = 0,
    @SerializedName("days_left") val daysLeft: Int = 7,
)
