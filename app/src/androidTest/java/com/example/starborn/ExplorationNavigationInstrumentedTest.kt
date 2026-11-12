package com.example.starborn

import android.content.Context
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.feature.exploration.ui.*
import com.example.starborn.feature.exploration.ui.hud.DirectionIndicatorsOverlay
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExplorationNavigationInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun hardwareMovementWorksWithWasdAndArrowsAfterClosingAnInspection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val services = compose.runOnUiThread { AppServices(IsolatedProgressionContext(context), isTestSession = true) }
        lateinit var vm: ExplorationViewModel
        try {
            runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
            }
            compose.runOnUiThread {
                assertTrue(services.startDebugScenario("tut_npc_dialogue"))
                services.clearDebugBootstrap()
                services.sessionStore.restore(services.sessionStore.state.value.copy(
                    worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_laundry_service",
                    activeQuests = emptySet(), completedQuests = setOf("w3_mq12"),
                    completedMilestones = setOf("ms_w3_mq12_complete"), questTasksCompleted = emptyMap()
                ))
                vm = ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
            }
            compose.waitUntil(15_000) { vm.uiState.value.currentRoom?.id == "spire_laundry_service" && !vm.uiState.value.isLoading }
            compose.runOnUiThread {
                services.tutorialManager.cancelAllScheduled()
                services.promptManager.discardMatching { true }
            }
            compose.mainClock.autoAdvance = false
            compose.setContent {
                StarbornTheme {
                    ExplorationScreen(vm, services.audioCuePlayer, services.uiEventBus, services.questPresentations, isTestSession = true)
                }
            }
            compose.mainClock.advanceTimeBy(2_000)
            compose.waitForIdle()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            fun key(code: Int) = instrumentation.sendKeyDownUpSync(code)
            fun move(code: Int, expected: String) {
                key(code)
                compose.waitUntil(5_000) { vm.uiState.value.currentRoom?.id == expected }
                compose.mainClock.advanceTimeBy(2_000)
                compose.waitForIdle()
            }
            compose.runOnUiThread { vm.showInspection("Read this before moving.") }
            compose.waitUntil(5_000) { vm.uiState.value.narrationPrompt?.message == "Read this before moving." }
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
            key(KeyEvent.KEYCODE_W)
            assertEquals("spire_laundry_service", vm.uiState.value.currentRoom?.id)
            key(KeyEvent.KEYCODE_ENTER)
            compose.waitUntil(5_000) { vm.uiState.value.narrationPrompt == null }
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
            move(KeyEvent.KEYCODE_W, "spire_staff_checkpoint")
            move(KeyEvent.KEYCODE_DPAD_DOWN, "spire_laundry_service")
            move(KeyEvent.KEYCODE_D, "spire_prism_gallery")
            move(KeyEvent.KEYCODE_DPAD_LEFT, "spire_laundry_service")
            move(KeyEvent.KEYCODE_S, "spire_uniform_sorting")
            move(KeyEvent.KEYCODE_DPAD_UP, "spire_laundry_service")
            move(KeyEvent.KEYCODE_A, "spire_laundry_scale_01")
            move(KeyEvent.KEYCODE_DPAD_RIGHT, "spire_laundry_service")
        } finally {
            compose.runOnUiThread { runCatching { vm.runtime.close() }; services.release() }
        }
    }

    @Test fun centeredMenuAndSouthExitHaveSeparateThumbTargetsAtBothPortraitHeights() {
        var height by mutableStateOf(720)
        var menu=0;var south=0
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(360.dp,height.dp).testTag("viewport")) {
                    ExplorationBottomControls(onOpenMenu={menu++},modifier=Modifier.align(Alignment.BottomCenter))
                    DirectionIndicatorsOverlay(mapOf("south" to DirectionIndicatorUi("south",DirectionIndicatorStatus.EXPLORED)),
                        {south++},Modifier.fillMaxSize().navigationBarsPadding().padding(12.dp),
                        ExplorationControlSpacing.southExitBottomClearance)
                }
            }
        } }
        listOf(640,720).forEach { h ->
            compose.runOnIdle {height=h}
            val viewport=compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
            val button=compose.onNodeWithTag("exploration-menu-button").fetchSemanticsNode().boundsInRoot
            val arrow=compose.onNodeWithContentDescription("Travel south").fetchSemanticsNode().boundsInRoot
            assertEquals(viewport.center.x,button.center.x,1f)
            assertTrue("South target must be above Menu",arrow.bottom<button.top)
            assertTrue(button.height>=48f && arrow.height>=48f && arrow.width>=48f)
            compose.onNodeWithTag("exploration-menu-button").performClick()
            compose.onNodeWithContentDescription("Travel south").performClick()
        }
        compose.runOnIdle {assertEquals(2,menu);assertEquals(2,south)}
    }

    @Test fun systemBackClosesInformationAndMenuBeforeLeavingAnEligibleRoom() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        lateinit var services:AppServices
        lateinit var vm:ExplorationViewModel
        var exits=0
        compose.runOnUiThread {services=AppServices(IsolatedProgressionContext(context),isTestSession=true)}
        try {
            runBlocking {kotlinx.coroutines.delay(1000);services.userSettingsStore.setTutorialsEnabled(false)}
            compose.runOnUiThread {
                assertTrue(services.startDebugScenario("tut_npc_dialogue"))
                services.clearDebugBootstrap()
                services.sessionStore.restore(services.sessionStore.state.value.copy(
                    worldId="world_1",hubId="hub_1_homestead",roomId="pit_L1_landing",
                    completedMilestones=setOf("ms_w1_mq01_jed_talked")))
                vm=ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
            }
            compose.waitUntil(15000) {vm.uiState.value.currentRoom?.id=="pit_L1_landing" && !vm.uiState.value.isLoading}
            compose.runOnUiThread {services.tutorialManager.cancelAllScheduled();services.promptManager.discardMatching {true}}
            compose.setContent {StarbornTheme {ExplorationScreen(vm,services.audioCuePlayer,services.uiEventBus,services.questPresentations,onReturnToHub={exits++},isTestSession=true)}}
            // Finish the entry title before testing Back at the room root.
            compose.waitUntil(10000) {vm.uiState.value.canReturnToHub}
            compose.mainClock.advanceTimeBy(1500)
            compose.onNodeWithTag("exploration-menu-button").assertIsDisplayed()
            compose.onAllNodesWithText("OVERWORLD MAP").assertCountEquals(1)
            compose.onAllNodesWithText("OVERWORLD",substring=false).assertCountEquals(0)
            compose.runOnUiThread {vm.openMenuOverlay()}
            compose.waitUntil(5000) {vm.uiState.value.isMenuOverlayVisible}
            compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()}
            compose.waitUntil(5000) {!vm.uiState.value.isMenuOverlayVisible}
            compose.runOnIdle {assertEquals(0,exits)}
            compose.runOnUiThread {services.promptManager.enqueue(TutorialPrompt(TutorialEntry("back-test","test","Read before leaving")))}
            compose.waitUntil(5000) {vm.uiState.value.prompt!=null}
            compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()}
            compose.waitUntil(5000) {vm.uiState.value.prompt==null}
            compose.runOnIdle {assertEquals(0,exits)}
            compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()}
            compose.waitUntil(5000) {exits==1}
        } finally {compose.runOnUiThread {runCatching {vm.runtime.close()};services.release()}}
    }
}
