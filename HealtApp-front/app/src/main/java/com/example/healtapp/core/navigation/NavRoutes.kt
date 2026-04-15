package com.example.healtapp.navigation

sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object Onboarding : NavRoutes("onboarding")
    data object Dashboard : NavRoutes("dashboard")
    data object Profile : NavRoutes("profile")
    data object Sleep : NavRoutes("sleep")
    data object Nutrition : NavRoutes("nutrition")
    data object Hydration : NavRoutes("hydration")
    data object Activity : NavRoutes("activity")
    data object Recommendations : NavRoutes("recommendations")
    data object Timeline : NavRoutes("timeline")
    data object ActionPlan : NavRoutes("action_plan")
}