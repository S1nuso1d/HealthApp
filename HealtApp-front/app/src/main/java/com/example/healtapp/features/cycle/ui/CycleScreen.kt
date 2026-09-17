package com.example.healtapp.features.cycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.CycleCalculator
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import com.example.healtapp.features.aicoach.ui.components.AiInlineNotice
import com.example.healtapp.features.cycle.presentation.CycleViewModel
import com.example.healtapp.features.cycle.ui.components.CycleAddEntryDock
import com.example.healtapp.features.cycle.ui.components.CycleAddSheet
import com.example.healtapp.features.cycle.ui.components.CycleCalendarCard
import com.example.healtapp.features.cycle.ui.components.CycleCollapsibleHistorySection
import com.example.healtapp.features.cycle.ui.components.CycleDayMoodCard
import com.example.healtapp.features.cycle.ui.components.CycleForecastCard
import com.example.healtapp.features.cycle.ui.components.CycleHeroBar
import com.example.healtapp.features.cycle.ui.components.CycleMyCyclesCard
import com.example.healtapp.features.cycle.ui.components.CyclePhaseBubble
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    onBack: () -> Unit,
) {
    val viewModel: CycleViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<CycleEntryDto?>(null) }
    var prefillStart by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember { LocalDate.now() }
    var displayMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    val insight = remember(uiState.entries, today) { CycleCalculator.buildInsight(uiState.entries, today) }
    val stats = remember(uiState.entries) { CycleCalculator.buildCycleStats(uiState.entries) }
    val monthDays = remember(uiState.entries, displayMonth, today) {
        CycleCalculator.monthDays(displayMonth, uiState.entries, today)
    }
    val selectedDay = remember(monthDays, selectedDate) {
        monthDays.find { it.date == selectedDate }
    }
    val selectedMood = remember(uiState.entries, selectedDate) {
        CycleCalculator.moodForDate(uiState.entries, selectedDate)
    }

    fun openNewEntry(start: LocalDate? = null) {
        editingEntry = null
        prefillStart = start
        showAddSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(screenBackgroundGradient()))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CycleHeroBar(
                phaseLabel = insight.phase.labelRu,
                cycleDay = insight.cycleDay,
                entriesCount = uiState.entries.size,
                isLate = insight.isLate,
                daysLate = insight.daysLate,
                isDueToday = insight.isDueToday,
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.error?.let { error ->
                    item(key = "error") {
                        AiInlineNotice(text = error, isError = true)
                    }
                }

                item(key = "phase") {
                    CyclePhaseBubble(
                        insight = insight,
                        onMarkPeriodStart = if (insight.isLate || insight.isDueToday) {
                            { openNewEntry(today) }
                        } else {
                            null
                        },
                    )
                }

                if (insight.nextPeriodDate != null) {
                    item(key = "forecast") {
                        CycleForecastCard(insight = insight, stats = stats)
                    }
                }

                uiState.insights?.let { cycleInsights ->
                    if (
                        !cycleInsights.recoveryTip.isNullOrBlank() ||
                        cycleInsights.observations.isNotEmpty() ||
                        cycleInsights.phaseStats.any { it.days > 0 }
                    ) {
                        item(key = "phase_link") {
                            AppCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Фаза и восстановление",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        buildString {
                                            append("Средний цикл ${cycleInsights.averageCycleLength} дней")
                                            append(" · месячные ${cycleInsights.averagePeriodLength} дн.")
                                            cycleInsights.currentPhaseTitle?.let { title ->
                                                append(" · сейчас $title")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    cycleInsights.recoveryTip?.let { tip ->
                                        Text(
                                            tip,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    cycleInsights.phaseStats.filter { it.days > 0 }.forEach { stat ->
                                        val minutes = stat.activityMinutes?.let { min ->
                                            "${min.toInt()} мин"
                                        } ?: "—"
                                        val sleep = stat.sleepHours?.let { h ->
                                            "%.1f ч".format(h)
                                        } ?: "—"
                                        Text(
                                            "${stat.phaseTitle}: сон $sleep · активность $minutes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    cycleInsights.observations.take(3).forEach { obs ->
                                        Text("• ${obs.text.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (cycleInsights.observations.isEmpty() && cycleInsights.recoveryTip.isNullOrBlank()) {
                                        Text(
                                            cycleInsights.message
                                                ?: "Добавь ещё циклы и записи сна — здесь появятся наблюдения.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "calendar") {
                    CycleCalendarCard(
                        month = displayMonth,
                        monthDays = monthDays,
                        selectedDate = selectedDate,
                        onSelectDate = { selectedDate = it },
                        onPreviousMonth = { displayMonth = displayMonth.minusMonths(1) },
                        onNextMonth = { displayMonth = displayMonth.plusMonths(1) },
                    )
                }

                item(key = "day_mood") {
                    CycleDayMoodCard(
                        date = selectedDate,
                        mood = selectedMood,
                        phase = selectedDay?.phase ?: insight.phase,
                        cycleDay = selectedDay?.cycleDay,
                    )
                }

                if (uiState.entries.isNotEmpty()) {
                    item(key = "my_cycles") {
                        CycleMyCyclesCard(stats = stats)
                    }
                }

                if (uiState.entries.isNotEmpty()) {
                    item(key = "history") {
                        CycleCollapsibleHistorySection(
                            entries = uiState.entries,
                            onDelete = viewModel::deleteEntry,
                            onEdit = { entry ->
                                editingEntry = entry
                                prefillStart = null
                                showAddSheet = true
                            },
                            initiallyExpanded = false,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp, top = 4.dp),
            ) {
                CycleAddEntryDock(onClick = { openNewEntry() })
            }
        }
    }

    CycleAddSheet(
        visible = showAddSheet,
        initialEntry = editingEntry,
        initialStartDate = prefillStart,
        onDismiss = {
            showAddSheet = false
            editingEntry = null
            prefillStart = null
        },
        onSave = { start, end, symptoms, notes ->
            val editing = editingEntry
            if (editing != null) {
                viewModel.updateEntry(editing.id, start, end, symptoms, notes)
            } else {
                viewModel.addEntry(start, end, symptoms, notes)
            }
            showAddSheet = false
            editingEntry = null
            prefillStart = null
        },
    )
}
