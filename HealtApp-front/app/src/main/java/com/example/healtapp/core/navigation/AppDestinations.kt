package com.example.healtapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant

object AppDestinations {
    val bottomNavItems = listOf(
        BottomNavItem("Главная", NavRoutes.Dashboard.route, Icons.Outlined.Home),
        BottomNavItem("Сон", NavRoutes.Sleep.route, Icons.Outlined.MonitorHeart),
        BottomNavItem("Питание", NavRoutes.Nutrition.route, Icons.Outlined.Restaurant),
        BottomNavItem("Вода", NavRoutes.Hydration.route, Icons.Outlined.LocalDrink),
        BottomNavItem("Активность", NavRoutes.Activity.route, Icons.Outlined.DirectionsWalk),
        BottomNavItem("AI", NavRoutes.Recommendations.route, Icons.Outlined.AutoAwesome),
        BottomNavItem("Профиль", NavRoutes.Profile.route, Icons.Outlined.Person)
    )
}