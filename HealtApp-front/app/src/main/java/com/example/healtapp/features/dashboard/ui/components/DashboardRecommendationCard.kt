package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.recommendationPriorityColorHigh
import com.example.healtapp.core.ui.theme.recommendationPriorityColorLow
import com.example.healtapp.core.ui.theme.recommendationPriorityColorMedium
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.core.ui.theme.tipBannerColors
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.features.recommendations.presentation.RecommendationFormatting
import com.example.healtapp.features.recommendations.presentation.RecommendationPriorityKind
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem

@Composable
fun DashboardRecommendationCard(
    item: RecommendationUiItem,
    onClick: (() -> Unit)? = null,
    emphasized: Boolean = false,
) {
    val priorityKind = RecommendationFormatting.priorityKind(item.priority)
    val priorityColor = when (priorityKind) {
        RecommendationPriorityKind.HIGH -> recommendationPriorityColorHigh()
        RecommendationPriorityKind.MEDIUM -> recommendationPriorityColorMedium()
        RecommendationPriorityKind.LOW -> recommendationPriorityColorLow()
    }

    AppCard(onClick = onClick) {
        if (emphasized) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(subtleFillGradient()),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Главный совет дня",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentPrimaryColor(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (emphasized) 100.dp else 88.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(brandingGradient())),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = RecommendationFormatting.categoryLabelRu(item.category),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentPrimaryColor(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                themedCardLavender(),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = RecommendationFormatting.priorityLabelRu(item.priority),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor,
                        )
                    }
                }

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                item.progressLabel?.let { progress ->
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentPrimaryColor(),
                    )
                }

                item.personalizedTip?.takeIf { it.isNotBlank() }?.let { tip ->
                    val (tipBg, tipFg) = tipBannerColors()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tipBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = tipFg,
                        )
                    }
                }
            }
        }
    }
}
