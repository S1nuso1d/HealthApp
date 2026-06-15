package com.example.healtapp.wear

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthAppWearTheme {
                WearHomeScreen()
            }
        }
    }
}

@Composable
fun HealthAppWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = androidx.wear.compose.material.Colors(
            primary = Color(0xFF00BFA5),
            primaryVariant = Color(0xFF00897B),
            secondary = Color(0xFF03A9F4),
            background = Color.Black,
            surface = Color(0xFF202124),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun WearHomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сводка",
            color = MaterialTheme.colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Steps Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Шаги",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Text(
                text = "5 230",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Water Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Вода",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Text(
                text = "1250 мл",
                color = MaterialTheme.colors.secondary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val nodeClient = Wearable.getNodeClient(context)
                        val nodes = nodeClient.connectedNodes.await()
                        if (nodes.isEmpty()) {
                            Toast.makeText(context, "Телефон не подключен", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val messageClient = Wearable.getMessageClient(context)
                        var messageSent = false
                        for (node in nodes) {
                            messageClient.sendMessage(node.id, "/water/add", ByteArray(0)).await()
                            messageSent = true
                        }

                        if (messageSent) {
                            Toast.makeText(context, "+250 мл добавлено", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("WearApp", "Failed to send message", e)
                        Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = MaterialTheme.colors.secondary
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "+250 мл",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
