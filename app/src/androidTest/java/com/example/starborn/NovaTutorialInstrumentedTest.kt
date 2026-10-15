package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.tutorial.NovaProgressionTutorial as Guide
import com.example.starborn.feature.exploration.ui.UIPromptOverlay
import com.example.starborn.feature.exploration.ui.components.SkillTreeContent
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class NovaTutorialInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun crewCheckpointSupportsEveryPhoneTreeAndRefundsOnlyItsOwner() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: ExplorationViewModel
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(base), isTestSession = true) }
        try {
            runBlocking { kotlinx.coroutines.delay(1000); services.userSettingsStore.setTutorialsEnabled(false) }
            instrumentation.runOnMainSync {
                assertTrue(services.startDebugScenario("crew_build_middle"))
                services.clearDebugBootstrap()
                vm = ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
            }
            compose.waitUntil(15000) { !vm.uiState.value.isLoading }
            compose.setContent { StarbornTheme {
                val ui by vm.uiState.collectAsState()
                ui.skillTreeOverlay?.let { tree -> CompositionLocalProvider(LocalModernFieldMenu provides true) {
                    SkillTreeContent(tree, FieldMenuDesign.cyan, FieldMenuDesign.border,
                        onUnlockSkill = vm::unlockSkillNode, onResetBuild = { vm.resetPrototypeBuild(tree.characterId) },
                        modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp, 32.dp))
                } }
            } }
            assertEquals(36, services.sessionStore.state.value.playerAp)
            val crew = com.example.starborn.domain.leveling.CrewBuildPrototype
            crew.trees.forEach { tree ->
                instrumentation.runOnMainSync { vm.openSkillTree(tree.character) }
                compose.waitUntil(5000) { vm.uiState.value.skillTreeOverlay?.characterId == tree.character }
                compose.onAllNodesWithText("Unlock (1 AP)").onFirst().performScrollTo().performClick()
                compose.waitUntil(5000) { vm.uiState.value.skillTreeOverlay?.respecRefund == 1 }
                assertEquals(8, vm.uiState.value.skillTreeOverlay!!.branches.sumOf { it.nodes.size })
            }
            assertEquals(32, services.sessionStore.state.value.playerAp)
            assertEquals(4, services.sessionStore.state.value.purchasedSkillNodes.size)
            compose.waitForIdle()
            screenshot(base, "crew-gh0st-tree")
            compose.onNodeWithText("Reset build (+1 AP)").performScrollTo().performClick()
            compose.waitUntil(5000) { services.sessionStore.state.value.purchasedSkillNodes.size == 3 }
            assertEquals(33, services.sessionStore.state.value.playerAp)
            runBlocking(kotlinx.coroutines.Dispatchers.Main) { services.saveSlot(3) }
            val saved = services.sessionStore.state.value
            runBlocking(kotlinx.coroutines.Dispatchers.Main) {
                assertTrue(services.sessionStore.resetPrototypeBuild())
                assertEquals(36, services.sessionStore.state.value.playerAp)
                assertTrue(services.loadSlot(3))
            }
            assertEquals(saved.purchasedSkillNodes, services.sessionStore.state.value.purchasedSkillNodes)
            assertEquals(saved.playerAp, services.sessionStore.state.value.playerAp)
        } finally {
            instrumentation.runOnMainSync { runCatching { vm.runtime.close() }; services.release() }
        }
    }

    @Test fun firstApGuideOpensThePhoneTreeAndPurchaseKeepsTheChoice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: ExplorationViewModel
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(base), isTestSession = true) }
        try {
            runBlocking { kotlinx.coroutines.delay(1000) }
            runBlocking { services.userSettingsStore.setTutorialsEnabled(true) }
            instrumentation.runOnMainSync {
                assertTrue(services.startDebugScenario(Nova.SCENARIO))
                services.clearDebugBootstrap()
                vm = ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
            }
            compose.waitUntil(15000) { !vm.uiState.value.isLoading }
            instrumentation.runOnMainSync {
                services.tutorialManager.cancelAllScheduled()
                services.promptManager.discardMatching { true }
                // Boundary fixture for the real quest-completion award; route earning has separate coverage.
                services.sessionStore.completeQuest("w1_mq01")
            }
            compose.setContent { StarbornTheme {
                val ui by vm.uiState.collectAsState()
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    UIPromptOverlay(ui.prompt, vm::dismissPrompt, Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp))
                    ui.skillTreeOverlay?.let { tree -> CompositionLocalProvider(LocalModernFieldMenu provides true) {
                        SkillTreeContent(tree, FieldMenuDesign.cyan, FieldMenuDesign.border,
                            onUnlockSkill = vm::unlockSkillNode,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 32.dp))
                    } }
                }
            } }
            compose.waitUntil(10000) {
                val prompt = services.promptManager.state.value.current
                if (prompt != null && prompt.id != Guide.FIRST_AP) {
                    instrumentation.runOnMainSync { services.promptManager.dismissCurrent() }
                }
                vm.uiState.value.prompt?.id == Guide.FIRST_AP
            }
            screenshot(base, "first-ap")
            compose.waitUntil(10000) { compose.onAllNodesWithText("View Nova's Skills", ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(2, services.sessionStore.state.value.playerAp)
            assertTrue(services.sessionStore.state.value.purchasedSkillNodes.isEmpty())
            screenshot(base, "first-ap")
            compose.onNodeWithText("View Nova's Skills", ignoreCase = true).performClick()
            compose.waitUntil(5000) { vm.uiState.value.skillTreeOverlay?.tutorialGuide != null }
            compose.onNodeWithText(Guide.TREE_MESSAGE).assertIsDisplayed()
            assertNull(services.promptManager.state.value.current)
            screenshot(base, "first-choice")
            assertTrue(vm.uiState.value.isMenuOverlayVisible)
            compose.onNodeWithText("Unlock (1 AP)").performScrollTo().performClick()
            compose.waitUntil(5000) { Nova.GROUNDED in services.sessionStore.state.value.purchasedSkillNodes }
            assertEquals(1, services.sessionStore.state.value.playerAp)
            assertTrue(Guide.FIRST_AP in services.sessionStore.state.value.tutorialCompleted)
            compose.waitUntil(5000) { vm.uiState.value.skillTreeOverlay?.tutorialGuide == null }
        } finally {
            instrumentation.runOnMainSync { runCatching { vm.runtime.close() }; services.release() }
        }
    }

    private fun screenshot(base: Context, name: String) {
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(base.getExternalFilesDir(null), "nova-tutorial-test").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
