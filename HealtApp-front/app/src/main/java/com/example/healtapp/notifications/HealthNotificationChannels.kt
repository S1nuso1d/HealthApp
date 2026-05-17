package com.example.healtapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object HealthNotificationChannels {
    const val REMINDERS = "health_reminders"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            REMINDERS,
            "Напоминания HealthApp",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Вода, питание и персональные подсказки"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
