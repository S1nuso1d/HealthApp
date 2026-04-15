package com.example.healtapp.domain.model

data class UserProfile(
    val fullName: String,
    val age: Int,
    val sex: String,
    val heightCm: Int,
    val weightKg: Int,
    val mainGoal: String,
    val waterTargetMl: Int,
    val sleepTargetHours: Int,
    val activityLevel: String
)