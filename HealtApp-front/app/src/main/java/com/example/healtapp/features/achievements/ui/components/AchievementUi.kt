package com.example.healtapp.features.achievements.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.healtapp.core.common.ShareUtils
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import kotlinx.coroutines.launch

fun achievementIcon(key: String): ImageVector = when (key) {
    "steps" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "water" -> Icons.Filled.WaterDrop
    "sleep" -> Icons.Filled.Nightlight
    "fire" -> Icons.Filled.LocalFireDepartment
    "workout" -> Icons.Filled.FitnessCenter
    "run" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "speed" -> Icons.Filled.Speed
        "moon" -> Icons.Filled.ModeNight
        "apple" -> Icons.Filled.EmojiEvents
        else -> Icons.Filled.Star
}

fun formatAchievementProgressValue(value: Float, unit: String?): String = when (unit) {
    "км", "л" -> "${"%.1f".format(value)} $unit"
    "ч" -> "${"%.1f".format(value)} ч"
    null -> value.toInt().toString()
    else -> "${value.toInt()} $unit"
}

@Composable
fun ShareAchievementDialog(
    periodLabel: String,
    steps: String?,
    water: String?,
    sleep: String?,
    healthScore: Int?,
    onDismiss: () -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Поделиться прогрессом",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // The card to capture
                ShareableAchievementCard(
                    periodLabel = periodLabel,
                    steps = steps,
                    water = water,
                    sleep = sleep,
                    healthScore = healthScore,
                    graphicsLayer = graphicsLayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    AppButton(
                        text = "Отправить",
                        onClick = {
                            coroutineScope.launch {
                                val bitmap = graphicsLayer.toImageBitmap()
                                ShareUtils.shareImageBitmap(context, bitmap)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ShareableAchievementCard(
    modifier: Modifier = Modifier,
    periodLabel: String,
    steps: String?,
    water: String?,
    sleep: String?,
    healthScore: Int?,
    graphicsLayer: GraphicsLayer
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MintPrimary, SkyPrimary)
                )
            )
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HealthApp",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Мой прогресс",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp
            )
            Text(
                text = periodLabel,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                steps?.let {
                    ShareStatRow(icon = Icons.AutoMirrored.Filled.DirectionsWalk, label = "Шаги", value = it)
                }
                water?.let {
                    ShareStatRow(icon = Icons.Filled.WaterDrop, label = "Вода", value = it)
                }
                sleep?.let {
                    ShareStatRow(icon = Icons.Filled.Nightlight, label = "Сон", value = it)
                }
                healthScore?.takeIf { it > 0 }?.let {
                    ShareStatRow(icon = Icons.Filled.Favorite, label = "Индекс здоровья", value = "$it/100")
                }
            }
        }
    }
}

@Composable
private fun ShareStatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
