package com.example.healtapp.wear

import android.content.Context
import com.example.healtapp.data.preferences.WidgetSnapshot
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Отправляет на часы те же цифры, что видит виджет телефона.
 *
 * Раньше приложение на часах показывало зашитые «5 230 шагов» и «1250 мл».
 * Кнопка «+250 мл» работала, а сводка — нет. Теперь снимок пишется в Data Layer
 * каждый раз, когда обновляется дашборд.
 */
object WearSnapshotSync {
    const val PATH = "/health/snapshot"

    suspend fun push(context: Context, snapshot: WidgetSnapshot) {
        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putInt("steps", snapshot.stepsToday)
            dataMap.putInt("steps_goal", snapshot.stepsGoal)
            dataMap.putInt("water_ml", snapshot.waterMl)
            dataMap.putInt("water_goal", snapshot.waterGoalMl)
            dataMap.putInt("health_score", snapshot.healthScore)
            dataMap.putLong("updated_at", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request).await()
    }
}
