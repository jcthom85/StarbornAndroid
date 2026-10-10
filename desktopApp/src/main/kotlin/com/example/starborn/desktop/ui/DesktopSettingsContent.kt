package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.DesktopDisplayMode
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class SettingsSection(val label: String) {
    ALL("All Decks"),
    AUDIO("Audio Mixer"),
    DISPLAY("Display"),
    ACCESSIBILITY("Accessibility"),
    CONTROLS("Controls"),
    SAVES("Save Archive")
}

@Composable
internal fun DesktopSettingsContent(
    services: DesktopAppServices,
    userSettings: UserSettings,
    currentRoomTitle: String?,
    onReturnToTitle: (() -> Unit)? = null,
    onOpenControls: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val mode by services.userSettingsStore.displayMode.collectAsState(initial = DesktopDisplayMode.WINDOWED)
    var archiveOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(SettingsSection.ALL) }

    // Live dialogue chatter preview state
    var selectedSpeaker by remember { mutableStateOf("nova") }
    var isTestingChatter by remember { mutableStateOf(false) }
    var testPitchDisplay by remember { mutableStateOf<Float?>(null) }

    val audioDeck: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopMenuSection("Audio Mixer Deck")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = FieldMenuDesign.cyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "LIVE MIX",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = FieldMenuDesign.cyan,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                }

                DesktopVolumeControl("Master volume", userSettings.masterVolume) {
                    scope.launch { services.userSettingsStore.setMasterVolume(it) }
                }
                DesktopVolumeControl("Music", userSettings.musicVolume) {
                    scope.launch { services.userSettingsStore.setMusicVolume(it) }
                }
                DesktopVolumeControl("Ambience", userSettings.ambienceVolume) {
                    scope.launch { services.userSettingsStore.setAmbienceVolume(it) }
                }
                DesktopVolumeControl("Sound effects", userSettings.sfxVolume, isSfx = true, services = services) {
                    scope.launch { services.userSettingsStore.setSfxVolume(it) }
                }
                DesktopVolumeControl("Voice", userSettings.voiceVolume) {
                    scope.launch { services.userSettingsStore.setVoiceVolume(it) }
                }

                HorizontalDivider(color = FieldMenuDesign.cyan.copy(alpha = 0.15f))

                // Interactive Dialogue Chatter & Celeste Pitch Shift Tester
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF091420))
                        .border(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Dynamic Dialogue Chatter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Modulates pitch dynamically up & down as characters speak",
                                style = MaterialTheme.typography.bodySmall,
                                color = FieldMenuDesign.textMuted
                            )
                        }

                        testPitchDisplay?.let { pitch ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FieldMenuDesign.gold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "Pitch: ${"%.2f".format(pitch)}x",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    color = FieldMenuDesign.gold,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Speaker Selection Chips
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "nova" to "Nova",
                            "orion" to "Orion",
                            "zeke" to "Zeke",
                            "gh0st" to "Gh0st",
                            "female" to "Female NPC",
                            "male" to "Male NPC"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = selectedSpeaker == id,
                                onClick = { selectedSpeaker = id },
                                modifier = Modifier.desktopPointerHover(),
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FieldMenuDesign.cyan.copy(alpha = 0.22f),
                                    selectedLabelColor = FieldMenuDesign.cyan
                                )
                            )
                        }
                    }

                    // Test Trigger Button
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isTestingChatter) return@Button
                                scope.launch {
                                    isTestingChatter = true
                                    val profile = when (selectedSpeaker) {
                                        "orion" -> DialogueVoiceProfile.ORION
                                        "zeke" -> DialogueVoiceProfile.ZEKE
                                        "gh0st" -> DialogueVoiceProfile.GH0ST
                                        "female" -> DialogueVoiceProfile.FEMALE
                                        "male" -> DialogueVoiceProfile.MALE
                                        else -> DialogueVoiceProfile.NOVA
                                    }
                                    val sampleSentence = "Checking communication relays and local energy conduits... all clear?"
                                    val random = Random(System.currentTimeMillis())
                                    for (i in 0 until 6) {
                                        val cue = profile.randomCue(random)
                                        val pitch = profile.calculateCelestePitch(i * 3, sampleSentence, random)
                                        testPitchDisplay = pitch
                                        services.audioDriver.executeAll(
                                            listOf(
                                                AudioCommand.Play(
                                                    type = AudioCueType.VOICE,
                                                    cueId = cue,
                                                    loop = false,
                                                    fadeMs = 0L,
                                                    gain = userSettings.voiceVolume,
                                                    pitch = pitch
                                                )
                                            )
                                        )
                                        delay(115L)
                                    }
                                    delay(400L)
                                    testPitchDisplay = null
                                    isTestingChatter = false
                                }
                            },
                            enabled = !isTestingChatter,
                            modifier = Modifier.desktopPointerHover(),
                            colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.cyan)
                        ) {
                            Text(
                                if (isTestingChatter) "Speaking..." else "Test Dialogue Chatter",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DesktopSettingToggle("Mute when unfocused", userSettings.muteWhenUnfocused) {
                            scope.launch { services.userSettingsStore.setMuteWhenUnfocused(it) }
                        }
                    }
                }
            }
        }
    }

    val displayDeck: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DesktopMenuSection("Display & Video Deck")

                Text("Screen Mode", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DesktopDisplayMode.entries.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { scope.launch { services.userSettingsStore.setDisplayMode(option) } },
                            modifier = Modifier.desktopPointerHover(),
                            label = {
                                Text(
                                    when (option) {
                                        DesktopDisplayMode.WINDOWED -> "Windowed (1280×800)"
                                        DesktopDisplayMode.BORDERLESS -> "Borderless Window"
                                        DesktopDisplayMode.FULLSCREEN -> "Exclusive Fullscreen"
                                    }
                                )
                            }
                        )
                    }
                }

                Text("Interface Scaling", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1.0f to "100% (Standard)", 1.25f to "125% (Large)", 1.50f to "150% (High-DPI)").forEach { (scale, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(userSettings.uiScale - scale) < 0.05f,
                            modifier = Modifier.desktopPointerHover(),
                            onClick = { scope.launch { services.userSettingsStore.setUiScale(scale) } },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Environmental Effects & Atmospheric Shaders", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnvironmentalEffectsQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = userSettings.environmentalEffectsQuality == quality,
                            modifier = Modifier.desktopPointerHover(),
                            onClick = { scope.launch { services.userSettingsStore.setEnvironmentalEffectsQuality(quality) } },
                            label = {
                                Text(
                                    when (quality) {
                                        EnvironmentalEffectsQuality.FULL -> "Full (Weather, Dust & Rays)"
                                        EnvironmentalEffectsQuality.REDUCED -> "Reduced (Optimized)"
                                        EnvironmentalEffectsQuality.OFF -> "Off (Minimal)"
                                    }
                                )
                            }
                        )
                    }
                }

                DesktopSettingToggle("Cinematic Room Vignette", userSettings.vignetteEnabled) {
                    scope.launch { services.userSettingsStore.setVignetteEnabled(it) }
                }

                Text(
                    "Windowed display mode supports interactive resizing with min constraints 1024×700.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FieldMenuDesign.textMuted
                )
            }
        }
    }

    val accessibilityDeck: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopMenuSection("Guidance & Accessibility")

                Text("Text Reveal Speed", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.7f to "Slow (0.7x)", 1.0f to "Normal (1.0x)", 1.6f to "Fast (1.6x)", 0.0f to "Instant").forEach { (speed, label) ->
                        FilterChip(
                            selected = if (speed == 0.0f) userSettings.textSpeed == 0.0f else kotlin.math.abs(userSettings.textSpeed - speed) < 0.1f,
                            modifier = Modifier.desktopPointerHover(),
                            onClick = { scope.launch { services.userSettingsStore.setTextSpeed(speed) } },
                            label = { Text(label) }
                        )
                    }
                }

                DesktopSettingToggle("Auto-advance dialogue lines", userSettings.autoAdvanceDialogue) {
                    scope.launch { services.userSettingsStore.setAutoAdvanceDialogue(it) }
                }
                DesktopSettingToggle("Tutorials & Field Guidance", userSettings.tutorialsEnabled) {
                    scope.launch { services.userSettingsStore.setTutorialsEnabled(it) }
                }
                DesktopSettingToggle("Reduce intense screen flashes", userSettings.disableFlashes) {
                    scope.launch { services.userSettingsStore.setFlashesDisabled(it) }
                }
                DesktopSettingToggle("Disable screen shake & rumble", userSettings.disableScreenshake) {
                    scope.launch { services.userSettingsStore.setScreenshakeDisabled(it) }
                }
                DesktopSettingToggle("High contrast interface & text outlines", userSettings.highContrastMode) {
                    scope.launch { services.userSettingsStore.setHighContrastMode(it) }
                }
                DesktopSettingToggle("Larger click & touch targets", userSettings.largeTouchTargets) {
                    scope.launch { services.userSettingsStore.setLargeTouchTargets(it) }
                }
            }
        }
    }

    val controlsDeck: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopMenuSection("Tactical Keyboard Reference")
                    onOpenControls?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.desktopPointerHover()) {
                            Text("Full Guide Overlay [H]")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF091420))
                        .border(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "W / A / S / D  or  Arrow Keys" to "Navigate Sector & Move Party",
                        "E  or  Space  or  Enter" to "Interact with Terminals, NPCs & Items",
                        "Tab  or  M" to "Open / Close Tactical Field Menu",
                        "1, 2, 3, 4, 5, 0" to "Quick Decks: Cargo, Kit, Journal, Map, Settings, Stats",
                        "F5" to "Instant Quick Save to Active Slot",
                        "Esc" to "Cancel / Back / Close Dialog"
                    ).forEach { (keys, desc) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FieldMenuDesign.cyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    keys,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = FieldMenuDesign.cyan,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }

    val savesDeck: @Composable () -> Unit = {
        DesktopMenuCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopMenuSection("Save Archive & Title System")
                Text(
                    "Store mission progress into dedicated save slots. Existing slots require confirmation before replacement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { archiveOpen = true },
                        modifier = Modifier.desktopPointerHover(),
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold)
                    ) {
                        Text("Open save archive", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    onReturnToTitle?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.desktopPointerHover()) {
                            Text("Save and return to title")
                        }
                    }
                }

                Text(
                    "Starborn Multiplatform PC Core · v2.4 (JVM JBR Engine)",
                    style = MaterialTheme.typography.labelSmall,
                    color = FieldMenuDesign.textMuted
                )
            }
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Filter Strip
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSection.entries.forEach { section ->
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    modifier = Modifier.desktopPointerHover(),
                    label = { Text(section.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FieldMenuDesign.gold.copy(alpha = 0.2f),
                        selectedLabelColor = FieldMenuDesign.gold
                    )
                )
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val isWide = maxWidth >= 880.dp
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedSection) {
                    SettingsSection.AUDIO -> {
                        item { audioDeck() }
                    }
                    SettingsSection.DISPLAY -> {
                        item { displayDeck() }
                    }
                    SettingsSection.ACCESSIBILITY -> {
                        item { accessibilityDeck() }
                    }
                    SettingsSection.CONTROLS -> {
                        item { controlsDeck() }
                    }
                    SettingsSection.SAVES -> {
                        item { savesDeck() }
                    }
                    SettingsSection.ALL -> {
                        if (isWide) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        audioDeck()
                                        displayDeck()
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        accessibilityDeck()
                                        controlsDeck()
                                        savesDeck()
                                    }
                                }
                            }
                        } else {
                            item { audioDeck() }
                            item { displayDeck() }
                            item { accessibilityDeck() }
                            item { controlsDeck() }
                            item { savesDeck() }
                        }
                    }
                }
            }
        }
    }

    if (archiveOpen) {
        DesktopSaveLoadDialog(
            services = services,
            currentRoomTitle = currentRoomTitle,
            onLoadState = {},
            onDismiss = { archiveOpen = false },
            allowLoad = false
        )
    }
}

@Composable
private fun DesktopVolumeControl(
    label: String,
    persisted: Float,
    isSfx: Boolean = false,
    services: DesktopAppServices? = null,
    onCommit: (Float) -> Unit
) {
    var volume by remember(persisted) { mutableStateOf(persisted) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text("${(volume * 100).toInt()}%", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
        Slider(
            value = volume,
            onValueChange = { volume = it },
            onValueChangeFinished = {
                onCommit(volume)
                if (isSfx) {
                    services?.audioDriver?.execute(
                        AudioCommand.Play(
                            type = AudioCueType.UI,
                            cueId = "ui_confirm",
                            gain = volume
                        )
                    )
                }
            },
            modifier = Modifier.desktopPointerHover(),
            colors = SliderDefaults.colors(
                thumbColor = FieldMenuDesign.cyan,
                activeTrackColor = FieldMenuDesign.cyan,
                inactiveTrackColor = FieldMenuDesign.cyan.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun DesktopSettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            modifier = Modifier.desktopPointerHover(),
            colors = SwitchDefaults.colors(
                checkedThumbColor = FieldMenuDesign.cyan,
                checkedTrackColor = FieldMenuDesign.cyan.copy(alpha = 0.35f)
            )
        )
    }
}
