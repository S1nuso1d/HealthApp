package com.example.healtapp.features.pills.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.data.network.dto.health.PillDto
import com.example.healtapp.data.preferences.PillDoseStatus
import com.example.healtapp.features.aicoach.ui.components.AiInlineNotice
import com.example.healtapp.features.pills.presentation.PillsViewModel
import com.example.healtapp.features.pills.ui.components.PillReminderBubble
import com.example.healtapp.features.pills.ui.components.PillReminderSheet
import com.example.healtapp.features.pills.ui.components.PillsAddReminderDock
import com.example.healtapp.features.pills.ui.components.PillsFilterStrip
import com.example.healtapp.features.pills.ui.components.PillsHeroBar
import com.example.healtapp.features.pills.ui.components.PillsWelcomePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillsScreen(
    onBack: () -> Unit,
    viewModel: PillsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var editingPill by remember { mutableStateOf<PillDto?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* отказ — уведомления не покажутся, пока пользователь не разрешит в настройках */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val activePills = uiState.pills.filter { it.isActive }
    val displayed = when (tab) {
        0 -> activePills
        else -> uiState.pills
    }

    LaunchedEffect(displayed.size, tab) {
        if (displayed.isNotEmpty()) {
            listState.animateScrollToItem(displayed.lastIndex.coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(screenBackgroundGradient()))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PillsHeroBar(
                activeCount = activePills.size,
                adherencePercent = uiState.adherencePercent,
                onBack = onBack,
            )

            if (uiState.isLoading && uiState.pills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    state = listState,
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

                    item(key = "filters") {
                        PillsFilterStrip(
                            selectedTab = tab,
                            activeCount = activePills.size,
                            totalCount = uiState.pills.size,
                            onSelect = { tab = it },
                        )
                    }

                    if (displayed.isEmpty()) {
                        item(key = "welcome") {
                            PillsWelcomePanel(showActiveTab = tab == 0)
                        }
                    } else {
                        items(displayed, key = { pill -> pill.id ?: pill.hashCode() }) { pill ->
                            val pillId = pill.id
                            PillReminderBubble(
                                pill = pill,
                                todayStatus = pillId?.let { uiState.todayStatusByPillId[it] },
                                onTaken = {
                                    pillId?.let { viewModel.logDose(it, PillDoseStatus.Taken) }
                                },
                                onSkipped = {
                                    pillId?.let { viewModel.logDose(it, PillDoseStatus.Skipped) }
                                },
                                onEdit = { editingPill = pill },
                                onDelete = {
                                    pillId?.let { id -> viewModel.deletePill(id) }
                                },
                                onToggleActive = { isActive ->
                                    pillId?.let { id ->
                                        viewModel.updatePill(id, pill.name, pill.dosage, pill.timeOfDay, isActive)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp, top = 4.dp),
            ) {
                PillsAddReminderDock(
                    onClick = { showAddSheet = true },
                    enabled = !uiState.isLoading,
                )
            }
        }
    }

    PillReminderSheet(
        visible = showAddSheet,
        pill = null,
        onDismiss = { showAddSheet = false },
        onConfirm = { name, dosage, time ->
            viewModel.addPill(name, dosage, time)
            showAddSheet = false
        },
    )

    PillReminderSheet(
        visible = editingPill != null,
        pill = editingPill,
        onDismiss = { editingPill = null },
        onConfirm = { name, dosage, time ->
            editingPill?.id?.let { id ->
                viewModel.updatePill(id, name, dosage, time, editingPill?.isActive ?: true)
            }
            editingPill = null
        },
    )
}
