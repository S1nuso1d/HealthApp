package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.BmiHelper
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.bmiCategoryColor
import com.example.healtapp.core.ui.theme.bmiScaleGradient

@Composable
fun ProfileBodyStatsCard(
    heightCm: String,
    weightKg: String,
    onEditBody: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = heightCm.toFloatOrNull()
    val weight = weightKg.toFloatOrNull()
    val bmi = BmiHelper.calculate(height, weight)
    val range = height?.let { BmiHelper.healthyWeightRangeKg(it) }

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(
                        title = "Показатели тела",
                        subtitle = "ИМТ по росту и весу из профиля",
                    )
                }
                IconButton(onClick = onEditBody) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Изменить рост и вес")
                }
            }

            if (bmi == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Укажите рост и вес в «Основных данных», чтобы увидеть ИМТ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BmiGauge(
                        bmi = bmi.value,
                        category = bmi.category,
                        modifier = Modifier.weight(0.42f),
                    )
                    Column(
                        modifier = Modifier.weight(0.58f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Text(
                                text = bmi.labelRu,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = bmi.hintRu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildString {
                                append("Рост ${heightCm.ifBlank { "—" }} см · вес ${weightKg.ifBlank { "—" }} кг")
                                range?.let { (lo, hi) ->
                                    append("\nНорма веса: ${"%.0f".format(lo)}–${"%.0f".format(hi)} кг")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BmiGauge(
    bmi: Float,
    category: BmiHelper.Category,
    modifier: Modifier = Modifier,
) {
    val fraction = ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f)
    val markerColor = bmiCategoryColor(category)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = BmiHelper.formatValue(bmi),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "ИМТ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.horizontalGradient(bmiScaleGradient()),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(markerColor.copy(alpha = 0.25f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .padding(end = 2.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.02f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(markerColor),
                )
            }
        }
    }
}
