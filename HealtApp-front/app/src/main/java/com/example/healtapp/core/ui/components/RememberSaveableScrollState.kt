package com.example.healtapp.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private fun scrollHandleKey(key: String) = "app_scroll_$key"

@Composable
fun rememberSaveableScrollState(
    key: String? = null,
    initial: Int = 0,
): ScrollState {
    val scrollKey = key ?: "default"
    val entry = LocalLifecycleOwner.current as? NavBackStackEntry
    val savedHandle = entry?.savedStateHandle
    val handleKey = scrollHandleKey(scrollKey)
    val handleOffset = savedHandle?.get<Int>(handleKey)

    if (entry == null) {
        return rememberSaveable(key = scrollKey, saver = ScrollState.Saver) {
            ScrollState(initial)
        }
    }

    val scrollState = rememberSaveable(
        inputs = arrayOf(entry.id, scrollKey),
        key = "${entry.id}_$scrollKey",
        saver = ScrollState.Saver,
    ) {
        ScrollState(handleOffset ?: initial)
    }

    LaunchedEffect(entry.id, scrollKey) {
        val saved = savedHandle?.get<Int>(handleKey) ?: return@LaunchedEffect
        if (saved <= 0) return@LaunchedEffect
        snapshotFlow { scrollState.maxValue }
            .filter { it > 0 }
            .first()
        val target = saved.coerceAtMost(scrollState.maxValue)
        if (scrollState.value != target) {
            scrollState.scrollTo(target)
        }
    }

    LaunchedEffect(scrollState, savedHandle, handleKey) {
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .distinctUntilChanged()
            .collect { (offset, max) ->
                if (max > 0) {
                    savedHandle?.set(handleKey, offset)
                }
            }
    }

    return scrollState
}
