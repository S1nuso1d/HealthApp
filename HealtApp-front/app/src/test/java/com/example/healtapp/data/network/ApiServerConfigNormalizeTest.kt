package com.example.healtapp.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiServerConfigNormalizeTest {

    @Test
    fun addsTrailingSlash() {
        val url = ApiServerConfig.normalizeBaseUrl("https://api.example.com")
        assertEquals("https://api.example.com/", url)
    }

    @Test
    fun keepsExistingSlash() {
        val url = ApiServerConfig.normalizeBaseUrl("https://api.example.com/")
        assertEquals("https://api.example.com/", url)
    }

    @Test
    fun trimsWhitespace() {
        val url = ApiServerConfig.normalizeBaseUrl("  https://api.example.com  ")
        assertEquals("https://api.example.com/", url)
    }

    @Test
    fun rejectsBlankByFallingBackToBuildConfig() {
        val url = ApiServerConfig.normalizeBaseUrl("   ")
        assertTrue(url.endsWith("/"))
        assertTrue(url.startsWith("http"))
    }
}
