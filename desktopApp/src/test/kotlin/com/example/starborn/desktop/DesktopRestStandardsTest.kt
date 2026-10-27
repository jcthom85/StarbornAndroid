package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import javax.imageio.ImageIO
import java.io.File
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopRestStandardsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun restAndCookingStaySeparateAndRespectDarknessAndBlocking() {
        val directory = Files.createTempDirectory("starborn-rest-controls-").toFile()
        val s = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            val room = s.worldDataSource.loadRooms().first { it.id == "foundry_forge_control_alcove" }
            val actions = listOf(RestStopAction("control alcove cot", restEvent = "w4_camp_forge_alcove"),
                RestStopAction("smelter break station", cookSource = "Thermal Cooker"))
            val ui = com.example.starborn.feature.exploration.viewmodel.ExplorationUiState(currentRoom = room, actions = actions)
            var dark by mutableStateOf(false)
            var blocked by mutableStateOf(false)
            compose.setContent { DesktopStarbornTheme(s) {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    DesktopExplorationPanels(s, ui, portraitBackdropLayout(Size(1280f, 800f), Size(1088f, 1920f)),
                        room.description, null, dark, blocked) {}
                }
            } }
            compose.onNodeWithText("Rest").assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("Cook").assertIsDisplayed().assertIsEnabled()
            val output = File("build/reports/rest-standard").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(output, "rest-and-cook.png"))
            compose.runOnIdle { blocked = true }
            compose.onNodeWithText("Rest").assertIsNotEnabled()
            compose.onNodeWithText("Cook").assertIsNotEnabled()
            compose.runOnIdle { dark = true }
            compose.onNodeWithTag("rest-services").assertDoesNotExist()
        } finally { s.close(); directory.deleteRecursively() }
    }

    @Test fun allAuthoredRestAndCookingStopsKeepTheirRecoveryEventsRewardsAndDiscovery() {
        val directory = Files.createTempDirectory("starborn-rest-standards-").toFile()
        val s = DesktopAppServices(directory, RecordingAudioDriver())
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            val runtime = s.exploration
            compose.waitUntil(10000) { !runtime.uiState.value.isLoading }
            val baseline = s.sessionStore.state.value
            val rooms = s.worldDataSource.loadRooms().filter { r -> r.actions.any { it["type"] == "rest_stop" } }
            var cookingSource: String? = null
            scope.launch { runtime.events.collect { if (it is ExplorationEvent.OpenCooking) cookingSource = it.sourceId } }
            var rests = 0
            var cooks = 0
            rooms.forEach { room ->
                room.actions.filter { it["type"] == "rest_stop" }.forEach { raw ->
                    val name = raw["name"] as String
                    s.inventoryService.restore(baseline.inventory)
                    s.sessionStore.restore(baseline.copy(roomId = room.id, activeQuests = emptySet(),
                        partyMembers = listOf("nova"), partyMemberHp = mapOf("nova" to 20),
                        completedEvents = emptySet(), roomStates = mapOf(room.id to mapOf("light_on" to true, "dark" to false))))
                    compose.waitUntil(10000) { runtime.uiState.value.currentRoom?.id == room.id &&
                        runtime.uiState.value.actions.filterIsInstance<RestStopAction>().any { it.name == name } }
                    val ui = runtime.uiState.value
                    val action = ui.actions.filterIsInstance<RestStopAction>().single { it.name == name }
                    val plan = buildInlineActionPlan(room.description, ui.actions, ui.actionHints, room)
                    assertTrue("${room.id}: rest/cook prose stays plain", plan?.segments.orEmpty().none {
                        (it.target as? InlineActionTarget.Room)?.action is RestStopAction
                    })
                    val supplies = s.inventoryService.snapshot()["painkillers"] ?: 0
                    cookingSource = null
                    runtime.inspectRoomAction(action)
                    assertEquals(action, runtime.uiState.value.roomObjectOffer?.action)
                    assertEquals(20, s.sessionStore.state.value.partyMemberHp["nova"])
                    runtime.confirmRoomObject()
                    if (action.cookSource != null) {
                        cooks++
                        assertEquals("Cook", restStopServiceLabel(action))
                        compose.waitUntil(5000) { cookingSource == action.cookSource }
                        assertEquals("${room.id}: cooking must not rest", 20, s.sessionStore.state.value.partyMemberHp["nova"])
                        assertEquals(true, s.sessionStore.state.value.roomStates[room.id]?.get("cooking_discovered"))
                    } else {
                        rests++
                        assertEquals("Rest", restStopServiceLabel(action))
                        compose.waitUntil(5000) { (s.sessionStore.state.value.partyMemberHp["nova"] ?: 0) > 20 }
                        assertNull(cookingSource)
                        action.restEvent?.let { event ->
                            if (event.contains("_camp_")) {
                                assertTrue("$event still executes", event in s.sessionStore.state.value.completedEvents)
                                assertEquals("$event preserves camp supplies", supplies + 1,
                                    s.inventoryService.snapshot()["painkillers"] ?: 0)
                                runtime.onActionSelected(action)
                                assertEquals("$event must not duplicate camp rewards", supplies + 1,
                                    s.inventoryService.snapshot()["painkillers"] ?: 0)
                            }
                        }
                        if (room.id == "pit_nova_bunk") assertEquals(true,
                            s.sessionStore.state.value.roomStates[room.id]?.get("bunk_checked"))
                    }
                }
            }
            assertEquals("Every authored rest spot was checked", 14, rests)
            assertEquals("Every authored cooking station was checked", 14, cooks)
        } finally { scope.cancel(); s.close(); directory.deleteRecursively() }
    }
}
