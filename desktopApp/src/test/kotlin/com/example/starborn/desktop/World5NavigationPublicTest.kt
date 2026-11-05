package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World5NavigationPublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world5-navigation").toFile()
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
        error("World 5 dialogue did not finish")
    }

    private fun go(s: DesktopAppServices, direction: String, expected: String) {
        assertTrue(direction in setOf("north", "south", "east", "west"))
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == expected }
        dismissNarration(s)
        assertEquals(expected, s.sessionStore.state.value.roomId)
    }

    private fun takeLift(s: DesktopAppServices, direction: String, target: String) {
        val lift = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>().single { it.direction == direction }
        compose.runOnIdle { s.exploration.onActionSelected(lift) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == target }
        dismissNarration(s)
    }

    @Test fun everyRingRoomHasWorkingCardinalTravelAndReturn() = runWith { s ->
        val ids = s.worldDataSource.loadHubNodes().filter { it.hubId in setOf("hub_9_orbital_ring", "hub_10_deep_ring") }.flatMap { it.rooms }.toSet()
        val rooms = s.worldDataSource.loadRooms().filter { it.id in ids }.associateBy { it.id }
        restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_9_orbital_ring", roomId = "orbital_executive_dock",
            activeQuests = emptySet(), completedQuests = (21..25).map { "w5_mq$it" }.toSet() + (21..25).map { "w5_sq$it" },
            completedEvents = emptySet(), completedMilestones = setOf("ms_w5_mq23_complete"), questTasksCompleted = emptyMap(),
            roomStates = mapOf("deep_anchor_chamber" to mapOf("firewall_access_open" to true),
                "orbital_zero_g_junction" to mapOf("magnetic_anchor_engaged" to true))))
        val visited = mutableSetOf<String>(); val traversed = mutableSetOf<Pair<String, String>>()
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        fun walk(id: String) {
            if (!visited.add(id)) return
            assertEquals(rooms.getValue(id).connections.keys.filter { it in reverse }.toSet(), s.exploration.uiState.value.directionIndicators.keys)
            assertEquals(rooms.getValue(id).specialExits.keys,
                s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>().map { it.direction }.toSet())
            for ((d, dest) in rooms.getValue(id).connections.filterKeys { it in reverse }) {
                go(s, d, dest); traversed += id to dest; walk(dest)
                go(s, reverse.getValue(d), id); traversed += dest to id
            }
        }
        walk("orbital_executive_dock")
        assertEquals(81, visited.size); assertEquals(ids, visited)
        assertEquals(rooms.values.sumOf { it.connections.keys.count { d -> d in reverse } }, traversed.size)
    }

    @Test fun realVictoryCallbackOpensAnchorDoorAndReloadedWalkbackDoesNotPayAgain() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_10_deep_ring", roomId = "orbital_server_farm",
            activeQuests = setOf("w5_mq23"), completedQuests = setOf("w5_mq22"), completedMilestones = setOf("ms_w5_firewall_gamma_down"),
            completedEvents = emptySet(), roomStates = emptyMap(), unlockedExits = emptySet(),
            questTasksCompleted = mapOf("w5_mq23" to setOf("navigate_maze", "firewall_alpha", "firewall_beta", "firewall_gamma"))))
        compose.runOnIdle { s.exploration.travel("east") }
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("orbital_server_farm", s.sessionStore.state.value.roomId)
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
        go(s, "north", "deep_mainframe_nave"); go(s, "east", "deep_firewall_alpha")
        go(s, "east", "deep_firewall_beta"); go(s, "east", "deep_firewall_gamma")
        assertTrue("firewall_construct" in s.exploration.uiState.value.enemies)
        compose.runOnIdle { s.exploration.engageEnemy("firewall_construct") }
        assertTrue(s.sessionStore.state.value.pendingBattleJson.isNotBlank())
        val xp = s.sessionStore.state.value.playerXp
        // Transition fixture only; the earned campaign separately plays the actual fight.
        compose.runOnIdle { s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
            com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY, enemyIds = listOf("firewall_construct"), roomId = "deep_firewall_gamma")) }
        compose.waitUntil(5000) { "w5_mq23" in s.sessionStore.state.value.completedQuests }; dismissNarration(s)
        assertEquals(xp + 1200, s.sessionStore.state.value.playerXp)
        assertEquals(true, s.sessionStore.state.value.roomStates["deep_anchor_chamber"]?.get("firewall_access_open"))
        takeLift(s, "down", "deep_anchor_chamber"); takeLift(s, "up", "deep_firewall_gamma")
        go(s, "west", "deep_firewall_beta"); go(s, "west", "deep_firewall_alpha"); go(s, "west", "deep_mainframe_nave")
        go(s, "south", "orbital_server_farm"); go(s, "east", "deep_anchor_chamber")
        val rewarded = s.sessionStore.state.value
        assertTrue(s.saveManager.saveGame(9, rewarded.copy(roomStates = rewarded.roomStates - "deep_anchor_chamber")))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "deep_anchor_chamber" }
        go(s, "west", "orbital_server_farm"); go(s, "west", "deep_sysadmin_nest")
        go(s, "north", "orbital_zero_g_junction"); go(s, "west", "orbital_service_shaft")
        assertEquals("hub_9_orbital_ring", s.sessionStore.state.value.hubId)
        go(s, "east", "orbital_zero_g_junction"); go(s, "south", "deep_sysadmin_nest")
        go(s, "east", "orbital_server_farm"); go(s, "east", "deep_anchor_chamber")
        assertEquals(rewarded.playerXp, s.sessionStore.state.value.playerXp)
        assertEquals(rewarded.inventory, s.sessionStore.state.value.inventory)
    }

    @Test fun oldSavesAlreadyBeyondAnchorDoorRetainAnExitWithoutBossCredit() = runWith { s ->
        for ((room, route) in listOf(
            "deep_anchor_chamber" to listOf("orbital_server_farm"),
            "deep_throne_room" to listOf("deep_anchor_chamber", "orbital_server_farm"),
            "deep_tear" to listOf("deep_throne_room", "deep_anchor_chamber", "orbital_server_farm")
        )) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_10_deep_ring", roomId = room,
                activeQuests = setOf("w5_mq23"), completedQuests = emptySet(), completedEvents = emptySet(), completedMilestones = emptySet(),
                roomStates = emptyMap(), questTasksCompleted = emptyMap(), unlockedExits = emptySet(), visitedNodes = emptySet()))
            val old = s.sessionStore.state.value
            assertTrue(s.saveManager.saveGame(9, old)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
            if (room == "deep_anchor_chamber") {
                takeLift(s, "up", "deep_firewall_gamma"); takeLift(s, "down", "deep_anchor_chamber")
            }
            for (destination in route) go(s, "west", destination)
            assertEquals(true, s.sessionStore.state.value.roomStates["deep_anchor_chamber"]?.get("firewall_access_open"))
            assertFalse("w5_mq23" in s.sessionStore.state.value.completedQuests)
            assertFalse("ms_w5_mq23_complete" in s.sessionStore.state.value.completedMilestones)
            assertEquals(old.playerXp, s.sessionStore.state.value.playerXp)
            assertEquals(old.inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun formerDiagonalBranchesReloadWithCardinalReturns() = runWith { s ->
        for ((room, destination) in listOf("orbital_security_hub_scale_01" to "orbital_security_hub", "deep_server_farm_scale_01" to "deep_backup_rack")) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = if (room.startsWith("orbital_")) "hub_9_orbital_ring" else "hub_10_deep_ring",
                roomId = room, activeQuests = emptySet(), completedQuests = setOf("w5_mq23"), completedMilestones = emptySet(),
                completedEvents = emptySet(), questTasksCompleted = emptyMap(), unlockedExits = setOf("$room:southwest")))
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
            go(s, "north", destination)
        }
    }

    @Test fun magneticClampStillGatesOnlyTheOptionalLabAndPreservesSavedReturn() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_9_orbital_ring", roomId = "orbital_zero_g_junction",
            activeQuests = emptySet(), completedQuests = setOf("w5_mq23"), completedEvents = emptySet(), completedMilestones = emptySet(),
            roomStates = emptyMap(), unlockedExits = emptySet(), questTasksCompleted = emptyMap()))
        compose.runOnIdle { s.exploration.travel("east") }; compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("orbital_zero_g_junction", s.sessionStore.state.value.roomId)
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
        fun toggle() {
            val clamp = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ToggleAction>().single { it.stateKey == "magnetic_anchor_engaged" }
            compose.runOnIdle { s.exploration.onActionSelected(clamp) }; compose.waitForIdle(); dismissNarration(s)
        }
        toggle(); go(s, "east", "orbital_breached_avionics_lab")
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "orbital_breached_avionics_lab" }
        go(s, "west", "orbital_zero_g_junction"); toggle()
        compose.runOnIdle { s.exploration.travel("east") }; compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
        go(s, "south", "deep_sysadmin_nest"); go(s, "east", "orbital_server_farm")
    }

    @Test fun arrivalStillAllowsSupplyPreparationBeforeTheFighter() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_9_orbital_ring", roomId = "orbital_executive_dock",
            activeQuests = setOf("w5_mq21"), completedQuests = emptySet(), completedEvents = emptySet(), completedMilestones = emptySet(),
            roomStates = emptyMap(), questTasksCompleted = emptyMap()))
        go(s, "east", "orbital_grand_concourse"); go(s, "north", "orbital_customs_lounge")
        assertTrue(s.exploration.uiState.value.actions.any { it is com.example.starborn.domain.model.ShopAction && it.shopId == "orbital_dispensary" })
        val rest = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.RestStopAction>().first { it.cookSource == null }
        compose.runOnIdle { s.exploration.onActionSelected(rest) }; compose.waitForIdle(); dismissNarration(s)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "orbital_customs_lounge" }
        go(s, "south", "orbital_grand_concourse"); go(s, "west", "orbital_executive_dock")
        assertTrue("w5_mq21" in s.sessionStore.state.value.activeQuests)
        assertFalse("ms_w5_fighter_screen_broken" in s.sessionStore.state.value.completedMilestones)
    }
}
