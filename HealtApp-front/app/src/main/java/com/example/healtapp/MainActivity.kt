package com.example.healtapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* пользователь может отказать — тогда canPost останется false */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Градиентные хедеры должны уходить под системные панели. На Android 15+ это
        // и так обязательно при targetSdk 35, здесь выравниваем поведение и для старых версий.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        healthConnectForegroundSync.ensureStarted()
        requestNotificationPermissionIfNeeded()

        setContent {
            HealthAppRoot(themePreferences = themePreferences)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
