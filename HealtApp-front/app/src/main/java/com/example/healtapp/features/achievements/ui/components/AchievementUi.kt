package com.example.healtapp.features.achievements.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

fun achievementIcon(key: String): ImageVector = when (key) {
    "steps" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "water" -> Icons.Filled.WaterDrop
    "sleep" -> Icons.Filled.Nightlight
    "fire" -> Icons.Filled.LocalFireDepartment
    "workout" -> Icons.Filled.FitnessCenter
    "run" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "speed" -> Icons.Filled.Speed
    else -> Icons.Filled.Star
}

fun formatAchievementProgressValue(value: Float, unit: String?): String = when (unit) {
    "км", "л" -> "${"%.1f".format(value)} $unit"
    "ч" -> "${"%.1f".format(value)} ч"
    null -> value.toInt().toString()
    else -> "${value.toInt()} $unit"
}
