package com.example.healtapp.features.sleep.presentation

data class SleepRecordUi(
    val id: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationHours: Float,
    val qualityScore: Int,
    val note: String = ""
)

data class SleepUiState(
    val averageSleepHours: Float = 0f,
    val targetSleepHours: Float = 8.0f,
    val sleepDebtHours: Float = 0f,
    val consistencyPercent: Int = 0,
    val sleepQualityAverage: Int = 0,

    val sleepStartInput: String = "23:30",
    val sleepEndInput: String = "07:30",
    val qualityInput: String = "80",
    val noteInput: String = "",

    val records: List<SleepRecordUi> = emptyList(),

    val insightText: String = "Записывай сон регулярно, чтобы приложение могло строить аналитику.",
    val isLoading: Boolean = false,
    val error: String? = null
)