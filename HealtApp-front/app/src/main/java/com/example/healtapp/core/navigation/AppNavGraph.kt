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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.features.actionplan.ui.ActionPlanScreen
import com.example.healtapp.features.aicoach.ui.AiAssistantScreen
import com.example.healtapp.features.activity.ui.ActivityScreen
import com.example.healtapp.features.auth.ui.ForgotPasswordScreen
import com.example.healtapp.features.auth.ui.LoginScreen
import com.example.healtapp.features.auth.ui.RegisterScreen
import com.example.healtapp.features.dashboard.ui.DashboardScreen
import com.example.healtapp.features.health.ui.HealthVitalsScreen
import com.example.healtapp.features.hydration.ui.HydrationScreen
import com.example.healtapp.features.meal.ui.MealScreen
import com.example.healtapp.features.onboarding.ui.OnboardingScreen
import com.example.healtapp.features.profile.ui.ProfileScreen
import com.example.healtapp.features.recommendations.ui.RecommendationsScreen
import com.example.healtapp.features.settings.ui.DataImportScreen
import com.example.healtapp.features.settings.ui.DataPrivacyScreen
import com.example.healtapp.features.settings.ui.IntegrationsScreen
import com.example.healtapp.features.settings.ui.NotificationsScreen
import com.example.healtapp.notifications.HealthNotificationHelper
import com.example.healtapp.features.sleep.ui.SleepScreen
import com.example.healtapp.features.timeline.ui.TimelineScreen
import com.example.healtapp.core.ui.components.AppBottomNavigation

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val activity = LocalContext.current as ComponentActivity
    LaunchedEffect(activity.intent) {
        val route = activity.intent.getStringExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE) ?: return@LaunchedEffect
        activity.intent.removeExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE)
        val dest = when (route) {
            NavRoutes.Hydration.route,
            NavRoutes.Nutrition.route,
            NavRoutes.Recommendations.route,
            NavRoutes.Notifications.route -> route
            else -> null
        }
        if (dest != null && navController.currentDestination?.route != dest) {
            navController.navigate(dest) { launchSingleTop = true }
        }
    }

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
                AppBottomNavigation(
                    items = AppDestinations.bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(NavRoutes.Dashboard.route)
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
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

                val splashGradient = screenBackgroundGradient()
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
                    onGuestDemo = {
                        navController.navigate(NavRoutes.Splash.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(NavRoutes.Register.route)
                    },
                    onForgotPassword = {
                        navController.navigate(NavRoutes.ForgotPassword.route)
                    },
                )
            }

            composable(NavRoutes.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(NavRoutes.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(NavRoutes.RegisterSetup.route) {
                            popUpTo(NavRoutes.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(NavRoutes.RegisterSetup.route) {
                IntegrationsScreen(
                    onBack = { navController.popBackStack() },
                    registrationMode = true,
                    onContinueToApp = {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
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

            composable(NavRoutes.Dashboard.route) {
                DashboardScreen(
                    onOpenSleep = {
                        navController.navigate(NavRoutes.Sleep.route) { launchSingleTop = true }
                    },
                    onOpenHydration = {
                        navController.navigate(NavRoutes.Hydration.route) { launchSingleTop = true }
                    },
                    onOpenNutrition = {
                        navController.navigate(NavRoutes.Nutrition.route) { launchSingleTop = true }
                    },
                    onOpenActivity = {
                        navController.navigate(NavRoutes.Activity.route) { launchSingleTop = true }
                    },
                    onOpenRecommendations = {
                        navController.navigate(NavRoutes.Recommendations.route) { launchSingleTop = true }
                    },
                    onOpenActionPlan = {
                        navController.navigate(NavRoutes.ActionPlan.route) { launchSingleTop = true }
                    },
                    onOpenTimeline = {
                        navController.navigate(NavRoutes.Timeline.route) { launchSingleTop = true }
                    },
                    onOpenAiAssistant = {
                        navController.navigate(NavRoutes.AiAssistant.route) { launchSingleTop = true }
                    },
                )
            }
            composable(NavRoutes.Profile.route) {
                ProfileScreen(
                    onOpenDataPrivacy = { navController.navigate(NavRoutes.DataPrivacy.route) },
                    onOpenImport = { navController.navigate(NavRoutes.DataImport.route) },
                    onOpenIntegrations = { navController.navigate(NavRoutes.Integrations.route) },
                    onOpenHealthVitals = { navController.navigate(NavRoutes.HealthVitals.route) },
                    onOpenNotifications = { navController.navigate(NavRoutes.Notifications.route) },
                    onOpenAiAssistant = {
                        navController.navigate(NavRoutes.AiAssistant.route) { launchSingleTop = true }
                    },
                    onOpenTimeline = {
                        navController.navigate(NavRoutes.Timeline.route) { launchSingleTop = true }
                    },
                    onOpenActionPlan = {
                        navController.navigate(NavRoutes.ActionPlan.route) { launchSingleTop = true }
                    },
                    onLogout = { AppRefreshBus.notifyLogout() },
                )
            }
            composable(NavRoutes.DataPrivacy.route) {
                DataPrivacyScreen(
                    onBack = { navController.popBackStack() },
                    onAccountDeleted = {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.DataImport.route) {
                DataImportScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Integrations.route) {
                IntegrationsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.HealthVitals.route) {
                HealthVitalsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Sleep.route) {
                SleepScreen(
                    onOpenIntegrations = { navController.navigate(NavRoutes.Integrations.route) },
                    onOpenProfile = {
                        navController.navigate(NavRoutes.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.Nutrition.route) {
                MealScreen(
                    onOpenHydration = { navController.navigate(NavRoutes.Hydration.route) },
                    onOpenHealthVitals = { navController.navigate(NavRoutes.HealthVitals.route) },
                )
            }
            composable(NavRoutes.Activity.route) {
                ActivityScreen(
                    onOpenProfile = {
                        navController.navigate(NavRoutes.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            // оставляем вне нижней панели
            composable(NavRoutes.Hydration.route) { HydrationScreen() }
            composable(NavRoutes.Recommendations.route) { RecommendationsScreen() }
            composable(
                route = NavRoutes.Timeline.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                TimelineScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.ActionPlan.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                ActionPlanScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.AiAssistant.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                AiAssistantScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}