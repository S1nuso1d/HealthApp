package com.example.healtapp.features.recommendations.presentation

enum class RecommendationPriorityKind {
    HIGH,
    MEDIUM,
    LOW,
}

object RecommendationFormatting {

    fun priorityKind(priority: String): RecommendationPriorityKind {
        val key = priority.lowercase()
        return when {
            key in setOf("high", "high_priority", "высокий", "critical") -> RecommendationPriorityKind.HIGH
            key in setOf("medium", "средний", "normal") -> RecommendationPriorityKind.MEDIUM
            else -> RecommendationPriorityKind.LOW
        }
    }

    fun priorityLabelRu(priority: String): String = when (priorityKind(priority)) {
        RecommendationPriorityKind.HIGH -> "Важно"
        RecommendationPriorityKind.MEDIUM -> "Средне"
        RecommendationPriorityKind.LOW -> "Можно позже"
    }

    fun categoryLabelRu(category: String): String = when (category.lowercase()) {
        "hydration", "water" -> "Вода"
        "sleep" -> "Сон"
        "activity", "steps" -> "Активность"
        "nutrition", "meal", "food" -> "Питание"
        "health", "vitals" -> "Здоровье"
        else -> category.replaceFirstChar { it.uppercase() }
    }
}
