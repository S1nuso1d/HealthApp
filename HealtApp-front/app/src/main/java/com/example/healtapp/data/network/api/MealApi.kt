package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MealApi {

    @GET("meal/today")
    suspend fun getTodayMeal(): MealDto?

    @GET("meal/history")
    suspend fun getMealHistory(): List<MealDto>

    @POST("meal/")
    suspend fun createMeal(
        @Body request: MealCreateRequestDto
    ): MealDto
}