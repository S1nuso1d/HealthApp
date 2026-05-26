package com.example.healtapp.features.meal

import com.example.healtapp.features.meal.util.FatSecretParse
import com.example.healtapp.features.meal.util.FatSecretServingOption
import com.google.gson.JsonObject

object DishIngredientFactory {

    fun fromFoodDetailJson(detailJson: JsonObject, foodId: String? = null): DishIngredient? {
        val parsed = FatSecretParse.parseFoodDetail(detailJson) ?: return null
        val serving = parsed.servings.firstOrNull() ?: return null
        return fromServing(parsed.foodName, serving, foodId)
    }

    fun fromServing(name: String, serving: FatSecretServingOption, foodId: String? = null): DishIngredient {
        val refG = serving.metricGrams?.takeIf { it > 0f } ?: 100f
        val to100 = 100f / refG
        return DishIngredient(
            name = name,
            foodId = foodId,
            caloriesPer100g = (serving.calories ?: 0f) * to100,
            proteinPer100g = (serving.proteinG ?: 0f) * to100,
            fatPer100g = (serving.fatG ?: 0f) * to100,
            carbsPer100g = (serving.carbsG ?: 0f) * to100,
            grams = 0f,
        )
    }

    fun templatesForApply(templates: List<DishIngredient>): List<DishIngredient> =
        templates.map { t ->
            if (t.isTemplate) {
                t.copy(grams = if (t.grams > 0f) t.grams else 100f)
            } else {
                t.copy(grams = if (t.grams > 0f) t.grams else 100f)
            }
        }
}
