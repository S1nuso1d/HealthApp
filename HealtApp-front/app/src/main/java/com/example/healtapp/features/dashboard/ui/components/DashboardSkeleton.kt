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
import com.example.healtapp.core.ui.components.ShimmerBox

@Composable
fun DashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(148.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(148.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(148.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(148.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp))
    }
}
