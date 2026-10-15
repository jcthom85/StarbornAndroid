package com.example.starborn.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.session.AstraTravel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import com.example.starborn.domain.session.ArcadeCabinetProgress
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files

class DesktopAstraCatchLogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recreationLaunchesTheSelectedRestoredCabinetAndKeepsProgress() = runBlocking {
        val directory = Files.createTempDirectory("starborn-recreation").toFile()
        val services = DesktopAppServices(directory)
        try {
            check(services.startDebugScenario("tut_npc_dialogue"))
            val progress = mapOf("canopy_hopper" to ArcadeCabinetProgress(discovered = true, repaired = true, installed = true, highScore = 15000))
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                worldId = AstraTravel.WORLD_ID, hubId = AstraTravel.HUB_ID, roomId = "astra_simulation_deck",
                arcadeProgress = progress, completedMilestones = setOf("ms_arcade_canopy_hopper_install_seen")))
            val current = withTimeout(15000) { services.exploration.uiState.first { it.currentRoom?.id == "astra_simulation_deck" && it.actions.any { a -> a.name == "recreation console" } } }
            val console = current.actions.single { it.name == "recreation console" }
            compose.setContent { DesktopStarbornTheme {
                val state by services.exploration.uiState.collectAsState()
                DesktopRuntimeOverlays(services, services.exploration, state)
            } }
            compose.runOnIdle { services.exploration.onActionSelected(console) }
            compose.onNodeWithText("Canopy Hopper").assertIsDisplayed()
            compose.onNodeWithText("Deep Mine Asteroid Drill").assertDoesNotExist()
            compose.runOnIdle { services.exploration.openRecreationArcade("deep_mine_asteroid_drill") }
            assertTrue(services.exploration.uiState.value.isAstraRecreationVisible)
            val launched = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5000) { services.exploration.events.first { it is ExplorationEvent.OpenArcade } }
            }
            compose.onNodeWithText("Canopy Hopper").performClick()
            assertEquals("canopy_hopper", (launched.await() as ExplorationEvent.OpenArcade).cabinetId)
            assertEquals(progress, services.sessionStore.state.value.arcadeProgress)
            assertFalse(services.exploration.uiState.value.isAstraRecreationVisible)
            compose.runOnIdle { services.exploration.onActionSelected(console) }
            compose.onNodeWithText("Great Frontier films").performClick()
            compose.waitForIdle()
            assertTrue(services.exploration.uiState.value.isTapeDeckVisible)
            assertFalse(services.exploration.uiState.value.isAstraRecreationVisible)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun cargoTerminalOpensCurrentRecordsAndCanReopen() = runBlocking {
        val directory = Files.createTempDirectory("starborn-catch-log").toFile()
        val services = DesktopAppServices(directory)
        try {
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                worldId = AstraTravel.WORLD_ID, hubId = AstraTravel.HUB_ID,
                roomId = AstraTravel.ENTRY_ROOM_ID))
            val ui = withTimeout(15000) { services.exploration.uiState.first { it.actions.any { a -> a.name == "cargo records" } } }
            val terminal = ui.actions.single { it.name == "cargo records" }
            assertTrue(ui.currentRoom!!.description.contains("records terminal"))
            compose.setContent { DesktopStarbornTheme {
                val state by services.exploration.uiState.collectAsState()
                DesktopRuntimeOverlays(services, services.exploration, state)
            } }
            repeat(2) {
                compose.runOnIdle { services.exploration.onActionSelected(terminal) }
                compose.onNodeWithText("Cargo Manifest").assertIsDisplayed()
                compose.onNodeWithText("Catch Log").performClick()
                compose.onNodeWithText("Astra Catch Log").assertIsDisplayed()
                assertEquals(services.fishingService.getJournal(), services.exploration.uiState.value.fishingJournal)
                compose.onNodeWithText("Relics").performClick()
                compose.onNodeWithText("Relic Array").assertIsDisplayed()
                compose.onNodeWithText("Supplies").performClick()
                compose.onNodeWithText("Cargo Manifest").assertIsDisplayed()
                compose.onNodeWithText("Close").performClick()
                compose.onNodeWithText("Astra Catch Log").assertDoesNotExist()
            }
            val before = services.sessionStore.state.value.pendingBattleJson
            compose.runOnIdle { services.exploration.launchSimulationCombat(listOf("ascended_god")) }
            assertEquals(before, services.sessionStore.state.value.pendingBattleJson)
            for ((room, action, title) in listOf(
                Triple("astra_cargo_bay", "cargo records", "Cargo Manifest"),
                Triple("astra_common_room", "central table", "Crew Check-in"),
                Triple("astra_quarters", "crew downtime", "Crew Downtime"),
                Triple("astra_bridge", "Chime", "Chime Archive"))) {
                services.sessionStore.setRoom(room)
                val current = withTimeout(15000) { services.exploration.uiState.first { it.currentRoom?.id == room && it.actions.any { a -> a.name == action } } }
                compose.runOnIdle { services.exploration.onActionSelected(current.actions.single { it.name == action }) }
                compose.onNodeWithText(title).assertIsDisplayed()
                compose.onNodeWithText("Close").performClick()
                assertNull(services.exploration.uiState.value.astraFacility)
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
