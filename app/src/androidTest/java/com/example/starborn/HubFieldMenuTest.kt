package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.feature.hub.ui.*
import com.example.starborn.feature.hub.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class HubFieldMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullMenuAndQuestShortcutPreserveHubAndRoomProgressAtLargeText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: ExplorationViewModel
        lateinit var crafting: CraftingViewModel
        var hub by mutableStateOf(HubUiState())
        var scale by mutableStateOf(1f)
        var travelled = false
        compose.runOnUiThread { services = AppServices(IsolatedProgressionContext(context), true) }
        runBlocking { kotlinx.coroutines.delay(1000); services.userSettingsStore.setTutorialsEnabled(false) }
        compose.runOnUiThread {
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.clearDebugBootstrap()
            // Preserve the room to which Back from the hub would return, without entering it.
            services.sessionStore.setRoom("pit_L1_landing")
            vm = ExplorationViewModelFactory(services, hubMenu = true).create(ExplorationViewModel::class.java)
            crafting = CraftingViewModel(services.craftingService, services.inventoryService, services.sessionStore)
            val region = services.worldDataSource.loadHubs().first { it.id == services.sessionStore.state.value.hubId }
            val node = HubNodeUi("med_bay", "Med-Bay", "med_bay", .5f, .6f, 220f, true,
                description = "A place to recover and prepare for the road ahead.")
            hub = HubUiState(isLoading = false, hub = region, nodes = listOf(node), selectedNodeId = node.id,
                backgroundImage = region.backgroundImage, trackedQuest = HubQuestUi("w1_mq01", "Wake-Up Call", "Talk to Jed", null))
        }
        compose.waitUntil(15000) { !vm.uiState.value.isLoading }
        val before = services.sessionStore.state.value
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StarbornTheme { HubScreenContent(hub, SettingsUiState(), {}, { travelled = true }, {}, {}, {}, {}, {}, {}, {},
                    fieldMenuContent = { tab, quest, dismiss -> HubFieldMenu(vm, crafting, tab, quest, dismiss, {}, { false }, {}) }) }
            }
        }
        try {
            for (font in listOf(1f, 1.4f)) {
                compose.runOnIdle { scale = font }
                compose.onNodeWithContentDescription("Quick save").assertIsDisplayed()
                compose.onNodeWithText("Menu").assertIsDisplayed()
                compose.onNodeWithText("Enter Med-Bay").assertIsDisplayed()
                capture("hub-$font")
                compose.onNodeWithText("Menu").performClick()
                compose.waitUntil(5000) { vm.uiState.value.isMenuOverlayVisible }
                for (tab in listOf(MenuTab.STATS, MenuTab.INVENTORY, MenuTab.FIELD_KIT, MenuTab.JOURNAL, MenuTab.SETTINGS, MenuTab.MAP)) {
                    compose.runOnIdle { vm.selectMenuTab(tab) }
                    compose.waitForIdle()
                    capture("hub-$font-${tab.name}")
                }
                compose.onNodeWithText("Return to region map").performClick()
                compose.onNodeWithText("Enter Med-Bay").assertIsDisplayed()
                compose.onNodeWithText("Wake-Up Call").performClick()
                compose.waitUntil(5000) { vm.uiState.value.questDetail?.id == "w1_mq01" }
                capture("hub-$font-quest")
                androidx.test.espresso.Espresso.pressBack()
                compose.waitUntil(5000) { vm.uiState.value.questDetail == null }
                androidx.test.espresso.Espresso.pressBack()
                compose.waitUntil(5000) { !vm.uiState.value.isMenuOverlayVisible }
            }
            runBlocking {
                assertTrue(vm.saveGame(4))
                val saved = requireNotNull(services.slotState(4))
                assertEquals(before.roomId, saved.roomId)
                assertEquals(before.hubId, saved.hubId)
                assertEquals(before.completedEvents, saved.completedEvents)
            }
            compose.runOnIdle {
                val after = services.sessionStore.state.value
                assertFalse(travelled)
                assertEquals(before.roomId, after.roomId)
                assertEquals(before.hubId, after.hubId)
                assertEquals(before.roomStates, after.roomStates)
                assertEquals(before.completedEvents, after.completedEvents)
                assertEquals(before.completedMilestones, after.completedMilestones)
                assertNull(vm.uiState.value.currentRoom)
            }
        } finally { compose.runOnUiThread { crafting.runtime.close(); crafting.viewModelScope.cancel(); vm.viewModelScope.cancel(); services.release() } }
    }

    @Test fun navigationHostReleasesHubMenuBeforeEnteringExploration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        compose.runOnUiThread { services = AppServices(IsolatedProgressionContext(context), true) }
        runBlocking { kotlinx.coroutines.delay(1000); services.userSettingsStore.setTutorialsEnabled(false) }
        compose.runOnUiThread {
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.clearDebugBootstrap()
            services.sessionStore.setRoom(null)
        }
        compose.setContent { StarbornTheme {
            com.example.starborn.navigation.CampaignNavigationHost(providedServices = services,
                initialDestination = com.example.starborn.navigation.NavigationDestination.Hub.route)
        } }
        try {
            compose.onNodeWithText("Menu").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Items").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Items").onFirst().performClick()
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithText("Menu").assertIsDisplayed()
            assertNull(services.sessionStore.state.value.roomId)
            compose.onNode(hasText("Enter ", substring = true) and hasClickAction()).performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("exploration-menu-button").fetchSemanticsNodes().isNotEmpty() }
            assertNotNull(services.sessionStore.state.value.roomId)

        } finally { compose.runOnUiThread { services.release() } }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.getExternalFilesDir(null), "hub-menu-review").apply { mkdirs() }
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
}
