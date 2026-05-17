package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ActionPlanApi
import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.api.AnalyticsApi
import com.example.healtapp.data.network.api.DashboardApi
import com.example.healtapp.data.network.api.SmartApi
import com.example.healtapp.data.network.api.StatesApi
import com.example.healtapp.data.network.dto.wellness.ActionPlanStatusUpdateDto
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import com.example.healtapp.data.network.dto.wellness.AIExplainInsightRequestDto
import com.example.healtapp.data.network.dto.wellness.UserStateCreateDto
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.domain.repository.WellnessRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellnessRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val statesApi: StatesApi,
    private val actionPlanApi: ActionPlanApi,
    private val analyticsApi: AnalyticsApi,
    private val smartApi: SmartApi,
    private val aiApi: AiApi,
    private val dashboardCache: DashboardCache,
) : WellnessRepository {

    override suspend fun getDashboardHome(days: Int) = runCatching {
        dashboardApi.getHome(days).also { dashboardCache.save(it) }
    }.recoverCatching {
        dashboardCache.load() ?: throw it
    }

    override suspend fun createUserState(
        mood: Int?,
        energy: Int?,
        stress: Int?,
        focus: Int?,
        notes: String?,
    ) = runCatching {
        statesApi.createState(
            UserStateCreateDto(
                mood = mood,
                energy = energy,
                stress = stress,
                focus = focus,
                recordTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                notes = notes,
            ),
        )
    }

    override suspend fun listUserStates() = runCatching { statesApi.listStates() }

    override suspend fun listActionPlan() = runCatching { actionPlanApi.list() }

    override suspend fun generateActionPlan(limit: Int) = runCatching {
        actionPlanApi.generate(limit = limit).createdCount
    }

    override suspend fun updateActionPlanStatus(id: Int, status: String) = runCatching {
        actionPlanApi.updateStatus(id, ActionPlanStatusUpdateDto(status))
    }

    override suspend fun getAnalyticsOverview(days: Int) =
        runCatching { analyticsApi.getOverview(days) }

    override suspend fun getInsights() = runCatching { analyticsApi.getInsights() }

    override suspend fun getAnalysisRuns(limit: Int) =
        runCatching { analyticsApi.getRuns(limit) }

    override suspend fun activeReminders() =
        runCatching { smartApi.activeReminders() }

    override suspend fun completeReminder(id: Int) = runCatching {
        smartApi.completeReminder(id)
        Unit
    }

    override suspend fun dismissReminder(id: Int) = runCatching {
        smartApi.dismissReminder(id)
        Unit
    }

    override suspend fun aiChat(question: String, periodDays: Int) = runCatching {
        aiApi.chat(AIChatRequestDto(question = question, periodDays = periodDays))
    }

    override suspend fun dailyBrief(days: Int) = runCatching { aiApi.dailyBrief(days) }

    override suspend fun weeklyBrief(days: Int) = runCatching { aiApi.weeklyBrief(days) }

    override suspend fun explainInsight(title: String, periodDays: Int) = runCatching {
        aiApi.explainInsight(AIExplainInsightRequestDto(insightTitle = title, periodDays = periodDays))
    }
}
