package com.example.healtapp.core.utils

object ValidationUtils {
    fun isPasswordValid(password: String): Boolean = password.length >= 6
}