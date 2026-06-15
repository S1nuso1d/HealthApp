package com.example.healtapp.data.network.dto.ai

import androidx.annotation.Keep

@Keep
data class SleepSoundRecordDto(
    val time: String,
    val label: String,
    val peakRms: Float? = null
)

@Keep
data class SleepSummaryRequestDto(
    val sounds: List<SleepSoundRecordDto>
)

@Keep
data class SleepSummaryResponseDto(
    val summary: String,
    val generated_at: String
)
