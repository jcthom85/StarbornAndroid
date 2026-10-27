package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DesktopHubMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hubMenuPreservesLocationSelectionAndQuestProgress() {
        val directory = Files.createTempDirectory("starborn-hub-menu").toFile()
        val services = DesktopAppServices(directory)
        try {
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.setRoom(null)
            val before = services.sessionStore.state.value
            var travelled = false
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(900.dp,650.dp)) {
                DesktopHubScreen(services, { travelled = true }, {}, {})
            } } }
            compose.waitUntil(15000) { !services.exploration.uiState.value.isLoading &&
                compose.onAllNodesWithTag("hub-node-med_bay").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("hub-node-med_bay").performClick()
            compose.onAllNodesWithText("Menu").onFirst().performClick()
            compose.waitUntil(5000) { services.exploration.uiState.value.isMenuOverlayVisible }
            for (tab in listOf("Inventory", "Journal", "Party", "Settings", "Map")) {
                compose.onAllNodesWithText(tab).onFirst().performClick()
                compose.waitForIdle()
            }
            compose.onNodeWithText("Return to region map").performClick()
            compose.onNodeWithTag("hub-node-med_bay").assert(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Selected"))
            compose.onAllNodesWithText("Med-Bay").onLast().assertExists()
            compose.runOnIdle {
                val after = services.sessionStore.state.value
                assertFalse(travelled)
                assertNull(after.roomId)
                assertEquals(before.hubId, after.hubId)
                assertEquals(before.completedEvents, after.completedEvents)
                assertEquals(before.completedMilestones, after.completedMilestones)
                assertEquals(before.roomStates, after.roomStates)
                assertFalse(services.exploration.uiState.value.isMenuOverlayVisible)
            }
            // The hub must let the field menu consume text and its own Back behavior.
            compose.onAllNodesWithText("Menu").onFirst().performClick()
            compose.onAllNodesWithText("Inventory").onFirst().performClick()
            compose.onNodeWithText("Search items").performTextInput("iron")
            compose.onNodeWithText("Search items").assertExists()
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.waitUntil(5000) { !services.exploration.uiState.value.isMenuOverlayVisible }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
