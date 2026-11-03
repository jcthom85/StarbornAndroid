package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import javax.imageio.ImageIO
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.crafting.CraftingController
import com.example.starborn.ui.menu.OverflowPartyRow
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World2ExplorationPolishTest {
    @get:Rule val compose = createComposeRule()
    private fun services(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world2-polish").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { s.close(); dir.deleteRecursively() }
    }
    @Test fun shortcutConfirmsEachDirectionOnceAndEarlierSavesRestoreTheChoice() = services { s ->
        val earlier = s.sessionStore.state.value.copy(roomId = "sector9_hangar_bay", activeQuests = setOf("w2_mq05"),
            completedMilestones = setOf("ms_w2_astra_inspected"), completedEvents = emptySet(),
            roomStates = emptyMap(), questTasksCompleted = mapOf("w2_mq05" to setOf("inspect_astra")))
        s.sessionStore.restore(earlier)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == earlier.roomId }
        fun forward() = s.exploration.onActionSelected(s.exploration.uiState.value.actions.single { it.name == "salvage return route" })
        forward()
        assertNotNull(s.exploration.uiState.value.roomObjectOffer)
        assertEquals(earlier.roomId, s.sessionStore.state.value.roomId)
        s.exploration.dismissRoomObject()
        forward(); s.exploration.confirmRoomObject()
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "sector9_landing_drop" }
        assertEquals("Pod Fragment", s.exploration.uiState.value.areaEntry?.title)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(s.loadSlot(9))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        assertTrue(s.sessionStore.state.value.roomStates[earlier.roomId!!]?.get("route_confirmed_w2_mq05_return_to_pod") == true)
        s.sessionStore.setRoom("sector9_hangar_bay")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == earlier.roomId }
        forward()
        compose.waitUntil(5000) { s.sessionStore.state.value.roomId == "sector9_landing_drop" }
        assertNull(s.exploration.uiState.value.roomObjectOffer)
        s.sessionStore.setQuestTaskCompleted("w2_mq05", "collect_power_conduits", true)
        s.sessionStore.setMilestone("ms_w2_conduits_collected")
        compose.waitUntil(5000) { s.exploration.uiState.value.actions.any { (it as? com.example.starborn.domain.model.GenericAction)?.actionEvent == "w2_mq05_return_to_hangar" } }
        val back = s.exploration.uiState.value.actions.single { (it as? com.example.starborn.domain.model.GenericAction)?.actionEvent == "w2_mq05_return_to_hangar" }
        s.exploration.onActionSelected(back)
        assertNotNull(s.exploration.uiState.value.roomObjectOffer)
        s.exploration.confirmRoomObject()
        compose.waitUntil(5000) { s.sessionStore.state.value.roomId == earlier.roomId }
        s.sessionStore.restore(earlier)
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == earlier.roomId &&
            s.exploration.uiState.value.roomState["route_confirmed_w2_mq05_return_to_pod"] != true &&
            s.exploration.uiState.value.actions.any { it.name == "salvage return route" } }
        forward(); assertNotNull(s.exploration.uiState.value.roomObjectOffer)
    }
    @Test fun questStarterRemainsVisibleWhileDormantQuestTasksStayPlain() = services { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_canopy", activeQuests = emptySet(),
            completedQuests = emptySet(), completedMilestones = emptySet(), completedEvents = emptySet(), questTasksCompleted = emptyMap()))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_canopy" }
        val starter = s.exploration.uiState.value.actions.single { it.name == "Scavenge remains" }
        s.exploration.onActionSelected(starter)
        compose.waitUntil(5000) { "w2_sq02" in s.sessionStore.state.value.activeQuests }
        s.sessionStore.setRoom("sector9_wilds_hollow")
        compose.waitUntil(5000) { s.exploration.uiState.value.actions.any { it.name == "distress signature beta" } }
        s.sessionStore.restore(s.sessionStore.state.value.copy(activeQuests = emptySet()))
        compose.waitUntil(5000) { s.exploration.uiState.value.actions.none { it.name == "distress signature beta" } }
    }
    @Test fun signalReflectionUsesPersistentPassiveInspectionStyling() = services { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_beach_cliffs"))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_beach_cliffs" }
        val action = s.exploration.uiState.value.actions.single { it.name == "signal reflection" }
        val key = requireNotNull(com.example.starborn.feature.exploration.presentation.inspectionReceiptKey(action, null))
        s.exploration.onActionSelected(action)
        compose.waitUntil(5000) { s.exploration.uiState.value.roomState[key] == true }
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(s.loadSlot(9))
        compose.waitUntil(10000) { s.exploration.uiState.value.roomState[key] == true }
    }

    @Test fun schematicViewedStatePersistsWithoutCraftingOrConsumingAnything() = services { s ->
        s.craftingService.learnSchematic("mod_source_resin")
        val runtime = CraftingController(s.craftingService, s.inventoryService, s.sessionStore, CoroutineScope(Dispatchers.Unconfined))
        try {
            assertTrue(runtime.uiState.value.learnedRecipes.single { it.id == "mod_source_resin" }.isNew)
            val inventory = s.inventoryService.snapshot()
            runtime.markSchematicViewed("mod_source_resin")
            compose.waitUntil(5000) { !runtime.uiState.value.learnedRecipes.single { it.id == "mod_source_resin" }.isNew }
            assertEquals(inventory, s.inventoryService.snapshot())
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
            assertTrue(s.loadSlot(9))
            assertTrue("mod_source_resin" in s.sessionStore.state.value.viewedSchematics)
        } finally { runtime.close() }
    }
    @Test fun partyOverflowCuesDisappearWhenEverythingFitsAndSelectionIsVisible() {
        var count by mutableStateOf(4)
        var selected by mutableStateOf("0")
        compose.setContent {
            Box(Modifier.width(350.dp)) {
                OverflowPartyRow((0 until count).map { "$it" }, selected, 150.dp, Color.Black) { i ->
                    Text("Companion $i", Modifier.height(64.dp))
                }
            }
        }
        compose.onNodeWithTag("party-more-right").assertExists()
        compose.onNodeWithTag("party-more-left").assertDoesNotExist()
        compose.runOnIdle { selected = "3" }
        compose.waitForIdle()
        compose.onNodeWithText("Companion 3").assertIsDisplayed()
        compose.onNodeWithTag("party-more-left").assertExists()
        compose.runOnIdle { count = 2; selected = "0" }
        compose.waitForIdle()
        compose.onNodeWithTag("party-more-left").assertDoesNotExist()
        compose.onNodeWithTag("party-more-right").assertDoesNotExist()
    }
    @Test fun schematicDetailsPrioritizeMaterialsAndSpecificMissingParts() = services { s ->
        s.craftingService.learnSchematic("mod_rapid_capacitor")
        compose.setContent {
            com.example.starborn.desktop.ui.DesktopStarbornTheme(s) {
                Box(Modifier.size(980.dp, 700.dp)) { com.example.starborn.desktop.ui.DesktopTinkeringContent(s) }
            }
        }
        compose.onNodeWithText("Schematics").performClick()
        compose.onNodeWithText("Materials · owned / required").assertIsDisplayed()
        compose.onNodeWithText("Synthesize").assertIsNotEnabled()
        compose.onNodeWithText("Missing:", substring = true).assertIsDisplayed()
        val image = File("build/reports/desktop/screenshots/world2-polish/schematic-details.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", image)
    }

    @Test fun echoUsesKnownEventsWithoutSpoilingOrion() = services { s ->
        val before = s.sessionStore.state.value.copy(roomId = "sector9_hall_of_echoes", activeQuests = setOf("w2_mq03"), completedMilestones = emptySet())
        s.sessionStore.restore(before)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == before.roomId }
        s.exploration.onNpcInteraction("Caretaker Echo")
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue != null }
        assertTrue(s.exploration.uiState.value.dialogueChoices.any { it.id == "echo_opt_stasis" })
        s.exploration.onDialogueChoiceSelected("echo_opt_stasis")
        assertFalse(s.exploration.uiState.value.activeDialogue!!.line.text.contains("Orion"))
        s.sessionStore.restore(before.copy(completedMilestones = setOf("ms_w2_stasis_awake", "ms_w2_orion_identified")))
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue == null }
        s.exploration.onNpcInteraction("Caretaker Echo")
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "echo_opt_stasis" })
        s.exploration.onDialogueChoiceSelected("echo_opt_orion_awake")
        assertTrue(s.exploration.uiState.value.activeDialogue!!.line.text.contains("Orion"))
    }
}
