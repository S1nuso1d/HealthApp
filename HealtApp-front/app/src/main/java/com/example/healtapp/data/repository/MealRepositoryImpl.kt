package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.MealApi
import com.example.healtapp.data.network.dto.meal.CopyDayRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.domain.repository.MealRepository
import javax.inject.Inject

class MealRepositoryImpl @Inject constructor(
    private val api: MealApi,
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
        request: MealCreateRequestDto,
    ): Result<MealDto> {
        return runCatching {
            api.createMeal(request)
        }
    }

    override suspend fun updateMeal(id: Int, request: MealCreateRequestDto): Result<MealDto> {
        return runCatching {
            api.updateMeal(id, request)
        }
    }

    override suspend fun deleteMeal(id: Int): Result<Unit> {
        return runCatching {
            api.deleteMeal(id)
            Unit
        }
    }

    override suspend fun listSavedDishes() = runCatching { api.listSavedDishes() }

    override suspend fun createSavedDish(body: SavedDishCreateRequestDto) =
        runCatching { api.createSavedDish(body) }

    override suspend fun deleteSavedDish(id: Int) = runCatching {
        api.deleteSavedDish(id)
        Unit
    }

    override suspend fun copyMealsFromDay(sourceDateIso: String, targetDateIso: String?) =
        runCatching {
            api.copyDay(CopyDayRequestDto(source_date = sourceDateIso, target_date = targetDateIso))
        }
}
