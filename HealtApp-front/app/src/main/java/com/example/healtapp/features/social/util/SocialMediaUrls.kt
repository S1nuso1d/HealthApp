package com.example.healtapp.features.social.util

object SocialMediaUrls {
    fun resolveMediaUrl(baseUrl: String, path: String?): String? {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            return raw
        }
        val root = baseUrl.trimEnd('/')
        return if (raw.startsWith("/")) "$root$raw" else "$root/$raw"
    }

    fun userAvatarUrl(baseUrl: String, userId: Int, hasAvatar: Boolean): String? {
        if (!hasAvatar) return null
        return "${baseUrl.trimEnd('/')}/social/users/$userId/avatar"
    }
}
