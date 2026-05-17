package com.example.healtapp

import android.app.Application
import com.example.healtapp.notifications.HealthNotificationChannels
import com.example.healtapp.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HealthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HealthNotificationChannels.createAll(this)
        ReminderScheduler.rescheduleAll(this)
    }
}