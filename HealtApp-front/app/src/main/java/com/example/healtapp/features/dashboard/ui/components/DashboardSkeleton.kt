package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ShimmerBox

@Composable
fun DashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // header placeholder
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(34.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp))

        repeat(3) {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ShimmerBox(modifier = Modifier.size(72.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(16.dp))
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(18.dp))
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp))
                    }
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
            }
        }
    }
}

