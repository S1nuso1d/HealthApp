package com.example.healtapp.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.healtapp.MainActivity
import com.example.healtapp.R

object HealthNotificationHelper {

    const val EXTRA_NAV_ROUTE = "nav_route"

    const val ID_HYDRATION = 1001
    const val ID_MEAL_BREAKFAST = 2001
    const val ID_MEAL_LUNCH = 2002
    const val ID_MEAL_DINNER = 2003
    const val ID_RECOMMENDATION = 3001

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        navRoute: String? = null,
    ) {
        if (!canPost(context)) return
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            navRoute?.let { putExtra(EXTRA_NAV_ROUTE, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, HealthNotificationChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun hydrationReminder(context: Context) {
        show(
            context = context,
            notificationId = ID_HYDRATION,
            title = "Время выпить воды",
            body = "Небольшой стакан воды поможет держать гидратацию в норме.",
            navRoute = "hydration",
        )
    }

    fun mealReminder(context: Context, mealLabel: String) {
        val id = when (mealLabel) {
            "Завтрак" -> ID_MEAL_BREAKFAST
            "Обед" -> ID_MEAL_LUNCH
            else -> ID_MEAL_DINNER
        }
        show(
            context = context,
            notificationId = id,
            title = "Запишите $mealLabel",
            body = "Добавьте приём пищи в дневник — так проще следить за калориями и БЖУ.",
            navRoute = "nutrition",
        )
    }

    fun recommendationReminder(context: Context) {
        show(
            context = context,
            notificationId = ID_RECOMMENDATION,
            title = "Новые рекомендации",
            body = "Откройте советы HealthApp — они обновляются по вашим данным.",
            navRoute = "recommendations",
        )
    }
}
