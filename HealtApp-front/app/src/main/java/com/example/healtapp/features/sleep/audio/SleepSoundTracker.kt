package com.example.healtapp.features.sleep.audio

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SleepSoundTrackerState(
    val isTracking: Boolean = false,
    val sessionStartedAtEpochMs: Long? = null,
    val clipsThisSession: Int = 0,
    val isRecordingClip: Boolean = false,
    val clips: List<SleepSoundClip> = emptyList(),
)

@Singleton
class SleepSoundTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: SleepSoundStorage,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SleepSoundTrackerState> = _state.asStateFlow()

    fun refreshClips() {
        _state.update { it.copy(clips = storage.loadClips()) }
    }

    fun startTracking() {
        SleepSoundRecorderService.start(context)
        val startedAt = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_TRACKING, true)
            .putLong(KEY_SESSION_STARTED, startedAt)
            .apply()
        _state.update {
            it.copy(
                isTracking = true,
                sessionStartedAtEpochMs = startedAt,
                clipsThisSession = 0,
                isRecordingClip = false,
            )
        }
    }

    fun stopTracking() {
        SleepSoundRecorderService.stop(context)
        prefs.edit()
            .putBoolean(KEY_TRACKING, false)
            .remove(KEY_SESSION_STARTED)
            .apply()
        refreshClips()
        _state.update {
            it.copy(
                isTracking = false,
                sessionStartedAtEpochMs = null,
                clipsThisSession = 0,
                isRecordingClip = false,
                clips = storage.loadClips(),
            )
        }
    }

    fun onMonitoringTick(clipsCount: Int, isRecordingClip: Boolean) {
        _state.update {
            it.copy(
                clipsThisSession = clipsCount,
                isRecordingClip = isRecordingClip,
            )
        }
    }

    fun onClipSaved(clip: SleepSoundClip) {
        _state.update {
            it.copy(
                clips = listOf(clip) + it.clips.filterNot { c -> c.id == clip.id },
            )
        }
    }

    fun deleteClip(id: String): Boolean {
        val deleted = storage.deleteClip(id)
        if (deleted) refreshClips()
        return deleted
    }

    fun syncWithPersistedState() {
        val tracking = prefs.getBoolean(KEY_TRACKING, false)
        val startedAt = prefs.getLong(KEY_SESSION_STARTED, 0L).takeIf { it > 0L }
        _state.update {
            it.copy(
                isTracking = tracking,
                sessionStartedAtEpochMs = startedAt,
                clips = storage.loadClips(),
            )
        }
    }

    private fun loadInitialState(): SleepSoundTrackerState {
        val tracking = prefs.getBoolean(KEY_TRACKING, false)
        val startedAt = prefs.getLong(KEY_SESSION_STARTED, 0L).takeIf { it > 0L }
        return SleepSoundTrackerState(
            isTracking = tracking,
            sessionStartedAtEpochMs = startedAt,
            clips = storage.loadClips(),
        )
    }

    companion object {
        private const val PREFS_NAME = "sleep_sound_tracker"
        private const val KEY_TRACKING = "is_tracking"
        private const val KEY_SESSION_STARTED = "session_started_at"
    }
}
