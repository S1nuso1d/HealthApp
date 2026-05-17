package com.example.healtapp.core.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AppRefreshBus {

    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<Unit> = _events

    private val _sessionExpired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    fun notifyDataChanged() {
        _events.tryEmit(Unit)
    }

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }

    /** Явный выход из аккаунта — та же навигация, что при истёкшей сессии. */
    fun notifyLogout() {
        _sessionExpired.tryEmit(Unit)
    }
}