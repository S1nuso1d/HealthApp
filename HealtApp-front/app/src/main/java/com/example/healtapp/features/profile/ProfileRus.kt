package com.example.healtapp.features.profile

import com.example.healtapp.core.common.Constants

object ProfileRus {
    fun goalLabel(goalIdOrText: String): String = when (goalIdOrText) {
        Constants.Goals.BETTER_SLEEP -> "Лучше спать"
        Constants.Goals.LOSE_WEIGHT -> "Похудеть"
        Constants.Goals.GAIN_MUSCLE -> "Набрать массу"
        Constants.Goals.IMPROVE_ENERGY -> "Больше энергии"
        else -> goalIdOrText
    }

    fun activityLevelLabel(levelIdOrText: String): String = when (levelIdOrText) {
        Constants.ActivityLevel.LOW -> "Низкая"
        Constants.ActivityLevel.MEDIUM -> "Средняя"
        Constants.ActivityLevel.HIGH -> "Высокая"
        else -> levelIdOrText
    }
}
