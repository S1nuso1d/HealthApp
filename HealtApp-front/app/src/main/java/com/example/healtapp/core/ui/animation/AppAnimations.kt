package com.example.healtapp.core.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay

object AppAnimations {
    fun fadeSlideUpEnter(offsetFraction: Float = 5f): EnterTransition =
        fadeIn(AppMotion.tweenMedium()) + slideInVertically(AppMotion.tweenMedium()) { full -> full / offsetFraction.toInt() }

    fun fadeSlideUpExit(offsetFraction: Float = 5f): ExitTransition =
        fadeOut(AppMotion.tweenShort()) + slideOutVertically(AppMotion.tweenShort()) { full -> full / offsetFraction.toInt() }

    fun fadeSlideInFromEnd(): EnterTransition =
        fadeIn(AppMotion.tweenMedium()) + slideInHorizontally(AppMotion.tweenMedium()) { full -> full / 4 }

    fun fadeSlideOutToStart(): ExitTransition =
        fadeOut(AppMotion.tweenShort()) + slideOutHorizontally(AppMotion.tweenShort()) { full -> -full / 4 }

    fun fadeSlideInFromStart(): EnterTransition =
        fadeIn(AppMotion.tweenMedium()) + slideInHorizontally(AppMotion.tweenMedium()) { full -> -full / 4 }

    fun fadeSlideOutToEnd(): ExitTransition =
        fadeOut(AppMotion.tweenShort()) + slideOutHorizontally(AppMotion.tweenShort()) { full -> full / 4 }

    fun expandFadeEnter(): EnterTransition =
        fadeIn(AppMotion.tweenMedium()) + expandVertically(AppMotion.tweenMedium())

    fun shrinkFadeExit(): ExitTransition =
        fadeOut(AppMotion.tweenShort()) + shrinkVertically(AppMotion.tweenShort())

    fun tabCrossfade(): ContentTransform =
        fadeIn(AppMotion.tweenMedium()) togetherWith fadeOut(AppMotion.tweenShort())

    fun horizontalSlide(): ContentTransform =
        (fadeIn(AppMotion.tweenMedium()) + slideInHorizontally(AppMotion.tweenMedium()) { it / 4 })
            .togetherWith(fadeOut(AppMotion.tweenShort()) + slideOutHorizontally(AppMotion.tweenShort()) { -it / 4 })
}

@Composable
fun AppAppearOnce(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = AppAnimations.fadeSlideUpEnter(),
        exit = ExitTransition.None,
    ) {
        content()
    }
}

@Composable
fun AppAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = AppAnimations.fadeSlideUpEnter(),
        exit = AppAnimations.fadeSlideUpExit(),
    ) {
        content()
    }
}

@Composable
fun <T> AppTabAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "appTab",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { AppAnimations.tabCrossfade() },
        label = label,
        content = { state -> content(state) },
    )
}

@Composable
fun <T> AppPaneAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "appPane",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val forward = targetState.hashCode() >= initialState.hashCode()
            val dir = if (forward) 1 else -1
            (fadeIn(AppMotion.tweenMedium()) + slideInHorizontally(AppMotion.tweenMedium()) { full -> full / 3 * dir })
                .togetherWith(fadeOut(AppMotion.tweenShort()) + slideOutHorizontally(AppMotion.tweenShort()) { full -> -full / 3 * dir })
        },
        label = label,
        content = { state -> content(state) },
    )
}

fun Modifier.appPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = AppMotion.springSnappy(),
        label = "appPressScale",
    )
    scale(scale)
}

fun Modifier.appSelectionScale(selected: Boolean, selectedScale: Float = 1.04f): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (selected) selectedScale else 1f,
        animationSpec = AppMotion.springGentle(),
        label = "appSelectionScale",
    )
    scale(scale)
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.appListItemModifier(): Modifier =
    Modifier.animateItem(
        fadeInSpec = AppMotion.tweenMedium(),
        fadeOutSpec = AppMotion.tweenShort(),
        placementSpec = AppMotion.tweenMedium(),
    )
