package com.example.healtapp.features.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.healthconnect.HealthConnectManager
import com.example.healtapp.features.auth.presentation.AuthEvent
import com.example.healtapp.features.auth.presentation.AuthViewModel
import com.example.healtapp.features.auth.presentation.RegisterStep
import com.example.healtapp.features.auth.ui.components.AuthHeroBanner
import com.example.healtapp.features.auth.ui.components.AuthScaffold
import com.example.healtapp.features.auth.ui.components.AuthStepAnimatedContent
import com.example.healtapp.features.auth.ui.components.AuthWizardProgress
import com.example.healtapp.features.auth.ui.components.RegisterCredentialsStep
import com.example.healtapp.features.auth.ui.components.RegisterDietaryStep
import com.example.healtapp.features.auth.ui.components.RegisterHealthConnectStep
import com.example.healtapp.features.auth.ui.components.RegisterProfileStep
import com.example.healtapp.features.auth.ui.components.RegisterVerifyStep
import com.example.healtapp.features.auth.ui.components.RegisterWizardNav
import dagger.hilt.android.EntryPointAccessors

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val passwordsMatch = uiState.password == uiState.repeatPassword || uiState.repeatPassword.isBlank()

    val healthConnectManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            HealthConnectEntryPoint::class.java,
        ).healthConnectManager()
    }
    val healthConnectAvailable = remember { healthConnectManager.isSupported() }
    val hcPermissions = remember { HealthConnectReader.coreReadPermissions() }

    val requestHealthPermissions = androidx.activity.compose.rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onEvent(
            AuthEvent.HealthConnectPermissionsResult(granted.containsAll(hcPermissions)),
        )
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            viewModel.consumeAuthorization()
            onRegisterSuccess()
        }
    }

    val (title, subtitle) = when (uiState.registerStep) {
        RegisterStep.Credentials -> "Регистрация" to "Email и пароль для входа"
        RegisterStep.Verify -> "Подтверждение" to "Введите 6-значный код из письма"
        RegisterStep.Profile -> "О вас" to "Расскажите немного о себе — всё необязательно"
        RegisterStep.Dietary -> "Ваши предпочтения" to "Религиозные, этические ограничения и аллергии"
        RegisterStep.HealthConnect -> "Health Connect" to "Подключите шаги и сон — или пропустите"
    }

    AuthScaffold(onBack = onBackClick) {
        AuthHeroBanner(title = title, subtitle = subtitle)

        AuthWizardProgress(currentStep = uiState.registerStep)

        AuthStepAnimatedContent(targetState = uiState.registerStep) { step ->
            when (step) {
                RegisterStep.Profile -> RegisterProfileStep(uiState, viewModel::onEvent)
                RegisterStep.Dietary -> RegisterDietaryStep(uiState, viewModel::onEvent)
                RegisterStep.HealthConnect -> RegisterHealthConnectStep(
                    uiState = uiState,
                    healthConnectAvailable = healthConnectAvailable,
                    onRequestPermissions = { requestHealthPermissions.launch(hcPermissions) },
                    onSkip = { viewModel.onEvent(AuthEvent.HealthConnectSkip) },
                )
                RegisterStep.Credentials -> RegisterCredentialsStep(
                    uiState = uiState,
                    passwordsMatch = passwordsMatch,
                    onEvent = viewModel::onEvent,
                )
                RegisterStep.Verify -> RegisterVerifyStep(uiState, viewModel::onEvent)
            }
        }

        RegisterWizardNav(
            uiState = uiState,
            passwordsMatch = passwordsMatch,
            onEvent = viewModel::onEvent,
            showBack = uiState.registerStep != RegisterStep.Credentials,
        )
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface HealthConnectEntryPoint {
    fun healthConnectManager(): HealthConnectManager
}
