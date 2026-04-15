package com.example.healtapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.actionplan.ui.ActionPlanScreen
import com.example.healtapp.features.activity.ui.ActivityScreen
import com.example.healtapp.features.auth.ui.LoginScreen
import com.example.healtapp.features.auth.ui.RegisterScreen
import com.example.healtapp.features.dashboard.ui.DashboardScreen
import com.example.healtapp.features.hydration.ui.HydrationScreen
import com.example.healtapp.features.meal.ui.MealScreen
import com.example.healtapp.features.onboarding.ui.OnboardingScreen
import com.example.healtapp.features.profile.ui.ProfileScreen
import com.example.healtapp.features.recommendations.ui.RecommendationsScreen
import com.example.healtapp.features.sleep.ui.SleepScreen
import com.example.healtapp.features.timeline.ui.TimelineScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tokenStorage = remember { TokenStorage(context) }
    val profileRepository = remember { AppModule.provideProfileRepository(context) }

    var startResolved by remember { mutableStateOf(false) }

    val bottomBarRoutes = AppDestinations.bottomNavItems.map { it.route }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        scope.launch {
            val token = tokenStorage.getToken()

            if (token.isNullOrBlank()) {
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                val profileResult = profileRepository.getMyProfile()
                val profile = profileResult.getOrNull()

                val profileCompleted = profile != null &&
                        profile.age != null &&
                        profile.height_cm != null &&
                        profile.weight_kg != null

                if (profileCompleted) {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }

            startResolved = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    AppDestinations.bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(NavRoutes.Dashboard.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(item.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (startResolved) "Переход..." else "Загрузка...")
                }
            }

            composable(NavRoutes.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(NavRoutes.Register.route)
                    }
                )
            }

            composable(NavRoutes.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(NavRoutes.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(NavRoutes.Dashboard.route) {
                            popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Dashboard.route) { DashboardScreen() }
            composable(NavRoutes.Profile.route) { ProfileScreen() }
            composable(NavRoutes.Sleep.route) { SleepScreen() }
            composable(NavRoutes.Nutrition.route) { MealScreen() }
            composable(NavRoutes.Hydration.route) { HydrationScreen() }
            composable(NavRoutes.Activity.route) { ActivityScreen() }
            composable(NavRoutes.Recommendations.route) { RecommendationsScreen() }
            composable(NavRoutes.Timeline.route) { TimelineScreen() }
            composable(NavRoutes.ActionPlan.route) { ActionPlanScreen() }
        }
    }
}