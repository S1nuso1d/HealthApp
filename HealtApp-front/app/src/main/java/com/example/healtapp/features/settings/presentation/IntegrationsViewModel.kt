package com.example.healtapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.network.api.IntegrationsApi
import com.example.healtapp.data.network.dto.integrations.FatSecretLinkDto
import com.example.healtapp.domain.repository.ImportRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IntegrationsUiState(
    val healthConnectSupported: Boolean = false,
    val healthConnectNeedsProviderUpdate: Boolean = false,
    val healthConnectCanRequestPermissions: Boolean = false,
    val healthConnectPermissionsGranted: Boolean = false,
    val fatSecretToken: String = "",
    val fatSecretSecret: String = "",
    val fatSecretSearchQuery: String = "",
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val fatSecretPreview: String? = null,
)

@HiltViewModel
class IntegrationsViewModel @Inject constructor(
    private val healthConnectReader: HealthConnectReader,
    private val importRepository: ImportRepository,
    private val integrationsApi: IntegrationsApi,
) : ViewModel() {

    private val gson = Gson()

    private val _uiState = MutableStateFlow(IntegrationsUiState())
    val uiState: StateFlow<IntegrationsUiState> = _uiState.asStateFlow()

    init {
        refreshHealthConnectAvailability()
        refreshHealthConnectPermissions()
    }

    fun refreshHealthConnectAvailability() {
        _uiState.update {
            it.copy(
                healthConnectSupported = healthConnectReader.isSupported(),
                healthConnectNeedsProviderUpdate = healthConnectReader.needsProviderUpdate(),
                healthConnectCanRequestPermissions = healthConnectReader.canRequestPermissions(),
            )
        }
    }

    fun refreshHealthConnectPermissions() {
        if (!healthConnectReader.canRequestPermissions()) {
            _uiState.update { it.copy(healthConnectPermissionsGranted = false) }
            return
        }
        viewModelScope.launch {
            val ok = runCatching { healthConnectReader.areReadPermissionsGranted() }
                .getOrDefault(false)
            _uiState.update { it.copy(healthConnectPermissionsGranted = ok) }
        }
    }

    fun reportHealthConnectPermissionError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun updateFatSecretToken(v: String) {
        _uiState.update { it.copy(fatSecretToken = v, error = null) }
    }

    fun updateFatSecretSecret(v: String) {
        _uiState.update { it.copy(fatSecretSecret = v, error = null) }
    }

    fun updateFatSecretSearchQuery(v: String) {
        _uiState.update { it.copy(fatSecretSearchQuery = v, error = null) }
    }

    fun syncFromHealthConnect(days: Int = 14) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, error = null) }
            val payload = healthConnectReader.buildImportPayload(days)
            payload.fold(
                onSuccess = { batch ->
                    importRepository.importBatch(batch)
                        .onSuccess { res ->
                            val msg = buildString {
                                append("Сон: ${res.sleeps_created}, активность: ${res.activities_created}")
                                if (res.meals_created > 0) append(", питание: ${res.meals_created}")
                                if (res.health_samples_created > 0) {
                                    append(", показатели (пульс, вес и др.): ${res.health_samples_created}")
                                }
                                if (res.errors.isNotEmpty()) {
                                    append("\n")
                                    append(res.errors.take(3).joinToString("; "))
                                }
                            }
                            _uiState.update {
                                it.copy(isBusy = false, message = msg, fatSecretPreview = null)
                            }
                            AppRefreshBus.notifyDataChanged()
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    error = e.message ?: "Ошибка импорта",
                                )
                            }
                        }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = e.message ?: "Нет доступа к Health Connect",
                        )
                    }
                },
            )
        }
    }

    fun linkFatSecret() {
        val s = _uiState.value
        if (s.fatSecretToken.isBlank() || s.fatSecretSecret.isBlank()) {
            _uiState.update { it.copy(error = "Укажи token и secret из OAuth FatSecret") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                integrationsApi.linkFatSecret(
                    FatSecretLinkDto(
                        access_token = s.fatSecretToken.trim(),
                        access_secret = s.fatSecretSecret.trim(),
                    ),
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = "FatSecret привязан",
                        fatSecretPreview = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        error = e.message ?: "Не удалось привязать FatSecret",
                    )
                }
            }
        }
    }

    fun unlinkFatSecret() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { integrationsApi.unlinkFatSecret() }
                .onSuccess {
                    _uiState.update {
                        it.copy(isBusy = false, message = "Связь с FatSecret снята", fatSecretPreview = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = e.message ?: "Не удалось отвязать",
                        )
                    }
                }
        }
    }

    fun searchFatSecretFoods() {
        val q = _uiState.value.fatSecretSearchQuery.trim()
        if (q.isBlank()) {
            _uiState.update { it.copy(error = "Введи запрос для поиска") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { integrationsApi.searchFoods(q) }
                .onSuccess { json ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            fatSecretPreview = gson.toJson(json),
                            message = "Ответ FatSecret (поиск)",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = e.message ?: "Ошибка поиска",
                        )
                    }
                }
        }
    }
}
