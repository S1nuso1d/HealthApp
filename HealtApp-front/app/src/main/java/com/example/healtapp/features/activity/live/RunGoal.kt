package com.example.healtapp.features.activity.live

sealed class RunGoal {
    data object Free : RunGoal()
    data class DistanceKm(val km: Float) : RunGoal()
    data class Calories(val kcal: Int) : RunGoal()
    data class BeatPrevious(
        val previousDistanceKm: Float,
        val previousDurationMin: Int,
        val label: String,
    ) : RunGoal()
}

fun RunGoal.progressFraction(
    distanceKm: Double,
    calories: Float,
): Float = when (this) {
    is RunGoal.Free -> 0f
    is RunGoal.DistanceKm -> if (km <= 0f) 0f else (distanceKm / km).toFloat().coerceIn(0f, 1f)
    is RunGoal.Calories -> if (kcal <= 0) 0f else (calories / kcal).coerceIn(0f, 1f)
    is RunGoal.BeatPrevious ->
        if (previousDistanceKm <= 0f) 0f
        else (distanceKm / previousDistanceKm).toFloat().coerceIn(0f, 1f)
}

fun RunGoal.isCompleted(distanceKm: Double, calories: Float): Boolean = when (this) {
    is RunGoal.Free -> false
    is RunGoal.DistanceKm -> distanceKm >= km
    is RunGoal.Calories -> calories >= kcal
    is RunGoal.BeatPrevious -> distanceKm >= previousDistanceKm
}

fun RunGoal.titleRu(): String = when (this) {
    is RunGoal.Free -> "Без цели"
    is RunGoal.DistanceKm -> "%.1f км".format(km)
    is RunGoal.Calories -> "$kcal ккал"
    is RunGoal.BeatPrevious -> label
}
