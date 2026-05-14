package com.example.healtapp.features.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun updateSleepStart(value: String) {
        _uiState.value = _uiState.value.copy(sleepStartInput = value)
    }

    fun updateSleepEnd(value: String) {
        _uiState.value = _uiState.value.copy(sleepEndInput = value)
    }

    fun updateQuality(value: String) {
        _uiState.value = _uiState.value.copy(qualityInput = value)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(noteInput = value)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val sleepResult = repository.getSleepHistory()
            val profileResult = profileRepository.getMyProfile()

            sleepResult.onSuccess { sleepList ->
                val profile = profileResult.getOrNull()

                val records = sleepList.map { dto ->
                    SleepRecordUi(
                        id = dto.id,
                        date = dto.sleep_start.substring(0, 10),
                        startTime = dto.sleep_start.substring(11, 16),
                        endTime = dto.sleep_end.substring(11, 16),
                        durationHours = dto.duration_hours,
                        qualityScore = (dto.quality_score ?: 0).toInt(),
                        note = dto.notes ?: ""
                    )
                }

                val averageSleep = if (sleepList.isNotEmpty()) {
                    sleepList.map { it.duration_hours }.average().toFloat()
                } else {
                    0f
                }

                val averageQuality = if (sleepList.isNotEmpty()) {
                    val qualities = sleepList.mapNotNull { it.quality_score?.toInt() }
                    if (qualities.isNotEmpty()) qualities.average().toInt() else 0
                } else {
                    0
                }

                val targetSleep = profile?.target_sleep_hours ?: 8f

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    records = records,
                    averageSleepHours = averageSleep,
                    targetSleepHours = targetSleep,
                    sleepDebtHours = (targetSleep - averageSleep).coerceAtLeast(0f),
                    sleepQualityAverage = averageQuality,
                    consistencyPercent = if (sleepList.isNotEmpty()) 80 else 0
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось загрузить сон"
                )
            }
        }
    }

    fun saveSleepRecord() {
        viewModelScope.launch {
            val current = _uiState.value
            val quality = current.qualityInput.toIntOrNull() ?: 80

            val request = CreateSleepRequestDto(
                sleep_start = buildSleepStartIso(current.sleepStartInput),
                sleep_end = buildSleepEndIso(current.sleepStartInput, current.sleepEndInput),
                quality_score = quality,
                notes = current.noteInput.ifBlank { null },
                source = "manual"
            )

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            repository.addSleep(request)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        sleepStartInput = "23:30",
                        sleepEndInput = "07:30",
                        qualityInput = "80",
                        noteInput = ""
                    )
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось сохранить сон"
                    )
                }
        }
    }

    private fun buildSleepStartIso(start: String): String {
        val date = LocalDate.now().minusDays(1)
        val time = parseTime(start)
        return LocalDateTime.of(date, time).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun buildSleepEndIso(start: String, end: String): String {
        val startTime = parseTime(start)
        val endTime = parseTime(end)

        val endDate = if (endTime.isAfter(startTime)) {
            LocalDate.now().minusDays(1)
        } else {
            LocalDate.now()
        }

        return LocalDateTime.of(endDate, endTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun parseTime(value: String): LocalTime {
        return try {
            LocalTime.parse(value)
        } catch (_: Exception) {
            LocalTime.of(23, 30)
        }
    }
}