package com.example.healtapp.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

object AppDestinations {

    val bottomNavItems = listOf(
        BottomNavItem(
            route = NavRoutes.Dashboard.route,
            title = "Главная",
            icon = Icons.Filled.MonitorHeart
        ),
        BottomNavItem(
            route = NavRoutes.Sleep.route,
            title = "Сон",
            icon = Icons.Filled.Bedtime
        ),
        BottomNavItem(
            route = NavRoutes.Nutrition.route,
            title = "Питание",
            icon = Icons.Filled.Restaurant
        ),
        BottomNavItem(
            route = NavRoutes.Activity.route,
            title = "Активность",
            icon = Icons.AutoMirrored.Filled.DirectionsWalk
        ),
        BottomNavItem(
            route = NavRoutes.Profile.route,
            title = "Профиль",
            icon = Icons.Filled.Person
        )
    )
}