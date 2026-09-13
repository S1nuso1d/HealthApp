package com.example.healtapp.features.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.DateRules
import com.example.healtapp.core.common.LocaleRu
import com.example.healtapp.core.common.UserFacingMessages
import android.media.MediaPlayer
import com.example.healtapp.data.healthconnect.HealthConnectForegroundSync
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.ai.SleepSoundRecordDto
import com.example.healtapp.data.network.dto.ai.SleepSummaryRequestDto
import com.example.healtapp.domain.repository.AiRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import com.example.healtapp.features.sleep.audio.SleepSoundClip
import com.example.healtapp.features.sleep.audio.SleepSoundStorage
import com.example.healtapp.features.sleep.audio.SleepSoundSummaryStorage
import com.example.healtapp.features.sleep.audio.SleepSoundTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val profileRepository: ProfileRepository,
    private val aiRepository: AiRepository,
    private val healthConnectForegroundSync: HealthConnectForegroundSync,
    private val sleepSoundTracker: SleepSoundTracker,
    private val sleepSoundStorage: SleepSoundStorage,
    private val sleepSoundSummaryStorage: SleepSoundSummaryStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SleepUiState(
            sleepDateInput = LocalDate.now().minusDays(1).toString(),
        ),
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        sleepSoundTracker.syncWithPersistedState()
        sleepSoundSummaryStorage.pruneToRetention()
        load()
        viewModelScope.launch {
            AppRefreshBus.events.collect { load() }
        }
        viewModelScope.launch {
            sleepSoundTracker.state.collect { trackerState ->
                val clipUi = trackerState.clips.map(::toClipUi)
                val groups = groupClipsByDay(clipUi)
                val sessionClips = if (trackerState.isTracking && trackerState.sessionStartedAtEpochMs != null) {
                    clipUi.filter { it.recordedAtEpochMs >= trackerState.sessionStartedAtEpochMs }
                } else {
                    emptyList()
                }
                _uiState.update { state ->
                    state.copy(
                        isSoundTracking = trackerState.isTracking,
                        soundClipsThisSession = trackerState.clipsThisSession,
                        isRecordingSoundClip = trackerState.isRecordingClip,
                        sessionClips = sessionClips,
                        soundClipGroups = groups,
                        canPlaybackSounds = !trackerState.isTracking,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }

    fun startSleepSoundTracking() {
        sleepSoundTracker.startTracking()
        _uiState.update {
            it.copy(
                snackMessage = "Отслеживание звуков запущено",
                showSoundPlaybackHint = false,
                lastSessionClips = emptyList(),
                aiSummary = null,
                canPlaybackSounds = false,
            )
        }
    }

    fun stopSleepSoundTracking() {
        val sessionStart = sleepSoundTracker.state.value.sessionStartedAtEpochMs ?: 0L
        val sessionCount = _uiState.value.soundClipsThisSession
        sleepSoundTracker.stopTracking()
        stopPlayback()
        val refreshed = sleepSoundStorage.loadClips().map(::toClipUi)
        val lastSession = refreshed.filter { it.recordedAtEpochMs >= sessionStart }
        _uiState.update {
            it.copy(
                snackMessage = if (lastSession.isNotEmpty()) {
                    "Запись завершена — нажмите ▶ у фрагмента, чтобы прослушать"
                } else if (sessionCount > 0) {
                    "Отслеживание остановлено"
                } else {
                    "Отслеживание остановлено"
                },
                showSoundPlaybackHint = lastSession.isNotEmpty(),
                lastSessionClips = lastSession,
                soundClipGroups = groupClipsByDay(refreshed),
                canPlaybackSounds = true,
            )
        }
        if (lastSession.isNotEmpty()) {
            generateSummary(lastSession)
        }
    }

    private fun generateSummary(clips: List<SleepSoundClipUi>) {
        if (clips.isEmpty()) return
        val dateKey = clips.maxByOrNull { it.recordedAtEpochMs }?.dateKey
            ?: SleepSoundStorage.clipDayKey(System.currentTimeMillis())
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true, aiSummary = null) }
            val request = SleepSummaryRequestDto(
                sounds = clips.map { clip ->
                    SleepSoundRecordDto(
                        time = clip.timeLabel,
                        label = clip.label,
                        peakRms = null,
                    )
                },
            )
            aiRepository.getSleepSummary(request)
                .onSuccess { res ->
                    sleepSoundSummaryStorage.save(dateKey, res.summary)
                    val refreshed = sleepSoundStorage.loadClips().map(::toClipUi)
                    _uiState.update {
                        it.copy(
                            isGeneratingSummary = false,
                            aiSummary = res.summary,
                            soundClipGroups = groupClipsByDay(refreshed),
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isGeneratingSummary = false,
                            snackMessage = "Умный анализ недоступен — показана базовая сводка по звукам",
                        )
                    }
                }
        }
    }

    fun deleteSleepSoundClip(id: String) {
        if (_uiState.value.playingSoundClipId == id) stopPlayback()
        if (sleepSoundTracker.deleteClip(id)) {
            val refreshed = sleepSoundStorage.loadClips().map(::toClipUi)
            sleepSoundSummaryStorage.deleteOrphans(refreshed.map { it.dateKey }.toSet())
            _uiState.update {
                it.copy(
                    snackMessage = "Фрагмент удалён",
                    soundClipGroups = groupClipsByDay(refreshed),
                    lastSessionClips = it.lastSessionClips.filterNot { clip -> clip.id == id },
                    sessionClips = it.sessionClips.filterNot { clip -> clip.id == id },
                )
            }
        }
    }

    fun toggleSleepSoundPlayback(clipId: String) {
        val current = _uiState.value
        if (!current.canPlaybackSounds) return
        if (current.playingSoundClipId == clipId) {
            stopPlayback()
            return
        }
        val clip = findClipById(clipId) ?: return
        stopPlayback()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(clip.filePath)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            _uiState.update { it.copy(playingSoundClipId = clipId) }
        }.onFailure {
            _uiState.update {
                it.copy(error = "Не удалось воспроизвести запись")
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        _uiState.update { it.copy(playingSoundClipId = null) }
    }

    private fun toClipUi(clip: SleepSoundClip): SleepSoundClipUi {
        val zone = ZoneId.systemDefault()
        val zoned = Instant.ofEpochMilli(clip.recordedAtEpochMs).atZone(zone)
        val time = zoned.toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm", LocaleRu))
        val seconds = (clip.durationMs / 1000).coerceAtLeast(1)
        return SleepSoundClipUi(
            id = clip.id,
            dateKey = SleepSoundStorage.clipDayKey(clip.recordedAtEpochMs, zone),
            recordedAtEpochMs = clip.recordedAtEpochMs,
            timeLabel = time,
            durationLabel = "$seconds сек",
            label = clip.label,
            filePath = sleepSoundStorage.clipFile(clip).absolutePath,
        )
    }

    private fun findClipById(id: String): SleepSoundClipUi? {
        val state = _uiState.value
        return state.lastSessionClips.find { it.id == id }
            ?: state.sessionClips.find { it.id == id }
            ?: state.soundClipGroups.flatMap { it.clips }.find { it.id == id }
    }

    private fun groupClipsByDay(clips: List<SleepSoundClipUi>): List<SleepSoundDayGroupUi> {
        if (clips.isEmpty()) return emptyList()
        val summaries = sleepSoundSummaryStorage.loadAll()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", LocaleRu)
        return clips
            .groupBy { it.dateKey }
            .entries
            .sortedByDescending { it.key }
            .map { (dateKey, dayClips) ->
                val date = runCatching { LocalDate.parse(dateKey) }.getOrElse { today }
                val label = when (date) {
                    today -> "Сегодня"
                    today.minusDays(1) -> "Вчера"
                    else -> date.format(dateFormatter)
                }
                SleepSoundDayGroupUi(
                    dateKey = dateKey,
                    dayLabel = label,
                    clips = dayClips.sortedByDescending { it.recordedAtEpochMs },
                    aiSummary = summaries[dateKey]?.summary,
                )
            }
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun updateSleepDate(value: String) {
        val date = runCatching { LocalDate.parse(value.trim()) }.getOrNull() ?: return
        if (DateRules.isFuture(date)) {
            _uiState.update {
                it.copy(error = UserFacingMessages.FUTURE_DATE_NOT_ALLOWED)
            }
            return
        }
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

                val averageSleep = SleepHelper.averageHoursLast7(records)
                val todaySleepHours = SleepHelper.todaySleepHours(records)
                val lastNightHours = SleepHelper.lastNightHours(records)
                val weekly = SleepHelper.weeklySleep(records)

                val todayKey = LocalDate.now().toString()
                val todayQualities = records
                    .filter { SleepHelper.wakeDateKey(it.sleepEndIso) == todayKey && it.qualityScore > 0 }
                    .map { it.qualityScore }
                val averageQuality = todayQualities.average().toInt().takeIf { todayQualities.isNotEmpty() } ?: 0

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
                        todaySleepHours = todaySleepHours,
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
            if (DateRules.isFutureDateString(current.sleepDateInput)) {
                _uiState.update {
                    it.copy(error = UserFacingMessages.FUTURE_DATE_NOT_ALLOWED)
                }
                return@launch
            }
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
            if (DateRules.isFutureDateString(sleepDate)) {
                _uiState.update {
                    it.copy(error = UserFacingMessages.FUTURE_DATE_NOT_ALLOWED)
                }
                return@launch
            }
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
            .let(DateRules::clampToTodayOrPast)
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
