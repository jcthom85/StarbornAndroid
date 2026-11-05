package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World3NavigationPublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world3-navigation").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { services.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !services.exploration.uiState.value.isLoading }
            block(services)
        } finally { compose.runOnIdle { services.close() }; dir.deleteRecursively() }
    }

    private fun restore(s: DesktopAppServices, state: GameSessionState) {
        compose.runOnIdle { s.sessionStore.restore(state) }
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == state.roomId }
        dismissNarration(s)
    }

    private fun dismissNarration(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }
            compose.waitForIdle()
        }
    }

    private fun act(s: DesktopAppServices, event: String) {
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }
        compose.runOnIdle { s.exploration.onActionSelected(action) }
        compose.waitForIdle()
    }

    private fun finish(s: DesktopAppServices, seen: MutableList<String> = mutableListOf()) {
        repeat(25) {
            val line = s.exploration.uiState.value.activeDialogue?.line ?: return
            seen += line.id
            val choices = s.exploration.uiState.value.dialogueChoices
            if (choices.isEmpty()) compose.runOnIdle { s.exploration.advanceDialogue() }
            else compose.runOnIdle { s.exploration.onDialogueChoiceSelected(choices.single { it.id == "correct" }.id) }
            compose.waitForIdle()
        }
        error("World 3 dialogue did not finish")
    }

    private fun go(s: DesktopAppServices, direction: String, expected: String) {
        assertTrue(direction in setOf("north", "south", "east", "west"))
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == expected }
        dismissNarration(s)
        assertEquals(expected, s.sessionStore.state.value.roomId)
    }

    @Test fun prismControlsUseTextOnlyPresentation() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
            roomId = "spire_prism_gallery", activeQuests = setOf("w3_mq14"),
            completedQuests = setOf("w3_mq11", "w3_mq12", "w3_mq13"), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w3_containment_field_read", "ms_w3_prism_shutters_read", "ms_w3_command_tethers_traced"),
            questTasksCompleted = mapOf("w3_mq14" to setOf("enter_archive"))))
        act(s, "w3_mq14_route_prisms")
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue != null }
        assertEquals("Prism Controls", s.exploration.uiState.value.activeDialogue?.line?.speaker)
        assertNull(s.exploration.uiState.value.activeDialogue?.portrait)
    }

    @Test fun completedCluesRemainReadableAndExhaustedControlsDisappearAfterReload() = runWith { s ->
        val base = s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
            roomId = "spire_prism_gallery", activeQuests = emptySet(),
            completedQuests = (11..15).map { "w3_mq$it" }.toSet(),
            completedMilestones = setOf("ms_w3_mq12_complete", "ms_w3_mq14_complete"),
            completedEvents = setOf("w3_mq14_read_prism_shutters", "w3_mq12_source_disguises"))
        restore(s, base)
        repeat(2) {
            val clue = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == "prism shutters" }
            assertNull(clue.actionEvent)
            assertTrue(clue.completedInspection)
            compose.runOnIdle { s.exploration.onActionSelected(clue) }
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            dismissNarration(s)
        }
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        assertTrue(s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == "prism shutters" }.completedInspection)
        restore(s, base.copy(hubId = "hub_5_lower_city", roomId = "spire_safehouse_roof"))
        assertFalse(s.exploration.uiState.value.actions.any { it.name == "laundry lines" })
    }

    @Test fun evelynAndRobertCanLeaveTheirActualConversations() = runWith { s ->
        for ((npc, root) in listOf("Evelyn" to "evelyn", "Robert" to "robert")) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
                roomId = "spire_skypark_dome", activeQuests = emptySet(), completedQuests = (11..15).map { "w3_mq$it" }.toSet()))
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue != null }
            assertTrue(s.exploration.uiState.value.dialogueChoices.any { it.id == "${root}_leave" })
            compose.runOnIdle { s.exploration.onDialogueChoiceSelected("${root}_leave") }
            compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue == null }
        }
    }

    @Test fun everyWorldThreeRoomCanBeWalkedIntoAndBackOutThroughCardinalControls() = runWith { s ->
        val nodes = s.worldDataSource.loadHubNodes().filter { it.hubId in setOf("hub_5_lower_city", "hub_6_upper_city") }
        val ids = nodes.flatMap { it.rooms }.toSet()
        val rooms = s.worldDataSource.loadRooms().filter { it.id in ids }.associateBy { it.id }
        val base = s.sessionStore.state.value.copy(worldId = "world_3", activeQuests = emptySet(),
            completedQuests = (11..15).map { "w3_mq$it" }.toSet(), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w3_mq12_complete", "ms_w3_mq14_complete"),
            roomStates = mapOf("spire_donor_gallery" to mapOf("laser_grid_bypassed" to true)), questTasksCompleted = emptyMap())
        val visited = mutableSetOf<String>()
        val traversed = mutableSetOf<Pair<String, String>>()
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        fun walk(id: String) {
            if (!visited.add(id)) return
            assertEquals("Every local exit must have a compass indicator", rooms.getValue(id).connections.keys.filter { it in reverse }.toSet(),
                s.exploration.uiState.value.directionIndicators.keys)
            assertEquals(rooms.getValue(id).specialExits.keys,
                s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>().map { it.direction }.toSet())
            for ((direction, destination) in rooms.getValue(id).connections.filterKeys { it in reverse }) {
                go(s, direction, destination); traversed += id to destination
                walk(destination)
                go(s, reverse.getValue(direction), id); traversed += destination to id
            }
        }
        for ((hub, entry) in listOf("hub_5_lower_city" to "spire_sewers_landing", "hub_6_upper_city" to "spire_laundry_service")) {
            restore(s, base.copy(hubId = hub, roomId = entry))
            walk(entry)
            assertEquals(entry, s.sessionStore.state.value.roomId)
        }
        assertEquals("All existing rooms must remain accessible", ids, visited)
        assertEquals(rooms.values.sumOf { it.connections.keys.count { d -> d in reverse } }, traversed.size)
    }

    @Test fun savesInsideFormerDiagonalBranchesReloadWithWorkingSwipeReturns() = runWith { s ->
        val fixtures = listOf(
            Triple("spire_the_static_scale_01", "west", "spire_the_static"),
            Triple("spire_night_market_scale_01", "south", "spire_lantern_bridge"),
            Triple("spire_transit_plaza_scale_01", "north", "spire_security_kiosk"),
            Triple("spire_archive_scale_01", "north", "spire_archive_vault")
        )
        for ((room, direction, target) in fixtures) {
            val upper = room.startsWith("spire_archive")
            restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = if (upper) "hub_6_upper_city" else "hub_5_lower_city",
                roomId = room, activeQuests = emptySet(), completedQuests = (11..15).map { "w3_mq$it" }.toSet(),
                completedMilestones = setOf("ms_w3_mq12_complete", "ms_w3_mq14_complete"), questTasksCompleted = emptyMap(),
                unlockedExits = setOf("$room:southwest")))
            val before = s.sessionStore.state.value
            assertTrue(s.saveManager.saveGame(9, before)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
            go(s, direction, target)
            assertEquals(before.completedQuests, s.sessionStore.state.value.completedQuests)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun blackSiteLaserGateStillRequiresItsConsoleAndAllowsReturnAfterReload() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_donor_gallery",
            activeQuests = emptySet(), completedQuests = (11..15).map { "w3_mq$it" }.toSet(),
            completedMilestones = setOf("ms_w3_mq12_complete", "ms_w3_mq14_complete"),
            completedEvents = emptySet(), unlockedExits = emptySet(), questTasksCompleted = emptyMap(), roomStates = emptyMap()))
        compose.runOnIdle { s.exploration.travel("north") }; compose.waitForIdle()
        assertEquals("spire_donor_gallery", s.sessionStore.state.value.roomId)
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }; compose.waitForIdle()
        val console = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ToggleAction>()
            .single { it.stateKey == "laser_grid_bypassed" }
        compose.runOnIdle { s.exploration.onActionSelected(console) }; compose.waitForIdle(); dismissNarration(s)
        assertEquals(true, s.sessionStore.state.value.roomStates["spire_donor_gallery"]?.get("laser_grid_bypassed"))
        go(s, "north", "spire_black_site_lab")
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "spire_black_site_lab" }
        go(s, "south", "spire_donor_gallery")
        val reset = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ToggleAction>()
            .single { it.stateKey == "laser_grid_bypassed" }
        compose.runOnIdle { s.exploration.onActionSelected(reset) }; compose.waitForIdle(); dismissNarration(s)
        compose.runOnIdle { s.exploration.travel("north") }; compose.waitForIdle()
        assertEquals("spire_donor_gallery", s.sessionStore.state.value.roomId)
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
    }
}
