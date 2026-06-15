package com.example.healtapp.data.network.dto.ai

data class MealPlanResponseDto(
    val generated_at: String,
    val days: List<MealPlanDayDto>,
    val grocery_list: List<GroceryListItemDto>,
    val source: String? = "llm",
)

data class MealPlanDayDto(
    val day_name: String,
    val meals: List<MealPlanItemDto>,
    val total_calories: Int,
    val total_protein: Float,
    val total_fat: Float,
    val total_carbs: Float
)

data class MealPlanItemDto(
    val meal_type: String,
    val name: String,
    val calories: Int,
    val protein_g: Float,
    val fat_g: Float,
    val carbs_g: Float,
    val recipe: String?
)

data class GroceryListItemDto(
    val category: String,
    val name: String,
    val amount: String
)

data class WorkoutPlanResponseDto(
    val generated_at: String,
    val workouts: List<WorkoutPlanItemDto>
)

data class WorkoutPlanItemDto(
    val day_name: String,
    val workout_type: String,
    val title: String,
    val duration_minutes: Int,
    val description: String
)
