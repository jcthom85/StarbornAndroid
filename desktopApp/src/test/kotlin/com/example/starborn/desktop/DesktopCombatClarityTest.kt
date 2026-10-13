package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopCombatClarityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun liveFeedFollowsNewEntriesWhenTheRetainedHistoryIsFull() {
        val history = androidx.compose.runtime.mutableStateListOf<String>()
        repeat(60) { history.add("Log line $it") }
        compose.setContent { DesktopStarbornTheme {
            com.example.starborn.desktop.ui.DesktopCombatLiveFeed(history.toList(), onExpandLog = {})
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.runOnIdle { history.add("Latest combat message"); history.removeAt(0) }
        compose.waitForIdle()
        compose.onNodeWithText("Latest combat message").assertIsDisplayed()
    }

    @Test fun snackEffectsAreVisibleBeforeActivationAndExplainCooldownAfterward() {
        val directory = Files.createTempDirectory("starborn-combat-clarity-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            services.sessionStore.setPartyMembers(listOf("nova"))
            services.inventoryService.addItem("comet_gummies")
            services.sessionStore.setEquippedItem("nova:snack", "comet_gummies")
            compose.setContent { DesktopStarbornTheme(services) {
                DesktopCombatScreen(services, listOf("echo_borer"), onVictory = {}, onDefeat = {}, onFlee = {})
            } }
            compose.waitUntil(15000) { compose.onAllNodesWithTag("combat-snack-preview").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("combat-snack-preview").assertTextContains("Weapon crit chance +30%", substring = true)
            compose.onNodeWithTag("combat-snack-preview").assertTextContains("Spends one action", substring = true)
            val target = File("build/reports/combat-clarity/snack-preview.png").apply { requireNotNull(parentFile).mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", target)
            compose.onNodeWithTag("combat-action-snack").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Ready in 5 turns for this character.", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("combat-action-retreat").performScrollTo().assertIsDisplayed()
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png",
                File("build/reports/combat-clarity/snack-active-buffs.png"))
        } finally { services.close(); directory.deleteRecursively() }
    }
}
