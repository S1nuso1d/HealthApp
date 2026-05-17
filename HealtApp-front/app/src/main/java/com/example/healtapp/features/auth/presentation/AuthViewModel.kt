package com.example.healtapp.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                _uiState.value = _uiState.value.copy(
                    email = event.value,
                    error = null,
                    infoMessage = null,
                )
            }

            is AuthEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(
                    password = event.value,
                    error = null,
                )
            }

            is AuthEvent.RepeatPasswordChanged -> {
                _uiState.value = _uiState.value.copy(
                    repeatPassword = event.value,
                    error = null,
                )
            }

            is AuthEvent.VerificationCodeChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(6)
                _uiState.value = _uiState.value.copy(
                    verificationCode = filtered,
                    error = null,
                )
            }

            AuthEvent.SubmitLogin -> login()
            AuthEvent.SubmitRegisterSendCode -> sendRegistrationCode()
            AuthEvent.SubmitRegisterConfirm -> confirmRegistration()
            AuthEvent.RegisterEditCredentials -> {
                _uiState.value = _uiState.value.copy(
                    awaitingEmailVerification = false,
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

            val result = authRepository.login(
                email = state.email.trim(),
                password = state.password,
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    isAuthorized = true,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Ошибка входа",
                )
            }
        }
    }

    private fun sendRegistrationCode() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank() || state.repeatPassword.isBlank()) {
            _uiState.value = state.copy(error = "Заполни все поля")
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
                        awaitingEmailVerification = true,
                        verificationCode = "",
                        error = null,
                        infoMessage = message,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось отправить код",
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

            authRepository.confirmRegistration(
                email = state.email.trim(),
                password = state.password,
                code = state.verificationCode,
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
                        error = throwable.message ?: "Неверный код или ошибка сервера",
                    )
                }
        }
    }

    fun consumeAuthorization() {
        _uiState.value = _uiState.value.copy(isAuthorized = false)
    }
}
