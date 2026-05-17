package com.example.healtapp.features.meal.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.ui.components.MealDailySummaryCard
import com.example.healtapp.features.meal.ui.components.MealDiaryRowCompact
import com.example.healtapp.features.meal.ui.components.MealSavedDishesGrid
import com.example.healtapp.features.meal.ui.components.MealSlotSection
import com.example.healtapp.features.meal.ui.components.MealSummarySkeleton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateHeaderRu = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru", "RU"))

private val mealTypeOrder = listOf(
    "breakfast" to "Завтрак",
    "lunch" to "Обед",
    "snack" to "Перекус",
    "dinner" to "Ужин",
)

private val diarySlotApiTypes = mealTypeOrder.map { it.first }.toSet()

@Composable
fun MealScreen(
    onOpenHydration: () -> Unit = {},
    onOpenHealthVitals: () -> Unit = {},
) {
    val mealViewModel: MealViewModel = hiltViewModel()
    val mealUiState by mealViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearchSheet by remember { mutableStateOf(false) }
    var activeSlotLabel by remember { mutableStateOf("Завтрак") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showPastDays by remember { mutableStateOf(false) }

    var mealToDelete by remember { mutableStateOf<MealDto?>(null) }
    var mealToEdit by remember { mutableStateOf<MealDto?>(null) }
    var editMealType by remember { mutableStateOf("") }
    var editMealName by remember { mutableStateOf("") }
    var editCal by remember { mutableStateOf("") }
    var editProt by remember { mutableStateOf("") }
    var editFat by remember { mutableStateOf("") }
    var editCarb by remember { mutableStateOf("") }
    var editCaf by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showBarcodeScanner = true
    }

    LaunchedEffect(mealUiState.snackMessage) {
        mealUiState.snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mealViewModel.clearSnackMessage()
        }
    }

    val todayKey = LocalDate.now().toString()
    val todayMeals = remember(mealUiState.mealHistory) {
        mealUiState.mealHistory
            .filter { it.meal_time.take(10) == todayKey }
            .sortedBy { it.meal_time }
    }
    val pastMeals = remember(mealUiState.mealHistory) {
        mealUiState.mealHistory
            .filter { it.meal_time.take(10) != todayKey }
            .sortedByDescending { it.meal_time }
    }
    val pastByDate = remember(pastMeals) {
        pastMeals.groupBy { it.meal_time.take(10) }.toList().sortedByDescending { it.first }
    }

    val kcalProgress =
        if (mealUiState.caloriesTarget > 0) {
            (mealUiState.dayCaloriesTotal / mealUiState.caloriesTarget.toFloat()).coerceIn(0f, 1.15f)
        } else {
            0f
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppScreen(
                title = "Питание",
                subtitle = "Дневник приёмов пищи и калорий",
                headerIcon = Icons.Filled.Restaurant,
                scrollable = true,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onOpenHealthVitals) {
                        Icon(Icons.Outlined.ShowChart, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Показатели")
                    }
                    TextButton(onClick = onOpenHydration) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Вода")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppButton(
                        text = "Как вчера",
                        onClick = { mealViewModel.copyYesterdayMeals() },
                        enabled = !mealUiState.isSaving,
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (mealUiState.isLoading) {
                    MealSummarySkeleton()
                } else {
                    MealDailySummaryCard(
                        consumed = mealUiState.dayCaloriesTotal,
                        target = mealUiState.caloriesTarget,
                        protein = mealUiState.dayProteinTotal,
                        fat = mealUiState.dayFatTotal,
                        carbs = mealUiState.dayCarbsTotal,
                        caffeine = mealUiState.dayCaffeineTotal,
                        progress = kcalProgress,
                        targetProteinG = mealUiState.targetProteinG,
                        targetFatG = mealUiState.targetFatG,
                        targetCarbsG = mealUiState.targetCarbsG,
                    )
                }

                SectionHeader(title = "Дневник на сегодня", subtitle = "Все приёмы пищи")

                mealTypeOrder.forEach { (apiType, titleRu) ->
                    val block = todayMeals.filter { it.meal_type.equals(apiType, ignoreCase = true) }
                    MealSlotSection(
                        titleRu = titleRu,
                        meals = block,
                        onAdd = {
                            activeSlotLabel = titleRu
                            mealViewModel.focusMealSlot(titleRu)
                            showSearchSheet = true
                        },
                        onEdit = { meal ->
                            mealToEdit = meal
                            editMealType = MealViewModel.displayMealTypeFromApi(meal.meal_type)
                            editMealName = meal.name
                            editCal = meal.calories?.toString().orEmpty()
                            editProt = meal.protein_g?.toString().orEmpty()
                            editFat = meal.fat_g?.toString().orEmpty()
                            editCarb = meal.carbs_g?.toString().orEmpty()
                            editCaf = meal.caffeine_mg?.toString().orEmpty()
                        },
                        onDelete = { mealToDelete = it },
                    )
                }

                val otherToday = todayMeals.filter { meal ->
                    meal.meal_type.lowercase() !in diarySlotApiTypes
                }
                if (otherToday.isNotEmpty()) {
                    MealSlotSection(
                        titleRu = "Прочее",
                        meals = otherToday,
                        onAdd = {
                            activeSlotLabel = "Перекус"
                            mealViewModel.focusMealSlot("Перекус")
                            showSearchSheet = true
                        },
                        onEdit = { meal ->
                            mealToEdit = meal
                            editMealType = MealViewModel.displayMealTypeFromApi(meal.meal_type)
                            editMealName = meal.name
                            editCal = meal.calories?.toString().orEmpty()
                            editProt = meal.protein_g?.toString().orEmpty()
                            editFat = meal.fat_g?.toString().orEmpty()
                            editCarb = meal.carbs_g?.toString().orEmpty()
                            editCaf = meal.caffeine_mg?.toString().orEmpty()
                        },
                        onDelete = { mealToDelete = it },
                    )
                }

                SectionHeader(title = "Мои блюда", subtitle = "Быстрый ввод")
                MealSavedDishesGrid(
                    dishes = mealUiState.savedDishes,
                    onApply = { dish -> mealViewModel.addSavedDishToDiary(dish) },
                    onDelete = mealViewModel::deleteSavedDish,
                )

                SectionHeader(
                    title = "Ранее",
                    subtitle = if (pastMeals.isEmpty()) "Нет записей" else "${pastMeals.size} записей",
                )
                if (pastMeals.isNotEmpty()) {
                    TextButton(onClick = { showPastDays = !showPastDays }) {
                        Text(if (showPastDays) "Свернуть" else "Показать историю")
                    }
                    AnimatedVisibility(
                        visible = showPastDays,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pastByDate.forEach { (dateKey, meals) ->
                                val header = runCatching {
                                    LocalDate.parse(dateKey).format(dateHeaderRu)
                                }.getOrDefault(dateKey)
                                SectionHeader(title = header)
                                meals.forEach { meal ->
                                    MealDiaryRowCompact(
                                        meal = meal,
                                        onEdit = {
                                            mealToEdit = meal
                                            editMealType = MealViewModel.displayMealTypeFromApi(meal.meal_type)
                                            editMealName = meal.name
                                            editCal = meal.calories?.toString().orEmpty()
                                            editProt = meal.protein_g?.toString().orEmpty()
                                            editFat = meal.fat_g?.toString().orEmpty()
                                            editCarb = meal.carbs_g?.toString().orEmpty()
                                            editCaf = meal.caffeine_mg?.toString().orEmpty()
                                        },
                                        onDelete = { mealToDelete = meal },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(72.dp))
            }

            MealSearchSheet(
                visible = showSearchSheet,
                mealSlotLabel = activeSlotLabel,
                uiState = mealUiState,
                onDismiss = { showSearchSheet = false },
                onQueryChange = mealViewModel::updateFoodSearchQuery,
                onSearchDebounced = mealViewModel::searchFoodByNameDebounced,
                onSearchNow = mealViewModel::searchFoodByNameNow,
                onSelectFood = mealViewModel::applyFoodFromSearch,
                onOpenBarcode = {
                    if (hasCameraPermission(context)) {
                        showBarcodeScanner = true
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSelectServing = mealViewModel::selectServing,
                onMultiplierChange = mealViewModel::updatePortionMultiplier,
                onAddToDiary = {
                    mealViewModel.addSelectedFoodToDiary {
                        showSearchSheet = false
                    }
                },
            )

            BarcodeScannerSheet(
                visible = showBarcodeScanner,
                onDismiss = { showBarcodeScanner = false },
                onBarcode = { code ->
                    mealViewModel.searchFoodByBarcode(code)
                    showBarcodeScanner = false
                },
            )

            mealToDelete?.let { m ->
                AlertDialog(
                    onDismissRequest = { mealToDelete = null },
                    title = { Text("Удалить приём пищи?") },
                    text = { Text(m.name) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mealViewModel.deleteMeal(m.id)
                                mealToDelete = null
                            },
                        ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mealToDelete = null }) { Text("Отмена") }
                    },
                )
            }

            mealToEdit?.let { m ->
                AlertDialog(
                    onDismissRequest = { mealToEdit = null },
                    title = { Text("Редактировать приём пищи") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(editMealType, { editMealType = it }, label = "Тип")
                            AppTextField(editMealName, { editMealName = it }, label = "Название")
                            AppTextField(editCal, { editCal = it }, label = "Калории")
                            AppTextField(editProt, { editProt = it }, label = "Белки")
                            AppTextField(editFat, { editFat = it }, label = "Жиры")
                            AppTextField(editCarb, { editCarb = it }, label = "Углеводы")
                            AppTextField(editCaf, { editCaf = it }, label = "Кофеин (мг)")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val req = MealCreateRequestDto(
                                    meal_type = MealViewModel.apiMealTypeFromDisplay(
                                        editMealType.ifBlank { MealViewModel.displayMealTypeFromApi(m.meal_type) },
                                    ),
                                    name = editMealName.ifBlank { m.name },
                                    calories = editCal.toFloatOrNull(),
                                    protein_g = editProt.toFloatOrNull(),
                                    fat_g = editFat.toFloatOrNull(),
                                    carbs_g = editCarb.toFloatOrNull(),
                                    fiber_g = m.fiber_g,
                                    sugar_g = m.sugar_g,
                                    caffeine_mg = editCaf.toFloatOrNull(),
                                    water_ml = m.water_ml,
                                    portion_g = m.portion_g,
                                    glycemic_load = m.glycemic_load,
                                    meal_category = m.meal_category,
                                    minutes_before_sleep = m.minutes_before_sleep,
                                    is_late_meal = m.is_late_meal,
                                    meal_time = m.meal_time,
                                    notes = m.notes,
                                    source = m.source,
                                )
                                mealViewModel.updateMealRecord(m.id, req)
                                mealToEdit = null
                            },
                        ) { Text("Сохранить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mealToEdit = null }) { Text("Отмена") }
                    },
                )
            }
        }
    }
}
