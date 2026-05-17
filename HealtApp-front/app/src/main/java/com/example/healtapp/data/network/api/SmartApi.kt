package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.wellness.SmartReminderDto
import com.example.healtapp.data.network.dto.wellness.SmartTriggerDto
import com.example.healtapp.data.network.dto.wellness.SmartTriggerGenerateResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SmartApi {
    @POST("smart/generate")
    suspend fun generate(@Query("period_days") periodDays: Int = 7): SmartTriggerGenerateResponseDto

    @GET("smart/triggers/active")
    suspend fun activeTriggers(): List<SmartTriggerDto>

    @GET("smart/reminders/active")
    suspend fun activeReminders(): List<SmartReminderDto>

    @PATCH("smart/reminders/{id}/complete")
    suspend fun completeReminder(@Path("id") id: Int): Map<String, String>

    @PATCH("smart/reminders/{id}/dismiss")
    suspend fun dismissReminder(@Path("id") id: Int): Map<String, String>
}
