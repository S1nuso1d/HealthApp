package com.example.healtapp.core.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

object AppMotion {
    const val SHORT_MS = 220
    const val MEDIUM_MS = 360
    const val LONG_MS = 480

    fun <T> tweenShort() = tween<T>(durationMillis = SHORT_MS, easing = FastOutSlowInEasing)
    fun <T> tweenMedium() = tween<T>(durationMillis = MEDIUM_MS, easing = FastOutSlowInEasing)
}
