package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.io.File
import javax.imageio.ImageIO

class DesktopMenuSnapshotTest {
    @get:Rule val compose = createComposeRule()

    @Test @OptIn(ExperimentalTestApi::class) fun menuTabsRenderAndInventoryFilteringPreservesOwnedItems() {
        val directory = Files.createTempDirectory("starborn-menu-check-").toFile()
        val services = DesktopAppServices(directory)
        val narrow = mutableStateOf(false)
        try {
            assertTrue(services.startNewGame())
            services.inventoryService.addItem("medkit", 3)
            services.inventoryService.addItem("scrap_metal", 4)
            services.inventoryService.addItem("mining_pistol", 1)
            services.sessionStore.restore(services.sessionStore.state.value.copy(partyMemberHp = mapOf("nova" to 1),
                roomStates = services.sessionStore.state.value.roomStates + ("pit_L2_corridor" to mapOf("visited" to true, "discovered" to true))))
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(if (narrow.value) 800.dp else 1024.dp, if (narrow.value) 600.dp else 768.dp)) {
                    DesktopFieldMenuContent(services, currentRoomTitle = "Nova's bunk", onOpenFieldKit = {}, onReturnToTitle = {}, onDismiss = {})
                }
            } }
            compose.waitUntil(20_000) { !services.exploration.uiState.value.isLoading }
            compose.onRoot().performKeyInput { pressKey(Key.Four) }
            compose.onNodeWithText("Gold: current room", substring = true).assertExists()
            compose.onRoot().performKeyInput { pressKey(Key.One) }
            capture("menu-inventory")
            compose.onNodeWithText("Search cargo").performTextInput("medkit")
            assertTrue(compose.onAllNodesWithText(services.itemRepository.findItem("medkit")!!.name).fetchSemanticsNodes().isNotEmpty())
            assertEquals(3, services.sessionStore.state.value.inventory["medkit"])
            compose.onNodeWithText("Search cargo").performTextClearance()
            compose.onNodeWithText("USE ITEM").performClick()
            compose.onNodeWithText("Use on Nova").performClick()
            compose.waitUntil(5_000) { services.sessionStore.state.value.inventory["medkit"] == 2 }
            assertTrue((services.sessionStore.state.value.partyMemberHp["nova"] ?: 0) > 1)
            compose.onNodeWithText("Gear").performClick()
            compose.onNodeWithText("Search equipment").assertExists()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Mining Pistol · equip"))
            compose.onNodeWithText("Mining Pistol · equip").assertIsNotEnabled()
            compose.runOnIdle { services.sessionStore.restore(services.sessionStore.state.value.copy(unlockedWeapons = services.sessionStore.state.value.unlockedWeapons + "mining_pistol")) }
            compose.onNodeWithText("Mining Pistol · equip").assertIsEnabled()
            compose.onNodeWithText("Mining Pistol · equip").performClick()
            compose.waitUntil(5_000) { services.sessionStore.state.value.equippedWeapons["nova"] == "mining_pistol" }
            capture("menu-equipment")
            listOf("Journal", "Map", "Stats", "Settings").forEach { tab ->
                compose.onAllNodesWithText(tab).onFirst().performClick()
                compose.waitForIdle()
                capture("menu-" + tab.lowercase())
                if (tab == "Map") {
                    compose.waitUntil(5_000) { services.exploration.uiState.value.fullMap?.cells?.any { it.roomId == "pit_L2_corridor" && it.discovered } == true }
                    compose.onAllNodesWithText("Pod Row").onFirst().performClick()
                    assertEquals("pit_nova_bunk", services.sessionStore.state.value.roomId)
                    capture("menu-connected-map")
                }
                if (tab == "Journal") {
                    compose.onNodeWithText("Stop tracking").performScrollTo().performClick()
                    compose.waitUntil(5_000) { services.sessionStore.state.value.trackedQuestId == null }
                    compose.onNodeWithText("Track this quest").performScrollTo().performClick()
                    compose.waitUntil(5_000) { services.sessionStore.state.value.trackedQuestId != null }
                }
            }
            compose.onNodeWithText("Voice").assertExists()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Open save archive"))
            compose.onNodeWithText("Open save archive").assertExists()
            compose.onNodeWithText("Open save archive").performClick()
            compose.onAllNodesWithText("Save archive").onLast().assertExists()
            compose.onNodeWithText("Close").performClick()
            compose.runOnIdle { narrow.value = true }
            listOf("Inventory", "Journal", "Map", "Settings").forEach { tab ->
                compose.onAllNodesWithText(tab).onFirst().performClick()
                compose.waitForIdle()
                capture("narrow-" + tab.lowercase())
            }
        } finally { services.close(); directory.deleteRecursively() }
    }

    private fun capture(name: String) {
        val folder = File("build/reports/desktop/screenshots").apply { mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(folder, "$name.png"))
    }
}
