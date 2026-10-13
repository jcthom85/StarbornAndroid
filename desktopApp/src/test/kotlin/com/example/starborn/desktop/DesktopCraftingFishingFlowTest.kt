package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.fishing.FishingFight
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.domain.fishing.FishingResult
import com.example.starborn.feature.fishing.viewmodel.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.domain.inventory.ItemUseController
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopCraftingFishingFlowTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, dialog: Boolean = false) {
        val directory = File("build/reports/system-previews").apply { mkdirs() }
        ImageIO.write((if (dialog) compose.onAllNodes(isRoot()).onLast() else compose.onRoot()).captureToImage().asAwtImage(), "png", File(directory, "$name.png"))
    }

    @Test fun assignedModMoveRequiresConfirmationAndCancelKeepsTheSource() {
        val directory = Files.createTempDirectory("starborn-mod-transfer-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            services.sessionStore.setPartyMembers(listOf("nova", "zeke"))
            services.sessionStore.setMilestone("ms_w1_mq01_complete")
            services.sessionStore.setEquippedWeapon("nova", "nova_laser_blaster")
            services.sessionStore.setEquippedWeapon("zeke", "zeke_shock_fists")
            services.inventoryService.addItem("power_lens_mk_i")
            assertEquals(com.example.starborn.domain.inventory.ModEquipResult.Success,
                services.exploration.requestEquipInventoryMod("weapon_mod1", "power_lens_mk_i", "nova"))
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(960.dp, 720.dp)) { DesktopRuntimeGearContent(services, "zeke") }
            } }
            compose.onNodeWithText("Weapon modification 1").performScrollTo().performClick()
            compose.onNodeWithText("Equip").performClick()
            compose.onNodeWithText("Move Power Lens Mk. I?").assertExists()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals("power_lens_mk_i", services.sessionStore.state.value.equippedItems["nova:weapon_mod1"])
            assertNull(services.sessionStore.state.value.equippedItems["zeke:weapon_mod1"])
            compose.onNodeWithText("Equip").performClick()
            capture("mod-transfer-confirmation", dialog = true)
            compose.onNodeWithTag("mod-transfer-confirm").performClick()
            compose.waitUntil(5000) { services.sessionStore.state.value.equippedItems["zeke:weapon_mod1"] == "power_lens_mk_i" }
            assertNull(services.sessionStore.state.value.equippedItems["nova:weapon_mod1"])
            assertEquals(1, services.inventoryService.snapshot()["power_lens_mk_i"])
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun desktopFishingPersistsRecordsAndGrantsCraftingUsesAndRewards() {
        val directory = Files.createTempDirectory("starborn-fishing-records-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            val result = services.fishingService.secureCatch(FishingResult("raw_glowfish", 1, "Caught",
                zoneId = "sector9_stream", cleanCatch = true))
            assertTrue(result.uses.isNotEmpty())
            assertTrue(result.rewards.isNotEmpty())
            assertTrue("ms_fishing_first_clean" in services.sessionStore.state.value.completedMilestones)
            assertEquals(1, services.inventoryService.snapshot()["shiny_lure"])
            val saved = services.sessionStore.state.value
            services.sessionStore.restore(saved)
            assertTrue(services.fishingService.getJournal().first { it.zoneId == "sector9_stream" }
                .species.first { it.itemId == "raw_glowfish" }.clean)
            services.fishingService.secureCatch(result)
            assertEquals(1, services.inventoryService.snapshot()["shiny_lure"])
            assertEquals(1, services.inventoryService.snapshot()["raw_glowfish"])
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun cookingReplacementCancelPreservesIngredients() {
        val directory = Files.createTempDirectory("starborn-cook-eat-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            val use = ItemUseController(services.inventoryService, services.craftingService, services.sessionStore,
                charactersProvider = { services.characterDefinitions })
            services.inventoryService.addItem("glowfish_broth")
            runBlocking { use.useItem("glowfish_broth") }
            val recipe = services.craftingService.cookingRecipes.single { it.id == "provision_spiced_ration_cache" }
            recipe.ingredients.forEach { (id, amount) -> services.inventoryService.addItem(id, amount * 2) }
            val before = services.inventoryService.snapshot()
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(560.dp, 650.dp)) {
                    DesktopCookingScreen(services.craftingService, services.inventoryService, "Cooking", {},
                        onCookAndEat = use::cookAndEat, highContrastMode = true, largeTouchTargets = true)
                }
            } }
            compose.onNodeWithText("Find a recipe").performScrollTo().performTextInput(recipe.name)
            compose.onAllNodesWithText("Cook and eat one portion").onFirst().performScrollTo().performClick()
            compose.onNodeWithText("Replace your active meal?").assertExists()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(before, services.inventoryService.snapshot())
            capture("cooking-compact")
            compose.onAllNodesWithText("Cook and eat one portion").onFirst().performScrollTo().performClick()
            compose.onNodeWithText("Replace meal").performClick()
            val ingredient = recipe.ingredients.entries.first()
            compose.waitUntil(5000) { services.inventoryService.snapshot()[ingredient.key] == ingredient.value }
            assertTrue(services.sessionStore.state.value.activeMealBuff != null)
            compose.onAllNodesWithText("Cook and eat one portion").onFirst().assertIsEnabled()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun scrapCancelPreservesInventoryAndConfirmConsumesOne() {
        val directory = Files.createTempDirectory("starborn-scrap-flow-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            services.inventoryService.addItem("salvage_lure", 2)
            val before = services.inventoryService.snapshot()
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(560.dp, 650.dp)) { DesktopTinkeringContent(services) }
            } }
            compose.onNodeWithText("Scrap").performClick()
            compose.onNodeWithTag("station-stacked-panes").assertExists()
            compose.onAllNodesWithText("Scrap").filter(hasClickAction()).onLast().performClick()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(before, services.inventoryService.snapshot())
            capture("tinkering-compact")
            compose.onAllNodesWithText("Scrap").filter(hasClickAction()).onLast().performClick()
            compose.onNodeWithText("Scrap one").performClick()
            compose.waitUntil(5000) { services.inventoryService.snapshot()["salvage_lure"] == 1 }
            assertEquals((before["scrap_metal"] ?: 0) + 1, services.inventoryService.snapshot()["scrap_metal"])
            compose.onNodeWithText("Workbench").performClick()
            compose.onNodeWithTag("station-stacked-panes").assertExists()
            compose.onNodeWithText("Find a part").assertExists()
            capture("tinkering-workbench-compact")
            val recipe = services.craftingService.tinkeringRecipes.single { it.id == "mod_ergonomic_grip_1" }
            services.craftingService.learnSchematic(recipe.id)
            services.sessionStore.setMilestone("ms_w1_mq01_complete")
            services.sessionStore.setEquippedWeapon("nova", "nova_laser_blaster")
            services.inventoryService.addItem("power_lens_mk_i")
            assertEquals(com.example.starborn.domain.inventory.ModEquipResult.Success,
                services.exploration.requestEquipInventoryMod("weapon_mod1", "power_lens_mk_i", "nova"))
            compose.onNodeWithText("Schematics").performClick()
            compose.onNodeWithText("Find a schematic").performTextInput(recipe.name)
            compose.onNodeWithText("Focus -2", substring = true).assertExists()
            capture("tinkering-schematics-compact")
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun wideCookingSeparatesRecipeBrowserAndSelectedDetails() {
        val directory = Files.createTempDirectory("starborn-cooking-browser-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            var screenWidth by mutableStateOf(1100.dp)
            val use = ItemUseController(services.inventoryService, services.craftingService, services.sessionStore,
                charactersProvider = { services.characterDefinitions })
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(screenWidth, 720.dp)) {
                    DesktopCookingScreen(services.craftingService, services.inventoryService, "Cooking", {}, onCookAndEat = use::cookAndEat)
                }
            } }
            compose.onNodeWithTag("station-side-by-side-panes").assertExists()
            val recipe = services.craftingService.cookingRecipes.single { it.id == "provision_spiced_ration_cache" }
            compose.onNodeWithText("Find a recipe").performScrollTo().performTextInput(recipe.name)
            compose.onNodeWithTag("cooking-select-${recipe.id}").performScrollTo().performClick()
            compose.onNode(hasText(recipe.name) and hasAnyAncestor(hasTestTag("cooking-recipe-details"))).assertExists()
            compose.onNode(hasText("Ingredients consumed") and hasAnyAncestor(hasTestTag("cooking-recipe-details"))).assertExists()
            compose.onNodeWithText("×3").performClick()
            compose.onNodeWithText("Yield: 6 portions", substring = true).assertExists()
            capture("cooking-wide")
            compose.runOnIdle { screenWidth = 560.dp }
            compose.onNodeWithTag("cooking-recipe-details").assertDoesNotExist()
            compose.onNodeWithText("Yield: 6 portions", substring = true).assertExists()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun fishingPracticeFocusPauseFightAndSecuredResult() {
        val directory = Files.createTempDirectory("starborn-fishing-acceptance-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        val scope = TestScope()
        var foreground by mutableStateOf(true)
        services.startNewGame()
        services.inventoryService.addItem("wooden_rod")
        services.inventoryService.addItem("basic_lure")
        val runtime = FishingController(services.fishingService, "sector9_stream", Random(1), scope)
        try {
            scope.runCurrent()
            compose.setContent { DesktopStarbornTheme(services) {
                CompositionLocalProvider(LocalDesktopForeground provides foreground) {
                    Box(Modifier.size(960.dp, 720.dp)) { DesktopFishingContent(services, runtime) {} }
                }
            } }
            compose.onNodeWithTag("cast-line-button").performClick()
            scope.runCurrent()
            compose.runOnIdle { foreground = false }
            compose.waitForIdle()
            val waiting = runtime.uiState.value.waitingState
            scope.advanceTimeBy(10_000); scope.runCurrent()
            assertEquals(waiting, runtime.uiState.value.waitingState)
            compose.runOnIdle { foreground = true }; compose.waitForIdle()
            scope.advanceTimeBy(3_600); scope.runCurrent(); compose.waitForIdle()
            assertEquals(FishingState.HOOKSET, runtime.uiState.value.fishingState)
            compose.onNodeWithText("Your first bite - take your time.", substring = true).assertExists()
            runtime.onHookButtonPressed(); scope.runCurrent()
            runtime.beginReeling(); scope.runCurrent()
            runtime.onReelPressed()
            scope.advanceTimeBy(240); scope.runCurrent()
            compose.runOnIdle { foreground = false }; compose.waitForIdle()
            val pausedReel = runtime.uiState.value.reelState
            scope.advanceTimeBy(5_000); scope.runCurrent()
            assertEquals(pausedReel, runtime.uiState.value.reelState)
            compose.runOnIdle { foreground = true }; compose.waitForIdle()
            repeat(300) {
                val reel = runtime.uiState.value.reelState
                if (reel != null && reel.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && reel.tension < .65f)
                    runtime.onReelPressed() else runtime.onReelReleased()
                scope.advanceTimeBy(FishingFight.TICK_MS); scope.runCurrent()
            }
            compose.waitForIdle()
            assertEquals(FishingState.RESULT, runtime.uiState.value.fishingState)
            val result = requireNotNull(runtime.uiState.value.lastCatchResult)
            assertTrue(result.secured)
            assertTrue(services.inventoryService.hasItem(result.itemId, result.quantity))
            result.uses.forEach { compose.onNodeWithText(it).assertExists() }
            capture("fishing-result")
            val before = services.inventoryService.snapshot()
            compose.onNodeWithTag("fishing-continue-button").performScrollTo().performClick()
            scope.runCurrent()
            assertEquals(before, services.inventoryService.snapshot())
        } finally { runtime.close(); services.close(); directory.deleteRecursively() }
    }
}
