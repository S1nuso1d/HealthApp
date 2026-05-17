package com.example.healtapp.features.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.healthconnect.HealthConnectForegroundSync
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val profileRepository: ProfileRepository,
    private val healthConnectForegroundSync: HealthConnectForegroundSync,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SleepUiState(
            sleepDateInput = LocalDate.now().minusDays(1).toString(),
        ),
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            AppRefreshBus.events.collect { load() }
        }
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun updateSleepDate(value: String) {
        _uiState.update { it.copy(sleepDateInput = value, error = null) }
    }

    fun updateSleepStart(value: String) {
        _uiState.update { it.copy(sleepStartInput = value, error = null) }
    }

    fun updateSleepEnd(value: String) {
        _uiState.update { it.copy(sleepEndInput = value, error = null) }
    }

    fun updateQuality(value: String) {
        _uiState.update { it.copy(qualityInput = value.filter { ch -> ch.isDigit() }.take(3), error = null) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(noteInput = value, error = null) }
    }

    fun importFromHealthConnect() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isHcImporting = true, hcImportMessage = null, error = null)
            }
            healthConnectForegroundSync.importOnUserRequest()
                .onSuccess { res ->
                    val msg = buildString {
                        if (res.sleeps_created > 0) {
                            append("Добавлено записей о сне: ${res.sleeps_created}")
                        } else {
                            append("Новых ночей из Health Connect нет")
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isHcImporting = false,
                            hcImportMessage = msg,
                            snackMessage = if (res.sleeps_created > 0) msg else null,
                        )
                    }
                    load()
                    if (res.sleeps_created > 0) AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isHcImporting = false,
                            hcImportMessage = e.message ?: "Импорт не удался",
                        )
                    }
                }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val sleepResult = repository.getSleepHistory()
            val profileResult = profileRepository.getMyProfile()

            sleepResult.onSuccess { sleepList ->
                val profile = profileResult.getOrNull()
                val targetSleep = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f

                val records = sleepList.map { dto ->
                    val duration = dto.duration_hours.takeIf { it > 0f }
                        ?: SleepHelper.computeDurationHours(dto.sleep_start, dto.sleep_end)
                    SleepRecordUi(
                        id = dto.id,
                        date = SleepHelper.bedtimeDateKey(dto.sleep_start),
                        startTime = formatTimeForDisplay(dto.sleep_start),
                        endTime = formatTimeForDisplay(dto.sleep_end),
                        durationHours = duration,
                        qualityScore = (dto.quality_score ?: 0).toInt(),
                        note = dto.notes.orEmpty(),
                        sleepStartIso = dto.sleep_start,
                        sleepEndIso = dto.sleep_end,
                    )
                }.sortedByDescending { it.sleepEndIso }

                val weekly = SleepHelper.weeklySleep(records)
                val averageSleep = SleepHelper.averageHoursLast7(records)
                val lastNightHours = SleepHelper.lastNightHours(records)

                val qualities = sleepList.mapNotNull { it.quality_score?.toInt() }
                val averageQuality = if (qualities.isNotEmpty()) qualities.average().toInt() else 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        records = records,
                        weeklySleep = weekly,
                        averageSleepHours = averageSleep,
                        targetSleepHours = targetSleep,
                        sleepDebtHours = (targetSleep - averageSleep).coerceAtLeast(0f),
                        sleepQualityAverage = averageQuality,
                        lastNightHours = lastNightHours,
                        consistencyPercent = SleepHelper.consistencyPercent(records, targetSleep),
                        insightText = SleepHelper.buildInsight(
                            records = records,
                            targetHours = targetSleep,
                            averageHours = averageSleep,
                            qualityAverage = averageQuality,
                            lastNightHours = lastNightHours,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось загрузить сон",
                    )
                }
            }
        }
    }

    fun saveSleepRecord() {
        viewModelScope.launch {
            val current = _uiState.value
            val quality = current.qualityInput.toIntOrNull()?.coerceIn(0, 100) ?: 80
            val (sleepStartIso, sleepEndIso) = buildSleepStartEndIso()

            val request = CreateSleepRequestDto(
                sleep_start = sleepStartIso,
                sleep_end = sleepEndIso,
                quality_score = quality,
                notes = current.noteInput.ifBlank { null },
                source = "manual",
            )

            _uiState.update { it.copy(isSaving = true, error = null) }

            repository.addSleep(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            sleepDateInput = LocalDate.now().minusDays(1).toString(),
                            sleepStartInput = "23:30",
                            sleepEndInput = "07:30",
                            qualityInput = "80",
                            noteInput = "",
                            snackMessage = "Сон сохранён",
                        )
                    }
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Не удалось сохранить сон",
                        )
                    }
                }
        }
    }

    fun updateSleepRecord(
        id: Int,
        sleepDate: String,
        sleepStart: String,
        sleepEnd: String,
        quality: Int,
        note: String?,
    ) {
        viewModelScope.launch {
            val (startIso, endIso) = buildSleepStartEndIso(sleepDate, sleepStart, sleepEnd)
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = CreateSleepRequestDto(
                sleep_start = startIso,
                sleep_end = endIso,
                quality_score = quality.coerceIn(0, 100),
                notes = note?.ifBlank { null },
                source = "manual",
            )
            repository.updateSleep(id, request)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, snackMessage = "Запись обновлена") }
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось обновить запись",
                        )
                    }
                }
        }
    }

    fun deleteSleepRecord(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.deleteSleep(id)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, snackMessage = "Запись удалена") }
                    load()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось удалить запись",
                        )
                    }
                }
        }
    }

    private fun buildSleepStartEndIso(): Pair<String, String> {
        val state = _uiState.value
        return buildSleepStartEndIso(state.sleepDateInput, state.sleepStartInput, state.sleepEndInput)
    }

    private fun buildSleepStartEndIso(
        sleepDate: String,
        sleepStart: String,
        sleepEnd: String,
    ): Pair<String, String> {
        val day = runCatching { LocalDate.parse(sleepDate.trim()) }
            .getOrElse { LocalDate.now().minusDays(1) }
        val startTime = parseTime(sleepStart)
        val endTime = parseTime(sleepEnd)
        val startLdt = LocalDateTime.of(day, startTime)
        val endDay = if (endTime.isAfter(startTime)) day else day.plusDays(1)
        val endLdt = LocalDateTime.of(endDay, endTime)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return startLdt.format(fmt) to endLdt.format(fmt)
    }

    private fun parseTime(value: String): LocalTime =
        runCatching { LocalTime.parse(value.trim()) }.getOrElse { LocalTime.of(23, 30) }

    private fun formatTimeForDisplay(iso: String): String {
        val trimmed = iso.trim()
        return runCatching {
            java.time.OffsetDateTime.parse(trimmed).toLocalTime()
        }.getOrElse {
            runCatching { LocalDateTime.parse(trimmed.take(19)).toLocalTime() }
                .getOrElse {
                    if (trimmed.length >= 16) LocalTime.parse(trimmed.substring(11, 16))
                    else LocalTime.of(0, 0)
                }
        }.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
