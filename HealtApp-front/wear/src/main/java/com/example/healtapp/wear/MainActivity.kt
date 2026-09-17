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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val stepsState = mutableIntStateOf(-1)
    private val waterState = mutableIntStateOf(-1)
    private val waterGoalState = mutableIntStateOf(2500)
    private val connectedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val steps by stepsState
            val water by waterState
            val waterGoal by waterGoalState
            val hasSnapshot by connectedState
            HealthAppWearTheme {
                WearHomeScreen(
                    steps = steps,
                    waterMl = water,
                    waterGoal = waterGoal,
                    hasSnapshot = hasSnapshot,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Wearable.getDataClient(this).dataItems.addOnSuccessListener { buffer ->
            buffer.forEach { item ->
                if (item.uri.path == "/health/snapshot") {
                    applyMap(DataMapItem.fromDataItem(item).dataMap)
                }
            }
            buffer.release()
        }
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/health/snapshot") {
                applyMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
        dataEvents.release()
    }

    private fun applyMap(map: com.google.android.gms.wearable.DataMap) {
        stepsState.intValue = map.getInt("steps", 0)
        waterState.intValue = map.getInt("water_ml", 0)
        waterGoalState.intValue = map.getInt("water_goal", 2500)
        connectedState.value = true
    }
}

@Composable
fun HealthAppWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = androidx.wear.compose.material.Colors(
            primary = Color(0xFF4FE3BE),
            primaryVariant = Color(0xFF10AC84),
            secondary = Color(0xFF7CC0FF),
            background = Color(0xFF07161A),
            surface = Color(0xFF10262C),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}

@Composable
fun WearHomeScreen(
    steps: Int,
    waterMl: Int,
    waterGoal: Int,
    hasSnapshot: Boolean,
) {
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Сводка",
            color = MaterialTheme.colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        MetricBlock(
            label = "Шаги",
            value = if (hasSnapshot && steps >= 0) "%,d".format(steps).replace(',', ' ') else "—",
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetricBlock(
            label = "Вода",
            value = if (hasSnapshot && waterMl >= 0) {
                "$waterMl / $waterGoal мл"
            } else {
                "—"
            },
            accent = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                        if (nodes.isEmpty()) {
                            Toast.makeText(context, "Телефон не подключен", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val messageClient = Wearable.getMessageClient(context)
                        nodes.forEach { node ->
                            messageClient.sendMessage(node.id, "/water/add", ByteArray(0)).await()
                        }
                        Toast.makeText(context, "+250 мл отправлено", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("WearApp", "Failed to send message", e)
                        Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = MaterialTheme.colors.secondary,
            ),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text(
                text = "+250 мл",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, accent: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 12.sp)
        Text(
            text = value,
            color = if (accent) MaterialTheme.colors.secondary else Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
