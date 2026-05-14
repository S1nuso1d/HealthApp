package com.example.healtapp.core.navigation

sealed class NavRoute(val route: String) {
    data object Splash : NavRoute("splash")
    data object Login : NavRoute("login")
    data object Register : NavRoute("register")
    data object Onboarding : NavRoute("onboarding")
    data object Dashboard : NavRoute("dashboard")
    data object Profile : NavRoute("profile")
    data object Sleep : NavRoute("sleep")
    data object Nutrition : NavRoute("nutrition")
    data object Hydration : NavRoute("hydration")
    data object Activity : NavRoute("activity")
    data object Recommendations : NavRoute("recommendations")
    data object Timeline : NavRoute("timeline")
    data object ActionPlan : NavRoute("action_plan")
}

object NavRoutes {
    val Splash = NavRoute.Splash
    val Login = NavRoute.Login
    val Register = NavRoute.Register
    val Onboarding = NavRoute.Onboarding
    val Dashboard = NavRoute.Dashboard
    val Profile = NavRoute.Profile
    val Sleep = NavRoute.Sleep
    val Nutrition = NavRoute.Nutrition
    val Hydration = NavRoute.Hydration
    val Activity = NavRoute.Activity
    val Recommendations = NavRoute.Recommendations
    val Timeline = NavRoute.Timeline
    val ActionPlan = NavRoute.ActionPlan
}