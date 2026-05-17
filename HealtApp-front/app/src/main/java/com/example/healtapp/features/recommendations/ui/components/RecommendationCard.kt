package com.example.healtapp.features.recommendations.ui.components

import androidx.compose.runtime.Composable
import com.example.healtapp.features.dashboard.ui.components.DashboardRecommendationCard
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem

@Composable
fun RecommendationCard(
    item: RecommendationUiItem,
    onClick: (() -> Unit)? = null,
) {
    DashboardRecommendationCard(item = item, onClick = onClick)
}
