package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.meal.CopyDayResponseDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto

interface MealRepository {
    suspend fun getTodayMeal(): Result<MealDto?>
    suspend fun getMealHistory(): Result<List<MealDto>>
    suspend fun createMeal(request: MealCreateRequestDto): Result<MealDto>
    suspend fun updateMeal(id: Int, request: MealCreateRequestDto): Result<MealDto>
    suspend fun deleteMeal(id: Int): Result<Unit>

    suspend fun listSavedDishes(): Result<List<SavedDishDto>>
    suspend fun createSavedDish(body: SavedDishCreateRequestDto): Result<SavedDishDto>
    suspend fun deleteSavedDish(id: Int): Result<Unit>
    suspend fun copyMealsFromDay(sourceDateIso: String, targetDateIso: String? = null): Result<CopyDayResponseDto>
}
