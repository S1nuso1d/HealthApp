package com.example.healtapp.core.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import com.example.healtapp.core.navigation.AppDestinations
import com.example.healtapp.core.navigation.NavRoutes

object AppNavTransitions {
    private val tabRoutes = AppDestinations.bottomNavItems.map { it.route }.toSet()
    private val authRoutes = setOf(
        NavRoutes.Splash.route,
        NavRoutes.Login.route,
        NavRoutes.Register.route,
        NavRoutes.ForgotPassword.route,
        NavRoutes.Onboarding.route,
        NavRoutes.RegisterSetup.route,
    )

    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        transitionBetween(initialState.destination.route, targetState.destination.route, forward = true)
    }

    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        transitionBetweenExit(initialState.destination.route, targetState.destination.route, forward = true)
    }

    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        transitionBetween(initialState.destination.route, targetState.destination.route, forward = false)
    }

    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        transitionBetweenExit(initialState.destination.route, targetState.destination.route, forward = false)
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.transitionBetween(
        from: String?,
        to: String?,
        forward: Boolean,
    ): EnterTransition {
        if (from == null || to == null) return fadeIn(AppMotion.tweenMedium())
        return when {
            from in tabRoutes && to in tabRoutes -> fadeIn(AppMotion.tweenMedium())
            to in authRoutes || from in authRoutes -> fadeIn(AppMotion.tweenLong())
            forward -> AppAnimations.fadeSlideInFromEnd()
            else -> AppAnimations.fadeSlideInFromStart()
        }
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.transitionBetweenExit(
        from: String?,
        to: String?,
        forward: Boolean,
    ): ExitTransition {
        if (from == null || to == null) return fadeOut(AppMotion.tweenShort())
        return when {
            from in tabRoutes && to in tabRoutes -> fadeOut(AppMotion.tweenShort())
            to in authRoutes || from in authRoutes -> fadeOut(AppMotion.tweenMedium())
            forward -> AppAnimations.fadeSlideOutToStart()
            else -> AppAnimations.fadeSlideOutToEnd()
        }
    }
}
