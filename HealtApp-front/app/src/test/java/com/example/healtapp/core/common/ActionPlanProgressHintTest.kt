package com.example.healtapp.core.common

import com.example.healtapp.features.dashboard.presentation.ActionPlanItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionPlanProgressHintTest {

    private fun item(category: String, status: String = "pending") = ActionPlanItemUi(
        id = 1,
        title = "Тест",
        description = "",
        category = category,
        status = status,
        priority = "normal",
    )

    @Test
    fun hydrationShowsRemainingMl() {
        val label = ActionPlanProgressHint.label(
            item = item("hydration"),
            waterMl = 400,
            waterTargetMl = 2000,
            stepsToday = 0,
            stepsGoal = 8000,
            caloriesBurnedToday = 0,
            caloriesBurnGoal = 0,
            sleepHours = 0f,
            sleepTargetHours = 8f,
            caloriesToday = 0,
            caloriesTarget = 2000,
            activityMinutesToday = 0,
            moodSavedToday = false,
        )
        assertEquals("Осталось 1600 мл воды", label)
    }

    @Test
    fun doneItemReturnsNull() {
        val label = ActionPlanProgressHint.label(
            item = item("hydration", status = "done"),
            waterMl = 0,
            waterTargetMl = 2000,
            stepsToday = 0,
            stepsGoal = 8000,
            caloriesBurnedToday = 0,
            caloriesBurnGoal = 0,
            sleepHours = 0f,
            sleepTargetHours = 8f,
            caloriesToday = 0,
            caloriesTarget = 2000,
            activityMinutesToday = 0,
            moodSavedToday = false,
        )
        assertNull(label)
    }
}
