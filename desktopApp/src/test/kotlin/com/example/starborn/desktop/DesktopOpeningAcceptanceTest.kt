package com.example.starborn.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.io.File
import javax.imageio.ImageIO

class DesktopOpeningAcceptanceTest {
    @get:Rule val compose = createComposeRule()

    @Test @OptIn(ExperimentalTestApi::class) fun openingActionsDriveAuthoredProgressionAndPersistInventory() {
        val directory = Files.createTempDirectory("starborn-opening-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            assertTrue(services.sessionStore.state.value.inventory.isEmpty())
            assertFalse(services.sessionStore.state.value.equippedWeapons.containsKey("nova"))
            compose.setContent { com.example.starborn.desktop.ui.DesktopStarbornTheme { DesktopExplorationScreen(services, {}, {}, {}, {}, {}, {}) } }
            compose.waitUntil(20_000) { !services.exploration.uiState.value.isLoading }
            clearPresentations(services)
            compose.onNodeWithContentDescription("bunk light", useUnmergedTree = true).performClick()
            compose.waitUntil(5_000) { services.sessionStore.state.value.roomStates["pit_nova_bunk"]?.get("light_on") == true }
            clearPresentations(services)
            compose.onAllNodesWithContentDescription("door control panel", useUnmergedTree = true)[0].performClick()
            compose.waitUntil(5_000) { services.sessionStore.state.value.roomStates["pit_nova_bunk"]?.get("conduit_isolated") == true }
            clearPresentations(services)
            compose.onNodeWithTag("travel-west").performClick()
            val debugFolder = File("build/reports/desktop/screenshots").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(debugFolder, "opening-exit-check.png"))
            compose.waitUntil(5_000) {
                compose.mainClock.advanceTimeBy(250)
                services.sessionStore.state.value.roomId == "pit_L2_corridor"
            }
            assertTrue("Authored quest progress must be recorded", services.sessionStore.state.value.completedMilestones.contains("ms_w1_mq01_safety_fault_inspected"))
            clearPresentations(services)
            services.inventoryService.addItem("medkit", 3)
            services.inventoryService.removeItem("medkit", 1)
            compose.onRoot().performKeyInput { pressKey(Key.F5) }
            compose.waitUntil(5_000) { services.saveManager.hasSave(-1) }
            assertEquals(2, services.saveManager.loadGame(-1)?.inventory?.get("medkit"))
            assertTrue(services.saveManager.saveGame(1, services.sessionStore.state.value))
            assertEquals(2, services.saveManager.loadGame(1)?.inventory?.get("medkit"))
            val screenshots = File("build/reports/desktop/screenshots").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(screenshots, "opening-pod-row.png"))
        } finally { services.close(); directory.deleteRecursively() }
    }

    private fun clearPresentations(services: DesktopAppServices) {
        repeat(60) {
            compose.runOnIdle {
                val runtime = services.exploration
                val state = runtime.uiState.value
                when {
                    state.cinematic != null -> runtime.advanceCinematic()
                    state.eventAnnouncement != null -> runtime.dismissEventAnnouncement()
                    state.narrationPrompt != null -> runtime.dismissNarration()
                    state.prompt != null -> runtime.dismissPrompt()
                }
            }
            compose.waitForIdle()
            val state = services.exploration.uiState.value
            if (state.cinematic == null && state.eventAnnouncement == null && state.narrationPrompt == null && state.prompt == null && !state.forceBlackScreen) return
            compose.mainClock.advanceTimeBy(250)
            Thread.sleep(50)
        }
        val state = services.exploration.uiState.value
        error("Opening presentations did not finish: cinematic=${state.cinematic?.sceneId}, fade=${state.fadeOverlay}, black=${state.forceBlackScreen}, narration=${state.narrationPrompt}, prompt=${state.prompt?.id}")
    }
}
