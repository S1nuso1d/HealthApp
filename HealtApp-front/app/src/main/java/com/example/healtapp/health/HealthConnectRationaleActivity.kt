package com.example.healtapp.health

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

/**
 * Показывается по ссылке «Политика конфиденциальности» на экране разрешений Health Connect (Android 14+).
 * Без этого activity с intent-filter запрос разрешений может не открываться.
 */
class HealthConnectRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle("Доступ к данным здоровья")
            .setMessage(
                "HealthApp запрашивает чтение сна и шагов через Health Connect, чтобы показать сводку " +
                    "и при желании импортировать данные в твой аккаунт на сервере. " +
                    "Мы не продаём эти данные третьим лицам; подробности — в разделе «Данные и конфиденциальность» в приложении.",
            )
            .setPositiveButton("Понятно") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
