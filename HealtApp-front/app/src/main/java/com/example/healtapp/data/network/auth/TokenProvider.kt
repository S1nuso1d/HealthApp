package com.example.healtapp.data.network.auth

interface TokenProvider {
    fun getToken(): String?
}

