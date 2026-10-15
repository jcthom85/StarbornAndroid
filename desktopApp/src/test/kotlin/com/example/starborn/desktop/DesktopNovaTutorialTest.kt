package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.tutorial.NovaProgressionTutorial as Guide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopNovaTutorialTest {
    @get:Rule val compose = createComposeRule()
    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-nova-tutorial-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame(novaPrototype = true)
            runBlocking { withTimeout(10000) { services.exploration.uiState.first { !it.isLoading } } }
            services.tutorialManager.cancelAllScheduled()
            services.promptManager.discardMatching { true }
            runBlocking { services.userSettingsStore.setTutorialsEnabled(true) }
            block(services)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun firstEarnedApOpensChoiceGuideAndPurchaseClearsItWithoutSpendingAutomatically() = withServices { services ->
        assertFalse(services.tutorialManager.hasSeen(Guide.FIRST_AP))
        services.sessionStore.completeQuest("w1_mq01")
        compose.waitUntil(10000) { services.promptManager.state.value.current?.id == Guide.FIRST_AP }
        services.sessionStore.completeQuest("w1_mq01")
        assertEquals(2, services.sessionStore.state.value.playerAp)
        assertTrue(services.sessionStore.state.value.purchasedSkillNodes.isEmpty())
        assertFalse(services.promptManager.state.value.queue.any { it.id == Guide.FIRST_AP })
        compose.setContent { DesktopStarbornTheme(services) {
            val prompt by services.promptManager.state.collectAsState()
            val ui by services.exploration.uiState.collectAsState()
            Box(Modifier.size(1280.dp, 800.dp)) {
                ui.skillTreeOverlay?.let { DesktopFieldMenuTheme(services) { DesktopMenuSkillsContent(services, it) } }
                prompt.current?.let { DesktopExplorationPromptBanner(services, it, services.exploration::dismissPrompt) }
            }
        } }
        compose.onNodeWithText("Shape Nova's build").assertIsDisplayed()
        screenshot("first-ap")
        compose.onNodeWithText("View Nova's Skills").performClick()
        compose.waitUntil(5000) { services.exploration.uiState.value.skillTreeOverlay?.tutorialGuide != null }
        compose.onNodeWithText(Guide.TREE_MESSAGE).assertIsDisplayed()
        assertNull(services.promptManager.state.value.current)
        screenshot("first-choice")
        assertEquals(2, services.sessionStore.state.value.playerAp)
        compose.onNodeWithText("Unlock (1 AP)").performClick()
        compose.waitUntil(5000) { services.exploration.uiState.value.skillTreeOverlay?.tutorialGuide == null }
        assertEquals(mapOf(Nova.GROUNDED to 1), services.sessionStore.state.value.purchasedSkillNodes)
        assertTrue(Guide.FIRST_AP in services.sessionStore.state.value.tutorialCompleted)
        assertNull(Guide.firstAp(services.sessionStore.state.value, true))
        runBlocking { services.saveManager.saveGame(1, services.sessionStore.state.value) }
        services.sessionStore.restore(services.sessionStore.state.value.copy(tutorialCompleted = emptySet()))
        assertTrue(services.loadSlot(1))
        assertTrue(Guide.FIRST_AP in services.sessionStore.state.value.tutorialCompleted)
    }

    @Test fun disablingTutorialsDiscardsPendingGuideAndReenablingOffersIt() = withServices { services ->
        services.sessionStore.completeQuest("w1_mq01")
        compose.waitUntil(10000) { services.promptManager.state.value.current?.id == Guide.FIRST_AP }
        runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
        compose.waitUntil(5000) { services.promptManager.state.value.current?.id != Guide.FIRST_AP }
        assertFalse(Guide.FIRST_AP in services.sessionStore.state.value.tutorialCompleted)
        assertNull(services.exploration.uiState.value.skillTreeOverlay)
        runBlocking { services.userSettingsStore.setTutorialsEnabled(true) }
        compose.waitUntil(5000) { services.promptManager.state.value.current?.id == Guide.FIRST_AP }
        assertTrue(services.promptManager.state.value.current is TutorialPrompt)
    }

    private fun screenshot(name: String) {
        val target = File("build/reports/progression-prototype/tutorial-$name.png").apply { requireNotNull(parentFile).mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", target)
    }

    @Test fun perkCuePausesActualCombatUntilAcknowledgedAndPersistsThroughSaveLoad() = withServices { services ->
        services.sessionStore.completeQuest("w1_mq01")
        assertTrue(services.sessionStore.purchasePrototypeNode(Nova.STEPS))
        val combat = com.example.starborn.feature.combat.viewmodel.CombatController(services.worldDataSource,
            services.combatEngine, services.statusRegistry, services.sessionStore, services.inventoryService,
            services.itemRepository, services.levelingManager, services.progressionData, services.audioRouter,
            services.themeRepository, services.environmentThemeManager, services.encounterCoordinator,
            listOf("the_iron_warden"), tutorialsEnabled = true)
        try {
            runBlocking { withTimeout(15000) { combat.atbMeters.first { (it["nova"] ?: 0f) >= .999f } } }
            combat.selectReadyPlayer("nova")
            combat.playerAttack(combat.enemyCombatantIds.first())
            runBlocking { withTimeout(5000) { combat.buildHint.first { it?.key == Guide.STEPS } } }
            runBlocking { delay(100) }
            val meters = combat.atbMeters.value
            val logSize = combat.combatState!!.log.size
            runBlocking { delay(1500) }
            assertEquals(meters, combat.atbMeters.value)
            assertEquals(logSize, combat.combatState!!.log.size)
            // The real renderer releases these animation pauses when its lunge finishes.
            combat.onLungeFinished(combat.lungeToken.value)
            combat.onMissLungeFinished(combat.missLungeToken.value)
            runBlocking { delay(200) }
            assertEquals(meters, combat.atbMeters.value)
            assertFalse(Guide.STEPS in services.sessionStore.state.value.tutorialCompleted)
            combat.dismissBuildHint()
            assertNull(combat.buildHint.value)
            assertTrue(Guide.STEPS in services.sessionStore.state.value.tutorialCompleted)
            runBlocking { withTimeout(5000) { combat.atbMeters.first { it != meters } } }
        } finally { combat.close() }
        runBlocking { services.saveManager.saveGame(1, services.sessionStore.state.value) }
        services.sessionStore.restore(services.sessionStore.state.value.copy(tutorialCompleted = emptySet()))
        assertTrue(services.loadSlot(1))
        assertTrue(Guide.STEPS in services.sessionStore.state.value.tutorialCompleted)
    }
}
