package com.example.healtapp.data.network.api

import retrofit2.http.GET
import retrofit2.http.Path

data class OpenFoodFactsResponse(
    val code: String?,
    val status: Int,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    val product_name: String?,
    val nutriments: OpenFoodFactsNutriments?
)

data class OpenFoodFactsNutriments(
    val energy_kcal_100g: Float?,
    val proteins_100g: Float?,
    val fat_100g: Float?,
    val carbohydrates_100g: Float?
)

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): OpenFoodFactsResponse
}
