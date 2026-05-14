package com.example.healtapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.healtapp.core.ui.theme.HealthAppTheme
import com.example.healtapp.core.navigation.AppNavGraph
import com.example.healtapp.data.network.realtime.RealtimeUpdatesClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var realtimeUpdatesClient: RealtimeUpdatesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HealthAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
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