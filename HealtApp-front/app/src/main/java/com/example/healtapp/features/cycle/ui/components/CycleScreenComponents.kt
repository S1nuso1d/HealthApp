package com.example.healtapp.features.cycle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.CycleCalculator
import com.example.healtapp.core.common.LocaleRu
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.animation.appPressScale
import com.example.healtapp.core.common.DateRules
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.FeatureHeroBar
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.components.ProgressRing
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val cycleRuLocale = LocaleRu
private val cycleDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", cycleRuLocale)
private val cycleMonthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", cycleRuLocale)

internal fun formatCycleDate(isoDate: String): String {
    val date = CycleCalculator.parseDate(isoDate) ?: return isoDate
    return formatCycleDate(date)
}

internal fun formatCycleDate(date: LocalDate): String =
    date.format(cycleDateFormatter).replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(cycleRuLocale) else char.toString()
    }

@Composable
fun CycleHeroBar(
    phaseLabel: String,
    cycleDay: Int?,
    entriesCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeatureHeroBar(
        title = "Женское здоровье",
        subtitle = "Календарь цикла и симптомы",
        icon = Icons.Filled.CalendarMonth,
        onBack = onBack,
        modifier = modifier,
        footer = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = when {
                    cycleDay != null -> "$phaseLabel · день $cycleDay"
                    entriesCount > 0 -> "Записей: $entriesCount"
                    else -> "Добавьте первую запись цикла"
                },
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    )
}

