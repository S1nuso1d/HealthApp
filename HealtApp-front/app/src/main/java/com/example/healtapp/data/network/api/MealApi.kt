package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.meal.CopyDayRequestDto
import com.example.healtapp.data.network.dto.meal.CopyDayResponseDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogSearchResponseDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogUpsertRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MealApi {

    @GET("meal/today")
    suspend fun getTodayMeal(): MealDto?

    @GET("meal/history")
    suspend fun getMealHistory(): List<MealDto>

    @GET("meal/saved")
    suspend fun listSavedDishes(): List<SavedDishDto>

    @POST("meal/saved")
    suspend fun createSavedDish(@Body body: SavedDishCreateRequestDto): SavedDishDto

    @PUT("meal/saved/{id}")
    suspend fun updateSavedDish(
        @Path("id") id: Int,
        @Body body: SavedDishCreateRequestDto
    ): SavedDishDto

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

    @GET("meal/foods/search")
    suspend fun searchFoodCatalog(@Query("q") query: String): FoodCatalogSearchResponseDto

    @GET("meal/foods/barcode/{code}")
    suspend fun getFoodByBarcode(@Path("code") barcode: String): FoodCatalogItemDto

    @POST("meal/foods/catalog")
    @Multipart
    suspend fun createFoodCatalogItem(
        @Part("name") name: RequestBody,
        @Part("barcode") barcode: RequestBody? = null,
        @Part("brand") brand: RequestBody? = null,
        @Part("calories_100g") calories100g: RequestBody? = null,
        @Part("protein_g_100g") proteinG100g: RequestBody? = null,
        @Part("fat_g_100g") fatG100g: RequestBody? = null,
        @Part("carbs_g_100g") carbsG100g: RequestBody? = null,
        @Part("off_image_url") offImageUrl: RequestBody? = null,
        @Part file: MultipartBody.Part? = null,
    ): FoodCatalogItemDto

    @PUT("meal/foods/catalog/{id}")
    suspend fun updateFoodCatalogItem(
        @Path("id") id: Int,
        @Body body: FoodCatalogUpsertRequestDto,
    ): FoodCatalogItemDto
}
