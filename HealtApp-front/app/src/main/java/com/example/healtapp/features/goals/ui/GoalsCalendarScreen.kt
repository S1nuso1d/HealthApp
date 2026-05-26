package com.example.healtapp.features.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.goals.presentation.GoalsCalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsCalendarScreen(onBack: () -> Unit = {}) {
    val viewModel: GoalsCalendarViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("d")

    AppScreen(
        title = "Календарь целей",
        subtitle = "Как в Samsung Health: дни, когда закрыты все цели",
        headerIcon = Icons.Filled.CalendarMonth,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }

        SectionHeader(title = "Легенда")
        AppCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = MaterialTheme.colorScheme.primary, label = "Все цели")
                LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "Частично")
                LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = "Нет данных")
            }
        }

        SectionHeader(title = "Последние ${uiState.days.size} дней")
        AppCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.days.forEach { day ->
                    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
                    val color = when {
                        day.all_goals_met -> MaterialTheme.colorScheme.primary
                        day.sleep_met || day.hydration_met || day.activity_met || day.nutrition_met ->
                            MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = date?.format(formatter) ?: "?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Детали за сегодня")
        val today = uiState.days.lastOrNull()
        if (today != null) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalRow("Сон", today.sleep_met)
                    GoalRow("Вода", today.hydration_met)
                    GoalRow("Активность", today.activity_met)
                    GoalRow("Питание", today.nutrition_met)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    RowWithDot(color, label)
}

@Composable
private fun RowWithDot(color: androidx.compose.ui.graphics.Color, label: String) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GoalRow(name: String, met: Boolean) {
    Text(
        text = if (met) "✓ $name" else "○ $name",
        style = MaterialTheme.typography.bodyMedium,
        color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
