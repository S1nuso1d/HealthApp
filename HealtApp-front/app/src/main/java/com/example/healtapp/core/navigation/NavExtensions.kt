package com.example.healtapp.core.navigation

import androidx.navigation.NavHostController

fun NavHostController.navigateBottomTab(route: String) {
    navigate(route) {
        popUpTo(NavRoutes.Dashboard.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.navigateFeature(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}
