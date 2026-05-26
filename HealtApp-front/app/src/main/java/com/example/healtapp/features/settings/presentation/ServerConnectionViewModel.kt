package com.example.healtapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.realtime.RealtimeUpdatesClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ServerConnectionUiState(
    val draftUrl: String = "",
    val activeUrl: String = "",
    val buildDefaultUrl: String = "",
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class ServerConnectionViewModel @Inject constructor(
    private val serverConfig: ApiServerConfig,
    private val realtimeClient: RealtimeUpdatesClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ServerConnectionUiState(
            draftUrl = serverConfig.baseUrl(),
            activeUrl = serverConfig.baseUrl(),
            buildDefaultUrl = serverConfig.buildTimeDefault,
        ),
    )
    val uiState: StateFlow<ServerConnectionUiState> = _uiState.asStateFlow()

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun onUrlChange(value: String) {
        _uiState.update { it.copy(draftUrl = value, message = null) }
    }

    fun applyPreset(raw: String) {
        _uiState.update { it.copy(draftUrl = raw, message = null) }
    }

    fun resetToBuildDefault() {
        viewModelScope.launch {
            serverConfig.clearOverride()
            refreshFromConfig("Сброшено на адрес из сборки (домашняя Wi‑Fi).")
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                val draft = _uiState.value.draftUrl
                if (draft.isBlank() || draft == serverConfig.buildTimeDefault) {
                    serverConfig.clearOverride()
                } else {
                    serverConfig.applyOverride(draft)
                }
            }.onSuccess {
                refreshFromConfig("Адрес сервера сохранён. Переподключение…")
                realtimeClient.stop()
                realtimeClient.start()
                AppRefreshBus.notifyDataChanged()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = e.message ?: "Не удалось сохранить URL",
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, message = null) }
            val url = runCatching {
                ApiServerConfig.normalizeBaseUrl(_uiState.value.draftUrl)
            }.getOrElse { e ->
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        message = e.message,
                        messageIsError = true,
                    )
                }
                return@launch
            }
            val probe = Request.Builder()
                .url("${url.trimEnd('/')}/")
                .get()
                .build()
            runCatching {
                probeClient.newCall(probe).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}")
                    }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        message = "Сервер доступен",
                        messageIsError = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        message = "Нет связи: ${e.message ?: "ошибка"}. " +
                            "Дома — Wi‑Fi и IP ПК; вне дома — туннель (см. docs/REMOTE_DEVELOPMENT.md).",
                        messageIsError = true,
                    )
                }
            }
        }
    }

    private fun refreshFromConfig(message: String) {
        _uiState.update {
            it.copy(
                draftUrl = serverConfig.baseUrl(),
                activeUrl = serverConfig.baseUrl(),
                isSaving = false,
                message = message,
                messageIsError = false,
            )
        }
    }
}
