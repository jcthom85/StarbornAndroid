package com.example.starborn.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.ui.arcade.*
import com.example.starborn.feature.arcade.domain.ArcadeIds

@Composable
fun DesktopArcadeScreen(services: DesktopAppServices, onClose: () -> Unit) {
    val session by services.sessionStore.state.collectAsState()
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    val cabinetIds = listOf(ArcadeIds.DEEP_MINE, ArcadeIds.CANOPY_HOPPER, ArcadeIds.SPIRE_INFILTRATOR, ArcadeIds.SLAG_CATCHER, ArcadeIds.ORBITAL_DEFENSE, ArcadeIds.HARMONIC_PULSE)
    val actions = services.worldDataSource.loadRooms().firstOrNull { it.id == "astra_common_room" }?.actions.orEmpty()
    val cabinets = cabinetIds.mapIndexed { index, id ->
        val milestone = "ms_arcade_cabinet_0${index + 1}_repaired"
        id to (actions.firstOrNull { it["show_when_milestone"] == milestone }?.get("name")?.toString() ?: id)
    }
    var playing by remember { mutableStateOf(services.activeArcadeCabinet?.takeIf { services.arcadeService.progress(it).repaired }) }
    val back = { playing = null }
    val cue: (String) -> Unit = { services.audioDriver.executeAll(services.audioRouter.commandsForUi(it)) }
    when (playing) {
        ArcadeIds.DEEP_MINE -> DeepMineArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.CANOPY_HOPPER -> CanopyHopperArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.SPIRE_INFILTRATOR -> SpireInfiltratorArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.SLAG_CATCHER -> SlagCatcherArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.ORBITAL_DEFENSE -> OrbitalDefenseArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.HARMONIC_PULSE -> HarmonicPulseArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        else -> LazyColumn(Modifier.fillMaxSize().background(Color(0xFF07111A)).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Arcade", color = Color.White, style = MaterialTheme.typography.headlineMedium); TextButton(onClick = onClose) { Text("Back to exploration") } }
            cabinets.forEach { (id, title) -> item {
                val progress = session.arcadeProgress[id]
                OutlinedButton(onClick = { playing = id }, enabled = progress?.repaired == true) { Text("$title · High score ${progress?.highScore ?: 0} · ${if (progress?.repaired == true) "Ready" else "Find and repair the cabinet"}") }
            } }
        }
    }
}
