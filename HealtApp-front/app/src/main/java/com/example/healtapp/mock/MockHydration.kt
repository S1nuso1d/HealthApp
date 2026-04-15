package com.example.healtapp.mock

data class MockHydrationRecord(
    val amount: Int,
    val time: String
)

val mockHydrationRecords = listOf(
    MockHydrationRecord(300, "10:30"),
    MockHydrationRecord(250, "13:20"),
    MockHydrationRecord(500, "20:10")
)