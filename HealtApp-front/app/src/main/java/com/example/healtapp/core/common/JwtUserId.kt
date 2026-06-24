package com.example.healtapp.core.common

import android.util.Base64
import org.json.JSONObject

object JwtUserId {
    fun fromAccessToken(token: String?): Int? {
        if (token.isNullOrBlank()) return null
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val sub = JSONObject(String(decoded, Charsets.UTF_8)).optString("sub")
            sub.toIntOrNull()
        }.getOrNull()
    }
}
