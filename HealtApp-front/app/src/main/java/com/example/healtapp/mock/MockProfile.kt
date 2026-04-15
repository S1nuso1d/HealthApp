package com.example.healtapp.mock

data class MockProfile(
    val name: String,
    val goal: String,
    val waterTarget: Int,
    val sleepTarget: Int
)

val mockProfile = MockProfile(
    name = "Даниил",
    goal = "Улучшение сна",
    waterTarget = 2500,
    sleepTarget = 8
)