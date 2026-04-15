package com.example.healtapp.mock

data class MockSleepRecord(
    val date: String,
    val hours: Float
)

val mockSleepRecords = listOf(
    MockSleepRecord("01.04.2026", 7.5f),
    MockSleepRecord("31.03.2026", 6.8f),
    MockSleepRecord("30.03.2026", 8.1f)
)