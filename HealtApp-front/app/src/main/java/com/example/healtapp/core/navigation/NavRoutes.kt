package com.example.healtapp.core.navigation

sealed class NavRoute(val route: String) {
    data object Splash : NavRoute("splash")
    data object Login : NavRoute("login")
    data object Register : NavRoute("register")
    data object ForgotPassword : NavRoute("forgot_password")
    data object RegisterSetup : NavRoute("register_setup")
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
    data object DataPrivacy : NavRoute("data_privacy")
    data object DataImport : NavRoute("data_import")
    data object Integrations : NavRoute("integrations")
    data object MiBandBle : NavRoute("miband_ble")
    data object GoalsCalendar : NavRoute("goals_calendar")
    data object ServerConnection : NavRoute("server_connection")
    data object HealthVitals : NavRoute("health_vitals")
    data object Notifications : NavRoute("notifications")
    data object AiAssistant : NavRoute("ai_assistant")
    data object Achievements : NavRoute("achievements")
    data object Friends : NavRoute("friends")
    data object FriendProfile : NavRoute("friend_profile/{userId}") {
        fun route(userId: Int) = "friend_profile/$userId"
    }
}

object NavRoutes {
    val Splash = NavRoute.Splash
    val Login = NavRoute.Login
    val Register = NavRoute.Register
    val ForgotPassword = NavRoute.ForgotPassword
    val RegisterSetup = NavRoute.RegisterSetup
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
    val DataPrivacy = NavRoute.DataPrivacy
    val DataImport = NavRoute.DataImport
    val Integrations = NavRoute.Integrations
    val MiBandBle = NavRoute.MiBandBle
    val GoalsCalendar = NavRoute.GoalsCalendar
    val ServerConnection = NavRoute.ServerConnection
    val HealthVitals = NavRoute.HealthVitals
    val Notifications = NavRoute.Notifications
    val AiAssistant = NavRoute.AiAssistant
    val Achievements = NavRoute.Achievements
    val Friends = NavRoute.Friends
    val FriendProfile = NavRoute.FriendProfile
}