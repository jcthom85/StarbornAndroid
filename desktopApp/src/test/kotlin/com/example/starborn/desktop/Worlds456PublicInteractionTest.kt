package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class Worlds456PublicInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("worlds456-public").toFile()
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

    @Test fun fullNpcNamesResolveThorneAndValeAtTheirCurrentQuestScenes() = runWith { s ->
        for ((npc, quest, room, task, line) in listOf(
            listOf("thorne", "w5_mq22", "deep_mainframe_nave", "access_mainframe", "thorne_w5_mq22_intro"),
            listOf("vale", "w5_mq25", "deep_throne_room", "reach_throne", "vale_w5_mq25_soloist")
        )) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_10_deep_ring", roomId = room,
                activeQuests = setOf(quest), completedQuests = emptySet(), completedEvents = emptySet(),
                completedMilestones = emptySet(), questTasksCompleted = mapOf(quest to setOf(task))))
            assertTrue("NPC must be visible: $npc", npc in s.exploration.uiState.value.npcs)
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            compose.waitForIdle()
            assertEquals(line, s.exploration.uiState.value.activeDialogue?.line?.id)
            finish(s)
            assertTrue(if (quest == "w5_mq22") quest in s.sessionStore.state.value.completedQuests
                else "witness_soloist" in s.sessionStore.state.value.questTasksCompleted[quest].orEmpty())
        }
    }

    @Test fun phantomSceneAndSpringsConversationAwardTheKeyOnlyAfterDialogue() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_waste_intake",
            activeQuests = setOf("w4_mq17"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = emptySet(), questTasksCompleted = emptyMap()))
        act(s, "w4_mq17_access_phantom_terminal_scene")
        assertEquals("gh0st_w4_phantom_intro", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse("access_phantom_terminal" in s.sessionStore.state.value.questTasksCompleted["w4_mq17"].orEmpty())
        val seen = mutableListOf<String>(); finish(s, seen)
        assertTrue("nova_w4_phantom_prompt" in seen)
        assertTrue("access_phantom_terminal" in s.sessionStore.state.value.questTasksCompleted["w4_mq17"].orEmpty())
        restore(s, s.sessionStore.state.value.copy(roomId = "foundry_cooling_springs"))
        val xp = s.sessionStore.state.value.playerXp
        act(s, "w4_mq17_regroup_springs_scene")
        assertEquals("gh0st_w4_springs_intro", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse(s.inventoryService.hasItem("gh0st_override_key"))
        finish(s)
        assertEquals(xp + 800, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["gh0st_override_key"])
        assertTrue("w4_mq17" in s.sessionStore.state.value.completedQuests)
    }

    @Test fun bulkheadMistakeLeaveAndReloadPreserveRetryWithoutAwardingProgress() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_5", hubId = "hub_9_orbital_ring", roomId = "orbital_executive_dock",
            activeQuests = setOf("w5_sq23"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w5_pressure_loss_mapped"), questTasksCompleted = mapOf("w5_sq23" to setOf("map_pressure_loss"))))
        val before = s.sessionStore.state.value
        act(s, "w5_sq23_route_bulkheads")
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("glass") }; compose.waitForIdle()
        assertEquals("w5_bulkheads_retry", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse("ms_w5_breaches_sealed" in s.sessionStore.state.value.completedMilestones)
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("leave") }; compose.waitForIdle(); finish(s)
        assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
        act(s, "w5_sq23_route_bulkheads")
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("correct") }; compose.waitForIdle()
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.activeDialogue == null }
        dismissNarration(s)
        assertFalse("ms_w5_breaches_sealed" in s.sessionStore.state.value.completedMilestones)
        act(s, "w5_sq23_route_bulkheads"); finish(s)
        assertTrue("ms_w5_breaches_sealed" in s.sessionStore.state.value.completedMilestones)
        act(s, "w5_sq23_reopen_dock"); dismissNarration(s)
        assertEquals(before.playerXp + 450, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["mag_boots"])
        val completed = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
            .single { it.name == "dock access priority" }
        assertTrue(completed.completedInspection)
        assertNull(completed.actionEvent)
        compose.runOnIdle { s.exploration.onActionSelected(completed) }; dismissNarration(s)
        assertEquals(before.playerXp + 450, s.sessionStore.state.value.playerXp)
    }

    @Test fun crucibleDefeatCallbackAndReloadAllowTheSameRoundToRestart() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_crucible_arena",
            activeQuests = setOf("w4_sq_crucible"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = emptySet(), questTasksCompleted = emptyMap()))
        fun start() {
            assertTrue("pitmaster_brutus" in s.exploration.uiState.value.npcs)
            compose.runOnIdle { s.exploration.onNpcInteraction("pitmaster_brutus") }; compose.waitForIdle()
            compose.runOnIdle { s.exploration.onDialogueChoiceSelected("opt_brutus_start_round1") }; compose.waitForIdle()
            finish(s); dismissNarration(s)
            assertTrue(s.sessionStore.state.value.pendingBattleJson.isNotBlank())
        }
        start()
        compose.runOnIdle { s.exploration.onCombatDefeat(listOf("magma_drone")) }; compose.waitForIdle(); dismissNarration(s)
        assertTrue(s.sessionStore.state.value.pendingBattleJson.isBlank())
        assertFalse("ms_crucible_round1_cleared" in s.sessionStore.state.value.completedMilestones)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }; dismissNarration(s)
        start()
    }
    @Test fun pendingEndingResumesAfterFreshRuntimeAndCreditsCannotPayTwice() {
        val dir = Files.createTempDirectory("world6-ending-restart").toFile()
        var s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { s.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            restore(s, s.sessionStore.state.value.copy(worldId = "world_6", hubId = "hub_12_singularity", roomId = "source_center",
                activeQuests = setOf("w6_mq30"), completedQuests = emptySet(), completedEvents = emptySet(),
                completedMilestones = setOf("ms_w6_vale_defeated", "ms_w6_god_defeated"),
                questTasksCompleted = mapOf("w6_mq30" to setOf("confront_vale", "defeat_vale", "defeat_god_form"))))
            val xp = s.sessionStore.state.value.playerXp
            val action = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TuningPuzzleAction>().single()
            compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
            requireNotNull(s.exploration.uiState.value.tuningPuzzle).sliders.forEach { compose.runOnIdle { s.exploration.updateTuningSlider(it.id, it.target) } }
            compose.runOnIdle { s.exploration.submitTuningPuzzle() }; compose.waitForIdle()
            assertNotNull(s.cinematicCoordinator.state.value)
            assertFalse("ms_game_complete" in s.sessionStore.state.value.completedMilestones)
            assertEquals(xp, s.sessionStore.state.value.playerXp)
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
            compose.runOnIdle { s.close() }
            s = DesktopAppServices(dir, RecordingAudioDriver())
            assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.cinematicCoordinator.state.value != null }
            repeat(8) {
                if (s.cinematicCoordinator.state.value != null) { compose.runOnIdle { s.exploration.skipCinematic() }; compose.waitForIdle() }
            }
            assertTrue("ms_game_complete" in s.sessionStore.state.value.completedMilestones)
            assertTrue("ms_credits_seen" in s.sessionStore.state.value.completedMilestones)
            assertEquals("source_new_world", s.sessionStore.state.value.roomId)
            assertEquals(xp + 3000, s.sessionStore.state.value.playerXp)
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }; dismissNarration(s)
            compose.runOnIdle { s.exploration.skipCinematic() }; compose.waitForIdle()
            assertEquals(xp + 3000, s.sessionStore.state.value.playerXp)
            assertTrue("ms_credits_seen" in s.sessionStore.state.value.completedMilestones)
        } finally { compose.runOnIdle { s.close() }; dir.deleteRecursively() }
    }

}
