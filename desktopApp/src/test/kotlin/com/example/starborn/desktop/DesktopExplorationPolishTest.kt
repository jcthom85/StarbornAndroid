package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DesktopExplorationPolishTest {
    @get:Rule val compose = createComposeRule()

    @Test fun correctedDebugLocationsExistAndBelongToAuthoredNodes() {
        val directory = Files.createTempDirectory("starborn-polish-locations").toFile()
        val services = DesktopAppServices(directory)
        try {
            val nodes = services.worldDataSource.loadHubNodes()
            desktopDebugRooms.forEach { (scenario, room) ->
                assertTrue("$scenario targets missing $room", room in services.roomDefinitions)
                assertTrue("$scenario has no authored node", scenario == "weather_lab" || nodes.any { it.entryRoom == room || room in it.rooms })
            }
            for ((scenario, room) in listOf("tut_journal_quests" to "trade_strip", "w3_safehouse_plan" to "spire_zekes_apartment", "w6_fractured_minds" to "source_campfire")) {
                assertTrue(services.startDebugScenario(scenario))
                assertEquals(room, services.sessionStore.state.value.roomId)
                val node = nodes.first { room == it.entryRoom || room in it.rooms }
                assertEquals(node.hubId, services.sessionStore.state.value.hubId)
            }
            val previous = services.sessionStore.state.value
            assertFalse(services.startDebugScenario("missing-scenario"))
            assertEquals(previous, services.sessionStore.state.value)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun compactDrawersStartClosedAndHaveExplicitClose() {
        val directory = Files.createTempDirectory("starborn-polish-drawers").toFile()
        val services = DesktopAppServices(directory)
        try {
            val room = services.worldDataSource.loadRooms().first { it.id == "pit_jed_bunk" }
            val ui = ExplorationUiState(currentRoom = room)
            compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
                Box(Modifier.size(900.dp, 650.dp)) {
                    val layout = portraitBackdropLayout(Size(900f, 650f), Size(1088f, 1920f))
                    DesktopExplorationPanels(services, ui, layout, room.description, null, false, false) {}
                }
            } } }
            compose.onNodeWithTag("exploration-narrative").assertDoesNotExist()
            compose.onNodeWithTag("exploration-status").assertDoesNotExist()
            compose.onNodeWithText("Room").performClick()
            compose.onNodeWithTag("exploration-narrative").assertExists()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithTag("exploration-narrative").assertDoesNotExist()
            compose.onNodeWithText("Status").performClick()
            compose.onNodeWithTag("exploration-status").assertExists()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithTag("exploration-status").assertDoesNotExist()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun escapeClosesDrawerAndTabDoesNotOpenMenu() {
        val directory = Files.createTempDirectory("starborn-polish-keyboard").toFile()
        val services = DesktopAppServices(directory)
        try {
            runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
            }
            assertTrue(services.startDebugScenario("tut_npc_dialogue"))
            compose.setContent { Box(Modifier.size(900.dp, 650.dp)) { DesktopStarbornTheme {
                DesktopExplorationScreen(services, {}, {}, {}, {}, {}, {})
            } } }
            compose.waitUntil(10000) { services.exploration.uiState.value.currentRoom?.id == "pit_jed_bunk" }
            compose.onNodeWithText("Room").performClick()
            compose.onNodeWithTag("exploration-narrative").assertExists()
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.onNodeWithTag("exploration-narrative").assertDoesNotExist()
            assertFalse(services.exploration.uiState.value.isMenuOverlayVisible)
            compose.onRoot().performKeyInput { pressKey(Key.Tab) }
            assertFalse(services.exploration.uiState.value.isMenuOverlayVisible)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun inspectionModalKeepsAuthoredTextAndEnabledChoices() {
        var selected = false
        compose.setContent { DesktopStarbornTheme {
            DesktopExplorationModal("An authored inspection message.", {}, "Original title") {
                Button(onClick = { selected = true }) { Text("Original choice") }
                Button(onClick = {}, enabled = false) { Text("Unavailable choice") }
            }
        } }
        compose.onNodeWithText("Original title").assertExists()
        compose.onNodeWithText("An authored inspection message.").assertExists()
        compose.onNodeWithText("Unavailable choice").assertIsNotEnabled()
        compose.onNodeWithText("Original choice").performClick()
        assertTrue(selected)
    }

    @Test fun darkMapKeepsItsFrameWithoutExposingTopology() {
        var opened = false
        compose.setContent { DesktopStarbornTheme {
            Box(Modifier.size(300.dp, 164.dp)) {
                DesktopExplorationMinimap(null, true, obscured = true) { opened = true }
            }
        } }
        compose.onNodeWithContentDescription("Minimap obscured by darkness").assertExists().assertHasNoClickAction()
        assertFalse(opened)
    }

    @Test fun keyLegendHudRendersAndTriggersQuickSaveAndControls() {
        val directory = Files.createTempDirectory("starborn-legend-hud-").toFile()
        val services = DesktopAppServices(directory)
        try {
            runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
            }
            assertTrue(services.startDebugScenario("tut_npc_dialogue"))
            compose.setContent {
                Box(Modifier.size(1920.dp, 1080.dp)) {
                    DesktopStarbornTheme {
                        DesktopExplorationScreen(services, {}, {}, {}, {}, {}, {})
                    }
                }
            }
            compose.waitUntil(10000) { services.exploration.uiState.value.currentRoom?.id == "pit_jed_bunk" }
            compose.waitForIdle()

            // Key badges are present and interactive
            compose.onNodeWithTag("key-legend-F5").assertIsDisplayed()
            compose.onNodeWithTag("key-legend-H").assertIsDisplayed()
            compose.onNodeWithTag("key-legend-Esc").assertIsDisplayed()
            compose.onNodeWithTag("key-legend-WASD").assertIsDisplayed()

            // Click Controls opens controls dialog
            compose.onNodeWithTag("key-legend-H").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("TACTICAL CONTROLS & KEYBINDINGS").assertIsDisplayed()

            // Close dialog via Close button
            compose.onNodeWithContentDescription("Close").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("TACTICAL CONTROLS & KEYBINDINGS").assertDoesNotExist()

            // Click Quick Save triggers quicksave
            compose.onNodeWithTag("key-legend-F5").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Saved!").assertExists()
        } finally { services.close(); directory.deleteRecursively() }
    }
}

