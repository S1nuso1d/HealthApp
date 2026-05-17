package com.example.healtapp.features.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.features.settings.presentation.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DataImportScreen(
    onBack: () -> Unit,
) {
    val viewModel: ImportViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }.orEmpty()
            }
            if (text.isNotBlank()) {
                viewModel.importCsvText(text)
            }
        }
    }

    AppScreen(
        title = "Импорт данных",
        subtitle = "CSV с разделителем «;»",
        headerIcon = Icons.Filled.UploadFile,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        Text(
            text = "Подготовь UTF-8 файл (или выбери его из памяти телефона). Каждая строка — одна запись, поля через точку с запятой. Строки с # в начале считаются комментариями.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Примеры строк:\n" +
                        "hydration;2026-05-14T10:30:00;250\n" +
                        "sleep;2026-05-13T23:00:00;2026-05-14T07:00:00;75;спокойная ночь\n" +
                        "meal;breakfast;Овсянка;2026-05-14T08:00:00;350;12;8;40\n" +
                        "activity;walk;2026-05-14T18:00:00;2026-05-14T18:45:00;45;8000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AppButton(
            text = if (uiState.isLoading) "Импортируем…" else "Выбрать CSV-файл",
            onClick = { pickFile.launch("text/*") },
            enabled = !uiState.isLoading,
        )

        uiState.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        uiState.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
