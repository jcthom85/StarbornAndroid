package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class NamedVerticalTravelPublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("named-vertical-travel").toFile()
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
        error("Named travel dialogue did not finish")
    }

    private fun go(s: DesktopAppServices, direction: String, expected: String) {
        assertTrue(direction in setOf("north", "south", "east", "west"))
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == expected }
        dismissNarration(s)
        assertEquals(expected, s.sessionStore.state.value.roomId)
    }

    private fun take(s: DesktopAppServices, direction: String, target: String) {
        val ui = s.exploration.uiState.value
        val action = ui.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>().single { it.direction == direction }
        assertFalse("Vertical travel should use its named Paths control", direction in ui.directionIndicators)
        val room = requireNotNull(ui.currentRoom)
        val plan = requireNotNull(com.example.starborn.feature.exploration.presentation.buildInlineActionPlan(room.description, ui.actions, ui.actionHints, room))
        assertTrue(plan.description.contains("Paths:"))
        assertTrue("Named path must be a clickable room action", plan.segments.any {
            val link = it.target as? com.example.starborn.feature.exploration.presentation.InlineActionTarget.Room
            link?.action == action
        })
        compose.runOnIdle { s.exploration.onActionSelected(action) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == target }
        dismissNarration(s)
    }

    @Test fun reviewedPathsWorkBothWaysAcrossQuestPhasesAndReload() = runWith { s ->
        val routes = listOf(
            listOf("world_3", "hub_5_lower_city", "spire_underrail_platform", "spire_transit_plaza", "w3_mq12", "w3_mq15"),
            listOf("world_3", "hub_6_upper_city", "spire_service_lift", "spire_archive_vault", "w3_mq14", "w3_mq15"),
            listOf("world_5", "hub_9_orbital_ring", "orbital_service_shaft", "orbital_server_farm", "w5_mq22", "w5_mq24"),
            listOf("world_5", "hub_10_deep_ring", "deep_firewall_gamma", "deep_anchor_chamber", "w5_mq24", "w5_mq25")
        )
        for (route in routes) for (phase in route.takeLast(2)) {
            val completed = when (phase) {
                "w3_mq12" -> setOf("w3_mq11")
                "w3_mq14" -> (11..13).map { "w3_mq$it" }.toSet()
                "w3_mq15" -> (11..14).map { "w3_mq$it" }.toSet()
                "w5_mq22" -> setOf("w5_mq21")
                "w5_mq24" -> (21..23).map { "w5_mq$it" }.toSet()
                else -> (21..24).map { "w5_mq$it" }.toSet()
            }
            val cleared = "w5_mq23" in completed
            restore(s, s.sessionStore.state.value.copy(worldId = route[0], hubId = route[1], roomId = route[2],
                activeQuests = setOf(phase), completedQuests = completed, completedEvents = emptySet(),
                completedMilestones = if (phase == "w3_mq15") setOf("ms_w3_mq12_complete", "ms_w3_mq14_complete") else emptySet(),
                questTasksCompleted = when {
                    route[0] == "world_3" -> if (phase == "w3_mq12") emptyMap() else mapOf("w3_mq14" to setOf("enter_archive"))
                    cleared -> mapOf("w5_mq24" to setOf("enter_chamber"))
                    else -> mapOf("w5_mq22" to setOf("clear_security", "traverse_shaft"))
                },
                roomStates = if (cleared) mapOf("deep_anchor_chamber" to mapOf("firewall_access_open" to true),
                    "deep_firewall_gamma" to mapOf("encounter_cleared:firewall_construct" to true)) else emptyMap(),
                visitedNodes = emptySet(), unlockedExits = emptySet()))
            val before = s.sessionStore.state.value
            take(s, "down", route[3])
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == route[3] }
            dismissNarration(s); take(s, "up", route[2])
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertTrue(phase in s.sessionStore.state.value.activeQuests)
            assertEquals(route[1], s.sessionStore.state.value.hubId)
        }
    }

    @Test fun anchorLiftRemainsLockedBeforeVictoryIncludingReloadAfterNodeShutdown() = runWith { s ->
        for (nodesDown in listOf(false, true)) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_10_deep_ring", roomId = "deep_firewall_gamma",
                activeQuests = setOf("w5_mq23"), completedQuests = setOf("w5_mq22"), completedEvents = emptySet(),
                completedMilestones = if (nodesDown) setOf("ms_w5_firewall_gamma_down") else emptySet(),
                questTasksCompleted = if (nodesDown) mapOf("w5_mq23" to setOf("navigate_maze", "firewall_alpha", "firewall_beta", "firewall_gamma")) else emptyMap(),
                roomStates = emptyMap(), visitedNodes = emptySet(), unlockedExits = emptySet()))
            val before = s.sessionStore.state.value
            repeat(2) {
                val action = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>().single { it.direction == "down" }
                compose.runOnIdle { s.exploration.onActionSelected(action) }
                compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
                assertEquals("deep_firewall_gamma", s.sessionStore.state.value.roomId)
                assertTrue(s.exploration.uiState.value.blockedPrompt!!.message.contains("Construct"))
                compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
                assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
                compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "deep_firewall_gamma" }
            }
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertFalse("w5_mq23" in s.sessionStore.state.value.completedQuests)
        }
    }
}
