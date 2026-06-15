package com.example.healtapp.features.activity.ui.components

import com.example.healtapp.core.ui.animation.AppMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconTintColor
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.features.activity.presentation.TrainingTypeDef
import com.example.healtapp.features.activity.presentation.allTrainingTypes
import com.example.healtapp.features.activity.presentation.trainingFormFieldsFor

fun trainingIconForSlug(slug: String): ImageVector = when (slug.lowercase()) {
    "run" -> Icons.AutoMirrored.Filled.DirectionsRun
    "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
    "swim" -> Icons.Filled.Pool
    "yoga", "stretch", "pilates" -> Icons.Filled.SelfImprovement
    "strength", "crossfit" -> Icons.Filled.FitnessCenter
    "hiit" -> Icons.Filled.LocalFireDepartment
    "elliptical" -> Icons.Filled.Sports
    "rowing" -> Icons.Filled.Rowing
    "dance" -> Icons.Filled.Sports
    "boxing" -> Icons.Filled.SportsMartialArts
    "football" -> Icons.Filled.SportsSoccer
    "tennis" -> Icons.Filled.SportsTennis
    "hiking" -> Icons.Filled.Hiking
    else -> Icons.AutoMirrored.Filled.DirectionsRun
}

@Composable
fun ActivityTrainingStatsCard(
    minutesToday: Int,
    countToday: Int,
    caloriesToday: Int,
    minutesWeek: Int,
    countWeek: Int,
    caloriesWeek: Int,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Тренировки сегодня",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TrainingStatTile(
                    icon = Icons.Filled.Timer,
                    value = "$minutesToday мин",
                    label = "Время",
                    modifier = Modifier.weight(1f),
                )
                TrainingStatTile(
                    icon = Icons.Filled.Sports,
                    value = countToday.toString(),
                    label = "Занятий",
                    modifier = Modifier.weight(1f),
                )
                TrainingStatTile(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "$caloriesToday",
                    label = "Ккал",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "За 7 дней: $minutesWeek мин · $countWeek занятий · $caloriesWeek ккал",
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
            )
        }
    }
}

@Composable
private fun TrainingStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentSecondaryColor())
        }
    }
}

@Composable
fun ActivityTrainingQuickPickRow(
    quickPicks: List<TrainingTypeDef>,
    favoriteSlugs: Set<String>,
    onSelectType: (TrainingTypeDef) -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Добавить тренировку",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (favoriteSlugs.isEmpty()) {
                    "Частые типы или выберите из полного каталога"
                } else {
                    "Избранное и частые — нажмите «Ещё» для всех типов"
                },
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                quickPicks.take(3).forEach { type ->
                    TrainingTypeTile(
                        title = type.titleRu,
                        icon = trainingIconForSlug(type.slug),
                        isFavorite = type.slug in favoriteSlugs,
                        onClick = { onSelectType(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                TrainingMoreTile(
                    onClick = onOpenCatalog,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrainingTypeTile(
    title: String,
    icon: ImageVector,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grad = cardHeaderGradient(themedCardMint(), 0.5f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(grad)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTintColor(), modifier = Modifier.size(24.dp))
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
        }
    }
}

@Composable
private fun TrainingMoreTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.55f) })),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "Ещё",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            minLines = 2,
        )
    }
}

@Composable
fun ActivityTrainingCatalogScreen(
    favoriteSlugs: Set<String>,
    onBack: () -> Unit,
    onSelectType: (TrainingTypeDef) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Все тренировки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Нажмите на тип — откроется форма. Звёздочка — в избранное.",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            allTrainingTypes.chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowTypes.forEach { type ->
                        CatalogTypeCard(
                            type = type,
                            isFavorite = type.slug in favoriteSlugs,
                            onSelect = { onSelectType(type) },
                            onToggleFavorite = { onToggleFavorite(type.slug) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowTypes.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogTypeCard(
    type: TrainingTypeDef,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(cardHeaderGradient(themedCardBlue(), 0.45f))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(trainingIconForSlug(type.slug), null, tint = iconTintColor())
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else contentSecondaryColor(),
                    )
                }
            }
            Text(type.titleRu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(type.category, style = MaterialTheme.typography.labelSmall, color = contentSecondaryColor())
            AppButton(text = "Выбрать", onClick = onSelect, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityTrainingLogScreen(
    activityType: String,
    durationMinutes: String,
    onDurationChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    distanceKm: String,
    onDistanceChange: (String) -> Unit,
    intensity: String,
    intensityOptions: List<String>,
    onIntensitySelected: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    perceivedExertion: String,
    onPerceivedExertionChange: (String) -> Unit,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slug = remember(activityType) {
        allTrainingTypes.firstOrNull { it.titleRu == activityType }?.slug ?: "run"
    }
    val fields = remember(activityType) { trainingFormFieldsFor(activityType) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.55f) })),
                contentAlignment = Alignment.Center,
            ) {
                Icon(trainingIconForSlug(slug), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(activityType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Заполните детали и сохраните в дневник",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppTextField(
                    value = durationMinutes,
                    onValueChange = onDurationChange,
                    label = "Длительность (мин)",
                )
                AppTextField(
                    value = calories,
                    onValueChange = onCaloriesChange,
                    label = "Сожжённые калории (ккал)",
                )
                if (fields.showDistance) {
                    AppTextField(
                        value = distanceKm,
                        onValueChange = onDistanceChange,
                        label = fields.distanceLabel,
                    )
                }
                if (fields.showExertion) {
                    AppTextField(
                        value = perceivedExertion,
                        onValueChange = onPerceivedExertionChange,
                        label = "Оценка нагрузки (1–10)",
                    )
                    Text(
                        "1 — легко, 10 — максимум",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentSecondaryColor(),
                    )
                }
                if (fields.showNotes) {
                    AppTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        label = fields.notesLabel,
                        placeholder = fields.notesPlaceholder,
                    )
                }
                Text("Интенсивность", style = MaterialTheme.typography.labelLarge, color = contentSecondaryColor())
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    intensityOptions.forEach { level ->
                        FilterChip(
                            selected = intensity == level,
                            onClick = { onIntensitySelected(level) },
                            label = { Text(level) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
                AppButton(
                    text = if (isSaving) "Сохраняем…" else "Сохранить тренировку",
                    onClick = onSave,
                    enabled = !isSaving,
                )
            }
        }
    }
}

enum class ActivityTrainingPane { Hub, Catalog, Form }

@Composable
fun ActivityTrainingSection(
    minutesToday: Int,
    countToday: Int,
    caloriesToday: Int,
    minutesWeek: Int,
    countWeek: Int,
    caloriesWeek: Int,
    quickPicks: List<TrainingTypeDef>,
    favoriteSlugs: Set<String>,
    onSelectType: (TrainingTypeDef) -> Unit,
    onOpenCatalog: () -> Unit,
    historyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title = "Тренировки", subtitle = "Быстрый старт и дневник")
        ActivityTrainingStatsCard(
            minutesToday = minutesToday,
            countToday = countToday,
            caloriesToday = caloriesToday,
            minutesWeek = minutesWeek,
            countWeek = countWeek,
            caloriesWeek = caloriesWeek,
        )
        ActivityTrainingQuickPickRow(
            quickPicks = quickPicks,
            favoriteSlugs = favoriteSlugs,
            onSelectType = onSelectType,
            onOpenCatalog = onOpenCatalog,
        )
        historyContent()
    }
}
