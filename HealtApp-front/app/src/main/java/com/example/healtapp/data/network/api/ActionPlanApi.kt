package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.wellness.ActionPlanDto
import com.example.healtapp.data.network.dto.wellness.ActionPlanGenerateResponseDto
import com.example.healtapp.data.network.dto.wellness.ActionPlanStatusUpdateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ActionPlanApi {
    @POST("action-plan/generate")
    suspend fun generate(
        @Query("limit") limit: Int = 5,
        @Query("replace_existing") replaceExisting: Boolean = true,
    ): ActionPlanGenerateResponseDto

    @GET("action-plan/")
    suspend fun list(): List<ActionPlanDto>

    @PATCH("action-plan/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body body: ActionPlanStatusUpdateDto,
    ): ActionPlanDto
}
