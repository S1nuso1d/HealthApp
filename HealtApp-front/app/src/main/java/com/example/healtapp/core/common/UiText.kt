package com.example.healtapp.core.common

sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    fun asString(): String {
        return when (this) {
            is DynamicString -> value
        }
    }
}