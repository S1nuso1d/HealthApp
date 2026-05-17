package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.meal.CopyDayRequestDto
import com.example.healtapp.data.network.dto.meal.CopyDayResponseDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MealApi {

    @GET("meal/today")
    suspend fun getTodayMeal(): MealDto?

    @GET("meal/history")
    suspend fun getMealHistory(): List<MealDto>

    @GET("meal/saved")
    suspend fun listSavedDishes(): List<SavedDishDto>

    @POST("meal/saved")
    suspend fun createSavedDish(@Body body: SavedDishCreateRequestDto): SavedDishDto

    @DELETE("meal/saved/{id}")
    suspend fun deleteSavedDish(@Path("id") id: Int): Map<String, String>

    @POST("meal/copy-day")
    suspend fun copyDay(@Body body: CopyDayRequestDto): CopyDayResponseDto

    @POST("meal/")
    suspend fun createMeal(
        @Body request: MealCreateRequestDto
    ): MealDto

    @PUT("meal/{id}")
    suspend fun updateMeal(
        @Path("id") id: Int,
        @Body request: MealCreateRequestDto
    ): MealDto

    @DELETE("meal/{id}")
    suspend fun deleteMeal(@Path("id") id: Int): Map<String, String>
}
