package com.example.healtapp.features.sleep.presentation

data class SleepRecordUi(
    val id: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationHours: Float,
    val qualityScore: Int,
    val note: String = "",
    val sleepStartIso: String,
    val sleepEndIso: String,
)

data class SleepUiState(
    val averageSleepHours: Float = 0f,
    val targetSleepHours: Float = 8f,
    val sleepDebtHours: Float = 0f,
    val consistencyPercent: Int = 0,
    val sleepQualityAverage: Int = 0,
    val lastNightHours: Float = 0f,
    val weeklySleep: List<DaySleep> = emptyList(),

    val sleepDateInput: String = "",
    val sleepStartInput: String = "23:30",
    val sleepEndInput: String = "07:30",
    val qualityInput: String = "80",
    val noteInput: String = "",

    val records: List<SleepRecordUi> = emptyList(),
    val insightText: String = "Записывайте сон регулярно, чтобы видеть динамику за неделю.",

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,

    val isHcImporting: Boolean = false,
    val hcImportMessage: String? = null,
)
