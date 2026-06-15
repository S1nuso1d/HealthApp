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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.CycleCalculator
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.features.aicoach.ui.components.AiInlineNotice
import com.example.healtapp.features.cycle.presentation.CycleViewModel
import com.example.healtapp.features.cycle.ui.components.CycleAddEntryDock
import com.example.healtapp.features.cycle.ui.components.CycleAddSheet
import com.example.healtapp.features.cycle.ui.components.CycleCalendarCard
import com.example.healtapp.features.cycle.ui.components.CycleCollapsibleHistorySection
import com.example.healtapp.features.cycle.ui.components.CycleDayMoodCard
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
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                uiState.error?.let { error ->
                    item(key = "error") {
                        AiInlineNotice(text = error, isError = true)
                    }
                }

                item(key = "phase") {
                    CyclePhaseBubble(insight = insight)
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
                CycleAddEntryDock(onClick = { showAddSheet = true })
            }
        }
    }

    CycleAddSheet(
        visible = showAddSheet,
        onDismiss = { showAddSheet = false },
        onSave = { start, end, symptoms, notes ->
            viewModel.addEntry(start, end, symptoms, notes)
            showAddSheet = false
        },
    )
}
