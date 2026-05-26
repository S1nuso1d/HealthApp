package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDayDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val sleepColor = Color(0xFF7B6FD6)
private val waterColor = Color(0xFF4BA3E3)
private val stepsColor = Color(0xFF5EC9A0)
private val burnedColor = Color(0xFFF5A623)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardGoalsCalendarBlock(
    yearMonth: YearMonth,
    days: List<GoalsCalendarDayDto>,
    isLoading: Boolean,
    selectedDate: LocalDate?,
    detailDate: LocalDate?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthTitle = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
        .replaceFirstChar { it.uppercase() }
    val byDate = days.associateBy { it.date }
    val dayNumberColor = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Календарь целей",
            subtitle = "Нажмите на день — детали",
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
                    }
                    Text(
                        text = "$monthTitle ${yearMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = dayNumberColor,
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
                    }
                }

                if (isLoading && days.isEmpty()) {
                    Text("Загрузка…", color = mutedText)
                } else {
                    val firstOfMonth = yearMonth.atDay(1)
                    val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7
                    val daysInMonth = yearMonth.lengthOfMonth()
                    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

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
                                color = mutedText,
                            )
                        }
                    }

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
                                    val dto = byDate[date.toString()]
                                    GoalDayCell(
                                        day = dayNum,
                                        dto = dto,
                                        selected = selectedDate == date,
                                        dayNumberColor = dayNumberColor,
                                        onClick = { onDayClick(date) },
                                        onDoubleClick = { onDayClick(date) },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }

                    GoalsCalendarLegend(mutedText = mutedText)
                }
            }
        }
    }

    detailDate?.let { date ->
        val dto = byDate[date.toString()]
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissDetail,
            sheetState = sheetState,
        ) {
            if (dto != null) {
                GoalsDayDetailContent(date = date, dto = dto, onClose = onDismissDetail)
            } else {
                GoalsDayDetailEmpty(date = date, onClose = onDismissDetail)
            }
        }
    }
}

@Composable
private fun GoalsCalendarLegend(mutedText: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Под датой — четыре полоски: сон, вода, шаги, сожжённые ккал. Длина = прогресс к цели.",
            style = MaterialTheme.typography.labelMedium,
            color = mutedText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendItem("Сон", sleepColor, Modifier.weight(1f), mutedText)
            LegendItem("Вода", waterColor, Modifier.weight(1f), mutedText)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendItem("Шаги", stepsColor, Modifier.weight(1f), mutedText)
            LegendItem("Сожжено", burnedColor, Modifier.weight(1f), mutedText)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier, textColor: Color) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalDayCell(
    day: Int,
    dto: GoalsCalendarDayDto?,
    selected: Boolean,
    dayNumberColor: Color,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasData = dto?.has_any_data == true
    val allMet = dto?.all_goals_met == true
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        allMet -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        hasData -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        else -> Color.Transparent
    }
    val bgColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        allMet -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        hasData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (selected || hasData || allMet) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = dayNumberColor,
            )
            GoalMetricsStrip(
                sleep = dto?.sleep_progress ?: 0f,
                water = dto?.hydration_progress ?: 0f,
                steps = dto?.activity_progress ?: 0f,
                burned = dto?.nutrition_progress ?: 0f,
                hasSleep = (dto?.sleep_hours ?: 0f) > 0f,
                hasWater = (dto?.water_ml ?: 0) > 0,
                hasSteps = (dto?.steps ?: 0) > 0,
                hasBurned = (dto?.calories_burned ?: 0f) > 0f,
            )
        }
        if (allMet) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private data class GoalMetricSegment(
    val progress: Float,
    val color: Color,
    val hasData: Boolean,
)

/** Четыре отдельные полоски под числом — не сливаются, как на дашборде. */
@Composable
private fun GoalMetricsStrip(
    sleep: Float,
    water: Float,
    steps: Float,
    burned: Float,
    hasSleep: Boolean,
    hasWater: Boolean,
    hasSteps: Boolean,
    hasBurned: Boolean,
) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val segments = listOf(
        GoalMetricSegment(sleep, sleepColor, hasSleep),
        GoalMetricSegment(water, waterColor, hasWater),
        GoalMetricSegment(steps, stepsColor, hasSteps),
        GoalMetricSegment(burned, burnedColor, hasBurned),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .height(5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackColor),
            ) {
                if (segment.hasData) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(segment.progress.coerceIn(0.08f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(segment.color),
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsDayDetailEmpty(
    date: LocalDate,
    onClose: () -> Unit,
) {
    val title = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "За этот день нет записей в сводке.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
private fun GoalsDayDetailContent(
    date: LocalDate,
    dto: GoalsCalendarDayDto,
    onClose: () -> Unit,
) {
    val title = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
    val valueColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val burned = dto.calories_burned.takeIf { it > 0f } ?: 0f
    val consumed = dto.calories_consumed.takeIf { it > 0f }
        ?: dto.calories.takeIf { it > 0f }
        ?: 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )

        if (!dto.has_any_data) {
            Text(
                text = "За этот день нет записей в сводке.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        } else {
            DetailMetricRow(
                label = "Сон",
                value = "${"%.1f".format(dto.sleep_hours)} ч",
                progress = dto.sleep_progress,
                color = sleepColor,
                valueColor = valueColor,
                mutedColor = muted,
            )
            DetailMetricRow(
                label = "Вода",
                value = "${dto.water_ml} мл",
                progress = dto.hydration_progress,
                color = waterColor,
                valueColor = valueColor,
                mutedColor = muted,
            )
            DetailMetricRow(
                label = "Шаги",
                value = "%,d".format(dto.steps).replace(',', ' '),
                progress = dto.activity_progress,
                color = stepsColor,
                valueColor = valueColor,
                mutedColor = muted,
            )
            DetailMetricRow(
                label = "Сожжено",
                value = "${burned.toInt()} ккал",
                progress = dto.nutrition_progress,
                color = burnedColor,
                valueColor = valueColor,
                mutedColor = muted,
            )
            if (consumed > 0f) {
                Text(
                    text = "Съедено: ${consumed.toInt()} ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
            if (dto.protein_g > 0f || dto.fat_g > 0f || dto.carbs_g > 0f) {
                Text(
                    text = "БЖУ: ${dto.protein_g.toInt()} / ${dto.fat_g.toInt()} / ${dto.carbs_g.toInt()} г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
            if (dto.meals.isNotEmpty()) {
                Text(
                    text = "Приёмы пищи",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                )
                dto.meals.forEach { meal ->
                    val typeRu = when (meal.meal_type.lowercase()) {
                        "breakfast" -> "Завтрак"
                        "lunch" -> "Обед"
                        "dinner" -> "Ужин"
                        "snack" -> "Перекус"
                        "drink" -> "Напиток"
                        else -> meal.meal_type
                    }
                    Text(
                        text = "$typeRu · ${meal.name} — ${meal.calories.toInt()} ккал",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
            if (dto.all_goals_met) {
                Text(
                    text = "Все цели дня выполнены",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
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
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailMetricRow(
    label: String,
    value: String,
    progress: Float,
    color: Color,
    valueColor: Color,
    mutedColor: Color,
) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = valueColor,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor,
                    )
                }
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}
