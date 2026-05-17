package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.wellness.ActionPlanDto
import com.example.healtapp.data.network.dto.wellness.AIBriefDto
import com.example.healtapp.data.network.dto.wellness.AIResponseDto
import com.example.healtapp.data.network.dto.wellness.AnalysisRunDto
import com.example.healtapp.data.network.dto.wellness.AnalyticsResponseDto
import com.example.healtapp.data.network.dto.wellness.DashboardHomeDto
import com.example.healtapp.data.network.dto.wellness.InsightItemDto
import com.example.healtapp.data.network.dto.wellness.SmartReminderDto
import com.example.healtapp.data.network.dto.wellness.UserStateDto

interface WellnessRepository {
    suspend fun getDashboardHome(days: Int = 7): Result<DashboardHomeDto>
    suspend fun createUserState(
        mood: Int?,
        energy: Int?,
        stress: Int?,
        focus: Int?,
        notes: String?,
    ): Result<UserStateDto>
    suspend fun listUserStates(): Result<List<UserStateDto>>
    suspend fun listActionPlan(): Result<List<ActionPlanDto>>
    suspend fun generateActionPlan(limit: Int = 5): Result<Int>
    suspend fun updateActionPlanStatus(id: Int, status: String): Result<ActionPlanDto>
    suspend fun getAnalyticsOverview(days: Int = 7): Result<AnalyticsResponseDto>
    suspend fun getInsights(): Result<List<InsightItemDto>>
    suspend fun getAnalysisRuns(limit: Int = 20): Result<List<AnalysisRunDto>>
    suspend fun activeReminders(): Result<List<SmartReminderDto>>
    suspend fun completeReminder(id: Int): Result<Unit>
    suspend fun dismissReminder(id: Int): Result<Unit>
    suspend fun aiChat(question: String, periodDays: Int = 7): Result<AIResponseDto>
    suspend fun dailyBrief(days: Int = 3): Result<AIBriefDto>
    suspend fun weeklyBrief(days: Int = 7): Result<AIBriefDto>
    suspend fun explainInsight(title: String, periodDays: Int = 7): Result<AIResponseDto>
}
