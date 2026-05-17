package com.example.healtapp.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.healtapp.BuildConfig
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.data.network.dto.wellness.DashboardHomeDto
import com.example.healtapp.data.preferences.DashboardCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object HealthReportExporter {

    suspend fun buildReportText(
        context: Context,
        profile: ProfileDto?,
    ): String = withContext(Dispatchers.IO) {
        val home = DashboardCache(context).load()
        buildString {
            appendLine("HealthApp — отчёт о здоровье")
            appendLine("Дата: ${LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru", "RU")))}")
            appendLine()
            if (profile != null) {
                appendLine("Профиль")
                profile.age?.let { appendLine("• Возраст: $it") }
                profile.height_cm?.let { appendLine("• Рост: ${it.toInt()} см") }
                profile.weight_kg?.let { appendLine("• Вес: ${"%.1f".format(it)} кг") }
                profile.goal?.let { appendLine("• Цель: $it") }
                profile.target_steps?.let { appendLine("• Цель шагов: $it") }
                profile.target_water_ml?.let { appendLine("• Цель воды: ${it.toInt()} мл") }
                appendLine()
            }
            if (home != null) {
                appendSection(home)
            } else {
                appendLine("Сводка с сервера недоступна — откройте главную при подключении к сети.")
            }
            appendLine()
            appendLine("Сформировано в HealthApp")
        }
    }

    private fun StringBuilder.appendSection(home: DashboardHomeDto) {
        val summary = home.analytics.summary
        appendLine("Индексы (${summary.periodDays} дн.)")
        appendLine("• Health score: ${summary.healthScore}")
        appendLine("• Сон: ${summary.sleepScore}")
        appendLine("• Вода: ${summary.hydrationScore}")
        appendLine("• Активность: ${summary.activityScore}")
        appendLine("• Питание: ${summary.nutritionScore}")
        appendLine("• Состояние: ${summary.stateScore}")
        appendLine()
        home.dailyBrief?.let { brief ->
            appendLine("Дневной бриф")
            appendLine(brief.title)
            appendLine(brief.summary)
            brief.keyPoints.forEach { appendLine("• $it") }
            appendLine()
        }
        if (home.analytics.insights.isNotEmpty()) {
            appendLine("Инсайты")
            home.analytics.insights.take(5).forEach {
                appendLine("• ${it.title}: ${it.description}")
            }
            appendLine()
        }
        if (home.actionPlan.isNotEmpty()) {
            appendLine("План действий")
            home.actionPlan.take(8).forEach {
                appendLine("• [${it.status}] ${it.title}")
            }
        }
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "HealthApp — отчёт")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться отчётом"))
    }

    suspend fun shareAsFile(context: Context, text: String) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "health_report_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(intent, "Экспорт отчёта"))
        }
    }
}
