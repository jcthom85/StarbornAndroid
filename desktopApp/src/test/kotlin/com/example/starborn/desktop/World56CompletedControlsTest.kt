package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World56CompletedControlsTest {
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

    @Test fun completedControlsRemainReadOnlyWithEventReceiptsOrLegacyMilestones() = runWith { s ->
        data class Case(val room: String, val label: String, val event: String, val milestone: String)
        val cases = listOf(
            Case("orbital_executive_dock", "docking controls", "w5_mq21_force_dock", "ms_w5_forced_dock"),
            Case("deep_mainframe_nave", "thorne trace", "w5_mq22_find_thorne", "ms_w5_thorne_found"),
            Case("deep_anchor_chamber", "elara", "w5_mq24_find_elara", "ms_w5_elara_found"),
            Case("source_memory_bridge", "Zeke's anchor", "w6_mq28_anchor_zeke", "ms_w6_bridge_zeke_anchor"),
            Case("source_memory_stair", "Jed's door", "w6_mq29_refuse_jed_revision", "ms_w6_refused_jed_revision")
        )
        for (case in cases) for (legacy in listOf(false, true)) {
            restore(s, s.sessionStore.state.value.copy(
                worldId = if (case.room.startsWith("source_")) "world_6" else "world_5",
                roomId = case.room, activeQuests = emptySet(), completedQuests = emptySet(),
                completedEvents = if (legacy) emptySet() else setOf(case.event),
                completedMilestones = if (legacy) setOf(case.milestone) else emptySet(), roomStates = emptyMap(), questTasksCompleted = emptyMap()
            ))
            val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
                .single { it.name == case.label }
            assertTrue("${case.label} must be gray", action.completedInspection)
            assertNull(action.actionEvent)
            val before = s.sessionStore.state.value
            compose.runOnIdle { s.exploration.onActionSelected(action) }
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
            assertNull(s.exploration.uiState.value.activeDialogue)
        }
    }
}
