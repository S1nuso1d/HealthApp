package com.example.healtapp.health

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import com.example.healtapp.core.legal.LegalLinks

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
                "HealthApp запрашивает чтение сна, шагов и связанных метрик через Health Connect, " +
                    "чтобы показать сводку и при желании синхронизировать дневник. " +
                    "Данные не продаются третьим лицам. Полная политика открывается по кнопке ниже.",
            )
            .setPositiveButton("Политика") { _, _ ->
                LegalLinks.openPrivacyPolicy(this)
                finish()
            }
            .setNegativeButton("Понятно") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
