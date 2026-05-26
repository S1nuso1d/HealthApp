package com.example.healtapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataPrivacyUiState(
    val deletePassword: String = "",
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val deleteSuccessEvent: Boolean = false,
)

@HiltViewModel
class DataPrivacyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPrivacyUiState())
    val uiState: StateFlow<DataPrivacyUiState> = _uiState.asStateFlow()

    fun updateDeletePassword(value: String) {
        _uiState.update { it.copy(deletePassword = value, deleteError = null) }
    }

    fun consumeDeleteSuccess() {
        _uiState.update { it.copy(deleteSuccessEvent = false) }
    }

    fun deleteAccount() {
        val pwd = _uiState.value.deletePassword
        if (pwd.isBlank()) {
            _uiState.update {
                it.copy(deleteError = UserFacingMessages.PASSWORD_REQUIRED_TO_DELETE)
            }
            return
        }
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(deleteError = "В демо-режиме аккаунта на сервере нет. Выйди через «Выйти из аккаунта» в профиле.")
                }
                return@launch
            }
            _uiState.update {
                it.copy(isDeleting = true, deleteError = null)
            }
            authRepository.deleteAccount(pwd)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deletePassword = "",
                            deleteSuccessEvent = true,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = UserFacingMessages.fromThrowable(
                                e,
                                "Не удалось удалить аккаунт",
                            ),
                        )
                    }
                }
        }
    }
}
