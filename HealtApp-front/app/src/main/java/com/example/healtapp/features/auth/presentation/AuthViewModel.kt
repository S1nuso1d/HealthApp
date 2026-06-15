package com.example.healtapp.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AgeUtils
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    fun enterGuestMode(onSuccess: () -> Unit) {
        viewModelScope.launch {
            tokenStorage.setGuestMode(true)
            onSuccess()
        }
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.value, error = null, infoMessage = null)
            }
            is AuthEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.value, error = null)
            }
            is AuthEvent.RepeatPasswordChanged -> {
                _uiState.value = _uiState.value.copy(repeatPassword = event.value, error = null)
            }
            is AuthEvent.VerificationCodeChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(6)
                _uiState.value = _uiState.value.copy(verificationCode = filtered, error = null)
            }
            is AuthEvent.FirstNameChanged -> {
                _uiState.value = _uiState.value.copy(firstName = event.value, error = null)
            }
            is AuthEvent.LastNameChanged -> {
                _uiState.value = _uiState.value.copy(lastName = event.value, error = null)
            }
            is AuthEvent.NicknameChanged -> {
                _uiState.value = _uiState.value.copy(nickname = event.value, error = null)
            }
            is AuthEvent.BirthDateChanged -> {
                _uiState.value = _uiState.value.copy(birthDate = event.value, error = null)
            }
            is AuthEvent.IsVegetarianChanged -> {
                _uiState.value = _uiState.value.copy(isVegetarian = event.value, error = null)
            }
            is AuthEvent.HasAllergiesChanged -> {
                _uiState.value = _uiState.value.copy(hasAllergies = event.value, error = null)
            }
            is AuthEvent.AllergiesTextChanged -> {
                _uiState.value = _uiState.value.copy(
                    allergiesText = event.value,
                    hasAllergies = event.value.isNotBlank(),
                    error = null,
                )
            }
            is AuthEvent.DietaryNotesChanged -> {
                _uiState.value = _uiState.value.copy(dietaryNotes = event.value, error = null)
            }
            is AuthEvent.DietaryExclusionToggled -> {
                val current = _uiState.value.dietaryExclusions
                val next = if (event.id in current) current - event.id else current + event.id
                val vegetarian = "vegetarian" in next || "vegan" in next
                _uiState.value = _uiState.value.copy(
                    dietaryExclusions = next,
                    isVegetarian = vegetarian,
                    error = null,
                )
            }
            AuthEvent.SubmitLogin -> login()
            AuthEvent.SubmitRegisterNext -> advanceRegisterStep()
            AuthEvent.SubmitRegisterBack -> goBackRegisterStep()
            AuthEvent.SubmitRegisterSendCode -> sendRegistrationCode()
            AuthEvent.SubmitRegisterConfirm -> confirmRegistration()
            AuthEvent.HealthConnectSkip -> {
                _uiState.value = _uiState.value.copy(
                    healthConnectSkipped = true,
                    healthConnectGranted = false,
                    error = null,
                )
                advanceRegisterStep(skipValidation = true)
            }
            is AuthEvent.HealthConnectPermissionsResult -> {
                _uiState.value = _uiState.value.copy(
                    healthConnectGranted = event.granted,
                    healthConnectSkipped = !event.granted,
                    error = if (event.granted) null else "Разрешения не выданы — можно подключить позже в профиле",
                )
            }
            AuthEvent.RegisterEditCredentials -> {
                _uiState.value = _uiState.value.copy(
                    registerStep = RegisterStep.Credentials,
                    verificationCode = "",
                    error = null,
                    infoMessage = null,
                )
            }
        }
    }

    private fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Заполни email и пароль")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            authRepository.login(state.email.trim(), state.password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isAuthorized = true, error = null)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingMessages.fromThrowable(throwable, "Ошибка входа"),
                    )
                }
        }
    }

    private fun advanceRegisterStep(skipValidation: Boolean = false) {
        val state = _uiState.value
        when (state.registerStep) {
            RegisterStep.Credentials -> sendRegistrationCode()
            RegisterStep.Verify -> {
                if (state.verificationCode.length < 6) {
                    _uiState.value = state.copy(error = "Введи 6-значный код из письма")
                    return
                }
                _uiState.value = state.copy(registerStep = RegisterStep.Profile, error = null)
            }
            RegisterStep.Profile -> {
                if (!skipValidation && state.birthDate.isNotBlank()) {
                    val age = AgeUtils.ageFromBirthDate(state.birthDate)
                    if (age == null) {
                        _uiState.value = state.copy(error = "Проверьте дату рождения")
                        return
                    }
                }
                _uiState.value = state.copy(registerStep = RegisterStep.Dietary, error = null)
            }
            RegisterStep.Dietary -> {
                _uiState.value = state.copy(registerStep = RegisterStep.HealthConnect, error = null)
            }
            RegisterStep.HealthConnect -> confirmRegistration()
        }
    }

    private fun goBackRegisterStep() {
        val state = _uiState.value
        val previous = when (state.registerStep) {
            RegisterStep.Credentials -> null
            RegisterStep.Verify -> RegisterStep.Credentials
            RegisterStep.Profile -> RegisterStep.Verify
            RegisterStep.Dietary -> RegisterStep.Profile
            RegisterStep.HealthConnect -> RegisterStep.Dietary
        }
        if (previous != null) {
            _uiState.value = state.copy(registerStep = previous, error = null, infoMessage = null)
        }
    }

    private fun sendRegistrationCode() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.repeatPassword.isBlank()) {
            _uiState.value = state.copy(error = "Заполни email и пароль")
            return
        }
        if (state.password != state.repeatPassword) {
            _uiState.value = state.copy(error = "Пароли не совпадают")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "Пароль должен быть не менее 6 символов")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, infoMessage = null)
            authRepository.sendRegistrationCode(state.email.trim(), state.password)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registerStep = RegisterStep.Verify,
                        verificationCode = "",
                        error = null,
                        infoMessage = message,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingMessages.fromThrowable(throwable, "Не удалось отправить код"),
                    )
                }
        }
    }

    private fun confirmRegistration() {
        val state = _uiState.value
        if (state.verificationCode.length < 6) {
            _uiState.value = state.copy(error = "Введи 6-значный код из письма")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val profile = AuthRegistrationMapper.buildProfileDraft(state)
            authRepository.confirmRegistration(
                email = state.email.trim(),
                password = state.password,
                code = state.verificationCode,
                profile = profile,
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        isAuthorized = true,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingMessages.fromThrowable(
                            throwable,
                            "Неверный код или ошибка сервера",
                        ),
                    )
                }
        }
    }

    fun consumeAuthorization() {
        _uiState.value = _uiState.value.copy(isAuthorized = false)
    }
}
