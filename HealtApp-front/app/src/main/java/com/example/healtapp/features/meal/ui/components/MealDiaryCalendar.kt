package com.example.healtapp.features.meal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healtapp.core.common.LocaleRu
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.preferences.MealSlotPhotoStore
import com.example.healtapp.features.meal.presentation.MealViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val dayTitleFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", LocaleRu)

private val mealSlotLabels = listOf(
    "breakfast" to "Завтрак",
    "lunch" to "Обед",
    "snack" to "Перекус",
    "dinner" to "Ужин",
)

data class MealDayBucket(
    val date: LocalDate,
    val meals: List<MealDto>,
    val kcal: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDiaryCalendar(
    meals: List<MealDto>,
    photoTick: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonth by remember { mutableStateOf(false) }
    var detailDate by remember { mutableStateOf<LocalDate?>(null) }

    val byDate = remember(meals) {
        meals.groupBy { it.meal_time.take(10) }
            .mapNotNull { (key, dayMeals) ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
                key to MealDayBucket(
                    date = date,
                    meals = dayMeals.sortedBy { it.meal_time },
                    kcal = dayMeals.sumOf { (it.calories ?: 0f).toInt() },
                    protein = dayMeals.sumOf { (it.protein_g ?: 0f).toDouble() }.toFloat(),
                    fat = dayMeals.sumOf { (it.fat_g ?: 0f).toDouble() }.toFloat(),
                    carbs = dayMeals.sumOf { (it.carbs_g ?: 0f).toDouble() }.toFloat(),
                )
            }
            .toMap()
    }

    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0L..6L).map { weekStart.plusDays(it) }
    val monthTitle = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, LocaleRu)
        .replaceFirstChar { it.uppercase() }
    val canGoNext = yearMonth.isBefore(YearMonth.now())
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Дневник по дням")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (showMonth) "$monthTitle ${yearMonth.year}" else "Эта неделя",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (showMonth) "Неделя" else "Месяц",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMonth = !showMonth }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (!showMonth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        weekDates.forEach { date ->
                            val bucket = byDate[date.toString()]
                            val hasPhoto = remember(date, photoTick) {
                                MealSlotPhotoStore.photosForDate(context, date.toString()).isNotEmpty()
                            }
                            MealWeekDayCell(
                                date = date,
                                hasMeals = bucket != null,
                                hasPhoto = hasPhoto,
                                isToday = date == today,
                                onClick = { detailDate = date },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
                        }
                        Text(
                            text = "$monthTitle ${yearMonth.year}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(
                            onClick = { yearMonth = yearMonth.plusMonths(1) },
                            enabled = canGoNext,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = muted,
                            )
                        }
                    }
                    val firstOfMonth = yearMonth.atDay(1)
                    val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7
                    val daysInMonth = yearMonth.lengthOfMonth()
                    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
                    for (week in 0 until totalCells / 7) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (dow in 0 until 7) {
                                val cell = week * 7 + dow
                                val dayNum = cell - startOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = yearMonth.atDay(dayNum)
                                    val bucket = byDate[date.toString()]
                                    val hasPhoto = remember(date, photoTick) {
                                        MealSlotPhotoStore.photosForDate(context, date.toString()).isNotEmpty()
                                    }
                                    MealMonthDayCell(
                                        day = dayNum,
                                        hasMeals = bucket != null,
                                        hasPhoto = hasPhoto,
                                        isToday = date == today,
                                        onClick = { detailDate = date },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailDate?.let { date ->
        val bucket = byDate[date.toString()]
        val photos = remember(date, photoTick) {
            MealSlotPhotoStore.photosForDate(context, date.toString())
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { detailDate = null },
            sheetState = sheetState,
        ) {
            MealDayDetailSheet(
                date = date,
                bucket = bucket,
                photos = photos,
                onClose = { detailDate = null },
            )
        }
    }
}

@Composable
private fun MealWeekDayCell(
    date: LocalDate,
    hasMeals: Boolean,
    hasPhoto: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, LocaleRu)
        .take(2)
        .replaceFirstChar { it.uppercase() }
    val fill = when {
        hasMeals -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(fill)
                .border(
                    1.dp,
                    if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (hasPhoto) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

@Composable
private fun MealMonthDayCell(
    day: Int,
    hasMeals: Boolean,
    hasPhoto: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    hasMeals -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else -> Color.Transparent
                },
            )
            .border(
                width = if (isToday) 1.5.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasMeals) FontWeight.Bold else FontWeight.Medium,
            )
            if (hasPhoto) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

@Composable
private fun MealDayDetailSheet(
    date: LocalDate,
    bucket: MealDayBucket?,
    photos: Map<String, String>,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = date.format(dayTitleFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (bucket == null && photos.isEmpty()) {
            Text(
                text = "За этот день пока нет записей и фото.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (bucket != null) {
                Text(
                    text = "${bucket.kcal} ккал · Б ${bucket.protein.toInt()} / Ж ${bucket.fat.toInt()} / У ${bucket.carbs.toInt()} г",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                mealSlotLabels.forEach { (api, title) ->
                    val slotMeals = bucket.meals.filter { it.meal_type.equals(api, ignoreCase = true) }
                    val photo = photos[api]
                    if (slotMeals.isEmpty() && photo == null) return@forEach
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (photo != null) {
                            AsyncImage(
                                model = photo,
                                contentDescription = "Фото $title",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        slotMeals.forEach { meal ->
                            Text(
                                text = buildString {
                                    append(meal.name)
                                    meal.calories?.let { append(" · ${it.toInt()} ккал") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val other = bucket.meals.filter { m ->
                    mealSlotLabels.none { it.first.equals(m.meal_type, ignoreCase = true) }
                }
                if (other.isNotEmpty()) {
                    Text("Прочее", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    other.forEach { meal ->
                        Text(
                            text = "${MealViewModel.displayMealTypeFromApi(meal.meal_type)} · ${meal.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                photos.forEach { (api, path) ->
                    val title = mealSlotLabels.firstOrNull { it.first == api }?.second ?: api
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    AsyncImage(
                        model = path,
                        contentDescription = "Фото $title",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Text(
            text = "Закрыть",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClose)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
