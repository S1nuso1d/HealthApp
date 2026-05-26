package com.example.healtapp.features.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.features.timeline.presentation.TimelineEventUi
import com.example.healtapp.features.timeline.presentation.TimelineViewModel

@Composable
fun TimelineScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: TimelineViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = "Лента здоровья",
        subtitle = "Инсайты, аналитика и ваши отметки",
        headerIcon = Icons.Filled.Timeline,
        onNavigateBack = onBack,
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Сводка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        uiState.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Главное: ${uiState.insightHighlight}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (uiState.events.isEmpty()) {
                Text(
                    "Пока нет событий. Отметьте настроение на главной или добавьте данные в дневник.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SectionHeader(title = "События", subtitle = "${uiState.events.size} записей")
                uiState.events.forEach { event ->
                    TimelineEventRow(event)
                }
            }

            uiState.error?.let {
                AppMessageBanner(text = it, type = AppMessageType.Error)
            }
        }
    }
}

@Composable
private fun TimelineEventRow(event: TimelineEventUi) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(brandingGradient())),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(event.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
