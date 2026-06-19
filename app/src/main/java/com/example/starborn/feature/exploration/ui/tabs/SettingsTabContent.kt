package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import kotlin.math.roundToInt

@Composable
fun SettingsTabContent(
    settings: SettingsUiState,
    accentColor: Color,
    borderColor: Color,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit
) {
    MenuSectionCard(
        title = "Audio & Display",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        SettingsPanel(
            settings = settings,
            onMusicVolumeChange = onMusicVolumeChange,
            onSfxVolumeChange = onSfxVolumeChange,
            onToggleTutorials = onToggleTutorials,
            onToggleVignette = onToggleVignette,
            onQuickSave = onQuickSave,
            onSaveGame = onSaveGame,
            onLoadGame = onLoadGame
        )
    }
}

@Composable
private fun SettingsPanel(
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Music Volume ${ (settings.musicVolume * 100).roundToInt() }%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = settings.musicVolume,
                onValueChange = onMusicVolumeChange,
                valueRange = 0f..1f
            )
        }
        Column {
            Text(
                text = "Effects Volume ${ (settings.sfxVolume * 100).roundToInt() }%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = settings.sfxVolume,
                onValueChange = onSfxVolumeChange,
                valueRange = 0f..1f
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Room Vignette", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.vignetteEnabled) "Enabled" else "Disabled",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.vignetteEnabled,
                onCheckedChange = onToggleVignette
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tutorials", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.tutorialsEnabled) "Shown" else "Hidden",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.tutorialsEnabled,
                onCheckedChange = onToggleTutorials
            )
        }
        Button(
            onClick = onSaveGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
        Button(
            onClick = onLoadGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load")
        }
        Button(
            onClick = onQuickSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quick Save")
        }
        Text(
            text = "Quicksave writes your current progress immediately without leaving the game.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
