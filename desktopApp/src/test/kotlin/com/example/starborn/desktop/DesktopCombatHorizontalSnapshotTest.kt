package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopCombatScreen
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.File
import javax.imageio.ImageIO

class DesktopCombatHorizontalSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun inputRoot(): SemanticsNodeInteraction = composeTestRule.onAllNodes(isRoot())[0]

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun defeatExitCanBeAcknowledged() {
        val directory = java.nio.file.Files.createTempDirectory("starborn-defeat-check-").toFile()
        val services = DesktopAppServices(directory)
        var acknowledged = false
        try {
            services.startNewGame()
            services.sessionStore.restore(services.sessionStore.state.value.copy(partyMemberHp = mapOf("nova" to 1)))
            composeTestRule.setContent { com.example.starborn.desktop.ui.DesktopStarbornTheme {
                DesktopCombatScreen(services, listOf("the_iron_warden"), {}, { acknowledged = true }, {})
            } }
            repeat(6) {
                if (composeTestRule.onAllNodesWithText("Party defeated").fetchSemanticsNodes().isEmpty()) {
                    composeTestRule.waitUntil(15_000) {
                        composeTestRule.mainClock.advanceTimeBy(100)
                        composeTestRule.onAllNodesWithText("READY").fetchSemanticsNodes().isNotEmpty() || composeTestRule.onAllNodesWithText("Party defeated").fetchSemanticsNodes().isNotEmpty()
                    }
                    if (composeTestRule.onAllNodesWithText("Party defeated").fetchSemanticsNodes().isEmpty()) {
                        inputRoot().performKeyInput { pressKey(Key.Tab) }
                        composeTestRule.onNodeWithTag("combat-action-attack").assertIsEnabled()
                        inputRoot().performKeyInput { pressKey(Key.One) }
                        composeTestRule.onNodeWithText("Arrows select · Enter confirms · Esc cancels").assertExists()
                        inputRoot().performKeyInput { pressKey(Key.Enter) }
                        composeTestRule.mainClock.advanceTimeBy(1000)
                        Thread.sleep(450)
                    }
                }
            }
            composeTestRule.onNodeWithText("Party defeated").assertExists()
            composeTestRule.onNodeWithText("Continue").performClick()
            assertTrue(acknowledged)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun commandDialogsAndVictoryCanBeCompleted() {
        val directory = java.nio.file.Files.createTempDirectory("starborn-victory-check-").toFile()
        val services = DesktopAppServices(directory)
        var continued = false
        try {
            services.startNewGame()
            // Victory UI fixture: provide real unlocked gear rather than relying on
            // random unarmed damage to finish the encounter within eight commands.
            services.inventoryService.addItem("mining_pistol", 1)
            services.sessionStore.restore(services.sessionStore.state.value.copy(playerLevel = 99,
                partyMemberLevels = mapOf("nova" to 99), unlockedWeapons = setOf("mining_pistol"),
                equippedWeapons = mapOf("nova" to "mining_pistol")))
            services.inventoryService.addItem("medkit", 2)
            composeTestRule.setContent { com.example.starborn.desktop.ui.DesktopStarbornTheme {
                DesktopCombatScreen(services, listOf("echo_borer"), { continued = true }, {}, {})
            } }
            composeTestRule.waitUntil(10_000) {
                composeTestRule.mainClock.advanceTimeBy(100)
                composeTestRule.onAllNodesWithText("READY").fetchSemanticsNodes().isNotEmpty()
            }
            inputRoot().performKeyInput { pressKey(Key.Tab) }
            composeTestRule.onNodeWithTag("combat-action-attack").assertIsEnabled()
            inputRoot().performKeyInput { pressKey(Key.Two) }
            composeTestRule.onNodeWithText("Abilities").assertExists()
            composeTestRule.onNodeWithText("Back").performClick()
            inputRoot().performKeyInput { pressKey(Key.Three) }
            composeTestRule.onNodeWithText("Items").assertExists()
            composeTestRule.onNodeWithText("Back").performClick()
            repeat(8) {
                if (composeTestRule.onAllNodesWithText("Spoils Recovered").fetchSemanticsNodes().isEmpty()) {
                    composeTestRule.waitUntil(10_000) {
                        composeTestRule.mainClock.advanceTimeBy(100)
                        composeTestRule.onAllNodesWithText("READY").fetchSemanticsNodes().isNotEmpty() || composeTestRule.onAllNodesWithText("Spoils Recovered").fetchSemanticsNodes().isNotEmpty()
                    }
                    if (composeTestRule.onAllNodesWithText("Spoils Recovered").fetchSemanticsNodes().isEmpty()) {
                        inputRoot().performKeyInput { pressKey(Key.Tab) }
                        composeTestRule.onNodeWithTag("combat-action-attack").assertIsEnabled()
                        inputRoot().performKeyInput { pressKey(Key.One) }
                        inputRoot().performKeyInput { pressKey(Key.Enter) }
                        composeTestRule.mainClock.advanceTimeBy(1000)
                        Thread.sleep(450)
                    }
                }
            }
            composeTestRule.onNodeWithText("Spoils Recovered").assertExists()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.mainClock.advanceTimeBy(100)
                composeTestRule.onAllNodesWithText("Spoils Recovered").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.mainClock.advanceTimeBy(300)
            composeTestRule.onNodeWithText("Continue").performClick()
            assertTrue(continued)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun captureDesktopHorizontalCombat() {
        val narrow = mutableStateOf(false)
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_combat_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()
        val services = DesktopAppServices(saveDirectory = tempSaveDir)

        try {
            services.startNewGame()

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                com.example.starborn.desktop.ui.DesktopStarbornTheme { Box(Modifier.size(if (narrow.value) 800.dp else 1024.dp, if (narrow.value) 600.dp else 768.dp)) { DesktopCombatScreen(
                    services = services,
                    enemyIds = listOf("faulted_loader", "resonance_buoy", "echo_borer", "resonance_buoy"),
                    onVictory = {},
                    onDefeat = {},
                    onFlee = {}
                ) } }
            }
            composeTestRule.mainClock.advanceTimeBy(100)

            val artifactsDir = File("build/reports/desktop/screenshots")
            artifactsDir.mkdirs()

            val node = inputRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_horizontal_combat.png"))

            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.waitUntil(10_000) {
                composeTestRule.mainClock.advanceTimeBy(100)
                composeTestRule.onAllNodesWithText("READY").fetchSemanticsNodes().isNotEmpty()
            }
            inputRoot().performKeyInput { pressKey(Key.Tab) }
            composeTestRule.onNodeWithTag("combat-action-attack").assertIsEnabled()
            inputRoot().performKeyInput { pressKey(Key.One) }
            composeTestRule.onNodeWithText("Arrows select · Enter confirms · Esc cancels").assertExists()
            composeTestRule.runOnIdle { narrow.value = true }
            inputRoot().performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.DirectionDown); pressKey(Key.DirectionDown) }
            composeTestRule.mainClock.advanceTimeBy(800)
            composeTestRule.onNodeWithText("SELECTED TARGET").assertExists()
            ImageIO.write(inputRoot().captureToImage().asAwtImage(), "png", File(artifactsDir, "narrow-combat-target.png"))
            inputRoot().performKeyInput { pressKey(Key.Enter) }
            composeTestRule.onNodeWithText("Arrows select · Enter confirms · Esc cancels").assertDoesNotExist()

        } finally {
            services.close()
            tempSaveDir.deleteRecursively()
        }
    }
}
