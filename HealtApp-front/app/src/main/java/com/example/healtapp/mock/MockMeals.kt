package com.example.healtapp.mock

data class MockMealRecord(
    val title: String,
    val time: String
)

val mockMeals = listOf(
    MockMealRecord("Капучино", "18:40"),
    MockMealRecord("Паста с курицей", "22:10"),
    MockMealRecord("Завтрак: омлет", "08:20")
)