@Composable
fun CyclePhaseBubble(
    insight: CycleCalculator.Insight,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Текущая фаза",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MintPrimary,
        )
        Text(
            text = "День цикла и прогноз",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )
        ProgressRing(
            progress = insight.cycleDay?.let { day ->
                (day.toFloat() / insight.averageCycleLength).coerceIn(0f, 1f)
            } ?: 0f,
            text = insight.cycleDay?.toString() ?: "—",
        )
        Text(
            text = insight.phase.labelRu,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildString {
                insight.cycleDay?.let { append("День $it из ${insight.averageCycleLength}") }
                insight.daysUntilNextPeriod?.let {
                    if (isNotEmpty()) append(" · ")
                    append("До месячных: $it дн.")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        insight.nextPeriodDate?.let { next ->
            Text(
                text = "Прогноз начала: ${formatCycleDate(next)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        insight.ovulationDate?.let { ovulation ->
            Text(
                text = "Овуляция: ${formatCycleDate(ovulation)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }
        CycleMoodBadge(mood = insight.todayMood)
    }
}

@Composable
fun CycleMoodBadge(
    mood: CycleCalculator.MoodInsight,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Mood,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Настроение сегодня: ${mood.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
            )
        }
        Text(
            text = mood.description,
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )
    }
}

@Composable
fun CycleDayMoodCard(
    date: LocalDate,
    mood: CycleCalculator.MoodInsight,
    phase: CycleCalculator.Phase,
    cycleDay: Int?,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                title = formatCycleDate(date),
                subtitle = buildString {
                    append(phase.labelRu)
                    cycleDay?.let { append(" · день $it") }
                },
            )
            CycleMoodBadge(mood = mood)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleCalendarCard(
    month: YearMonth,
    monthDays: List<CycleCalculator.CalendarDay>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий месяц")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Календарь",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = month.format(cycleMonthFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий месяц")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val offset = (month.atDay(1).dayOfWeek.value + 6) % 7
        val cells = List(offset) { null as CycleCalculator.CalendarDay? } + monthDays
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        CycleCalendarCell(
                            day = day,
                            selected = day.date == selectedDate,
                            onClick = { onSelectDate(day.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CycleLegendDot(color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f), label = "Месячные")
            CycleLegendDot(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), label = "Прогноз")
            CycleLegendDot(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f), label = "Овуляция")
            CycleLegendDot(color = SkyPrimary.copy(alpha = 0.65f), label = "Фертильность")
        }
    }
}

@Composable
private fun CycleCalendarCell(
    day: CycleCalculator.CalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        day.isLoggedPeriod -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
        day.isPredictedPeriod -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        day.isOvulation -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
        day.isFertile -> SkyPrimary.copy(alpha = 0.35f)
        day.isToday -> MintPrimary.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .then(
                when {
                    selected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    day.isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (day.isToday || selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CycleLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CycleCollapsibleHistorySection(
    entries: List<CycleEntryDto>,
    onDelete: (Int) -> Unit,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val interaction = remember { MutableInteractionSource() }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = AppMotion.springGentle(),
        label = "cycleHistoryChevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appPressScale(interaction, pressedScale = 0.995f)
                .clickable(interactionSource = interaction, indication = null) {
                    expanded = !expanded
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "История циклов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentPrimaryColor(),
                )
                Text(
                    text = "${entries.size} ${entriesCountLabel(entries.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                modifier = Modifier.rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                entries.forEach { entry ->
                    CycleHistoryBubble(
                        entry = entry,
                        onDelete = { onDelete(entry.id) },
                    )
                }
            }
        }
    }
}

private fun entriesCountLabel(count: Int): String = when {
    count % 100 in 11..14 -> "записей"
    count % 10 == 1 -> "запись"
    count % 10 in 2..4 -> "записи"
    else -> "записей"
}

@Composable
fun CycleHistoryBubble(
    entry: CycleEntryDto,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        CycleAvatar()
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Запись цикла",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MintPrimary,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = "Начало: ${formatCycleDate(entry.start_date)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentPrimaryColor(),
            )
            entry.end_date?.let {
                Text(
                    text = "Конец: ${formatCycleDate(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.symptoms?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CycleAddEntryDock(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(Brush.linearGradient(brandingGradient()))
            .appPressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Добавить запись цикла",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Симптомы, даты и заметки",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CycleAddSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate?, symptoms: String?, notes: String?) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var symptoms by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedStart by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEnd by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            symptoms = ""
            notes = ""
            selectedStart = null
            selectedEnd = null
        }
    }

    val canSave = selectedStart != null

    val symptomPresets = listOf(
        "Спазмы", "Головная боль", "Усталость", "Вздутие", "Перепады настроения", "Акне",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 0.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(brandingGradient())),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Новая запись",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Выберите начало и конец месячных на одном календаре",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }

                val parsedStart = selectedStart
                val parsedEnd = selectedEnd

                if (parsedStart != null) {
                        CycleEntryPreviewCard(
                            startDate = parsedStart,
                            endDate = parsedEnd,
                            symptoms = symptoms.trim(),
                            notes = notes.trim(),
                        )
                    }

                    GradientFormPanel {
                        Text(
                            text = "1. Период месячных",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Сначала день начала, затем день окончания — всё на одном календаре",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentSecondaryColor(),
                        )
                        CyclePeriodRangeCalendar(
                            selectedStart = parsedStart,
                            selectedEnd = parsedEnd,
                            onRangeChange = { start, end ->
                                selectedStart = start
                                selectedEnd = end
                            },
                        )
                        when {
                            parsedStart == null -> {
                                Text(
                                    text = "Нажмите день начала месячных",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            parsedEnd == null -> {
                                Text(
                                    text = "Начало: ${formatCycleDate(parsedStart)} · выберите день окончания",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            else -> {
                                Text(
                                    text = "${formatCycleDate(parsedStart)} — ${formatCycleDate(parsedEnd)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Text(
                            text = "2. Симптомы",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Выберите из списка или напишите свои",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentSecondaryColor(),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            symptomPresets.forEach { preset ->
                                val selected = symptoms.contains(preset, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        symptoms = if (selected) {
                                            symptoms.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() && !it.equals(preset, ignoreCase = true) }
                                                .joinToString(", ")
                                        } else {
                                            listOf(symptoms.trim(), preset)
                                                .filter { it.isNotBlank() }
                                                .joinToString(", ")
                                        }
                                    },
                                    label = { Text(preset) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipSelectedColor(themedCardMint()),
                                    ),
                                )
                            }
                        }
                        GradientOutlinedField(
                            value = symptoms,
                            onValueChange = { symptoms = it },
                            label = "Симптомы",
                            singleLine = false,
                            maxLines = 3,
                        )

                        Text(
                            text = "3. Заметки",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        GradientOutlinedField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Заметки (необязательно)",
                            singleLine = false,
                            maxLines = 4,
                        )
                    }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 28.dp),
            ) {
                AppButton(
                    text = when {
                        !canSave -> "Укажите дату начала"
                        selectedEnd == null -> "Сохранить (без даты окончания)"
                        else -> "Сохранить запись"
                    },
                    onClick = {
                        val start = selectedStart ?: return@AppButton
                        onSave(
                            start,
                            selectedEnd,
                            symptoms.trim().takeIf { it.isNotBlank() },
                            notes.trim().takeIf { it.isNotBlank() },
                        )
                    },
                    enabled = canSave,
                )
            }
        }
    }
}

