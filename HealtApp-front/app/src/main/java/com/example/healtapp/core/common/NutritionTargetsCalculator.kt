package com.example.healtapp.core.common

/**
 * Оценка суточных калорий и БЖУ (Mifflin–St Jeor + коэффициент активности).
 * Синхронно с [com.example.healtapp] backend `nutrition_targets_service`.
 */
object NutritionTargetsCalculator {

    data class Result(
        val calories: Int,
        val proteinG: Float,
        val fatG: Float,
        val carbsG: Float,
        val waterMl: Float,
        val sleepHours: Float = 8f,
        val steps: Int = 10_000,
    )

    fun calculate(
        age: Int,
        sex: String,
        heightCm: Float,
        weightKg: Float,
        activityLevel: String,
        goal: String,
    ): Result? {
        if (age < 1 || heightCm <= 0f || weightKg <= 0f) return null

        val bmr = bmrKcal(age, sex, heightCm, weightKg)
        val mult = when (activityLevel) {
            Constants.ActivityLevel.LOW -> 1.375f
            Constants.ActivityLevel.HIGH -> 1.725f
            else -> 1.55f
        }
        val tdee = bmr * mult
        val calFactor = when (goal) {
            Constants.Goals.LOSE_WEIGHT -> 0.85f
            Constants.Goals.GAIN_MUSCLE -> 1.12f
            else -> 1f
        }
        val calories = (tdee * calFactor).toInt().coerceIn(1200, 8000)

        val proteinPerKg = when (goal) {
            Constants.Goals.LOSE_WEIGHT -> 1.8f
            Constants.Goals.GAIN_MUSCLE -> 2f
            else -> 1.5f
        }
        val proteinG = (weightKg * proteinPerKg * 10).toInt() / 10f
        val fatG = ((calories * 0.28f) / 9f * 10).toInt() / 10f
        val proteinKcal = proteinG * 4f
        val carbsKcal = (calories - proteinKcal - fatG * 9f).coerceAtLeast(0f)
        val carbsG = (carbsKcal / 4f * 10).toInt() / 10f

        val waterMl = (kotlin.math.max(2000f, weightKg * 35f) / 100f).toInt() * 100f

        return Result(
            calories = calories,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            waterMl = waterMl,
        )
    }

    private fun bmrKcal(age: Int, sex: String, heightCm: Float, weightKg: Float): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * age
        return if (sex == Constants.Sex.FEMALE) base - 161f else base + 5f
    }
}
