package com.example.healtapp.core.ui.components

import android.content.Context

enum class FeatureGuideScreen(val key: String) {
    Dashboard("dashboard"),
    Sleep("sleep"),
    Activity("activity"),
    Nutrition("nutrition"),
    Profile("profile"),
}

object FeatureGuidePrefs {
    private const val PREFS_NAME = "feature_guide_prefs"
    private const val LEGACY_NUTRITION_PREFS = "nutrition_guide_prefs"
    private const val LEGACY_NUTRITION_SEEN = "meal_diary_intro_seen"
    private const val LEGACY_NUTRITION_REQUEST = "meal_diary_intro_request_show"
    private const val LEGACY_MIGRATED = "legacy_nutrition_migrated"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun migrateLegacyNutrition(context: Context) {
        val sp = prefs(context)
        if (sp.getBoolean(LEGACY_MIGRATED, false)) return
        val legacy = context.getSharedPreferences(LEGACY_NUTRITION_PREFS, Context.MODE_PRIVATE)
        if (legacy.getBoolean(LEGACY_NUTRITION_SEEN, false)) {
            sp.edit().putBoolean(seenKey(FeatureGuideScreen.Nutrition), true).apply()
        }
        if (legacy.getBoolean(LEGACY_NUTRITION_REQUEST, false)) {
            sp.edit().putBoolean(requestKey(FeatureGuideScreen.Nutrition), true).apply()
        }
        sp.edit().putBoolean(LEGACY_MIGRATED, true).apply()
    }

    private fun seenKey(screen: FeatureGuideScreen) = "${screen.key}_seen"
    private fun requestKey(screen: FeatureGuideScreen) = "${screen.key}_request"

    fun hasSeen(context: Context, screen: FeatureGuideScreen): Boolean {
        migrateLegacyNutrition(context)
        return prefs(context).getBoolean(seenKey(screen), false)
    }

    fun shouldShow(context: Context, screen: FeatureGuideScreen): Boolean {
        migrateLegacyNutrition(context)
        val sp = prefs(context)
        return sp.getBoolean(requestKey(screen), false) || !sp.getBoolean(seenKey(screen), false)
    }

    fun markSeen(context: Context, screen: FeatureGuideScreen) {
        prefs(context).edit()
            .putBoolean(seenKey(screen), true)
            .putBoolean(requestKey(screen), false)
            .apply()
    }

    fun requestShowAgain(context: Context, screen: FeatureGuideScreen) {
        prefs(context).edit().putBoolean(requestKey(screen), true).apply()
    }

    fun requestShowAllAgain(context: Context) {
        FeatureGuideScreen.entries.forEach { requestShowAgain(context, it) }
    }
}
