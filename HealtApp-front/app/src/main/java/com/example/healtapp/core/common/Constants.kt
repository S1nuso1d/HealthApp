package com.example.healtapp.core.common

object Constants {
    const val APP_NAME = "HealthApp"

    object Goals {
        const val BETTER_SLEEP = "better_sleep"
        const val LOSE_WEIGHT = "lose_weight"
        const val GAIN_MUSCLE = "gain_muscle"
        const val IMPROVE_ENERGY = "improve_energy"
    }

    object ActivityLevel {
        const val LOW = "low"
        const val MEDIUM = "medium"
        const val HIGH = "high"
    }

    object Sex {
        const val MALE = "male"
        const val FEMALE = "female"
    }

    /** Максимальный размер файла аватара при отправке на сервер (синхронно с бэкендом). */
    const val AVATAR_MAX_BYTES = 5 * 1024 * 1024
}