package com.example.healtapp.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Лёгкий «пульс» прогресса после успешного сохранения ([celebrateToken] увеличивается). */
@Composable
fun Modifier.progressCelebrateEffect(celebrateToken: Int): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(celebrateToken) {
        if (celebrateToken <= 0) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
