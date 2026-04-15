package com.example.healtapp.features.recommendations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel
import com.example.healtapp.features.recommendations.ui.components.RecommendationCard

@Composable
fun RecommendationsScreen() {
    val context = LocalContext.current

    val viewModel = remember {
        RecommendationsViewModel(
            aiRepository = AppModule.provideAiRepository(context)
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI-рекомендации",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Индекс здоровья: ${uiState.healthScore}",
            style = MaterialTheme.typography.titleMedium
        )

        when {
            uiState.isLoading -> {
                Text(
                    text = "Загрузка рекомендаций...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Ошибка",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.recommendations.isEmpty() -> {
                Text(
                    text = "Пока рекомендаций нет",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            else -> {
                uiState.recommendations.forEach { recommendation ->
                    RecommendationCard(item = recommendation)
                }
            }
        }
    }
}