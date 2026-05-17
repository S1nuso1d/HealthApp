package com.example.healtapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isResolving: Boolean = true,
    val nextRoute: String? = null,
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        resolve()
    }

    private fun resolve() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = SplashUiState(
                    isResolving = false,
                    nextRoute = NavRoutes.Dashboard.route,
                )
                return@launch
            }

            val token = tokenStorage.getToken()
            if (token.isNullOrBlank()) {
                _uiState.value = SplashUiState(isResolving = false, nextRoute = NavRoutes.Login.route)
                return@launch
            }

            val profileResult = profileRepository.getMyProfile()

            if (profileResult.isFailure) {
                val err = profileResult.exceptionOrNull()
                if (err is HttpException && err.code() == 401) {
                    tokenStorage.clearToken()
                    _uiState.value = SplashUiState(
                        isResolving = false,
                        nextRoute = NavRoutes.Login.route,
                    )
                    return@launch
                }
                // Сеть/сервер недоступен или профиль не загрузился — всё равно в приложение,
                // а не на онбординг (заполнение профиля можно сделать позже из раздела «Профиль»).
                _uiState.value = SplashUiState(
                    isResolving = false,
                    nextRoute = NavRoutes.Dashboard.route,
                )
                return@launch
            }

            val profile = profileResult.getOrNull()
            val needsOnboarding = profile == null ||
                profile.age == null ||
                profile.height_cm == null ||
                profile.weight_kg == null

            _uiState.value = SplashUiState(
                isResolving = false,
                nextRoute = if (needsOnboarding) {
                    NavRoutes.Onboarding.route
                } else {
                    NavRoutes.Dashboard.route
                },
            )
        }
    }
}

