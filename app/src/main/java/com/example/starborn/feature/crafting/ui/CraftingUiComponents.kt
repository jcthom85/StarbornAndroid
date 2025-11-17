package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.crafting.IngredientUi
import com.example.starborn.feature.crafting.MinigameDifficulty
import com.example.starborn.feature.crafting.TimingMinigameUi
import java.util.Locale

@Composable
fun IngredientRow(ingredient: IngredientUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(ingredient.label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "${ingredient.available}/${ingredient.required}",
            style = MaterialTheme.typography.bodySmall,
            color = if (ingredient.available >= ingredient.required) Color.White else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun TimingMinigameOverlay(
    minigame: TimingMinigameUi,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    instructions: String,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            tonalElevation = 6.dp,
            color = if (highContrastMode) Color(0xFF101725) else MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(0.9f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Text(instructions, style = MaterialTheme.typography.bodySmall, color = bodyColor)
                MinigameMetaRow(
                    difficulty = minigame.difficulty,
                    timeRemainingSeconds = minigame.timeRemainingSeconds,
                    totalDurationSeconds = minigame.durationSeconds,
                    highContrastMode = highContrastMode
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(minigame.recipeId) {
                            detectTapGestures { onStop() }
                        }
                ) {
                    val trackHeight = size.height * 0.4f
                    val top = (size.height - trackHeight) / 2f
                    drawRoundRect(
                        color = Color.DarkGray,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    val successStart = minigame.successStart.coerceIn(0f, 1f) * size.width
                    val successEnd = minigame.successEnd.coerceIn(0f, 1f) * size.width
                    val successWidth = (successEnd - successStart).coerceAtLeast(6f)
                    drawRect(
                        color = Color(0xFF2E7D32),
                        topLeft = Offset(successStart, top),
                        size = Size(successWidth, trackHeight)
                    )
                    val perfectStart = minigame.perfectStart.coerceIn(0f, 1f) * size.width
                    val perfectEnd = minigame.perfectEnd.coerceIn(0f, 1f) * size.width
                    val perfectWidth = (perfectEnd - perfectStart).coerceAtLeast(4f)
                    drawRect(
                        color = Color(0xFFFFF176),
                        topLeft = Offset(perfectStart, top),
                        size = Size(perfectWidth, trackHeight)
                    )
                    val progressX = minigame.progress.coerceIn(0f, 1f) * size.width
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(progressX - 3f, top - 6f),
                        size = Size(6f, trackHeight + 12f)
                    )
                }
                val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onStop, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Stop") }
                    Button(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun MinigameMetaRow(
    difficulty: MinigameDifficulty,
    timeRemainingSeconds: Float,
    totalDurationSeconds: Float,
    highContrastMode: Boolean
) {
    val difficultyLabel = when (difficulty) {
        MinigameDifficulty.EASY -> "Easy"
        MinigameDifficulty.NORMAL -> "Normal"
        MinigameDifficulty.HARD -> "Hard"
    }
    val remainingLabel = String.format(Locale.getDefault(), "%.1fs", timeRemainingSeconds.coerceAtLeast(0f))
    val durationLabel = String.format(Locale.getDefault(), "%.0fs", totalDurationSeconds)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val color = if (highContrastMode) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.85f)
        Text(
            text = "Difficulty: $difficultyLabel",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
        Text(
            text = "Time left: $remainingLabel / $durationLabel",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
