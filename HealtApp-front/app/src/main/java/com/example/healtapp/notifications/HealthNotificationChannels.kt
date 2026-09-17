package com.example.healtapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object HealthNotificationChannels {
    const val REMINDERS = "health_reminders"
    const val PILLS = "health_pills"
    const val SOCIAL = "health_social"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channelReminders = NotificationChannel(
            REMINDERS,
            "Напоминания HealthApp",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Вода, питание и персональные подсказки"
            enableVibration(true)
        }
        manager.createNotificationChannel(channelReminders)

        val channelPills = NotificationChannel(
            PILLS,
            "Таблетки и витамины",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Напоминания о приёме лекарств и витаминов"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channelPills)

        val channelSocial = NotificationChannel(
            SOCIAL,
            "Социальные уведомления",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Заявки в друзья, реакции и комментарии"
            enableVibration(true)
        }
        manager.createNotificationChannel(channelSocial)
    }
}
