package com.example.healtapp.miband

import android.util.Log
import com.example.healtapp.miband.proto.XiaomiProto
import com.google.protobuf.InvalidProtocolBufferException

data class MiBandLiveStats(
    val steps: Int,
    val calories: Int,
    val heartRate: Int,
)

class MiBandCommandHandler(
    private val authSession: MiBandAuthSession,
    private val onState: (String) -> Unit,
    private val onAuthenticated: () -> Unit,
    private val onStats: (MiBandLiveStats) -> Unit,
    private val sendCommand: (XiaomiProto.Command) -> Unit,
) {
    fun handlePayload(bytes: ByteArray) {
        val cmd = try {
            XiaomiProto.Command.parseFrom(bytes)
        } catch (e: InvalidProtocolBufferException) {
            Log.w(TAG, "Invalid protobuf: ${e.message}")
            return
        }

        when (cmd.type) {
            MiBandAuthSession.TYPE_AUTH -> handleAuth(cmd)
            TYPE_HEALTH -> handleHealth(cmd)
            else -> Log.d(TAG, "Unhandled command type=${cmd.type} subtype=${cmd.subtype}")
        }
    }

    private fun handleAuth(cmd: XiaomiProto.Command) {
        when (cmd.subtype) {
            MiBandAuthSession.SUBTYPE_NONCE -> {
                val watchNonce = cmd.auth?.watchNonce ?: return
                onState("Обмен ключами…")
                val step2 = authSession.handleWatchNonce(watchNonce)
                if (step2 == null) {
                    onState("Ошибка: неверный auth key или браслет не привязан в Mi Fitness")
                    return
                }
                sendCommand(step2)
            }
            MiBandAuthSession.SUBTYPE_AUTH -> {
                if (cmd.auth?.status == 1 || cmd.subtype == MiBandAuthSession.SUBTYPE_AUTH) {
                    authSession.onAuthSuccess(cmd.subtype)
                    onState("Подключено (шифрование активно)")
                    onAuthenticated()
                } else {
                    onState("Аутентификация отклонена браслетом")
                }
            }
        }
    }

    private fun handleHealth(cmd: XiaomiProto.Command) {
        when (cmd.subtype) {
            SUBTYPE_REALTIME -> {
                val stats = cmd.health?.realTimeStats ?: return
                onStats(
                    MiBandLiveStats(
                        steps = stats.steps,
                        calories = stats.calories,
                        heartRate = stats.heartRate,
                    ),
                )
            }
            SUBTYPE_FETCH_TODAY -> onState("Запрошена синхронизация активности за сегодня…")
        }
    }

    fun buildFetchTodayCommand(): XiaomiProto.Command =
        XiaomiProto.Command.newBuilder()
            .setType(TYPE_HEALTH)
            .setSubtype(SUBTYPE_FETCH_TODAY)
            .setHealth(
                XiaomiProto.Health.newBuilder()
                    .setActivitySyncRequestToday(
                        XiaomiProto.ActivitySyncRequestToday.newBuilder().setUnknown1(0),
                    ),
            )
            .build()

    fun buildRealtimeStartCommand(): XiaomiProto.Command =
        XiaomiProto.Command.newBuilder()
            .setType(TYPE_HEALTH)
            .setSubtype(SUBTYPE_REALTIME_START)
            .build()

    fun buildRealtimeStopCommand(): XiaomiProto.Command =
        XiaomiProto.Command.newBuilder()
            .setType(TYPE_HEALTH)
            .setSubtype(SUBTYPE_REALTIME_STOP)
            .build()

    companion object {
        private const val TAG = "MiBandCommand"
        const val TYPE_HEALTH = 8
        const val SUBTYPE_FETCH_TODAY = 1
        const val SUBTYPE_REALTIME_START = 45
        const val SUBTYPE_REALTIME_STOP = 46
        const val SUBTYPE_REALTIME = 47
    }
}
