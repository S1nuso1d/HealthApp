package com.example.healtapp.features.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppPendingSyncBus
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.LanReachability
import com.example.healtapp.data.network.offline.OfflineActionQueue
import com.example.healtapp.data.network.offline.PendingAiChatStore
import com.example.healtapp.data.preferences.PendingSyncStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GlobalPendingSyncViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val pendingSyncStore: PendingSyncStore,
    private val offlineActionQueue: OfflineActionQueue,
    private val pendingAiChatStore: PendingAiChatStore,
    private val apiServerConfig: ApiServerConfig,
    private val connectivitySyncCoordinator: com.example.healtapp.data.network.offline.ConnectivitySyncCoordinator,
) : ViewModel() {

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _isFlushing = MutableStateFlow(false)
    val isFlushing: StateFlow<Boolean> = _isFlushing.asStateFlow()

    private val _needsTunnel = MutableStateFlow(false)
    val needsTunnel: StateFlow<Boolean> = _needsTunnel.asStateFlow()

    init {
        viewModelScope.launch {
            refreshCount()
            AppPendingSyncBus.events.collect { refreshCount() }
        }
        viewModelScope.launch {
            AppRefreshBus.events.collect { refreshCount() }
        }
    }

    fun refreshCount() {
        viewModelScope.launch {
            val mealsHydration = pendingSyncStore.pendingCount()
            val offline = offlineActionQueue.getActions().size
            val ai = pendingAiChatStore.load().size
            _pendingCount.value = mealsHydration + offline + ai
            _needsTunnel.value = LanReachability.needsPublicTunnel(appContext, apiServerConfig.baseUrl())
        }
    }

    fun flushNow() {
        if (_isFlushing.value) return
        viewModelScope.launch {
            _isFlushing.value = true
            runCatching { connectivitySyncCoordinator.flushAll() }
            _isFlushing.value = false
            refreshCount()
        }
    }
}
