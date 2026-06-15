package com.example.healtapp.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.features.profile.PoliticalRecommendations
import com.example.healtapp.features.profile.ProfileRus
import com.example.healtapp.features.profile.presentation.ProfileEditViewModel
import com.example.healtapp.features.profile.ui.components.PoliticalRecommendationHero
import com.example.healtapp.features.settings.ui.components.SettingsCapabilityPanel
import com.example.healtapp.features.settings.ui.components.SettingsInfoText

@Composable
fun PoliticalRecommendationsScreen(
    onBack: () -> Unit,
) {
    val viewModel: ProfileEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activityLevel = uiState.activityLevel.ifBlank { Constants.ActivityLevel.MEDIUM }
    val goal = uiState.goal.ifBlank { Constants.Goals.IMPROVE_ENERGY }
    val recommendation = PoliticalRecommendations.recommend(activityLevel, goal)
    val catalog = PoliticalRecommendations.catalog()

    FeatureScreenShell(
        title = "Политические рекомендации",
        subtitle = "Шуточный модуль · не агитация",
        icon = Icons.Filled.HowToVote,
        onBack = onBack,
        heroFooter = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = "Совпадение: ${recommendation.matchPercent}%",
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    ) {
        SettingsCapabilityPanel(
            title = "Учебная заглушка",
            subtitle = "«Партия» подбирается по уровню активности и цели из профиля. Никакой связи с реальной политикой нет.",
            items = listOf(
                Triple(Icons.Filled.Psychology, "Алгоритм", "По профилю"),
                Triple(Icons.Filled.EmojiEvents, "Цели", "Шаги и сон"),
                Triple(Icons.Filled.HowToVote, "Заглушка", "Для диплома"),
            ),
        )

        PoliticalRecommendationHero(
            recommendation = recommendation,
            activityLevel = activityLevel,
            goal = goal,
            highlighted = true,
        )

        FeatureSectionTitle(
            title = "Ваш профиль",
            subtitle = "Входные данные алгоритма",
        )

        FeatureGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileFactRow("Активность", ProfileRus.activityLevelLabel(activityLevel))
                ProfileFactRow("Цель", ProfileRus.goalLabel(goal))
                ProfileFactRow("Цель шагов", uiState.targetSteps.ifBlank { "—" })
                ProfileFactRow("Цель сна", "${uiState.targetSleep.ifBlank { "—" }} ч")
            }
        }

        FeatureSectionTitle(
            title = "Справочник заглушек",
            subtitle = "Все комбинации для отчёта",
        )

        catalog.forEach { entry ->
            val isCurrent = entry.activityLevel == activityLevel && entry.goal == goal
            PoliticalRecommendationHero(
                recommendation = entry.recommendation,
                activityLevel = entry.activityLevel,
                goal = entry.goal,
                highlighted = isCurrent,
                compact = !isCurrent,
            )
        }

        FeatureGlassCard {
            SettingsInfoText(
                text = "Модуль демонстрационный: достаточно изменить «Уровень активности» или «Цель» в профиле — рекомендация обновится автоматически.",
            )
        }
    }
}

@Composable
private fun ProfileFactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentSecondaryColor(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
