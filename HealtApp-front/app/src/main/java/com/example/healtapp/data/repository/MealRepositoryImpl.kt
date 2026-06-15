package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.MealApi
import com.example.healtapp.data.network.dto.meal.CopyDayRequestDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogUpsertRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.domain.repository.MealRepository
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    override suspend fun updateSavedDish(id: Int, body: SavedDishCreateRequestDto) =
        runCatching { api.updateSavedDish(id, body) }

    override suspend fun deleteSavedDish(id: Int) = runCatching {
        api.deleteSavedDish(id)
        Unit
    }

    override suspend fun copyMealsFromDay(sourceDateIso: String, targetDateIso: String?) =
        runCatching {
            api.copyDay(CopyDayRequestDto(source_date = sourceDateIso, target_date = targetDateIso))
        }

    override suspend fun searchFoodCatalog(query: String): Result<List<FoodCatalogItemDto>> =
        runCatching { api.searchFoodCatalog(query).items }

    override suspend fun getFoodByBarcode(barcode: String): Result<FoodCatalogItemDto> =
        runCatching { api.getFoodByBarcode(barcode) }

    override suspend fun createFoodCatalogItem(
        name: String,
        barcode: String?,
        brand: String?,
        calories100g: Float?,
        proteinG100g: Float?,
        fatG100g: Float?,
        carbsG100g: Float?,
        offImageUrl: String?,
        photoFile: File?,
    ): Result<FoodCatalogItemDto> = runCatching {
        val text = "text/plain".toMediaTypeOrNull()
        fun str(value: String?) = value?.toRequestBody(text)
        fun num(value: Float?) = value?.toString()?.toRequestBody(text)
        val photoPart = photoFile?.let { file ->
            val mime = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(mime.toMediaTypeOrNull()),
            )
        }
        api.createFoodCatalogItem(
            name = name.toRequestBody(text),
            barcode = str(barcode),
            brand = str(brand),
            calories100g = num(calories100g),
            proteinG100g = num(proteinG100g),
            fatG100g = num(fatG100g),
            carbsG100g = num(carbsG100g),
            offImageUrl = str(offImageUrl),
            file = photoPart,
        )
    }

    override suspend fun updateFoodCatalogItem(
        id: Int,
        body: FoodCatalogUpsertRequestDto,
    ): Result<FoodCatalogItemDto> = runCatching { api.updateFoodCatalogItem(id, body) }
}
