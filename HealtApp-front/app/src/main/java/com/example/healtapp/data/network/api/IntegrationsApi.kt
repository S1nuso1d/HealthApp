package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.integrations.FatSecretLinkDto
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface IntegrationsApi {

    @POST("integrations/fatsecret/link")
    suspend fun linkFatSecret(@Body body: FatSecretLinkDto): Map<String, String>

    @DELETE("integrations/fatsecret/link")
    suspend fun unlinkFatSecret(): Map<String, String>

    @GET("integrations/fatsecret/foods/search")
    suspend fun searchFoods(@Query("q") query: String): JsonObject

    @GET("integrations/fatsecret/foods/barcode")
    suspend fun searchBarcode(@Query("barcode") barcode: String): JsonObject

    @GET("integrations/fatsecret/food")
    suspend fun getFood(@Query("food_id") foodId: String): JsonObject
}
