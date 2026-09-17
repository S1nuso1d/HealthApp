package com.example.healtapp.data.network.dto.analytics

import com.google.gson.annotations.SerializedName

data class InfluenceFactorsResponseDto(
    @SerializedName("period_days") val periodDays: Int = 14,
    val factors: List<InfluenceFactorDto> = emptyList(),
    @SerializedName("has_enough_data") val hasEnoughData: Boolean = false,
    val message: String? = null,
)

data class InfluenceFactorDto(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val impact: String? = null,
    val confidence: Double? = null,
    val severity: String? = null,
    val strength: Int = 0,
    @SerializedName("affects_metric") val affectsMetric: String? = null,
    @SerializedName("affects_metric_title") val affectsMetricTitle: String? = null,
    @SerializedName("suggested_action") val suggestedAction: String? = null,
    val comparison: InfluenceComparisonDto? = null,
)

data class InfluenceComparisonDto(
    @SerializedName("with_factor_value") val withFactorValue: Double? = null,
    @SerializedName("with_factor_note") val withFactorNote: String? = null,
    @SerializedName("without_factor_value") val withoutFactorValue: Double? = null,
    @SerializedName("without_factor_note") val withoutFactorNote: String? = null,
    val unit: String? = null,
    @SerializedName("unit_label") val unitLabel: String? = null,
    @SerializedName("with_days") val withDays: Int? = null,
    @SerializedName("without_days") val withoutDays: Int? = null,
    @SerializedName("proof_line") val proofLine: String? = null,
)

data class AnalyticsCompareDto(
    val summary: AnalyticsCompareSummaryDto? = null,
    @SerializedName("score_deltas") val scoreDeltas: List<ScoreDeltaDto> = emptyList(),
    @SerializedName("progress_insights") val progressInsights: List<ProgressInsightDto> = emptyList(),
)

data class AnalyticsCompareSummaryDto(
    @SerializedName("previous_run_id") val previousRunId: Int? = null,
    @SerializedName("current_run_id") val currentRunId: Int? = null,
    @SerializedName("overall_trend") val overallTrend: String? = null,
    @SerializedName("health_score_delta") val healthScoreDelta: Int? = null,
)

data class ScoreDeltaDto(
    val metric: String? = null,
    @SerializedName("previous_value") val previousValue: Int? = null,
    @SerializedName("current_value") val currentValue: Int? = null,
    val delta: Int? = null,
    val trend: String? = null,
)

data class ProgressInsightDto(
    val metric: String? = null,
    val title: String? = null,
    val description: String? = null,
    val impact: String? = null,
)
