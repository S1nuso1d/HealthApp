package com.example.healtapp.core.utils

object FormatUtils {
    fun waterProgress(current: Int, target: Int): Float {
        if (target <= 0) return 0f
        return (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
}