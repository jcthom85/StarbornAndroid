package com.example.starborn.desktop.ui

import com.example.starborn.data.local.EnvironmentalEffectsQuality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.DesktopDisplayMode
import kotlinx.coroutines.launch

@Composable
internal fun DesktopSettingsContent(services: DesktopAppServices, userSettings: UserSettings, currentRoomTitle: String?, onReturnToTitle: (() -> Unit)? = null, onOpenControls: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val mode by services.userSettingsStore.displayMode.collectAsState(initial = DesktopDisplayMode.WINDOWED)
    var archiveOpen by remember { mutableStateOf(false) }
    val audio: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopMenuSection("Audio")
                DesktopVolumeControl("Master volume", userSettings.masterVolume) { scope.launch { services.userSettingsStore.setMasterVolume(it) } }
                DesktopVolumeControl("Music", userSettings.musicVolume) { scope.launch { services.userSettingsStore.setMusicVolume(it) } }
                DesktopVolumeControl("Ambience", userSettings.ambienceVolume) { scope.launch { services.userSettingsStore.setAmbienceVolume(it) } }
                DesktopVolumeControl("Sound effects", userSettings.sfxVolume) { scope.launch { services.userSettingsStore.setSfxVolume(it) } }
                DesktopVolumeControl("Voice", userSettings.voiceVolume) { scope.launch { services.userSettingsStore.setVoiceVolume(it) } }
                DesktopSettingToggle("Mute when window unfocused", userSettings.muteWhenUnfocused) { scope.launch { services.userSettingsStore.setMuteWhenUnfocused(it) } }
            }
        }
    }
    val display: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopMenuSection("Display")
                Text("Screen mode", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopDisplayMode.entries.forEach { option ->
                        FilterChip(mode == option, onClick = { scope.launch { services.userSettingsStore.setDisplayMode(option) } },
                            modifier = Modifier.desktopPointerHover(),
                            label = { Text(when (option) { DesktopDisplayMode.WINDOWED -> "Windowed"; DesktopDisplayMode.BORDERLESS -> "Borderless"; DesktopDisplayMode.FULLSCREEN -> "Fullscreen" }) })
                    }
                }
                Text("Interface scale", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1.0f to "100%", 1.25f to "125%", 1.50f to "150%").forEach { (scale, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(userSettings.uiScale - scale) < 0.05f,
                            modifier = Modifier.desktopPointerHover(),
                            onClick = { scope.launch { services.userSettingsStore.setUiScale(scale) } },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Environmental effects", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnvironmentalEffectsQuality.entries.forEach { quality ->
                        FilterChip(userSettings.environmentalEffectsQuality == quality,
                            modifier = Modifier.desktopPointerHover(),
                            onClick = { scope.launch { services.userSettingsStore.setEnvironmentalEffectsQuality(quality) } }, label = { Text(quality.label) })
                    }
                }
                Text("Windowed mode can be resized (minimum 1024×720).", style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                onOpenControls?.let { OutlinedButton(onClick = it, modifier = Modifier.desktopPointerHover()) { Text("Keyboard controls") } }
            }
        }
    }
    val accessibility: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopMenuSection("Guidance and accessibility")
                DesktopSettingToggle("Auto-advance dialogue", userSettings.autoAdvanceDialogue) { scope.launch { services.userSettingsStore.setAutoAdvanceDialogue(it) } }
                DesktopSettingToggle("Tutorials", userSettings.tutorialsEnabled) { scope.launch { services.userSettingsStore.setTutorialsEnabled(it) } }
                DesktopSettingToggle("Reduce flashes", userSettings.disableFlashes) { scope.launch { services.userSettingsStore.setFlashesDisabled(it) } }
                DesktopSettingToggle("Larger controls", userSettings.largeTouchTargets) { scope.launch { services.userSettingsStore.setLargeTouchTargets(it) } }
                DesktopSettingToggle("Disable screen shake", userSettings.disableScreenshake) { scope.launch { services.userSettingsStore.setScreenshakeDisabled(it) } }
                DesktopSettingToggle("High contrast", userSettings.highContrastMode) { scope.launch { services.userSettingsStore.setHighContrastMode(it) } }
            }
        }
    }
    val saves: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopMenuSection("Save archive")
                Text("Save to a manual slot or quick save. Existing slots require confirmation before replacement.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { archiveOpen = true }, modifier = Modifier.desktopPointerHover()) { Text("Open save archive") }
                Text("To load a different game, return to the title screen and choose Continue.", style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                onReturnToTitle?.let { OutlinedButton(onClick = it, modifier = Modifier.desktopPointerHover()) { Text("Save and return to title") } }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 880.dp) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { audio(); display() }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { accessibility(); saves() }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { audio() }
                item { display() }
                item { accessibility() }
                item { saves() }
            }
        }
    }
    if (archiveOpen) DesktopSaveLoadDialog(services, currentRoomTitle = currentRoomTitle,
        onLoadState = {}, onDismiss = { archiveOpen = false }, allowLoad = false)
}

@Composable
private fun DesktopVolumeControl(label: String, persisted: Float, onCommit: (Float) -> Unit) {
    var volume by remember(persisted) { mutableStateOf(persisted) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label); Text("${(volume * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
        }
        Slider(volume, { volume = it }, onValueChangeFinished = { onCommit(volume) },
            modifier = Modifier.desktopPointerHover(),
            colors = SliderDefaults.colors(thumbColor = FieldMenuDesign.cyan, activeTrackColor = FieldMenuDesign.cyan,
                inactiveTrackColor = FieldMenuDesign.cyan.copy(alpha = .12f)))
    }
}

@Composable
private fun DesktopSettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, onChange, modifier = Modifier.desktopPointerHover())
    }
}
