package com.example.healtapp.features.nutrition.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.FeatureGuideContent
import com.example.healtapp.core.ui.components.FeatureGuideOverlay
import com.example.healtapp.core.ui.components.FeatureGuidePrefs
import com.example.healtapp.core.ui.components.FeatureGuideScreen
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.features.fasting.ui.FastingTabContent
import com.example.healtapp.features.hydration.presentation.HydrationViewModel
import com.example.healtapp.features.hydration.ui.HydrationTabContent
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.ui.MealTabContent
import com.example.healtapp.features.meal.ui.MyDishesTab

/** Вкладки: 0 — Питание, 1 — Мои блюда, 2 — Вода, 3 — Голодание */
@Composable
fun NutritionHubScreen(initialTab: Int = 0, onOpenPlanner: () -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val snackbarHostState = remember { SnackbarHostState() }
    var openDishBuilder by remember { mutableStateOf(false) }
    val mealViewModel: MealViewModel = hiltViewModel()
    val hydrationViewModel: HydrationViewModel = hiltViewModel()
    val mealUiState by mealViewModel.uiState.collectAsStateWithLifecycle()
    val hydrationUiState by hydrationViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMealGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showMealGuide = FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Nutrition)
    }

    val isRefreshing = when (tab) {
        0 -> mealUiState.isLoading && mealUiState.mealHistory.isNotEmpty()
        1 -> mealUiState.isLoading && mealUiState.savedDishes.isNotEmpty()
        2 -> hydrationUiState.isLoading &&
            (hydrationUiState.waterToday > 0 || hydrationUiState.todayRecords.isNotEmpty())
        else -> false
    }

    val (title, subtitle, icon) = when (tab) {
        0 -> Triple("Питание", "Дневник приёмов пищи и КБЖУ", Icons.Filled.Restaurant)
        1 -> Triple("Мои блюда", "Составные блюда из продуктов", Icons.Filled.MenuBook)
        2 -> Triple("Вода", "Гидратация и быстрый ввод", Icons.Filled.WaterDrop)
        else -> Triple("Голодание", "Интервальное питание 16/8, 18/6, 20/4", Icons.Filled.Timer)
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
                        0, 1 -> mealViewModel.refresh()
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
                    scrollStateKey = "nutrition_hub_$tab",
                    extraBottomPadding = if (tab == 1) 72.dp else 0.dp,
                ) {
                    NutritionHubTabs(selected = tab, onSelect = { tab = it })

                    when (tab) {
                        0 -> Column(Modifier.fillMaxWidth()) {
                            MealTabContent(
                                snackbarHostState = snackbarHostState,
                                onOpenPlanner = onOpenPlanner,
                            )
                        }
                        1 -> Column(Modifier.fillMaxWidth()) {
                            MyDishesTab(
                                snackbarHostState = snackbarHostState,
                                openBuilderRequest = openDishBuilder,
                                onBuilderRequestConsumed = { openDishBuilder = false },
                            )
                        }
                        2 -> Column(Modifier.fillMaxWidth()) {
                            HydrationTabContent()
                        }
                        else -> Column(Modifier.fillMaxWidth()) {
                            FastingTabContent()
                        }
                    }
                }
            }

            FeatureGuideOverlay(
                visible = showMealGuide,
                pages = FeatureGuideContent.nutrition,
                sectionLabel = "Питание",
                onDismiss = {
                    FeatureGuidePrefs.markSeen(context, FeatureGuideScreen.Nutrition)
                    showMealGuide = false
                },
            )
        }
    }
}

@Composable
private fun NutritionHubTabs(selected: Int, onSelect: (Int) -> Unit) {
    RoundedSectionTabs(
        tabs = listOf(
            RoundedTabItem(0, "Питание", Icons.Filled.Restaurant),
            RoundedTabItem(1, "Мои блюда", Icons.Filled.MenuBook),
            RoundedTabItem(2, "Вода", Icons.Filled.WaterDrop),
            RoundedTabItem(3, "Голодание", Icons.Filled.Timer),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}
