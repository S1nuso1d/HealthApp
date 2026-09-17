package com.example.healtapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getIntExtra("PILL_ID", -1)
        val name = intent.getStringExtra("PILL_NAME") ?: "таблетку"
        val dosage = intent.getStringExtra("PILL_DOSAGE") ?: ""
        val timeOfDay = intent.getStringExtra("PILL_TIME")

        if (pillId != -1) {
            HealthNotificationHelper.pillReminder(context, pillId, name, dosage)
            
            if (timeOfDay != null) {
                PillReminderScheduler.schedulePillReminder(context, pillId, name, dosage, timeOfDay)
            }
        }
    }
}