@Composable
private fun CyclePeriodRangeCalendar(
    selectedStart: LocalDate?,
    selectedEnd: LocalDate?,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { DateRules.today() }
    val currentMonth = remember { YearMonth.from(today) }
    var displayMonth by remember { mutableStateOf(currentMonth) }

    LaunchedEffect(selectedStart) {
        if (selectedStart != null) {
            displayMonth = YearMonth.from(selectedStart)
        }
    }

    val rangeStart = listOfNotNull(selectedStart, selectedEnd).minOrNull()
    val rangeEnd = listOfNotNull(selectedStart, selectedEnd).maxOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий месяц")
            }
            Text(
                text = displayMonth.format(cycleMonthFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = { displayMonth = displayMonth.plusMonths(1) },
                enabled = displayMonth.isBefore(currentMonth),
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий месяц")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val firstDay = displayMonth.atDay(1)
        val offset = (firstDay.dayOfWeek.value + 6) % 7
        val monthDays = (1..displayMonth.lengthOfMonth()).map { displayMonth.atDay(it) }
        val cells = List(offset) { null as LocalDate? } + monthDays

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val enabled = !date.isAfter(today)
                        val inRange = rangeStart != null && rangeEnd != null &&
                            !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
                        val isStart = date == selectedStart
                        val isEnd = date == selectedEnd
                        CycleRangeDayCell(
                            date = date,
                            enabled = enabled,
                            inRange = inRange,
                            isStart = isStart,
                            isEnd = isEnd,
                            isToday = date == today,
                            onClick = {
                                if (!enabled) return@CycleRangeDayCell
                                val (newStart, newEnd) = when {
                                    selectedStart == null -> date to null
                                    selectedEnd == null && date.isBefore(selectedStart) -> date to null
                                    selectedEnd == null -> selectedStart to date
                                    else -> date to null
                                }
                                onRangeChange(newStart, newEnd)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun CycleRangeDayCell(
    date: LocalDate,
    enabled: Boolean,
    inRange: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        !enabled -> Color.Transparent
        isStart || isEnd -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        inRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        isToday -> MintPrimary.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .then(
                when {
                    isStart || isEnd -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun CycleEntryPreviewCard(
    startDate: LocalDate,
    endDate: LocalDate?,
    symptoms: String,
    notes: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CycleAvatar()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Так будет в истории",
                style = MaterialTheme.typography.labelSmall,
                color = contentSecondaryColor(),
            )
            Text(
                text = "Начало: ${formatCycleDate(startDate)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
            )
            endDate?.let {
                Text(
                    text = "Конец: ${formatCycleDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
            if (symptoms.isNotBlank()) {
                Text(
                    text = "Симптомы: $symptoms",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
            if (notes.isNotBlank()) {
                Text(
                    text = "Заметки: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
            }
        }
    }
}

@Composable
private fun CycleAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(brandingGradient())),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun CycleMyCyclesCard(
    stats: CycleCalculator.CycleStats,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "Мои циклы",
                subtitle = "Статистика по вашим записям",
            )
            CycleStatRow(
                label = "Длина предыдущего цикла",
                value = stats.previousCycleLength?.let { "$it дн." } ?: "—",
            )
            CycleStatRow(
                label = "Длина предыдущих месячных",
                value = stats.previousPeriodLength?.let { "$it дн." } ?: "—",
            )
            CycleStatRow(
                label = "Средняя длина цикла",
                value = "${stats.averageCycleLength} дн.",
            )
            CycleStatRow(
                label = "Средняя длина месячных",
                value = "${stats.averagePeriodLength} дн.",
            )
            CycleStatRow(
                label = "Колебания длины цикла",
                value = when {
                    stats.cycleLengthMin != null && stats.cycleLengthMax != null -> {
                        val spread = stats.cycleVariationDays ?: (stats.cycleLengthMax - stats.cycleLengthMin)
                        "${stats.cycleLengthMin}–${stats.cycleLengthMax} дн. (±$spread)"
                    }
                    else -> "Нужно больше записей"
                },
            )
        }
    }
}

@Composable
private fun CycleStatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentSecondaryColor(),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentPrimaryColor(),
        )
    }
}
