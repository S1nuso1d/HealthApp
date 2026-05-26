package com.example.healtapp.data.network.realtime

import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.auth.ApplicationScope
import com.example.healtapp.data.preferences.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeUpdatesClient @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val serverConfig: ApiServerConfig,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val client = OkHttpClient()
    private var websocket: WebSocket? = null
    private var connectJob: Job? = null
    private var manuallyStopped = false
    /** Не переподключаться с тем же JWT после 401/403 (истёкший/неверный токен). */
    private var pauseReconnectForAuth = false

    fun start() {
        manuallyStopped = false
        pauseReconnectForAuth = false
        connectJob?.cancel()
        connectJob = applicationScope.launch {
            connectInternal()
        }
    }

    fun stop() {
        manuallyStopped = true
        connectJob?.cancel()
        websocket?.close(1000, "App stopped")
        websocket = null
    }

    private suspend fun connectInternal() {
        if (pauseReconnectForAuth) return
        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) return

        val wsUrl = "${serverConfig.webSocketBase()}/ws?token=$token"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        websocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("\"type\": \"dashboard_update\"") ||
                        text.contains("\"type\":\"dashboard_update\"")
                    ) {
                        AppRefreshBus.notifyDataChanged()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    websocket = null
                    if (manuallyStopped) return
                    val code = response?.code
                    if (code == 401 || code == 403) {
                        // Не сбрасываем JWT: иначе при каждом входе WebSocket без /ws уводит на экран логина.
                        pauseReconnectForAuth = true
                        return
                    }
                    applicationScope.launch {
                        delay(5_000)
                        connectInternal()
                    }
                }
            }
        )
    }
}

