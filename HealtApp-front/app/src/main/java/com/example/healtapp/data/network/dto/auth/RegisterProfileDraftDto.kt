package com.example.healtapp.data.network.dto.auth

data class RegisterProfileDraftDto(
    val first_name: String? = null,
    val last_name: String? = null,
    val nickname: String? = null,
    val age: Int? = null,
    val is_vegetarian: Boolean? = null,
    val has_allergies: Boolean? = null,
    val allergies_text: String? = null,
)
