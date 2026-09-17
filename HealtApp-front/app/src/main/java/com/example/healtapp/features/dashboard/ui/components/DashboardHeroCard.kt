package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.LocaleRu
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.theme.Dimens
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.heroIconBackdrop
import com.example.healtapp.core.ui.theme.scoreRingGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", LocaleRu)

@Composable
fun DashboardHeroCard(
    greeting: String,
    healthScore: Int?,
    isRecommendationsLoading: Boolean,
    isEvening: Boolean = false,
) {
    val dateLabel = LocalDate.now().format(dayMonthFormatter)
    val heroGradient = heroBlockGradient()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusXl))
            .background(Brush.linearGradient(heroGradient)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        tint = heroContentColor().copy(alpha = 0.95f),
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = dateLabel.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = heroContentColor().copy(alpha = 0.85f),
                    )
                    if (isEvening) {
                        Text(
                            text = "Вечер",
                            style = MaterialTheme.typography.labelLarge,
                            color = heroContentColor(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = heroContentColor(),
                )
            }

            when {
                isRecommendationsLoading -> {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(44.dp))
                            .background(heroIconBackdrop()),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = heroContentColor(),
                        )
                    }
                }
                healthScore != null && healthScore > 0 -> {
                    DashboardHeroScoreRing(score = healthScore)
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(44.dp))
                            .background(heroIconBackdrop()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint = heroContentColor(),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroScoreRing(score: Int) {
    val heroText = heroContentColor()
    val ringGradient = scoreRingGradient()
    val progress = (score / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = AppMotion.tweenMedium(),
        label = "hero_score",
    )

    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val arcSize = size.minDimension - stroke
            val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)
            val arc = Size(arcSize, arcSize)
            drawArc(
                color = heroText.copy(alpha = 0.25f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(ringGradient),
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = heroText,
            )
            Text(
                text = "индекс",
                style = MaterialTheme.typography.labelSmall,
                color = heroText.copy(alpha = 0.85f),
            )
        }
    }
}
