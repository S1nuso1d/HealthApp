package com.example.healtapp.features.meal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.api.IntegrationsApi
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.data.preferences.PendingMealOp
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.sync.PendingSyncFlusher
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.common.NutritionTargetsCalculator
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.features.meal.DishIngredient
import com.example.healtapp.features.meal.DishIngredientFactory
import com.example.healtapp.features.meal.DishIngredientsJson
import com.example.healtapp.features.meal.DishIngredientsPayload
import com.example.healtapp.features.meal.util.FatSecretFoodHit
import com.example.healtapp.features.meal.util.FatSecretParse
import com.example.healtapp.features.meal.util.FatSecretServingOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class MealViewModel @Inject constructor(
    private val repository: MealRepository,
    private val integrationsApi: IntegrationsApi,
    private val profileRepository: ProfileRepository,
    private val pendingSyncStore: PendingSyncStore,
    private val pendingSyncFlusher: PendingSyncFlusher,
) : ViewModel() {

    companion object {
        val displayMealTypes = listOf("Завтрак", "Обед", "Перекус", "Ужин")

        fun apiMealTypeFromDisplay(display: String): String = when (display) {
            "Завтрак" -> "breakfast"
            "Обед" -> "lunch"
            "Перекус", "Полдник" -> "snack"
            "Ужин" -> "dinner"
            "Напиток" -> "drink"
            else -> display.lowercase().trim().takeIf { it in API_TYPES } ?: "snack"
        }

        fun displayMealTypeFromApi(api: String): String = when (api.lowercase()) {
            "breakfast" -> "Завтрак"
            "lunch" -> "Обед"
            "dinner" -> "Ужин"
            "snack" -> "Перекус"
            "drink" -> "Напиток"
            else -> api.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        private val API_TYPES = setOf("breakfast", "lunch", "dinner", "snack", "drink")

        private const val DEFAULT_AGE_YEARS = 30
        private const val DEFAULT_HEIGHT_CM = 170f
        private const val DEFAULT_WEIGHT_KG = 70f
    }

    private val searchCache = object : LinkedHashMap<String, List<FatSecretFoodHit>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FatSecretFoodHit>>): Boolean =
            size > 24
    }

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
        viewModelScope.launch {
            AppRefreshBus.events.collect { loadMeals() }
        }
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun refresh() {
        loadMeals()
    }

    fun saveNutritionTargets(
        calories: Int,
        proteinG: Float,
        fatG: Float,
        carbsG: Float,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTargets = true, error = null) }
            val result = profileRepository.updateNutritionTargets(
                targetDailyCalories = calories,
                targetProteinG = proteinG,
                targetFatG = fatG,
                targetCarbsG = carbsG,
            )
            result.onSuccess {
                AppRefreshBus.notifyDataChanged()
                _uiState.update {
                    it.copy(
                        isSavingTargets = false,
                        caloriesTarget = calories,
                        targetProteinG = proteinG,
                        targetFatG = fatG,
                        targetCarbsG = carbsG,
                        snackMessage = "Ориентиры сохранены",
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSavingTargets = false,
                        snackMessage = e.message ?: "Не удалось сохранить",
                    )
                }
            }
        }
    }

    fun focusMealSlot(displayType: String) {
        _uiState.update {
            it.copy(
                mealType = displayType,
                mealName = "",
                calories = "",
                protein = "",
                fat = "",
                carbs = "",
                caffeineMg = "",
                foodSearchQuery = "",
                foodSearchResults = emptyList(),
                foodSearchError = null,
                servingOptions = emptyList(),
                selectedServingIndex = 0,
                portionMultiplier = 1f,
                error = null,
            )
        }
    }

    fun loadMeals() {
        viewModelScope.launch {
            pendingSyncFlusher.flush()
            val pending = pendingSyncStore.load()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    pendingSyncCount = pending.hydration.size + pending.meals.size,
                )
            }

            val profileRes = profileRepository.getMyProfile()
            val targets = profileRes.getOrNull()
            val resolved = resolveNutritionTargets(targets)
            val calTarget = resolved.calories
            val tp = resolved.proteinG
            val tf = resolved.fatG
            val tc = resolved.carbsG
            val targetsHint = nutritionTargetsHint(resolved)
            maybePersistComputedTargets(targets, resolved)
            val autoSnack = if (resolved.autoFilled) {
                "Подобрали ориентиры: ${resolved.calories} ккал, Б ${resolved.proteinG.toInt()} / Ж ${resolved.fatG.toInt()} / У ${resolved.carbsG.toInt()} г"
            } else {
                null
            }

            val savedRes = repository.listSavedDishes()
            val savedList = savedRes.getOrNull().orEmpty()

            val today = repository.getTodayMeal()
            val history = repository.getMealHistory()
            if (today.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = today.exceptionOrNull()?.message ?: "Ошибка загрузки",
                    )
                }
                return@launch
            }
            if (history.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = history.exceptionOrNull()?.message ?: "Ошибка истории",
                    )
                }
                return@launch
            }
            val list = history.getOrNull().orEmpty()
            val todayKey = LocalDate.now().toString()
            val todayMeals = list.filter { m -> m.meal_time.take(10) == todayKey }
            val dayCals = todayMeals.sumOf { (it.calories ?: 0f).toDouble() }.toInt()
            val dayP = todayMeals.sumOf { (it.protein_g ?: 0f).toDouble() }.toFloat()
            val dayF = todayMeals.sumOf { (it.fat_g ?: 0f).toDouble() }.toFloat()
            val dayC = todayMeals.sumOf { (it.carbs_g ?: 0f).toDouble() }.toFloat()
            val dayCaf = todayMeals.sumOf { (it.caffeine_mg ?: 0f).toDouble() }.toFloat()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    todayMeal = today.getOrNull(),
                    mealHistory = list,
                    savedDishes = savedList,
                    caloriesTarget = calTarget,
                    targetProteinG = tp,
                    targetFatG = tf,
                    targetCarbsG = tc,
                    nutritionTargetsHint = targetsHint,
                    snackMessage = if (resolved.autoFilled && it.nutritionTargetsHint != targetsHint) {
                        autoSnack
                    } else {
                        it.snackMessage
                    },
                    dayCaloriesTotal = dayCals,
                    dayProteinTotal = dayP,
                    dayFatTotal = dayF,
                    dayCarbsTotal = dayC,
                    dayCaffeineTotal = dayCaf,
                    error = null,
                )
            }
        }
    }

    fun updateFoodSearchQuery(value: String) {
        _uiState.update { it.copy(foodSearchQuery = value, foodSearchError = null) }
    }

    fun clearFoodSearch() {
        _uiState.update {
            it.copy(foodSearchResults = emptyList(), foodSearchError = null, foodSearchQuery = "")
        }
    }

    fun searchFoodByNameDebounced() {
        val q = _uiState.value.foodSearchQuery.trim()
        if (q.length < 2) {
            _uiState.update { it.copy(foodSearchResults = emptyList(), foodSearchError = null) }
            return
        }
        viewModelScope.launch { executeFoodSearch(q, forceRefresh = false) }
    }

    fun searchFoodByNameNow() {
        val q = _uiState.value.foodSearchQuery.trim()
        if (q.length < 2) {
            _uiState.update { it.copy(foodSearchError = "Введите минимум 2 символа") }
            return
        }
        viewModelScope.launch { executeFoodSearch(q, forceRefresh = true) }
    }

    private suspend fun executeFoodSearch(q: String, forceRefresh: Boolean) {
        val key = q.lowercase()
        if (!forceRefresh) {
            searchCache[key]?.let { cached ->
                _uiState.update {
                    it.copy(
                        isFoodSearchLoading = false,
                        foodSearchResults = cached,
                        foodSearchError = if (cached.isEmpty()) "Ничего не найдено" else null,
                    )
                }
                return
            }
        }
        _uiState.update { it.copy(isFoodSearchLoading = true, foodSearchError = null) }
        runCatching {
            val raw = integrationsApi.searchFoods(q)
            FatSecretParse.parseSearchHits(raw)
        }.onSuccess { hits ->
            searchCache[key] = hits
            _uiState.update {
                it.copy(
                    isFoodSearchLoading = false,
                    foodSearchResults = hits,
                    foodSearchError = if (hits.isEmpty()) "Ничего не найдено" else null,
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isFoodSearchLoading = false,
                    foodSearchResults = emptyList(),
                    foodSearchError = e.message ?: "Ошибка поиска",
                )
            }
        }
    }

    fun searchBarcodeForDishTemplate(barcode: String, onAdded: (DishIngredient) -> Unit) {
        val code = barcode.trim()
        if (code.length < 4) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFoodSearchLoading = true, foodSearchError = null) }
            runCatching {
                val raw = integrationsApi.searchBarcode(code)
                val foodId = FatSecretParse.extractFoodIdFromBarcodeResponse(raw)
                    ?: error("Продукт по штрихкоду не найден")
                integrationsApi.getFood(foodId)
            }.onSuccess { detail ->
                val template = DishIngredientFactory.fromFoodDetailJson(detail, null)
                _uiState.update { it.copy(isFoodSearchLoading = false) }
                if (template != null) onAdded(template)
            }.onFailure {
                _uiState.update { it.copy(isFoodSearchLoading = false) }
            }
        }
    }

    fun searchFoodByBarcode(barcode: String) {
        val code = barcode.trim()
        if (code.length < 4) {
            _uiState.update { it.copy(foodSearchError = "Введите штрихкод") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFoodSearchLoading = true, foodSearchError = null) }
            runCatching {
                val raw = integrationsApi.searchBarcode(code)
                val foodId = FatSecretParse.extractFoodIdFromBarcodeResponse(raw)
                    ?: error("Продукт по штрихкоду не найден")
                integrationsApi.getFood(foodId)
            }.onSuccess { detail ->
                applyFoodDetail(detail)
            }.onFailure { e ->
                val msg = e.message ?: "Ошибка штрихкода"
                _uiState.update {
                    it.copy(
                        isFoodSearchLoading = false,
                        foodSearchError = "$msg. База FatSecret меньше магазинной — попробуйте текстовый поиск «$code» или введите вручную.",
                    )
                }
                if (code.length >= 4) {
                    searchFoodsFallbackQuery(code)
                }
            }
        }
    }

    private suspend fun searchFoodsFallbackQuery(query: String) {
        runCatching {
            val hits = FatSecretParse.parseSearchHits(integrationsApi.searchFoods(query))
            if (hits.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        foodSearchResults = hits.take(8),
                        foodSearchError = "По штрихкоду не найдено; варианты по поиску «$query»:",
                    )
                }
            }
        }
    }

    fun applyFoodFromSearch(foodId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFoodSearchLoading = true, foodSearchError = null) }
            runCatching {
                integrationsApi.getFood(foodId)
            }.onSuccess { detail ->
                applyFoodDetail(detail)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isFoodSearchLoading = false,
                        foodSearchError = e.message ?: "Ошибка загрузки продукта",
                    )
                }
            }
        }
    }

    fun fetchIngredientTemplate(foodId: String, onResult: (DishIngredient) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFoodSearchLoading = true, foodSearchError = null) }
            runCatching { integrationsApi.getFood(foodId) }
                .onSuccess { json ->
                    val template = DishIngredientFactory.fromFoodDetailJson(json, foodId)
                    _uiState.update { it.copy(isFoodSearchLoading = false) }
                    if (template != null) {
                        onResult(template)
                    } else {
                        _uiState.update { it.copy(foodSearchError = "Не удалось разобрать продукт") }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isFoodSearchLoading = false,
                            foodSearchError = e.message ?: "Ошибка загрузки",
                        )
                    }
                }
        }
    }

    fun filteredSavedDishes(query: String): List<SavedDishDto> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return _uiState.value.savedDishes.filter { it.name.lowercase().contains(q) }
    }

    fun clearMealSearchSelection() {
        _uiState.update {
            it.copy(
                mealName = "",
                calories = "",
                protein = "",
                fat = "",
                carbs = "",
                servingOptions = emptyList(),
                portionMultiplier = 1f,
                selectedServingIndex = 0,
            )
        }
    }

    fun addSelectedFoodToDiary(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.mealName.isBlank()) {
            _uiState.update { it.copy(error = "Сначала выберите продукт из списка или по штрихкоду") }
            return
        }
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val request = buildCreateRequest(LocalDateTime.now().format(formatter))
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createMeal(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            mealName = "",
                            calories = "",
                            protein = "",
                            fat = "",
                            carbs = "",
                            caffeineMg = "",
                            servingOptions = emptyList(),
                            portionMultiplier = 1f,
                            selectedServingIndex = 0,
                            foodSearchResults = emptyList(),
                            snackMessage = "Добавлено в «${state.mealType}»",
                            progressCelebrateToken = it.progressCelebrateToken + 1,
                        )
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                    onSuccess()
                }
                .onFailure { e ->
                    val pending = enqueueMealOffline(request)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = offlineMealError(e, pending),
                            pendingSyncCount = pending,
                            snackMessage = "Сохранено офлайн — отправим при появлении сети",
                        )
                    }
                }
        }
    }

    fun saveDishWithIngredients(
        name: String,
        mealTypeDisplay: String?,
        ingredients: List<DishIngredient>,
    ) {
        val templates = ingredients
            .filter { it.name.isNotBlank() }
            .map { if (it.isTemplate) it.copy(grams = 0f) else it }
        val totals = DishIngredientsPayload(templates.map { it.per100gPreview() }).referencePer100g()
        viewModelScope.launch {
            val body = SavedDishCreateRequestDto(
                name = name,
                meal_type = mealTypeDisplay?.let { apiMealTypeFromDisplay(it) },
                calories = totals.calories.takeIf { it > 0f },
                protein_g = totals.protein.takeIf { it > 0f },
                fat_g = totals.fat.takeIf { it > 0f },
                carbs_g = totals.carbs.takeIf { it > 0f },
                notes = DishIngredientsJson.encode(templates),
            )
            repository.createSavedDish(body)
                .onSuccess {
                    _uiState.update { it.copy(snackMessage = "Блюдо «$name» сохранено") }
                    loadMeals()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Не удалось сохранить блюдо") }
                }
        }
    }

    fun addSavedDishToDiaryFromIngredients(
        dishName: String,
        mealSlotDisplay: String,
        ingredients: List<DishIngredient>,
        onSuccess: () -> Unit = {},
    ) {
        val scaled = ingredients.map { it.withScaledMacros() }
        val totals = DishIngredientsPayload(scaled).totals()
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val request = MealCreateRequestDto(
                meal_type = apiMealTypeFromDisplay(mealSlotDisplay.ifBlank { "Завтрак" }),
                name = dishName,
                calories = totals.calories.takeIf { it > 0f },
                protein_g = totals.protein.takeIf { it > 0f },
                fat_g = totals.fat.takeIf { it > 0f },
                carbs_g = totals.carbs.takeIf { it > 0f },
                meal_time = LocalDateTime.now().format(formatter),
                source = "saved_dish",
                notes = DishIngredientsJson.encode(scaled),
            )
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createMeal(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, snackMessage = "«$dishName» добавлено")
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                    onSuccess()
                }
                .onFailure { e ->
                    val pending = enqueueMealOffline(request)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = offlineMealError(e, pending),
                            pendingSyncCount = pending,
                            snackMessage = "Сохранено офлайн — отправим при появлении сети",
                        )
                    }
                }
        }
    }

    fun addSavedDishToDiary(dish: SavedDishDto, onSuccess: () -> Unit = {}) {
        val ingredients = DishIngredientFactory.templatesForApply(DishIngredientsJson.decode(dish.notes))
        if (ingredients.isNotEmpty()) {
            val slot = dish.meal_type?.let { displayMealTypeFromApi(it) } ?: "Завтрак"
            addSavedDishToDiaryFromIngredients(dish.name, slot, ingredients, onSuccess)
            return
        }
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val slot = dish.meal_type?.let { displayMealTypeFromApi(it) } ?: _uiState.value.mealType
            val request = MealCreateRequestDto(
                meal_type = apiMealTypeFromDisplay(slot.ifBlank { "Завтрак" }),
                name = dish.name,
                calories = dish.calories,
                protein_g = dish.protein_g,
                fat_g = dish.fat_g,
                carbs_g = dish.carbs_g,
                meal_time = LocalDateTime.now().format(formatter),
                source = "saved_dish",
            )
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createMeal(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, snackMessage = "«${dish.name}» добавлено")
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                    onSuccess()
                }
                .onFailure { e ->
                    val pending = enqueueMealOffline(request)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = offlineMealError(e, pending),
                            pendingSyncCount = pending,
                            snackMessage = "Сохранено офлайн — отправим при появлении сети",
                        )
                    }
                }
        }
    }

    private fun applyFoodDetail(detailJson: com.google.gson.JsonObject) {
        val parsed = FatSecretParse.parseFoodDetail(detailJson)
            ?: run {
                _uiState.update {
                    it.copy(isFoodSearchLoading = false, foodSearchError = "Нет данных о порции")
                }
                return
            }
        val servings = parsed.servings.ifEmpty {
            listOf(
                FatSecretServingOption(
                    servingId = null,
                    description = "Порция",
                    calories = null,
                    proteinG = null,
                    fatG = null,
                    carbsG = null,
                ),
            )
        }
        _uiState.update { state ->
            val index = 0
            val macros = macrosFromServing(servings[index], state.portionMultiplier)
            state.copy(
                isFoodSearchLoading = false,
                mealName = parsed.foodName,
                servingOptions = servings,
                selectedServingIndex = index,
                calories = macros.calories,
                protein = macros.protein,
                fat = macros.fat,
                carbs = macros.carbs,
                foodSearchResults = emptyList(),
                foodSearchError = null,
                snackMessage = null,
            )
        }
    }

    fun selectServing(index: Int) {
        val state = _uiState.value
        if (index !in state.servingOptions.indices) return
        val macros = macrosFromServing(state.servingOptions[index], state.portionMultiplier)
        _uiState.update {
            it.copy(
                selectedServingIndex = index,
                calories = macros.calories,
                protein = macros.protein,
                fat = macros.fat,
                carbs = macros.carbs,
            )
        }
    }

    fun updatePortionMultiplier(multiplier: Float) {
        val mult = multiplier.coerceIn(0.25f, 4f)
        val state = _uiState.value
        val serving = state.servingOptions.getOrNull(state.selectedServingIndex) ?: return
        val macros = macrosFromServing(serving, mult)
        _uiState.update {
            it.copy(
                portionMultiplier = mult,
                calories = macros.calories,
                protein = macros.protein,
                fat = macros.fat,
                carbs = macros.carbs,
            )
        }
    }

    private data class MacroStrings(
        val calories: String,
        val protein: String,
        val fat: String,
        val carbs: String,
    )

    private fun macrosFromServing(serving: FatSecretServingOption, multiplier: Float): MacroStrings {
        fun scale(v: Float?, intFmt: Boolean = false): String {
            if (v == null) return ""
            val scaled = v * multiplier
            return if (intFmt) "%.0f".format(scaled) else "%.1f".format(scaled)
        }
        return MacroStrings(
            calories = scale(serving.calories, intFmt = true),
            protein = scale(serving.proteinG),
            fat = scale(serving.fatG),
            carbs = scale(serving.carbsG),
        )
    }

    fun applySavedDish(dish: SavedDishDto) {
        _uiState.update {
            it.copy(
                mealType = dish.meal_type?.let { t -> displayMealTypeFromApi(t) } ?: it.mealType,
                mealName = dish.name,
                calories = dish.calories?.let { v -> "%.0f".format(v) } ?: "",
                protein = dish.protein_g?.let { v -> "%.1f".format(v) } ?: "",
                fat = dish.fat_g?.let { v -> "%.1f".format(v) } ?: "",
                carbs = dish.carbs_g?.let { v -> "%.1f".format(v) } ?: "",
                servingOptions = emptyList(),
                portionMultiplier = 1f,
                snackMessage = "Блюдо «${dish.name}» в форме",
            )
        }
    }

    fun saveCurrentAsSavedDish() {
        val s = _uiState.value
        val name = s.mealName.ifBlank { "Моё блюдо" }
        viewModelScope.launch {
            val body = SavedDishCreateRequestDto(
                name = name,
                meal_type = apiMealTypeFromDisplay(s.mealType.ifBlank { "Завтрак" }),
                calories = s.calories.toFloatOrNull(),
                protein_g = s.protein.toFloatOrNull(),
                fat_g = s.fat.toFloatOrNull(),
                carbs_g = s.carbs.toFloatOrNull(),
                notes = "saved_from_form",
            )
            repository.createSavedDish(body)
                .onSuccess {
                    _uiState.update { it.copy(snackMessage = "Блюдо сохранено") }
                    loadMeals()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Не удалось сохранить блюдо") }
                }
        }
    }

    fun deleteSavedDish(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedDish(id)
                .onSuccess {
                    _uiState.update { it.copy(snackMessage = "Удалено из сохранённых") }
                    loadMeals()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Не удалось удалить") }
                }
        }
    }

    fun copyYesterdayMeals() {
        val src = LocalDate.now().minusDays(1).toString()
        viewModelScope.launch {
            repository.copyMealsFromDay(src, null)
                .onSuccess { res ->
                    val n = res.copied
                    _uiState.update {
                        it.copy(snackMessage = if (n == 0) "Вчера не было записей" else "Скопировано приёмов: $n")
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Не удалось скопировать") }
                }
        }
    }

    fun updateMealType(value: String) {
        _uiState.update { it.copy(mealType = value) }
    }

    fun updateMealName(value: String) {
        _uiState.update { it.copy(mealName = value) }
    }

    fun updateCalories(value: String) {
        _uiState.update { it.copy(calories = value) }
    }

    fun updateProtein(value: String) {
        _uiState.update { it.copy(protein = value) }
    }

    fun updateFat(value: String) {
        _uiState.update { it.copy(fat = value) }
    }

    fun updateCarbs(value: String) {
        _uiState.update { it.copy(carbs = value) }
    }

    fun updateCaffeine(value: String) {
        _uiState.update { it.copy(caffeineMg = value) }
    }

    private fun buildCreateRequest(mealTimeIso: String): MealCreateRequestDto {
        val state = _uiState.value
        val apiType = apiMealTypeFromDisplay(state.mealType.ifBlank { "Завтрак" })
        return MealCreateRequestDto(
            meal_type = apiType,
            name = state.mealName.ifBlank { "Прием пищи" },
            calories = state.calories.toFloatOrNull(),
            protein_g = state.protein.toFloatOrNull(),
            fat_g = state.fat.toFloatOrNull(),
            carbs_g = state.carbs.toFloatOrNull(),
            fiber_g = null,
            sugar_g = null,
            caffeine_mg = state.caffeineMg.toFloatOrNull(),
            water_ml = null,
            portion_g = null,
            glycemic_load = null,
            meal_category = null,
            minutes_before_sleep = null,
            is_late_meal = null,
            meal_time = mealTimeIso,
            notes = null,
            source = if (state.servingOptions.isNotEmpty()) "fatsecret" else "manual",
        )
    }

    fun saveMeal() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.mealName.isBlank()) {
                _uiState.update { it.copy(error = "Укажите название блюда") }
                return@launch
            }
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val request = buildCreateRequest(LocalDateTime.now().format(formatter))

            _uiState.update { state.copy(isSaving = true, error = null) }

            repository.createMeal(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            mealName = "",
                            calories = "",
                            protein = "",
                            fat = "",
                            carbs = "",
                            caffeineMg = "",
                            servingOptions = emptyList(),
                            portionMultiplier = 1f,
                            selectedServingIndex = 0,
                            error = null,
                            snackMessage = "Добавлено в дневник",
                        )
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { throwable ->
                    val pending = enqueueMealOffline(request)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = offlineMealError(throwable, pending),
                            pendingSyncCount = pending,
                            snackMessage = "Сохранено офлайн — отправим при появлении сети",
                        )
                    }
                }
        }
    }

    private suspend fun enqueueMealOffline(request: MealCreateRequestDto): Int {
        pendingSyncStore.enqueueMeal(
            PendingMealOp(
                mealType = request.meal_type,
                name = request.name,
                calories = request.calories,
                proteinG = request.protein_g,
                fatG = request.fat_g,
                carbsG = request.carbs_g,
            ),
        )
        val q = pendingSyncStore.load()
        return q.hydration.size + q.meals.size
    }

    private fun offlineMealError(cause: Throwable, pendingCount: Int): String =
        cause.message?.takeIf { it.isNotBlank() }
            ?: "Нет сети — в очереди $pendingCount"

    fun updateMealRecord(id: Int, request: MealCreateRequestDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.updateMeal(id, request)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, snackMessage = "Запись обновлена") }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось обновить",
                        )
                    }
                }
        }
    }

    fun deleteMeal(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.deleteMeal(id)
                .onSuccess {
                    _uiState.update { it.copy(snackMessage = "Запись удалена") }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Не удалось удалить",
                        )
                    }
                }
        }
    }

    private data class ResolvedTargets(
        val calories: Int,
        val proteinG: Float,
        val fatG: Float,
        val carbsG: Float,
        val fromProfile: Boolean,
        val usedBodyDefaults: Boolean,
        val autoFilled: Boolean,
    )

    private fun resolveNutritionTargets(profile: ProfileDto?): ResolvedTargets {
        val storedCal = profile?.target_daily_calories?.takeIf { it > 0 }
        val storedP = profile?.target_protein_g?.takeIf { it > 0f }
        val storedF = profile?.target_fat_g?.takeIf { it > 0f }
        val storedC = profile?.target_carbs_g?.takeIf { it > 0f }
        val allStored = storedCal != null && storedP != null && storedF != null && storedC != null
        if (allStored) {
            return ResolvedTargets(
                calories = storedCal,
                proteinG = storedP,
                fatG = storedF,
                carbsG = storedC,
                fromProfile = true,
                usedBodyDefaults = false,
                autoFilled = false,
            )
        }

        val usedBodyDefaults = profile == null ||
            (profile.age ?: 0) < 1 ||
            (profile.height_cm ?: 0f) <= 0f ||
            (profile.weight_kg ?: 0f) <= 0f

        val age = profile?.age?.takeIf { it > 0 } ?: DEFAULT_AGE_YEARS
        val height = profile?.height_cm?.takeIf { it > 0f } ?: DEFAULT_HEIGHT_CM
        val weight = profile?.weight_kg?.takeIf { it > 0f } ?: DEFAULT_WEIGHT_KG

        val computed = NutritionTargetsCalculator.calculate(
            age = age,
            sex = profile?.sex ?: Constants.Sex.MALE,
            heightCm = height,
            weightKg = weight,
            activityLevel = profile?.activity_level ?: Constants.ActivityLevel.MEDIUM,
            goal = profile?.goal ?: Constants.Goals.IMPROVE_ENERGY,
        ) ?: NutritionTargetsCalculator.calculate(
            age = DEFAULT_AGE_YEARS,
            sex = Constants.Sex.MALE,
            heightCm = DEFAULT_HEIGHT_CM,
            weightKg = DEFAULT_WEIGHT_KG,
            activityLevel = Constants.ActivityLevel.MEDIUM,
            goal = Constants.Goals.IMPROVE_ENERGY,
        )!!

        return ResolvedTargets(
            calories = storedCal ?: computed.calories,
            proteinG = storedP ?: computed.proteinG,
            fatG = storedF ?: computed.fatG,
            carbsG = storedC ?: computed.carbsG,
            fromProfile = false,
            usedBodyDefaults = usedBodyDefaults,
            autoFilled = !allStored,
        )
    }

    private fun nutritionTargetsHint(resolved: ResolvedTargets): String = when {
        resolved.fromProfile -> "Ваши цели из профиля"
        resolved.usedBodyDefaults ->
            "Подобрали ориентиры (~${resolved.calories} ккал) — укажите рост и вес в профиле для точности"
        else -> "Рассчитали КБЖУ по росту, весу, активности и цели"
    }

    private fun maybePersistComputedTargets(profile: ProfileDto?, resolved: ResolvedTargets) {
        if (profile == null || !resolved.autoFilled) return
        val needsSave = profile.target_daily_calories == null || profile.target_daily_calories == 0 ||
            profile.target_protein_g == null || profile.target_protein_g == 0f ||
            profile.target_fat_g == null || profile.target_fat_g == 0f ||
            profile.target_carbs_g == null || profile.target_carbs_g == 0f
        if (!needsSave) return
        viewModelScope.launch {
            profileRepository.updateNutritionTargets(
                targetDailyCalories = resolved.calories,
                targetProteinG = resolved.proteinG,
                targetFatG = resolved.fatG,
                targetCarbsG = resolved.carbsG,
            )
        }
    }
}
