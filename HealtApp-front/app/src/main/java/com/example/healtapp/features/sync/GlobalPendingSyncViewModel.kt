package com.example.healtapp.features.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppPendingSyncBus
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.sync.PendingSyncFlusher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GlobalPendingSyncViewModel @Inject constructor(
    private val pendingSyncStore: PendingSyncStore,
    private val pendingSyncFlusher: PendingSyncFlusher,
) : ViewModel() {

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _isFlushing = MutableStateFlow(false)
    val isFlushing: StateFlow<Boolean> = _isFlushing.asStateFlow()

    init {
        viewModelScope.launch {
            refreshCount()
            AppPendingSyncBus.events.collect { refreshCount() }
            AppRefreshBus.events.collect { refreshCount() }
        }
    }

    fun refreshCount() {
        viewModelScope.launch {
            _pendingCount.value = pendingSyncStore.pendingCount()
        }
    }

    fun flushNow() {
        viewModelScope.launch {
            if (_isFlushing.value) return@launch
            _isFlushing.value = true
            pendingSyncFlusher.flush()
            refreshCount()
            AppRefreshBus.notifyDataChanged()
            _isFlushing.value = false
        }
    }
}
