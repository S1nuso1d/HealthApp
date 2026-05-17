package com.example.healtapp.features.meal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.api.IntegrationsApi
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishCreateRequestDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            val profileRes = profileRepository.getMyProfile()
            val targets = profileRes.getOrNull()
            val calTarget = targets?.target_daily_calories ?: 2200
            val tp = targets?.target_protein_g
            val tf = targets?.target_fat_g
            val tc = targets?.target_carbs_g

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
                _uiState.update {
                    it.copy(
                        isFoodSearchLoading = false,
                        foodSearchError = e.message ?: "Ошибка штрихкода",
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
                        )
                    }
                    loadMeals()
                    AppRefreshBus.notifyDataChanged()
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось добавить в дневник",
                        )
                    }
                }
        }
    }

    fun addSavedDishToDiary(dish: SavedDishDto, onSuccess: () -> Unit = {}) {
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
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Не удалось добавить блюдо")
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
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Не удалось сохранить прием пищи",
                        )
                    }
                }
        }
    }

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
}
