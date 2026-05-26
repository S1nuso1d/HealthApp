package com.example.healtapp.features.achievements.data

import android.content.Context
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.auth.ApplicationScope
import com.example.healtapp.data.network.dto.gamification.AchievementRecentDto
import com.example.healtapp.data.network.dto.gamification.AchievementsResponseDto
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.GamificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class AchievementUnlockNotifier @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: GamificationRepository,
    private val tokenStorage: TokenStorage,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUnlock = MutableStateFlow<AchievementRecentDto?>(null)
    val currentUnlock: StateFlow<AchievementRecentDto?> = _currentUnlock.asStateFlow()

    private val queue = ArrayDeque<AchievementRecentDto>()
    private val checkMutex = Mutex()
    private var debounceJob: Job? = null

    init {
        scope.launch {
            AppRefreshBus.events.collect { scheduleCheck() }
        }
        scope.launch {
            AppRefreshBus.sessionExpired.collect { resetSession() }
        }
        scheduleCheck(delayMs = 1_200L)
    }

    fun scheduleCheck(delayMs: Long = 700L) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            runCheck()
        }
    }

    fun dismissCurrent() {
        val current = _currentUnlock.value ?: return
        markShown(current)
        _currentUnlock.value = null
        scope.launch {
            delay(350)
            showNextFromQueue()
        }
    }

    private suspend fun runCheck() {
        if (tokenStorage.isGuestMode() || tokenStorage.getToken().isNullOrBlank()) return
        checkMutex.withLock {
            repository.getMyAchievements()
                .onSuccess { dto -> processResponse(dto) }
        }
    }

    private fun processResponse(dto: AchievementsResponseDto) {
        if (!prefs.getBoolean(KEY_BASELINE_DONE, false)) {
            val keys = buildKnownKeys(dto)
            prefs.edit()
                .putStringSet(KEY_SHOWN, keys)
                .putBoolean(KEY_BASELINE_DONE, true)
                .apply()
            return
        }

        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        val fresh = dto.recent.filter { isRecentEnough(it) && unlockKey(it) !in shown }
        if (fresh.isEmpty()) return

        fresh.forEach { item ->
            val key = unlockKey(item)
            if (key !in shown && queue.none { unlockKey(it) == key }) {
                queue.addLast(item)
                shown.add(key)
            }
        }
        prefs.edit().putStringSet(KEY_SHOWN, shown).apply()
        if (_currentUnlock.value == null) {
            scope.launch { showNextFromQueue() }
        }
    }

    private fun showNextFromQueue() {
        val next = queue.removeFirstOrNull() ?: return
        _currentUnlock.value = next
    }

    private fun markShown(item: AchievementRecentDto) {
        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        shown.add(unlockKey(item))
        prefs.edit().putStringSet(KEY_SHOWN, shown).apply()
    }

    private fun resetSession() {
        debounceJob?.cancel()
        queue.clear()
        _currentUnlock.value = null
        prefs.edit()
            .remove(KEY_SHOWN)
            .remove(KEY_BASELINE_DONE)
            .apply()
    }

    private fun buildKnownKeys(dto: AchievementsResponseDto): Set<String> =
        dto.achievements
            .filter { it.unlocked }
            .map { "${it.code}|${it.unlocked_at.orEmpty()}" }
            .toSet()

    private fun unlockKey(item: AchievementRecentDto): String =
        "${item.code}|${item.unlocked_at.orEmpty()}"

    private fun isRecentEnough(item: AchievementRecentDto): Boolean {
        val unlockedAt = item.unlocked_at ?: return false
        return runCatching {
            val parsed = OffsetDateTime.parse(unlockedAt).withOffsetSameInstant(ZoneOffset.UTC)
            ChronoUnit.MINUTES.between(parsed, OffsetDateTime.now(ZoneOffset.UTC)) in 0L..30L
        }.getOrDefault(false)
    }

    companion object {
        private const val PREFS_NAME = "achievement_unlock_notifier"
        private const val KEY_SHOWN = "shown_unlock_keys"
        private const val KEY_BASELINE_DONE = "baseline_initialized"
    }
}
