package com.example.healtapp.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.ui.theme.AppBackgroundBottom
import com.example.healtapp.core.ui.theme.AppBackgroundBottomDark
import com.example.healtapp.core.ui.theme.AppBackgroundTop
import com.example.healtapp.core.ui.theme.AppBackgroundTopDark
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        AppRefreshBus.sessionExpired.collectLatest {
            val loginRoute = NavRoutes.Login.route
            if (navController.currentDestination?.route != loginRoute) {
                navController.navigate(loginRoute) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    val bottomBarRoutes = AppDestinations.bottomNavItems.map { it.route }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = NavigationBarDefaults.Elevation,
                ) {
                    AppDestinations.bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(NavRoutes.Dashboard.route)
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.65f,
                                ),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Splash.route) {
                val viewModel: SplashViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.nextRoute) {
                    val next = uiState.nextRoute ?: return@LaunchedEffect
                    navController.navigate(next) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                }

                val splashGradient = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                    listOf(AppBackgroundTopDark, AppBackgroundBottomDark)
                } else {
                    listOf(AppBackgroundTop, AppBackgroundBottom)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(splashGradient)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "HealthApp",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Твой ассистент здоровья",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    if (uiState.isResolving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            text = "Почти готово…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            composable(NavRoutes.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(NavRoutes.Splash.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
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
                        navController.navigate(NavRoutes.Splash.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
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
            composable(NavRoutes.Activity.route) { ActivityScreen() }

            // оставляем вне нижней панели
            composable(NavRoutes.Hydration.route) { HydrationScreen() }
            composable(NavRoutes.Recommendations.route) { RecommendationsScreen() }
            composable(NavRoutes.Timeline.route) { TimelineScreen() }
            composable(NavRoutes.ActionPlan.route) { ActionPlanScreen() }
        }
    }
}