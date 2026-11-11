package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World1CombatGatePublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world4-navigation").toFile()
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

    @Test fun worldOneGatesRequireVictoryAndRemainClearAfterRestore() = runWith { s ->
        val rooms = s.worldDataSource.loadRooms().associateBy { it.id }
        for ((id, target) in listOf(
            "workshop_yard" to "workshop_floor",
            "launch_lift" to "launch_checkpoint",
            "launch_checkpoint" to "launch_bay",
            "launch_bay" to "launch_pod"
        )) {
            val enemies = rooms.getValue(id).enemies.orEmpty()
            restore(s, s.sessionStore.state.value.copy(
                worldId = "world_1", hubId = if (id == "workshop_yard") "hub_1_homestead" else "hub_2_logistics",
                roomId = id, activeQuests = emptySet(), completedQuests = emptySet(),
                completedEvents = emptySet(), completedMilestones = emptySet(),
                unlockedExits = emptySet(), roomStates = emptyMap(), questTasksCompleted = emptyMap()
            ))
            compose.runOnIdle { s.exploration.travel("north") }
            compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
            assertEquals(id, s.sessionStore.state.value.roomId)
            compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
            val beforeVictory = s.sessionStore.state.value
            rooms.getValue(id).connections["south"]?.let { retreat ->
                go(s, "south", retreat)
                restore(s, beforeVictory)
            }
            compose.runOnIdle {
                s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                    outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                    enemyIds = enemies, roomId = id
                ))
            }
            compose.waitForIdle()
            val cleared = s.sessionStore.state.value
            restore(s, cleared)
            go(s, "north", target)
        }
    }
}
