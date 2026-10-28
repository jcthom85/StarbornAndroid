package com.example.starborn.desktop

import com.example.starborn.domain.audio.*
import com.example.starborn.feature.combat.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopCombatMusicPolicyTest {
    @Test fun eachEncounterRequestsFreshMusicOnceIncludingBosses() = runBlocking {
        val directory = Files.createTempDirectory("starborn-combat-music").toFile()
        val services = DesktopAppServices(directory)
        try {
            withContext(Dispatchers.Main) { check(services.startDebugScenario("tut_npc_dialogue")) }
            for (enemy in listOf("faulted_loader", "faulted_loader", "the_iron_warden")) {
                withContext(Dispatchers.Main) {
                    val controller = CombatController(services.worldDataSource, services.combatEngine, services.statusRegistry,
                        services.sessionStore, services.inventoryService, services.itemRepository,
                        services.levelingManager, services.progressionData, services.audioRouter,
                        services.themeRepository, services.environmentThemeManager, services.encounterCoordinator,
                        listOf(enemy), tutorialsEnabled = false, parentScope = this)
                    val plays = mutableListOf<AudioCommand.Play>()
                    val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                        controller.fxEvents.collect { event ->
                            if (event is CombatFxEvent.Audio) plays.addAll(event.commands.filterIsInstance<AudioCommand.Play>())
                        }
                    }
                    try {
                        controller.onScreenReady()
                        controller.onScreenReady()
                        withTimeout(5000) { while (plays.none { it.type == AudioCueType.MUSIC }) delay(10) }
                        val track = plays.single { it.type == AudioCueType.MUSIC }
                        assertEquals(if (enemy == "the_iron_warden") "music_w1_boss_warden" else "music_w1_combat", track.cueId)
                        assertTrue(track.restartFromBeginning)
                    } finally { collector.cancel(); controller.close() }
                }
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
