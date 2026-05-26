package com.example.healtapp.features.meal

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class DishIngredient(
    val name: String,
    val foodId: String? = null,
    /** КБЖУ на 100 г (из FatSecret), если задано — порция считается через [grams]. */
    val caloriesPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val carbsPer100g: Float = 0f,
    /** Граммы порции при добавлении в дневник; 0 в шаблоне блюда. */
    val grams: Float = 0f,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f,
) {
    val isTemplate: Boolean
        get() = caloriesPer100g > 0f || proteinPer100g > 0f || fatPer100g > 0f || carbsPer100g > 0f

    fun withScaledMacros(): DishIngredient {
        if (!isTemplate || grams <= 0f) return this
        val scale = grams / 100f
        return copy(
            calories = caloriesPer100g * scale,
            protein = proteinPer100g * scale,
            fat = fatPer100g * scale,
            carbs = carbsPer100g * scale,
        )
    }

    /** Для отображения эталона на 100 г в карточке блюда. */
    fun per100gPreview(): DishIngredient = if (isTemplate) {
        copy(
            grams = 100f,
            calories = caloriesPer100g,
            protein = proteinPer100g,
            fat = fatPer100g,
            carbs = carbsPer100g,
        )
    } else {
        this
    }
}

data class DishIngredientsPayload(
    val ingredients: List<DishIngredient> = emptyList(),
) {
    fun totals(): DishIngredient = DishIngredient(
        name = "Итого",
        calories = ingredients.sumOf { it.withScaledMacros().calories.toDouble() }.toFloat(),
        protein = ingredients.sumOf { it.withScaledMacros().protein.toDouble() }.toFloat(),
        fat = ingredients.sumOf { it.withScaledMacros().fat.toDouble() }.toFloat(),
        carbs = ingredients.sumOf { it.withScaledMacros().carbs.toDouble() }.toFloat(),
    )

    fun referencePer100g(): DishIngredient = DishIngredient(
        name = "На 100 г",
        calories = ingredients.sumOf { it.per100gPreview().calories.toDouble() }.toFloat(),
        protein = ingredients.sumOf { it.per100gPreview().protein.toDouble() }.toFloat(),
        fat = ingredients.sumOf { it.per100gPreview().fat.toDouble() }.toFloat(),
        carbs = ingredients.sumOf { it.per100gPreview().carbs.toDouble() }.toFloat(),
    )
}

object DishIngredientsJson {
    private val gson = Gson()
    private val type = object : TypeToken<DishIngredientsPayload>() {}.type

    fun encode(ingredients: List<DishIngredient>): String =
        gson.toJson(DishIngredientsPayload(ingredients))

    fun decode(notes: String?): List<DishIngredient> {
        if (notes.isNullOrBlank()) return emptyList()
        return runCatching {
            val payload: DishIngredientsPayload = gson.fromJson(notes, type)
            payload.ingredients
        }.getOrDefault(emptyList())
    }
}
