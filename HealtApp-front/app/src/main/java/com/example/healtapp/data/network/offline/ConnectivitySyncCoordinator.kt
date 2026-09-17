package com.example.healtapp.data.network.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healtapp.core.common.AppPendingSyncBus
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.auth.ApplicationScope
import com.example.healtapp.data.network.dto.wellness.AIChatRequestDto
import com.example.healtapp.data.preferences.AiChatHistoryStore
import com.example.healtapp.data.preferences.StoredChatMessage
import com.example.healtapp.data.sync.PendingSyncFlusher
import com.example.healtapp.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class AiQueuedAnswer(
    val question: String,
    val userDisplayText: String,
    val answer: String,
)

@Singleton
class ConnectivitySyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val pendingSyncFlusher: PendingSyncFlusher,
    private val pendingAiChatStore: PendingAiChatStore,
    private val aiRepository: AiRepository,
) {
    private val historyStore = AiChatHistoryStore(context)
    private val _aiAnswers = MutableSharedFlow<AiQueuedAnswer>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val aiAnswers: SharedFlow<AiQueuedAnswer> = _aiAnswers

    fun start() {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { flushAll() }
                }
            },
        )
    }

    suspend fun flushAll() {
        runCatching { pendingSyncFlusher.flush() }
        AppPendingSyncBus.notifyQueueChanged()
        enqueueOfflineWorker()
        flushPendingAi()
        AppRefreshBus.notifyDataChanged()
    }

    private fun enqueueOfflineWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OfflineSyncNow",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private suspend fun flushPendingAi() {
        val pending = pendingAiChatStore.load()
        for (item in pending) {
            val request = AIChatRequestDto(question = item.question, periodDays = 14)
            val result = aiRepository.streamChat(request)
            val answer = result.getOrNull()?.let { flow ->
                buildString { flow.collect { chunk -> append(chunk) } }
            }
            if (answer.isNullOrBlank()) continue
            pendingAiChatStore.remove(item.id)
            val current = historyStore.load().orEmpty().toMutableList()
            val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            current += StoredChatMessage(nextId, true, item.userDisplayText)
            current += StoredChatMessage(nextId + 1, false, answer)
            historyStore.save(current)
            _aiAnswers.emit(
                AiQueuedAnswer(
                    question = item.question,
                    userDisplayText = item.userDisplayText,
                    answer = answer,
                ),
            )
        }
    }
}
