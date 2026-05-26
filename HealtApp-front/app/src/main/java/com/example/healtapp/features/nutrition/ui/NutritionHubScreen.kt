package com.example.healtapp.features.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.features.hydration.presentation.HydrationViewModel
import com.example.healtapp.features.meal.presentation.MealViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.theme.accentColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconTintColor
import com.example.healtapp.features.hydration.ui.HydrationTabContent
import com.example.healtapp.features.meal.ui.MealTabContent
import com.example.healtapp.features.meal.ui.MyDishesTab
import com.example.healtapp.features.meal.ui.components.MealDiaryGuideOverlay
import com.example.healtapp.features.meal.ui.components.MealDiaryGuidePrefs

/** Вкладки: 0 — Питание, 1 — Мои блюда, 2 — Вода */
@Composable
fun NutritionHubScreen(initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val snackbarHostState = remember { SnackbarHostState() }
    var openDishBuilder by remember { mutableStateOf(false) }
    val mealViewModel: MealViewModel = hiltViewModel()
    val hydrationViewModel: HydrationViewModel = hiltViewModel()
    val mealUiState by mealViewModel.uiState.collectAsStateWithLifecycle()
    val hydrationUiState by hydrationViewModel.uiState.collectAsStateWithLifecycle()
    var refreshTriggered by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showMealGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showMealGuide = MealDiaryGuidePrefs.shouldShow(context)
    }

    val isRefreshing = when (tab) {
        0 -> mealUiState.isLoading && mealUiState.mealHistory.isNotEmpty()
        2 -> hydrationUiState.isLoading &&
            (hydrationUiState.waterToday > 0 || hydrationUiState.todayRecords.isNotEmpty())
        else -> false
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) refreshTriggered = true
        else if (refreshTriggered) {
            snackbarHostState.showSnackbar("Обновлено")
            refreshTriggered = false
        }
    }

    val (title, subtitle, icon) = when (tab) {
        0 -> Triple("Питание", "Дневник приёмов пищи и КБЖУ", Icons.Filled.Restaurant)
        1 -> Triple("Мои блюда", "Составные блюда из продуктов", Icons.Filled.MenuBook)
        else -> Triple("Вода", "Гидратация и быстрый ввод", Icons.Filled.WaterDrop)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(
                    onClick = { openDishBuilder = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Новое блюдо")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        PullToRefreshContainer(
            isRefreshing = isRefreshing,
            onRefresh = {
                when (tab) {
                    0 -> mealViewModel.refresh()
                    2 -> hydrationViewModel.load()
                    else -> Unit
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            AppScreen(
                title = title,
                subtitle = subtitle,
                headerIcon = icon,
                scrollable = true,
            ) {
                HubTabRow(selected = tab, onSelect = { tab = it })

                when (tab) {
                    0 -> MealTabContent(snackbarHostState = snackbarHostState)
                    1 -> MyDishesTab(
                        snackbarHostState = snackbarHostState,
                        openBuilderRequest = openDishBuilder,
                        onBuilderRequestConsumed = { openDishBuilder = false },
                    )
                    else -> HydrationTabContent()
                }
            }
        }

            MealDiaryGuideOverlay(
                visible = tab == 0 && showMealGuide,
                onDismiss = {
                    MealDiaryGuidePrefs.markSeen(context)
                    showMealGuide = false
                },
            )
        }
    }
}

@Composable
private fun HubTabRow(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple(0, "Питание", Icons.Filled.Restaurant),
        Triple(1, "Мои блюда", Icons.Filled.MenuBook),
        Triple(2, "Вода", Icons.Filled.WaterDrop),
    )
    TabRow(
        selectedTabIndex = selected,
        modifier = Modifier.fillMaxWidth(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        indicator = { tabPositions ->
            if (selected < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selected]),
                    color = accentColor(),
                )
            }
        },
    ) {
        tabs.forEach { (index, label, icon) ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected == index) accentColor() else contentSecondaryColor(),
                    )
                },
                icon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.height(20.dp),
                        tint = if (selected == index) iconTintColor() else contentSecondaryColor(),
                    )
                },
            )
        }
    }
}
