package com.example.healtapp.features.auth.presentation

import com.example.healtapp.core.common.AgeUtils
import com.example.healtapp.core.common.Constants
import com.example.healtapp.data.network.dto.auth.RegisterProfileDraftDto

object AuthRegistrationMapper {

    fun buildProfileDraft(state: AuthUiState): RegisterProfileDraftDto {
        val age = AgeUtils.ageFromBirthDate(state.birthDate)
        val dietaryText = buildDietaryText(state)
        val hasDietary = dietaryText != null

        return RegisterProfileDraftDto(
            first_name = state.firstName.trim().ifBlank { null },
            last_name = state.lastName.trim().ifBlank { null },
            nickname = state.nickname.trim().ifBlank { null },
            age = age,
            birth_date = state.birthDate.trim().take(10).ifBlank { null },
            sex = state.sex,
            height_cm = state.height.trim().replace(',', '.').toFloatOrNull(),
            weight_kg = state.weight.trim().replace(',', '.').toFloatOrNull(),
            goal = state.goal,
            activity_level = Constants.ActivityLevel.MEDIUM,
            is_vegetarian = state.isVegetarian ||
                "vegetarian" in state.dietaryExclusions ||
                "vegan" in state.dietaryExclusions,
            has_allergies = hasDietary,
            allergies_text = dietaryText,
        )
    }

    fun buildDietaryText(state: AuthUiState): String? {
        val parts = mutableListOf<String>()
        if (state.dietaryExclusions.isNotEmpty()) {
            val labels = state.dietaryExclusions.map { DietaryExclusionCatalog.labelFor(it) }
            parts.add("Исключения: ${labels.joinToString(", ")}")
        }
        if (state.dietaryNotes.isNotBlank()) {
            parts.add("Примечания: ${state.dietaryNotes.trim()}")
        }
        if (state.allergiesText.isNotBlank()) {
            parts.add("Аллергия: ${state.allergiesText.trim()}")
        }
        return parts.joinToString(". ").take(500).ifBlank { null }
    }
}
