package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.DialogueLine

@Composable
fun DialogueOverlay(
    line: DialogueLine,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.8f),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = line.speaker,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onAdvance) {
                Text(text = if (line.next != null) "Continue" else "Close")
            }
        }
    }
}
