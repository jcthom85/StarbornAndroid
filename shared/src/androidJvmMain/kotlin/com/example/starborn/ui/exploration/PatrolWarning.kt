package com.example.starborn.ui.exploration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.movement.EnemyAggressionState

@Composable
fun PatrolWarning(aggression: EnemyAggressionState, modifier: Modifier = Modifier) {
    val seconds = (aggression.remainingMs + 999) / 1_000
    Surface(
        modifier.testTag("patrol-warning"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xF023171A),
        border = BorderStroke(1.dp, Color(0xFFB97651))
    ) {
        Text(
            "Patrol spotted you · Combat in ${seconds}s",
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color(0xFFFFD8B0),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
