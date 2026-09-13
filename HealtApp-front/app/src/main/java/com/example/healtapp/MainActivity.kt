package com.example.healtapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.healtapp.core.ui.theme.HealthAppRoot
import com.example.healtapp.data.healthconnect.HealthConnectForegroundSync
import com.example.healtapp.data.network.realtime.RealtimeUpdatesClient
import com.example.healtapp.data.preferences.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var realtimeUpdatesClient: RealtimeUpdatesClient

    @Inject
    lateinit var healthConnectForegroundSync: HealthConnectForegroundSync

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Градиентные хедеры должны уходить под системные панели. На Android 15+ это
        // и так обязательно при targetSdk 35, здесь выравниваем поведение и для старых версий.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        healthConnectForegroundSync.ensureStarted()

        setContent {
            HealthAppRoot(themePreferences = themePreferences)
        }
    }

    override fun onStart() {
        super.onStart()
        realtimeUpdatesClient.start()
    }

    override fun onStop() {
        realtimeUpdatesClient.stop()
        super.onStop()
    }
}