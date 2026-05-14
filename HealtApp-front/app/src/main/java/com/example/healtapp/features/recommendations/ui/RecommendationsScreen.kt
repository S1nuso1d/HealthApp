package com.example.healtapp.features.recommendations.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.ui.theme.AppBackgroundBottom
import com.example.healtapp.core.ui.theme.AppBackgroundTop
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel
import com.example.healtapp.features.recommendations.ui.components.RecommendationCard

@Composable
fun RecommendationsScreen() {
    val viewModel: RecommendationsViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppBackgroundTop, AppBackgroundBottom)
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI-рекомендации",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
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
                    text = "На сегодня рекомендаций пока нет. Добавь данные о сне, питании, гидратации и активности, чтобы я смог помочь точнее.",
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