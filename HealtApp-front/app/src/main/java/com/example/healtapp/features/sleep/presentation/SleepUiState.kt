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
    val todaySleepHours: Float = 0f,
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

    val isSoundTracking: Boolean = false,
    val soundClipsThisSession: Int = 0,
    val isRecordingSoundClip: Boolean = false,
    /** Фрагменты текущей сессии (во время отслеживания — без воспроизведения). */
    val sessionClips: List<SleepSoundClipUi> = emptyList(),
    /** Фрагменты последней завершённой сессии — можно прослушать. */
    val lastSessionClips: List<SleepSoundClipUi> = emptyList(),
    val soundClipGroups: List<SleepSoundDayGroupUi> = emptyList(),
    val canPlaybackSounds: Boolean = true,
    val playingSoundClipId: String? = null,
    val showSoundPlaybackHint: Boolean = false,
    val aiSummary: String? = null,
    val isGeneratingSummary: Boolean = false,
)

data class SleepSoundClipUi(
    val id: String,
    val dateKey: String,
    val recordedAtEpochMs: Long,
    val timeLabel: String,
    val durationLabel: String,
    val label: String,
    val filePath: String,
)

data class SleepSoundDayGroupUi(
    val dateKey: String,
    val dayLabel: String,
    val clips: List<SleepSoundClipUi>,
)
