package com.example.healtapp.features.social.data

import android.content.Context
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.auth.ApplicationScope
import com.example.healtapp.data.network.dto.social.ClubNotificationDto
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.SocialRepository
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
class ClubNotificationNotifier @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: SocialRepository,
    private val tokenStorage: TokenStorage,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentNotification = MutableStateFlow<ClubNotificationDto?>(null)
    val currentNotification: StateFlow<ClubNotificationDto?> = _currentNotification.asStateFlow()

    private val queue = ArrayDeque<ClubNotificationDto>()
    private val checkMutex = Mutex()
    private var debounceJob: Job? = null

    init {
        scope.launch {
            AppRefreshBus.events.collect { scheduleCheck() }
        }
        scope.launch {
            AppRefreshBus.sessionExpired.collect { resetSession() }
        }
        scheduleCheck(delayMs = 1_500L)
    }

    fun scheduleCheck(delayMs: Long = 800L) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            runCheck()
        }
    }

    fun dismissCurrent() {
        val current = _currentNotification.value ?: return
        markShown(current)
        scope.launch {
            repository.markClubNotificationRead(current.id)
        }
        _currentNotification.value = null
        scope.launch {
            delay(350)
            showNextFromQueue()
        }
    }

    private suspend fun runCheck() {
        if (tokenStorage.getToken().isNullOrBlank()) return
        checkMutex.withLock {
            repository.getClubNotificationsRecent()
                .onSuccess { items -> processItems(items) }
        }
    }

    private fun processItems(items: List<ClubNotificationDto>) {
        if (!prefs.getBoolean(KEY_BASELINE_DONE, false)) {
            val keys = items.map { notificationKey(it) }.toSet()
            prefs.edit()
                .putStringSet(KEY_SHOWN, keys)
                .putBoolean(KEY_BASELINE_DONE, true)
                .apply()
            return
        }

        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        val fresh = items.filter { isRecentEnough(it) && notificationKey(it) !in shown }
        if (fresh.isEmpty()) return

        fresh.forEach { item ->
            val key = notificationKey(item)
            if (key !in shown && queue.none { notificationKey(it) == key }) {
                queue.addLast(item)
                shown.add(key)
            }
        }
        prefs.edit().putStringSet(KEY_SHOWN, shown).apply()
        if (_currentNotification.value == null) {
            scope.launch { showNextFromQueue() }
        }
    }

    private fun showNextFromQueue() {
        val next = queue.removeFirstOrNull() ?: return
        _currentNotification.value = next
    }

    private fun markShown(item: ClubNotificationDto) {
        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        shown.add(notificationKey(item))
        prefs.edit().putStringSet(KEY_SHOWN, shown).apply()
    }

    private fun resetSession() {
        debounceJob?.cancel()
        queue.clear()
        _currentNotification.value = null
        prefs.edit()
            .remove(KEY_SHOWN)
            .remove(KEY_BASELINE_DONE)
            .apply()
    }

    private fun notificationKey(item: ClubNotificationDto): String =
        "${item.id}|${item.created_at}"

    private fun isRecentEnough(item: ClubNotificationDto): Boolean {
        if (item.created_at.isBlank()) return false
        return runCatching {
            val parsed = OffsetDateTime.parse(item.created_at).withOffsetSameInstant(ZoneOffset.UTC)
            ChronoUnit.MINUTES.between(parsed, OffsetDateTime.now(ZoneOffset.UTC)) in 0L..30L
        }.getOrDefault(false)
    }

    companion object {
        private const val PREFS_NAME = "club_notification_notifier"
        private const val KEY_SHOWN = "shown_notification_keys"
        private const val KEY_BASELINE_DONE = "baseline_initialized"
    }
}
