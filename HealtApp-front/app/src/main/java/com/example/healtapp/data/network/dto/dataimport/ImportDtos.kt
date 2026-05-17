package com.example.healtapp.data.network.dto.dataimport

import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.hydration.CreateHydrationRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto

data class ImportBatchRequestDto(
    val hydration: List<CreateHydrationRequestDto> = emptyList(),
    val meals: List<MealCreateRequestDto> = emptyList(),
    val sleeps: List<CreateSleepRequestDto> = emptyList(),
    val activities: List<ActivityCreateRequestDto> = emptyList(),
    val health_samples: List<HealthSampleCreateDto> = emptyList(),
)

data class ImportBatchResponseDto(
    val hydration_created: Int = 0,
    val meals_created: Int = 0,
    val sleeps_created: Int = 0,
    val activities_created: Int = 0,
    val health_samples_created: Int = 0,
    val errors: List<String> = emptyList(),
)

data class CsvImportRequestDto(
    val text: String,
)
