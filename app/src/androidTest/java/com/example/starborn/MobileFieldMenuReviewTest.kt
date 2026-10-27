package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.crafting.*
import com.example.starborn.feature.exploration.ui.ExplorationScreen
import com.example.starborn.feature.exploration.ui.menu.*
import com.example.starborn.feature.exploration.ui.tabs.*
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import com.example.starborn.feature.mainmenu.BurgQuestDemo
import kotlinx.coroutines.cancel
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import java.io.File

/** Disposable phone renders, including enlarged text and the guided repair result. */
class MobileFieldMenuReviewTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.getExternalFilesDir(null), "mobile-menu-review").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun menuPagesAtNormalAndLargeText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: ExplorationViewModel
        lateinit var crafting: CraftingViewModel
        var scale by mutableStateOf(1f)
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(context), true) }
        SystemClock.sleep(1000)
        instrumentation.runOnMainSync {
            assertTrue(services.startDebugScenario("campaign_w1_mq02"))
            services.tutorialManager.cancelAllScheduled()
            services.promptManager.dismissCurrent()
            vm = ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
            crafting = CraftingViewModel(services.craftingService, services.inventoryService, services.sessionStore)
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StarbornTheme { ExplorationScreen(vm, services.audioCuePlayer, services.uiEventBus, services.questPresentations, craftingViewModel = crafting) }
            }
        }
        try {
            SystemClock.sleep(1500)
            for (font in listOf(1f, 1.3f)) {
                compose.runOnIdle { scale = font; vm.openMenuOverlay(MenuTab.STATS) }
                for (tab in listOf(MenuTab.STATS, MenuTab.INVENTORY, MenuTab.FIELD_KIT, MenuTab.JOURNAL, MenuTab.MAP, MenuTab.SETTINGS)) {
                    compose.runOnIdle { vm.selectMenuTab(tab) }
                    SystemClock.sleep(250)
                    capture("${font}-${tab.name}")
                }
            }
            compose.runOnIdle {
                services.sessionStore.setPartyMembers(BurgQuestDemo.party.take(2))
                vm.selectMenuTab(MenuTab.STATS)
            }
            compose.onNodeWithText("Skills").performScrollTo().performClick()
            compose.onNodeWithText("Zeke").performClick()
            compose.waitUntil(5000) { vm.uiState.value.skillTreeOverlay?.characterId == "zeke" }
            capture("1.3-skills-zeke")
            compose.onNodeWithText("Back to Party").performClick()
            compose.onNodeWithText("Attributes").performScrollTo().performClick()
            compose.onNodeWithText("Nova").performClick()
            compose.waitUntil(5000) { vm.uiState.value.partyMemberDetails?.id == "nova" }
            assertEquals("nova", vm.uiState.value.partyMemberDetails?.id)
            capture("1.3-attributes-nova")
            compose.runOnIdle {
                vm.closeMenuOverlay()
                services.sessionStore.restore(services.sessionStore.state.value.copy(
                    worldId = "world_astra", hubId = "hub_astra", roomId = "astra_cargo_bay"))
            }
            compose.waitUntil(10000) { vm.uiState.value.actions.any { it.name == "cargo records" } }
            compose.runOnIdle { vm.runtime.onActionSelected(vm.uiState.value.actions.single { it.name == "cargo records" }) }
            compose.onNodeWithText("Cargo Manifest").assertIsDisplayed()
            compose.onNodeWithText("Catch Log").performClick()
            compose.onNodeWithText("Astra Catch Log").assertIsDisplayed()
            capture("1.3-astra-catch-log")
            compose.onNodeWithText("Relics").performClick()
            compose.onNodeWithText("Relic Array").assertIsDisplayed()
            capture("1.3-astra-relic-array")
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithText("Astra Catch Log").assertDoesNotExist()
            for ((room, action, title) in listOf(
                Triple("astra_cargo_bay", "cargo records", "Cargo Manifest"),
                Triple("astra_common_room", "central table", "Crew Check-in"),
                Triple("astra_quarters", "crew downtime", "Crew Downtime"),
                Triple("astra_bridge", "Chime", "Chime Archive"))) {
                compose.runOnIdle { services.sessionStore.setRoom(room) }
                compose.waitUntil(10000) { vm.uiState.value.currentRoom?.id == room && vm.uiState.value.actions.any { it.name == action } }
                compose.runOnIdle { vm.runtime.onActionSelected(vm.uiState.value.actions.single { it.name == action }) }
                compose.onNodeWithText(title).assertIsDisplayed()
                capture("1.3-astra-${action.replace(' ', '-')}")
                compose.onNodeWithText("Close").performClick()
                compose.onNodeWithText(title).assertDoesNotExist()
            }
            compose.runOnIdle { services.sessionStore.setRoom("astra_simulation_deck") }
            compose.waitUntil(10000) { vm.uiState.value.actions.any { it.name == "recreation console" } }
            compose.runOnIdle { vm.runtime.onActionSelected(vm.uiState.value.actions.single { it.name == "recreation console" }) }
            compose.onNodeWithText("Recreation").assertIsDisplayed()
            compose.onNodeWithText("Great Frontier films").assertIsDisplayed()
            capture("1.3-astra-recreation")
            compose.onNodeWithText("Great Frontier films").performClick()
            compose.waitUntil(5000) { vm.uiState.value.isTapeDeckVisible }
            assertTrue(!vm.uiState.value.isAstraRecreationVisible)
        } finally {
            instrumentation.runOnMainSync { crafting.runtime.close(); crafting.viewModelScope.cancel(); vm.viewModelScope.cancel(); services.release() }
        }
    }

    @Test fun repairGuideAndResultRemainReachableWithLargeText() {
        var step by mutableStateOf(TinkeringTutorialStep.SLOT_BASE)
        var scale by mutableStateOf(1f)
        val accent = Color(0xFF75DCE8)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StarbornTheme {
                    ModernFieldMenu(MenuTab.FIELD_KIT, null, PartyStatusUi(), null, "125 credits",
                        rememberScrollState(), true, false, accent,
                        onSelectTab = {}, onSelectMember = {}, onBack = {}, onSave = {}, onClose = {}) {
                        val ready = step == TinkeringTutorialStep.SYNTHESIZE
                        ModernTinkerWorkbench(
                            bench = TinkeringBenchState(
                                mainItemId = if (step != TinkeringTutorialStep.SLOT_BASE) "cryo_inductor" else null,
                                mainItemName = if (step != TinkeringTutorialStep.SLOT_BASE) "Cryo-Inductor" else null,
                                componentNames = if (ready) listOf("Scrap Metal") else emptyList(),
                                preview = if (ready) TinkeringPreview("repair", "Functional Cryo-Inductor", null, "repair", null,
                                    "functional_cryo_inductor", true, "Restores the cold loop and unlocks Cryo Vent.") else null,
                                canCraftSelection = ready),
                            inventory = listOf(TinkeringItemChoice("cryo_inductor", "Cryo-Inductor", null, 1),
                                TinkeringItemChoice("scrap_metal", "Scrap Metal", null, 4)),
                            recipes = emptyList(), baseItemIds = setOf("cryo_inductor"),
                            componentOptions = mapOf("cryo_inductor" to setOf("scrap_metal")),
                            activeSlot = if (step == TinkeringTutorialStep.SLOT_BASE) ActiveBenchSlot.BASE else ActiveBenchSlot.COMPONENT_1,
                            accent = accent, tutorialStep = step, onSelectSlot = {}, onClearSlot = {},
                            onClear = {}, onItem = {}, onCraft = {}, onLoad = {})
                    }
                }
            }
        }
        for (font in listOf(1f, 1.3f)) {
            compose.runOnIdle { scale = font }
            for (current in listOf(TinkeringTutorialStep.SLOT_BASE, TinkeringTutorialStep.SLOT_COMPONENT, TinkeringTutorialStep.SYNTHESIZE)) {
                compose.runOnIdle { step = current }
                compose.onAllNodesWithText("Tutorial").assertCountEquals(1)
                compose.onNodeWithText("Component 2").assertDoesNotExist()
                if (current == TinkeringTutorialStep.SYNTHESIZE) {
                    compose.onNodeWithText("Functional Cryo-Inductor").assertIsDisplayed()
                    compose.onNodeWithText("Repair", useUnmergedTree = true).performScrollTo().assertIsDisplayed().assertIsEnabled()
                }
                capture("${font}-repair-${current.name}")
            }
        }
    }
}
