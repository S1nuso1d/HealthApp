package com.example.healtapp.core.navigation

import androidx.navigation.NavHostController

fun NavHostController.navigateBottomTab(route: String) {
    // С главной часто пушат сон/другие экраны поверх стека. Тап по «Главная»
    // должен вернуть на корень, а не зависнуть на вложенном экране.
    if (route == NavRoutes.Dashboard.route) {
        val popped = popBackStack(NavRoutes.Dashboard.route, inclusive = false)
        if (!popped && currentDestination?.route != NavRoutes.Dashboard.route) {
            navigate(NavRoutes.Dashboard.route) {
                launchSingleTop = true
            }
        }
        return
    }
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
    }
}
