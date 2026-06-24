package com.example.healtapp.features.social.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.navigation.NavRoutes
import com.example.healtapp.features.social.presentation.ClubNotificationViewModel
import com.example.healtapp.features.social.ui.components.ClubNotificationBanner

private val hiddenRoutes = setOf(
    NavRoutes.Splash.route,
    NavRoutes.Login.route,
    NavRoutes.Register.route,
    NavRoutes.ForgotPassword.route,
    NavRoutes.Onboarding.route,
    NavRoutes.RegisterSetup.route,
)

@Composable
fun ClubNotificationOverlay(
    currentRoute: String?,
    modifier: Modifier = Modifier,
    viewModel: ClubNotificationViewModel = hiltViewModel(),
) {
    if (currentRoute in hiddenRoutes) return

    val notification by viewModel.currentNotification.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(2f),
        contentAlignment = Alignment.TopCenter,
    ) {
        notification?.let { item ->
            ClubNotificationBanner(
                item = item,
                onDismiss = viewModel::dismissCurrent,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
