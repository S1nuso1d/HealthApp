package com.example.healtapp.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.healtapp.BuildConfig
import com.example.healtapp.core.common.LocaleRu
import com.example.healtapp.data.network.dto.export.ExportReportDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HealthReportExporter {

    fun buildReportText(report: ExportReportDto): String {
        val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", LocaleRu)
        return buildString {
            appendLine("HealthApp — отчёт о здоровье")
            appendLine(
                "Период: ${report.meta?.startDate.orEmpty()} — ${report.meta?.endDate.orEmpty()} " +
                    "(${report.meta?.periodDays ?: 30} дн.)",
            )
            appendLine("Сформирован: ${LocalDate.now().format(dateFmt)}")
            appendLine()
            report.profile?.let { profile ->
                appendLine("Профиль")
                profile.age?.let { appendLine("• Возраст: $it") }
                profile.heightCm?.let { appendLine("• Рост: ${it.toInt()} см") }
                profile.weightKg?.let { appendLine("• Вес: ${"%.1f".format(it)} кг") }
                profile.goal?.let { appendLine("• Цель: $it") }
                appendLine()
            }
            val scores = report.scores.orEmpty()
            if (scores.isNotEmpty()) {
                appendLine("Средние баллы за период")
                scores["health_score"]?.let { appendLine("• Health score: $it") }
                scores["sleep_score"]?.let { appendLine("• Сон: $it") }
                scores["hydration_score"]?.let { appendLine("• Вода: $it") }
                scores["activity_score"]?.let { appendLine("• Активность: $it") }
                scores["nutrition_score"]?.let { appendLine("• Питание: $it") }
                scores["state_score"]?.let { appendLine("• Состояние: $it") }
                appendLine()
            }
            if (report.daily.isNotEmpty()) {
                appendLine("Дни")
                report.daily.takeLast(31).forEach { day ->
                    appendLine(
                        "• ${day.date}: сон ${day.sleepHours ?: 0} ч, вода ${day.waterMl?.toInt() ?: 0} мл, " +
                            "шаги ${day.steps ?: 0}, ккал ${day.calories?.toInt() ?: 0}",
                    )
                }
            }
            appendLine()
            appendLine("Сформировано в HealthApp")
        }
    }

    suspend fun shareReport(
        context: Context,
        report: ExportReportDto,
        csvBytes: ByteArray?,
    ) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val text = buildReportText(report)
        val txtFile = File(dir, "health_report_$stamp.txt").apply { writeText(text) }
        val pdfFile = File(dir, "health_report_$stamp.pdf")
        writePdf(pdfFile, text)
        val csvFile = csvBytes?.let { bytes ->
            File(dir, "health_report_$stamp.csv").apply { writeBytes(bytes) }
        }
        val uris = buildList {
            add(fileUri(context, pdfFile))
            add(fileUri(context, txtFile))
            csvFile?.let { add(fileUri(context, it)) }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, "HealthApp — отчёт")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        withContext(Dispatchers.Main) {
            val chooser = Intent.createChooser(intent, "Экспорт отчёта").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun fileUri(context: Context, file: File) = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file,
    )

    private fun writePdf(file: File, text: String) {
        val document = PdfDocument()
        val paint = Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val lines = text.lines()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 16f
        val linesPerPage = ((pageHeight - margin * 2) / lineHeight).toInt()
        lines.chunked(linesPerPage.coerceAtLeast(1)).forEachIndexed { index, pageLines ->
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create(),
            )
            var y = margin + 12f
            pageLines.forEach { line ->
                page.canvas.drawText(line.take(90), margin, y, paint)
                y += lineHeight
            }
            document.finishPage(page)
        }
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }
}
