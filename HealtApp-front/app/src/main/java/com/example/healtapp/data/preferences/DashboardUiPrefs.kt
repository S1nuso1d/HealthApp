package com.example.healtapp.data.preferences

import android.content.Context

enum class DashboardLayoutMode(val key: String, val labelRu: String) {
    Classic("classic", "Классический"),
    Experimental("experimental", "Экспериментальный"),
    ;

    companion object {
        fun fromKey(key: String?): DashboardLayoutMode =
            entries.find { it.key == key } ?: Classic
    }
}

object DashboardUiPrefs {
    private const val PREFS_NAME = "dashboard_ui_prefs"
    private const val KEY_LAYOUT = "layout_mode"

    fun getLayoutMode(context: Context): DashboardLayoutMode {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAYOUT, DashboardLayoutMode.Classic.key)
        return DashboardLayoutMode.fromKey(key)
    }

    fun setLayoutMode(context: Context, mode: DashboardLayoutMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAYOUT, mode.key)
            .apply()
    }
}
