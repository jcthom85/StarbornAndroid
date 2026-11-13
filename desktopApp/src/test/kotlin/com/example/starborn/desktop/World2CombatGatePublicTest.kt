package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World2CombatGatePublicTest {
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

    @Test fun worldTwoGuardsAllowRetreatAndUnlockAfterVictoryAndRestore() = runWith { s ->
        val rooms = s.worldDataSource.loadRooms().associateBy { it.id }
        for ((id, direction, target) in listOf(
            Triple("sector9_stream_cave_depths", "east", "sector9_stream_sunken_passage"),
            Triple("sector9_foyer_left_gallery", "west", "sector9_foyer_security_hub")
        )) {
            restore(s, s.sessionStore.state.value.copy(
                worldId = "world_2", hubId = "hub_3_sector9", roomId = id,
                activeQuests = emptySet(), completedQuests = emptySet(), completedEvents = emptySet(),
                completedMilestones = emptySet(), unlockedExits = emptySet(), roomStates = emptyMap(),
                questTasksCompleted = emptyMap()
            ))
            compose.runOnIdle { s.exploration.travel(direction) }
            compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
            assertEquals(id, s.sessionStore.state.value.roomId)
            compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
            val beforeVictory = s.sessionStore.state.value
            go(s, "south", rooms.getValue(id).connections.getValue("south"))
            restore(s, beforeVictory)
            compose.runOnIdle {
                s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                    outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                    enemyIds = rooms.getValue(id).enemies.orEmpty(), roomId = id
                ))
            }
            compose.waitForIdle()
            restore(s, s.sessionStore.state.value)
            go(s, direction, target)
            go(s, if (direction == "east") "west" else "east", id)
        }
    }
    @Test fun historicalClearedFormationsStayClearedAfterRestoreAndLegacyVictory() = runWith { s ->
        val base=s.sessionStore.state.value.copy(worldId="world_2",hubId="hub_3_sector9",
            activeQuests=emptySet(),completedQuests=emptySet(),completedEvents=emptySet(),
            completedMilestones=emptySet(),unlockedExits=emptySet(),questTasksCompleted=emptyMap())
        val cases=listOf(
            "sector9_stream_pools" to listOf("siren_skimmer"),
            "sector9_stream_cave_depths" to listOf("echo_borer","echo_borer"),
            "sector9_hall_acoustics_lab" to listOf("spore_spitter"))
        for((room,ids) in cases) {
            val key="encounter_cleared:"+ids.sorted().joinToString("|")
            restore(s,base.copy(roomId=room,roomStates=mapOf(room to mapOf(key to true))))
            assertTrue("Old receipt must clear $room",s.exploration.uiState.value.enemies.isEmpty())
            val saved=s.sessionStore.state.value
            restore(s,saved)
            assertTrue(s.exploration.uiState.value.enemies.isEmpty())
            restore(s,base.copy(roomId=room,roomStates=emptyMap()))
            compose.waitUntil(5000) { s.exploration.uiState.value.enemies.isNotEmpty() }
            assertFalse("Fresh room must still offer its new formation in $room",s.exploration.uiState.value.enemies.isEmpty())
            // An already-running older battle may return the original IDs.
            compose.runOnIdle { s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                outcome=com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,enemyIds=ids,roomId=room)) }
            compose.waitForIdle()
            assertTrue("Legacy victory must retire the corresponding new formation",s.exploration.uiState.value.enemies.isEmpty())
            restore(s,s.sessionStore.state.value)
            assertTrue(s.exploration.uiState.value.enemies.isEmpty())
        }
    }

}
