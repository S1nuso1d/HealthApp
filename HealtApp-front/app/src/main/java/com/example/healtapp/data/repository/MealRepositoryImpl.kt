package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.MealApi
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.domain.repository.MealRepository

class MealRepositoryImpl(
    private val api: MealApi
) : MealRepository {

    override suspend fun getTodayMeal(): Result<MealDto?> {
        return runCatching {
            api.getTodayMeal()
        }
    }

    override suspend fun getMealHistory(): Result<List<MealDto>> {
        return runCatching {
            api.getMealHistory()
        }
    }

    override suspend fun createMeal(
        request: MealCreateRequestDto
    ): Result<MealDto> {
        return runCatching {
            api.createMeal(request)
        }
    }
}