package com.example.healtapp.domain.model

data class SleepRecord(
    val id: Int,
    val sleepStart: String,
    val sleepEnd: String,
    val qualityScore: Int,
    val note: String
)