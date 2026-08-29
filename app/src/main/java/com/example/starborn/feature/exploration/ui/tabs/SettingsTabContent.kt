package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import kotlin.math.roundToInt

@Composable
fun SettingsTabContent(
    settings: SettingsUiState,
    accentColor: Color,
    borderColor: Color,
    showSaveData: Boolean = true,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null,
    onDebugTinkeringTutorial: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showSaveData) {
            MenuSectionCard(
                title = "Save Data",
                accentColor = accentColor,
                borderColor = borderColor
            ) {
                SaveDataPanel(
                    accentColor = accentColor,
                    borderColor = borderColor,
                    onQuickSave = onQuickSave,
                    onSaveGame = onSaveGame,
                    onLoadGame = onLoadGame,
                    onReturnToTitle = onReturnToTitle
                )
            }
        }
        MenuSectionCard(
            title = "Audio & Display",
            accentColor = accentColor,
            borderColor = borderColor
        ) {
            SettingsPanel(
                settings = settings,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onVoiceVolumeChange = onVoiceVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette
            )
        }
        MenuSectionCard(
            title = "Accessibility & Comfort",
            accentColor = accentColor,
            borderColor = borderColor
        ) {
            AccessibilityPanel(
                settings = settings,
                onToggleHighContrast = onToggleHighContrast,
                onToggleLargeTouchTargets = onToggleLargeTouchTargets,
                onToggleScreenshakeDisabled = onToggleScreenshakeDisabled,
                onToggleFlashesDisabled = onToggleFlashesDisabled,
                onToggleHapticsDisabled = onToggleHapticsDisabled
            )
        }
        if (onDebugTinkeringTutorial != null) {
            MenuSectionCard(
                title = "Testing & Diagnostics",
                accentColor = Color(0xFF7BE8FF),
                borderColor = borderColor
            ) {
                Button(
                    onClick = onDebugTinkeringTutorial,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007799),
                        contentColor = Color.White
                    )
                ) {
                    Text("⚡ FAST TEST: Tinkering Tutorial (Cryo-Inductor)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPanel(
    settings: SettingsUiState,
    onToggleHighContrast: (Boolean) -> Unit,
    onToggleLargeTouchTargets: (Boolean) -> Unit,
    onToggleScreenshakeDisabled: (Boolean) -> Unit,
    onToggleFlashesDisabled: (Boolean) -> Unit,
    onToggleHapticsDisabled: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("High Contrast Mode", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.highContrastMode) "Enabled — Enhanced outlines and text" else "Disabled — Standard theme",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.highContrastMode,
                onCheckedChange = onToggleHighContrast
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Large Touch Targets", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.largeTouchTargets) "Enabled — Larger action & menu buttons" else "Disabled — Compact layout",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.largeTouchTargets,
                onCheckedChange = onToggleLargeTouchTargets
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Haptic Feedback", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (!settings.disableHaptics) "Enabled — Vibration rumble" else "Disabled — Vibration muted",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = !settings.disableHaptics,
                onCheckedChange = { onToggleHapticsDisabled(!it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Screen Shake", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (!settings.disableScreenshake) "Enabled — Dynamic camera impact" else "Disabled — Steady camera",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = !settings.disableScreenshake,
                onCheckedChange = { onToggleScreenshakeDisabled(!it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Combat Flashes", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (!settings.disableFlashes) "Enabled — Visual flash bursts" else "Disabled — Flashes suppressed",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = !settings.disableFlashes,
                onCheckedChange = { onToggleFlashesDisabled(!it) }
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit
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
        Column {
            Text(
                text = "Voice Volume ${ (settings.voiceVolume * 100).roundToInt() }%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = settings.voiceVolume,
                onValueChange = onVoiceVolumeChange,
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
    }
}

@Composable
private fun SaveDataPanel(
    accentColor: Color,
    borderColor: Color,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF09131B),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color(0xFF09131B),
                            Color(0xFF071018)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Save Data",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Manual slots, quicksave, and title-screen load all use the same save system.",
                        color = Color.White.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SaveActionButton(
                    label = "Save",
                    detail = "Pick a slot",
                    icon = Icons.Rounded.Save,
                    accentColor = accentColor,
                    onClick = onSaveGame,
                    modifier = Modifier.weight(1f)
                )
                SaveActionButton(
                    label = "Load",
                    detail = "Resume a slot",
                    icon = Icons.Rounded.Download,
                    accentColor = accentColor,
                    onClick = onLoadGame,
                    modifier = Modifier.weight(1f)
                )
            }
            SaveActionButton(
                label = "Quick Save",
                detail = "Write current progress immediately",
                icon = Icons.Rounded.FlashOn,
                accentColor = accentColor,
                onClick = onQuickSave,
                modifier = Modifier.fillMaxWidth()
            )
            if (onReturnToTitle != null) {
                SaveActionButton(
                    label = "Save & Return to Title",
                    detail = "Quicksave and exit to the main menu",
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    accentColor = Color(0xFFFFB300),
                    onClick = {
                        onQuickSave()
                        onReturnToTitle()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SaveActionButton(
    label: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = Color(0xFF0B1722),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = detail,
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
