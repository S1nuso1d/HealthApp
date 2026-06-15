package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.meal.CopyDayResponseDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogUpsertRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import java.io.File

interface MealRepository {
    suspend fun getTodayMeal(): Result<MealDto?>
    suspend fun getMealHistory(): Result<List<MealDto>>
    suspend fun createMeal(request: MealCreateRequestDto): Result<MealDto>
    suspend fun updateMeal(id: Int, request: MealCreateRequestDto): Result<MealDto>
    suspend fun deleteMeal(id: Int): Result<Unit>

    suspend fun listSavedDishes(): Result<List<SavedDishDto>>
    suspend fun createSavedDish(body: SavedDishCreateRequestDto): Result<SavedDishDto>
    suspend fun updateSavedDish(id: Int, body: SavedDishCreateRequestDto): Result<SavedDishDto>
    suspend fun deleteSavedDish(id: Int): Result<Unit>
    suspend fun copyMealsFromDay(sourceDateIso: String, targetDateIso: String? = null): Result<CopyDayResponseDto>

    suspend fun searchFoodCatalog(query: String): Result<List<FoodCatalogItemDto>>
    suspend fun getFoodByBarcode(barcode: String): Result<FoodCatalogItemDto>
    suspend fun createFoodCatalogItem(
        name: String,
        barcode: String? = null,
        brand: String? = null,
        calories100g: Float? = null,
        proteinG100g: Float? = null,
        fatG100g: Float? = null,
        carbsG100g: Float? = null,
        offImageUrl: String? = null,
        photoFile: File? = null,
    ): Result<FoodCatalogItemDto>
    suspend fun updateFoodCatalogItem(
        id: Int,
        body: FoodCatalogUpsertRequestDto,
    ): Result<FoodCatalogItemDto>
}
