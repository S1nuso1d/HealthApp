package com.example.healtapp.core.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Сигнал об изменении очереди офлайн-записей. */
object AppPendingSyncBus {
    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Unit> = _events

    fun notifyQueueChanged() {
        _events.tryEmit(Unit)
    }
}
