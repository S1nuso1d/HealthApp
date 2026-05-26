package com.example.healtapp.data.network.dto.wellness

import com.google.gson.annotations.SerializedName

data class UserStateCreateDto(
    val mood: Int? = null,
    val energy: Int? = null,
    val stress: Int? = null,
    val focus: Int? = null,
    @SerializedName("record_time") val recordTime: String,
    val notes: String? = null,
)

data class UserStateDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val mood: Int? = null,
    val energy: Int? = null,
    val stress: Int? = null,
    val focus: Int? = null,
    @SerializedName("record_time") val recordTime: String,
    val notes: String? = null,
)

data class ActionPlanDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    @SerializedName("action_text") val actionText: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class ActionPlanStatusUpdateDto(
    val status: String,
)

data class ActionPlanGenerateResponseDto(
    val message: String,
    @SerializedName("created_count") val createdCount: Int,
)

data class AIBriefDto(
    val title: String,
    val summary: String,
    @SerializedName("key_points") val keyPoints: List<String>,
    @SerializedName("generated_at") val generatedAt: String,
    val source: String = "llm",
)

data class AIChatRequestDto(
    val question: String,
    @SerializedName("period_days") val periodDays: Int = 7,
)

data class AIExplainInsightRequestDto(
    @SerializedName("insight_title") val insightTitle: String,
    @SerializedName("period_days") val periodDays: Int = 7,
)

data class AIResponseDto(
    val answer: String,
    @SerializedName("generated_at") val generatedAt: String,
    val source: String = "llm",
)

data class AnalyticsMetaDto(
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("data_points") val dataPoints: Int,
    @SerializedName("has_enough_data") val hasEnoughData: Boolean,
    val message: String? = null,
)

data class AnalyticsSummaryDto(
    @SerializedName("period_days") val periodDays: Int,
    @SerializedName("health_score") val healthScore: Int,
    @SerializedName("sleep_score") val sleepScore: Int,
    @SerializedName("hydration_score") val hydrationScore: Int,
    @SerializedName("activity_score") val activityScore: Int,
    @SerializedName("nutrition_score") val nutritionScore: Int,
    @SerializedName("state_score") val stateScore: Int,
)

data class InsightItemDto(
    val category: String,
    val title: String,
    val description: String,
    val confidence: Float,
    val impact: String,
    val severity: String,
    val evidence: List<AnalyticsEvidenceDto> = emptyList(),
)

data class AnalyticsEvidenceDto(
    val metric: String,
    val value: Float,
    val unit: String? = null,
    val note: String? = null,
)

data class RecommendationItemDto(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val confidence: Float,
    val action: String? = null,
    @SerializedName("related_insight_title") val relatedInsightTitle: String? = null,
    @SerializedName("related_insight_type") val relatedInsightType: String? = null,
)

data class AnalyticsTrendsDto(
    @SerializedName("avg_sleep_hours") val avgSleepHours: Float? = null,
    @SerializedName("avg_water_ml") val avgWaterMl: Float? = null,
    @SerializedName("avg_steps") val avgSteps: Float? = null,
    @SerializedName("avg_calories") val avgCalories: Float? = null,
    @SerializedName("sleep_delta_vs_prev") val sleepDeltaVsPrev: Float? = null,
    @SerializedName("water_delta_vs_prev") val waterDeltaVsPrev: Float? = null,
    @SerializedName("steps_delta_vs_prev") val stepsDeltaVsPrev: Float? = null,
    @SerializedName("goals_met_days") val goalsMetDays: Int = 0,
    @SerializedName("days_with_data") val daysWithData: Int = 0,
)

data class AnalyticsResponseDto(
    val meta: AnalyticsMetaDto,
    val summary: AnalyticsSummaryDto,
    val insights: List<InsightItemDto> = emptyList(),
    val recommendations: List<RecommendationItemDto> = emptyList(),
    val trends: AnalyticsTrendsDto? = null,
)

data class SmartTriggerGenerateResponseDto(
    @SerializedName("triggers_created") val triggersCreated: Int,
    @SerializedName("reminders_created") val remindersCreated: Int,
)

data class SmartTriggerDto(
    val id: Int,
    @SerializedName("trigger_type") val triggerType: String,
    val category: String,
    val title: String,
    val description: String,
    val severity: String,
    val confidence: Float,
)

data class SmartReminderDto(
    val id: Int,
    @SerializedName("reminder_type") val reminderType: String,
    val title: String,
    val message: String,
    val status: String,
    @SerializedName("remind_at_label") val remindAtLabel: String? = null,
)

data class DashboardHomeDto(
    val analytics: AnalyticsResponseDto,
    @SerializedName("action_plan") val actionPlan: List<ActionPlanDto> = emptyList(),
    @SerializedName("daily_brief") val dailyBrief: AIBriefDto? = null,
    @SerializedName("smart_triggers") val smartTriggers: List<SmartTriggerItemDto> = emptyList(),
    @SerializedName("smart_reminders") val smartReminders: List<SmartReminderItemDto> = emptyList(),
)

data class SmartTriggerItemDto(
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val confidence: Float? = null,
)

data class SmartReminderItemDto(
    val type: String,
    val title: String,
    val message: String,
    @SerializedName("recommended_time") val recommendedTime: String? = null,
)

data class AnalysisRunDto(
    val id: Int,
    @SerializedName("period_days") val periodDays: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("health_score") val healthScore: Float? = null,
    val status: String,
)
