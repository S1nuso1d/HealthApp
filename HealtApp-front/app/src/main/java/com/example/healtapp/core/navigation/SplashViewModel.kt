package com.example.healtapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.ProfileCache
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import javax.inject.Inject

data class SplashUiState(
    val isResolving: Boolean = true,
    val nextRoute: String? = null,
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val profileRepository: ProfileRepository,
    private val profileCache: ProfileCache,
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

            val cachedProfile = profileCache.load()
            if (cachedProfile != null) {
                _uiState.value = SplashUiState(
                    isResolving = false,
                    nextRoute = routeForProfile(cachedProfile),
                )
            }

            val profileResult = withTimeoutOrNull(SPLASH_PROFILE_TIMEOUT_MS) {
                profileRepository.getMyProfile()
            } ?: cachedProfile?.let { Result.success(it) }
                ?: Result.failure(java.util.concurrent.TimeoutException("profile timeout"))

            if (profileResult.isFailure) {
                val err = profileResult.exceptionOrNull()
                if (err is HttpException && err.code() == 401) {
                    tokenStorage.clearToken()
                    profileCache.clear()
                    _uiState.value = SplashUiState(
                        isResolving = false,
                        nextRoute = NavRoutes.Login.route,
                    )
                    return@launch
                }
                if (cachedProfile == null) {
                    _uiState.value = SplashUiState(
                        isResolving = false,
                        nextRoute = NavRoutes.Dashboard.route,
                    )
                }
                return@launch
            }

            val profile = profileResult.getOrNull()
            _uiState.value = SplashUiState(
                isResolving = false,
                nextRoute = routeForProfile(profile),
            )
        }
    }

    private fun routeForProfile(profile: com.example.healtapp.data.network.dto.profile.ProfileDto?) =
        if (profileCache.needsOnboarding(profile)) {
            NavRoutes.Onboarding.route
        } else {
            NavRoutes.Dashboard.route
        }

    private companion object {
        const val SPLASH_PROFILE_TIMEOUT_MS = 4_000L
    }
}
