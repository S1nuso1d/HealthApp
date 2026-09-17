package com.example.healtapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        if (!relevant) return

        val appContext = context.applicationContext
        ReminderScheduler.rescheduleAll(appContext)
        // Сразу из локального кэша — без ожидания сети.
        PillReminderScheduler.restoreFromLocal(appContext)
        // Затем подтянуть актуальный список с сервера, когда сеть появится.
        PillReminderRestoreWorker.enqueue(appContext)
    }
}
