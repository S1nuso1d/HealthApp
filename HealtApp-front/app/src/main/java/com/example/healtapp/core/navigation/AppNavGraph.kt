package com.example.healtapp.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
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
import com.example.healtapp.core.ui.animation.AppNavTransitions
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
import com.example.healtapp.features.onboarding.ui.OnboardingScreen
import com.example.healtapp.features.profile.ui.PoliticalRecommendationsScreen
import com.example.healtapp.features.profile.ui.ProfileScreen
import com.example.healtapp.features.recommendations.ui.RecommendationsScreen
import com.example.healtapp.features.settings.ui.DataImportScreen
import com.example.healtapp.features.settings.ui.DataPrivacyScreen
import com.example.healtapp.features.settings.ui.IntegrationsScreen
import com.example.healtapp.features.settings.ui.ServerConnectionScreen
import com.example.healtapp.features.miband.ui.MiBandConnectionScreen
import com.example.healtapp.features.settings.ui.NotificationsScreen
import com.example.healtapp.notifications.HealthNotificationHelper
import com.example.healtapp.features.sleep.ui.SleepScreen
import com.example.healtapp.features.timeline.ui.TimelineScreen
import com.example.healtapp.features.achievements.ui.AchievementUnlockOverlay
import com.example.healtapp.features.social.ui.ClubNotificationOverlay
import com.example.healtapp.features.achievements.ui.AchievementsScreen
import com.example.healtapp.features.social.ui.FriendsScreen
import com.example.healtapp.features.social.ui.ClubsScreen
import com.example.healtapp.features.social.ui.ClubDetailScreen
import com.example.healtapp.features.social.ui.FriendProfileScreen
import com.example.healtapp.features.cycle.ui.CycleScreen
import com.example.healtapp.features.pills.ui.PillsScreen
import com.example.healtapp.features.fasting.ui.FastingScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.healtapp.core.ui.components.AppBottomNavigation
import com.example.healtapp.core.ui.components.GlobalPendingSyncBanner
import com.example.healtapp.features.sync.GlobalPendingSyncViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val syncViewModel: GlobalPendingSyncViewModel = hiltViewModel()
    val pendingSyncCount by syncViewModel.pendingCount.collectAsStateWithLifecycle()
    val isSyncFlushing by syncViewModel.isFlushing.collectAsStateWithLifecycle()

    val activity = LocalContext.current as ComponentActivity
    LaunchedEffect(activity.intent) {
        val route = activity.intent.getStringExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE) ?: return@LaunchedEffect
        activity.intent.removeExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE)
        val dest = when (route) {
            NavRoutes.Dashboard.route,
            NavRoutes.Activity.route,
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

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                AppBottomNavigation(
                    items = AppDestinations.bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigateBottomTab(item.route)
                    },
                )
            }
        },
        // Инсеты системных панелей раздаёт не Scaffold, а сами экраны (AppScreen), иначе
        // при edge-to-edge отступ под статус-бар применился бы дважды. От Scaffold нам
        // нужна только высота нижней навигации.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Splash.route,
            modifier = Modifier
                .padding(innerPadding)
                // Нижняя навигация уже закрывает область системной панели, поэтому
                // внутри экранов этот инсет не должен учитываться повторно.
                .consumeWindowInsets(innerPadding),
            enterTransition = AppNavTransitions.enterTransition,
            exitTransition = AppNavTransitions.exitTransition,
            popEnterTransition = AppNavTransitions.popEnterTransition,
            popExitTransition = AppNavTransitions.popExitTransition,
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
                        navController.navigate(NavRoutes.Dashboard.route) {
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
                        navController.navigate(NavRoutes.Dashboard.route) {
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
                    onOpenSleep = { navController.navigateFeature(NavRoutes.Sleep.route) },
                    onOpenHydration = { navController.navigateFeature(NavRoutes.Hydration.route) },
                    onOpenNutrition = { navController.navigateFeature(NavRoutes.Nutrition.route) },
                    onOpenActivity = { navController.navigateFeature(NavRoutes.Activity.route) },
                    onOpenRecommendations = { navController.navigateFeature(NavRoutes.Recommendations.route) },
                    onOpenActionPlan = { navController.navigateFeature(NavRoutes.ActionPlan.route) },
                    onOpenTimeline = { navController.navigateFeature(NavRoutes.Timeline.route) },
                    onOpenAiAssistant = { navController.navigateFeature(NavRoutes.AiAssistant.route) },
                    onOpenHealthVitals = { navController.navigateFeature(NavRoutes.HealthVitals.route) },
                )
            }
            composable(NavRoutes.Profile.route) {
                ProfileScreen(
                    onOpenDataPrivacy = { navController.navigateFeature(NavRoutes.DataPrivacy.route) },
                    onOpenIntegrations = { navController.navigateFeature(NavRoutes.Integrations.route) },
                    onOpenMiBandBle = { navController.navigateFeature(NavRoutes.MiBandBle.route) },
                    onOpenNotifications = { navController.navigateFeature(NavRoutes.Notifications.route) },
                    onOpenAchievements = { navController.navigateFeature(NavRoutes.Achievements.route) },
                    onOpenFriends = { navController.navigateFeature(NavRoutes.Friends.route) },
                    onOpenClubs = { navController.navigateFeature(NavRoutes.Clubs.route) },
                    onOpenPoliticalRecommendations = {
                        navController.navigateFeature(NavRoutes.PoliticalRecommendations.route)
                    },
                    onOpenPills = { navController.navigateFeature(NavRoutes.Pills.route) },
                    onOpenCycle = { navController.navigateFeature(NavRoutes.Cycle.route) },
                    onLogout = { AppRefreshBus.notifyLogout() },
                )
            }
            composable(NavRoutes.PoliticalRecommendations.route) {
                PoliticalRecommendationsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.ServerConnection.route) {
                ServerConnectionScreen(onBack = { navController.popBackStack() })
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
                IntegrationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMiBandBle = { navController.navigate(NavRoutes.MiBandBle.route) },
                )
            }
            composable(NavRoutes.MiBandBle.route) {
                MiBandConnectionScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.HealthVitals.route) {
                HealthVitalsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Sleep.route) {
                SleepScreen(
                    onOpenProfile = {
                        navController.navigate(NavRoutes.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.Nutrition.route) {
                com.example.healtapp.features.nutrition.ui.NutritionHubScreen(
                    initialTab = 0,
                    onOpenPlanner = { navController.navigate(NavRoutes.MealPlanner.route) { launchSingleTop = true } }
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
            composable(NavRoutes.Hydration.route) {
                com.example.healtapp.features.nutrition.ui.NutritionHubScreen(initialTab = 2)
            }
            composable(NavRoutes.Recommendations.route) {
                RecommendationsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.Timeline.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                TimelineScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFriend = { userId ->
                        navController.navigate(NavRoutes.FriendProfile.route(userId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenFriends = {
                        navController.navigate(NavRoutes.Friends.route) { launchSingleTop = true }
                    },
                    onOpenClub = { clubId ->
                        navController.navigate(NavRoutes.ClubDetail.route(clubId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.MealPlanner.route) {
                com.example.healtapp.features.meal.ui.MealPlannerScreen(
                    onBack = { navController.popBackStack() }
                )
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
            composable(NavRoutes.Achievements.route) {
                AchievementsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Friends.route) {
                FriendsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFriend = { id ->
                        navController.navigate(NavRoutes.FriendProfile.route(id))
                    },
                )
            }
            composable(NavRoutes.Clubs.route) {
                ClubsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenClub = { id ->
                        navController.navigate(NavRoutes.ClubDetail.route(id))
                    },
                )
            }
            composable(
                route = NavRoutes.ClubDetail.route,
                arguments = listOf(navArgument("clubId") { type = NavType.IntType }),
            ) {
                val clubId = it.arguments?.getInt("clubId") ?: return@composable
                ClubDetailScreen(
                    clubId = clubId,
                    onBack = { navController.popBackStack() },
                    onOpenMember = { userId ->
                        navController.navigate(NavRoutes.FriendProfile.route(userId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.Cycle.route) {
                CycleScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Pills.route) {
                PillsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Fasting.route) {
                FastingScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.FriendProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType }),
            ) {
                FriendProfileScreen(onBack = { navController.popBackStack() })
            }
        }
    }

        if (
            currentRoute !in setOf(
                NavRoutes.Splash.route,
                NavRoutes.Login.route,
                NavRoutes.Register.route,
                NavRoutes.ForgotPassword.route,
                NavRoutes.Onboarding.route,
                NavRoutes.RegisterSetup.route,
            )
        ) {
            GlobalPendingSyncBanner(
                count = pendingSyncCount,
                isFlushing = isSyncFlushing,
                onTap = { syncViewModel.flushNow() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth(),
            )
        }

        AchievementUnlockOverlay(currentRoute = currentRoute)
        ClubNotificationOverlay(currentRoute = currentRoute)
    }
}