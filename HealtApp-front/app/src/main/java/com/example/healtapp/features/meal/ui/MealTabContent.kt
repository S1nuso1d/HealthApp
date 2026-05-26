package com.example.healtapp.features.meal.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppDialogMessage
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.DishIngredientsJson
import com.example.healtapp.features.meal.ui.components.SavedDishApplyDialog
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.PendingSyncBadge
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.ui.components.MealDiaryRowCompact
import com.example.healtapp.features.meal.ui.components.MealNutritionSummaryCard
import com.example.healtapp.features.meal.ui.components.MealNutritionTargetsSheet
import com.example.healtapp.features.meal.ui.components.MealSlotSection
import com.example.healtapp.features.meal.ui.components.MealSummarySkeleton
import kotlinx.coroutines.launch
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
fun MealTabContent(
    snackbarHostState: SnackbarHostState,
) {
    val mealViewModel: MealViewModel = hiltViewModel()
    val mealUiState by mealViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSearchSheet by remember { mutableStateOf(false) }
    var activeSlotLabel by remember { mutableStateOf("Завтрак") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showTargetsSheet by remember { mutableStateOf(false) }
    var savedDishToApply by remember { mutableStateOf<SavedDishDto?>(null) }
    var editTargetCalories by remember { mutableStateOf("") }
    var editTargetProtein by remember { mutableStateOf("") }
    var editTargetFat by remember { mutableStateOf("") }
    var editTargetCarbs by remember { mutableStateOf("") }

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

    MealNutritionTargetsSheet(
        visible = showTargetsSheet,
        targetCalories = editTargetCalories,
        targetProtein = editTargetProtein,
        targetFat = editTargetFat,
        targetCarbs = editTargetCarbs,
        isSaving = mealUiState.isSavingTargets,
        onDismiss = { showTargetsSheet = false },
        onCaloriesChange = { editTargetCalories = it },
        onProteinChange = { editTargetProtein = it },
        onFatChange = { editTargetFat = it },
        onCarbsChange = { editTargetCarbs = it },
        onSave = {
            val cal = editTargetCalories.toIntOrNull()
            val p = editTargetProtein.toFloatOrNull()
            val f = editTargetFat.toFloatOrNull()
            val c = editTargetCarbs.toFloatOrNull()
            when {
                cal == null || cal < 800 -> scope.launch {
                    snackbarHostState.showSnackbar("Укажите калории от 800")
                }
                p == null || p <= 0f -> scope.launch {
                    snackbarHostState.showSnackbar("Укажите цель по белкам")
                }
                f == null || f <= 0f -> scope.launch {
                    snackbarHostState.showSnackbar("Укажите цель по жирам")
                }
                c == null || c <= 0f -> scope.launch {
                    snackbarHostState.showSnackbar("Укажите цель по углеводам")
                }
                else -> {
                    mealViewModel.saveNutritionTargets(cal, p, f, c)
                    showTargetsSheet = false
                }
            }
        },
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PendingSyncBadge(count = mealUiState.pendingSyncCount)

        if (mealUiState.isLoading) {
            MealSummarySkeleton()
        } else {
            MealNutritionSummaryCard(
                caloriesConsumed = mealUiState.dayCaloriesTotal,
                caloriesTarget = mealUiState.caloriesTarget,
                proteinConsumed = mealUiState.dayProteinTotal,
                proteinTarget = mealUiState.targetProteinG,
                fatConsumed = mealUiState.dayFatTotal,
                fatTarget = mealUiState.targetFatG,
                carbsConsumed = mealUiState.dayCarbsTotal,
                carbsTarget = mealUiState.targetCarbsG,
                caffeine = mealUiState.dayCaffeineTotal,
                kcalProgress = kcalProgress,
                targetsHint = mealUiState.nutritionTargetsHint,
                celebrateToken = mealUiState.progressCelebrateToken,
                onEditTargets = {
                    editTargetCalories = mealUiState.caloriesTarget.toString()
                    editTargetProtein = mealUiState.targetProteinG?.let { "%.0f".format(it) }.orEmpty()
                    editTargetFat = mealUiState.targetFatG?.let { "%.0f".format(it) }.orEmpty()
                    editTargetCarbs = mealUiState.targetCarbsG?.let { "%.0f".format(it) }.orEmpty()
                    showTargetsSheet = true
                },
            )
        }

        CollapsibleAppCard(
            title = "Дневник на сегодня",
            subtitle = "${todayMeals.size} записей · все приёмы пищи",
            initiallyExpanded = true,
        ) {
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
        }

        if (pastMeals.isNotEmpty()) {
            CollapsibleAppCard(
                title = "Ранее",
                subtitle = "${pastMeals.size} записей",
                initiallyExpanded = false,
            ) {
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

        Spacer(Modifier.height(72.dp))
    }

    val matchingSavedDishes = remember(mealUiState.foodSearchQuery, mealUiState.savedDishes) {
        val q = mealUiState.foodSearchQuery.trim()
        if (q.length < 2) mealUiState.savedDishes
        else mealViewModel.filteredSavedDishes(q)
    }

    MealSearchSheet(
        visible = showSearchSheet,
        mealSlotLabel = activeSlotLabel,
        uiState = mealUiState,
        savedDishes = matchingSavedDishes,
        onDismiss = {
            showSearchSheet = false
            mealViewModel.clearMealSearchSelection()
            mealViewModel.updateFoodSearchQuery("")
        },
        onQueryChange = mealViewModel::updateFoodSearchQuery,
        onSearchDebounced = mealViewModel::searchFoodByNameDebounced,
        onSearchNow = mealViewModel::searchFoodByNameNow,
        onSelectFood = mealViewModel::applyFoodFromSearch,
        onSelectSavedDish = { dish ->
            savedDishToApply = dish
            mealViewModel.clearMealSearchSelection()
        },
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
            text = {
                AppDialogMessage(
                    warning = UserFacingMessages.DELETE_RECORD_WARNING,
                    body = m.name,
                )
            },
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

    savedDishToApply?.let { dish ->
        val templates = DishIngredientsJson.decode(dish.notes)
        if (templates.isNotEmpty()) {
            SavedDishApplyDialog(
                dishName = dish.name,
                initialIngredients = templates,
                mealSlotLabel = activeSlotLabel,
                onDismiss = { savedDishToApply = null },
                onConfirm = { adjusted, _ ->
                    mealViewModel.addSavedDishToDiaryFromIngredients(dish.name, activeSlotLabel, adjusted) {
                        savedDishToApply = null
                        showSearchSheet = false
                    }
                },
            )
        }
    }
}
