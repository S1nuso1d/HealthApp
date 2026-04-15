package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto

interface MealRepository {
    suspend fun getTodayMeal(): Result<MealDto?>
    suspend fun getMealHistory(): Result<List<MealDto>>
    suspend fun createMeal(request: MealCreateRequestDto): Result<MealDto>
}