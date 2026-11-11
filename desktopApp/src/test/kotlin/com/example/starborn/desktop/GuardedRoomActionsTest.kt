package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class GuardedRoomActionsTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("guarded-actions").toFile()
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

    @Test fun guardedInteractionsWaitForVictoryAndSurviveRestore() = runWith { s ->
        val rooms = s.worldDataSource.loadRooms().associateBy { it.id }
        val cases = listOf(
            Triple("medbay_vents", "toxic blockage", "world_1"),
            Triple("sector9_wilds_nest", "trampled supply pack", "world_2"),
            Triple("spire_exec_lounge_scale_05", "executive safe", "world_3")
        )
        for ((id, label, world) in cases) {
            restore(s, s.sessionStore.state.value.copy(
                worldId = world, roomId = id,
                activeQuests = if (world == "world_1") setOf("w1_sq02") else emptySet(),
                completedQuests = emptySet(), completedEvents = emptySet(),
                completedMilestones = emptySet(), roomStates = emptyMap(),
                questTasksCompleted = mapOf("w1_sq02" to setOf("inspect_cracked_vent", "reroute_airflow"))
            ))
            fun action() = s.exploration.uiState.value.actions.single { it.name == label }
            val before = s.sessionStore.state.value
            compose.runOnIdle { s.exploration.onActionSelected(action()) }
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertEquals(before.questTasksCompleted, s.sessionStore.state.value.questTasksCompleted)
            dismissNarration(s)
            compose.runOnIdle {
                s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                    outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                    enemyIds = rooms.getValue(id).enemies.orEmpty(), roomId = id
                ))
            }
            compose.waitForIdle()
            restore(s, s.sessionStore.state.value)
            val availableAction = action()
            compose.runOnIdle { s.exploration.onActionSelected(availableAction) }
            compose.waitForIdle()
            if (world == "world_1") {
                assertTrue("clear_toxic_blockage" in s.sessionStore.state.value.questTasksCompleted["w1_sq02"].orEmpty())
            } else {
                val container = availableAction as com.example.starborn.domain.model.ContainerAction
                assertEquals(true, s.sessionStore.state.value.roomStates[id]?.get(container.stateKey))
                val rewarded = s.sessionStore.state.value.inventory
                dismissNarration(s)
                compose.runOnIdle { s.exploration.onActionSelected(availableAction) }
                compose.waitForIdle()
                assertEquals(rewarded, s.sessionStore.state.value.inventory)
            }
        }
    }
}
