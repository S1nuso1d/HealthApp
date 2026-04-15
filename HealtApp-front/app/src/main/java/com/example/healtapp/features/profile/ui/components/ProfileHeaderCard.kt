package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun ProfileHeaderCard(
    fullName: String,
    age: Int,
    sex: String,
    heightCm: Int,
    weightKg: Int
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall
            )
            Text("Возраст: $age")
            Text("Пол: $sex")
            Text("Рост: $heightCm см")
            Text("Вес: $weightKg кг")
        }
    }
}