package com.example.healtapp.core.telemetry

import android.content.Context
import com.example.healtapp.BuildConfig
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid

/** Инициализация Crash/Error reporting. Без DSN — no-op (локальная разработка). */
object CrashReporting {
    fun init(context: Context) {
        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (dsn.isEmpty()) return
        SentryAndroid.init(context) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.release = "com.healthapp.android@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.isEnableAutoSessionTracking = true
            options.setDiagnosticLevel(SentryLevel.ERROR)
        }
    }
}
