package com.example.healtapp.data.sync

import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingSyncFlusher @Inject constructor(
    private val store: PendingSyncStore,
    private val hydrationRepository: HydrationRepository,
    private val mealRepository: MealRepository,
) {
    suspend fun flush(): Boolean {
        val queue = store.load()
        if (queue.hydration.isEmpty() && queue.meals.isEmpty()) return false

        var remainingHydration = queue.hydration
        var remainingMeals = queue.meals

        for (op in queue.hydration) {
            val ok = hydrationRepository.addHydration(op.amountMl).isSuccess
            if (ok) {
                remainingHydration = remainingHydration.drop(1)
            } else {
                break
            }
        }

        for (op in queue.meals) {
            val req = MealCreateRequestDto(
                meal_type = op.mealType,
                name = op.name,
                calories = op.calories,
                protein_g = op.proteinG,
                fat_g = op.fatG,
                carbs_g = op.carbsG,
                meal_time = java.time.LocalDateTime.now().toString(),
            )
            val ok = mealRepository.createMeal(req).isSuccess
            if (ok) {
                remainingMeals = remainingMeals.drop(1)
            } else {
                break
            }
        }

        store.save(
            com.example.healtapp.data.preferences.PendingSyncQueue(
                hydration = remainingHydration,
                meals = remainingMeals,
            ),
        )
        return remainingHydration.size < queue.hydration.size || remainingMeals.size < queue.meals.size
    }
}
