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
    const val ID_MISSED_MEAL = 4001
    const val ID_STEPS_GOAL = 5001
    const val ID_WATER_GOAL = 5002
    const val ID_WATER_LOW = 5003
    const val ID_STEPS_BEHIND = 5004
    const val ID_SLEEP_EVENING = 1002
    const val ID_WEIGHT_UPDATE = 1003

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

    fun missedMealReminder(context: Context, mealLabel: String) {
        show(
            context = context,
            notificationId = ID_MISSED_MEAL + mealLabel.hashCode() % 10,
            title = "Нет записи: $mealLabel",
            body = "Сегодня $mealLabel ещё не добавлен в дневник. Запишите приём пищи, чтобы видеть калории и БЖУ.",
            navRoute = "nutrition",
        )
    }

    fun stepsGoalReached(context: Context, steps: Int, goal: Int) {
        show(
            context = context,
            notificationId = ID_STEPS_GOAL,
            title = "Цель по шагам достигнута",
            body = "Сегодня $steps шагов — вы выполнили цель ($goal). Отличная работа!",
            navRoute = "activity",
        )
    }

    fun waterGoalReached(context: Context, ml: Int, target: Int) {
        show(
            context = context,
            notificationId = ID_WATER_GOAL,
            title = "Норма воды выполнена",
            body = "Сегодня $ml мл из $target мл. Гидратация на сегодня в норме.",
            navRoute = "hydration",
        )
    }

    fun stepsBehindPace(context: Context, current: Int, goal: Int, remaining: Int) {
        show(
            context = context,
            notificationId = ID_STEPS_BEHIND,
            title = "Шаги отстают от цели",
            body = "Сейчас $current из $goal. До цели осталось около $remaining шагов — прогулка поможет наверстать.",
            navRoute = "activity",
        )
    }

    fun waterLowReminder(context: Context, current: Int, target: Int) {
        show(
            context = context,
            notificationId = ID_WATER_LOW,
            title = "Мало воды за день",
            body = "Сейчас $current мл из $target мл. Добавьте стакан воды в дневник.",
            navRoute = "hydration",
        )
    }

    fun sleepEveningReminder(context: Context) {
        show(
            context = context,
            notificationId = ID_SLEEP_EVENING,
            title = "Время отдыха",
            body = "Запишите сон сегодня — так проще видеть восстановление и качество отдыха.",
            navRoute = "sleep",
        )
    }

    fun weightUpdateReminder(context: Context) {
        show(
            context = context,
            notificationId = ID_WEIGHT_UPDATE,
            title = "Обновите вес",
            body = "Раз в неделю полезно зафиксировать вес в профиле — так точнее КБЖУ и динамика.",
            navRoute = "profile",
        )
    }
}
