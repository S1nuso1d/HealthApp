package com.example.healtapp.core.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AppMotion {
    const val SHORT_MS = 220
    const val MEDIUM_MS = 360
    const val LONG_MS = 480
    const val STAGGER_MS = 45

    fun <T> tweenShort() = tween<T>(durationMillis = SHORT_MS, easing = FastOutSlowInEasing)
    fun <T> tweenMedium() = tween<T>(durationMillis = MEDIUM_MS, easing = FastOutSlowInEasing)
    fun <T> tweenLong() = tween<T>(durationMillis = LONG_MS, easing = FastOutSlowInEasing)

    fun <T> springGentle() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> springSnappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    fun staggerDelay(index: Int): Int = (index.coerceAtLeast(0) * STAGGER_MS).coerceAtMost(360)
}
