package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.TuningPuzzleAction
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.presentation.buildInlineActionPlan
import com.example.starborn.feature.exploration.presentation.resolveRoomDescription
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World56ReadinessPublicTest {
    @get:Rule val compose = createComposeRule()
    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world56-readiness").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { s.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { compose.runOnIdle { s.close() }; dir.deleteRecursively() }
    }
    private fun seed(s: DesktopAppServices, room: String) = s.sessionStore.state.value.copy(
        worldId = if (room.startsWith("source_")) "world_6" else "world_5",
        hubId = if (room.startsWith("source_")) "hub_12_singularity" else "hub_9_orbital_ring",
        roomId = room, activeQuests = emptySet(), completedQuests = emptySet(),
        completedEvents = emptySet(), completedMilestones = emptySet(), roomStates = emptyMap(),
        questTasksCompleted = emptyMap(), questStageById = emptyMap(),
        unlockedExits = emptySet(), visitedNodes = emptySet()
    )
    private fun restore(s: DesktopAppServices, state: GameSessionState) {
        compose.runOnIdle { s.sessionStore.restore(state) }
        compose.waitUntil(10000) {
            val ui = s.exploration.uiState.value
            !ui.isLoading && ui.currentRoom?.id == state.roomId &&
                ui.completedEvents == s.sessionStore.state.value.completedEvents &&
                ui.completedMilestones == s.sessionStore.state.value.completedMilestones
        }
        compose.waitForIdle()
        // A restored story arrival can queue a linear companion scene before room input.
        repeat(30) {
            if (s.exploration.uiState.value.activeDialogue != null) {
                assertTrue("Unexpected fixture choice", s.exploration.uiState.value.dialogueChoices.isEmpty())
                compose.runOnIdle { s.exploration.advanceDialogue() }; compose.waitForIdle()
            }
        }
        dismiss(s)
    }
    private fun dismiss(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }; compose.waitForIdle()
        }
    }
    private fun act(s: DesktopAppServices, event: String) {
        assertNull("$event blocked by dialogue", s.exploration.uiState.value.activeDialogue)
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }
        compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
        compose.waitUntil(5000) { event in s.exploration.uiState.value.completedEvents && s.exploration.uiState.value.completedMilestones == s.sessionStore.state.value.completedMilestones }
        assertTrue("$event: quests=${s.sessionStore.state.value.activeQuests}; milestones=${s.sessionStore.state.value.completedMilestones}; narration=${s.exploration.uiState.value.narrationPrompt}; hints=${s.exploration.uiState.value.actionHints}", event in s.sessionStore.state.value.completedEvents)
    }
    private fun prose(s: DesktopAppServices): String {
        val ui = s.exploration.uiState.value
        return resolveRoomDescription(ui.currentRoom, ui.roomState, ui.completedMilestones, false, ui.completedEvents)!!
    }

    @Test fun bothFilmCachesBecomeOrdinaryEmptyProseAndStayEmptyAfterReload() = runWith { s ->
        for ((room, event, item) in listOf(
            Triple("orbital_solarium", "find_vhs_tape_09", "vhs_tape_09"),
            Triple("source_memory_bridge_span", "find_vhs_tape_10", "vhs_tape_10")
        )) {
            restore(s, seed(s, room))
            val label = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }.name
            val before = s.sessionStore.state.value.inventory[item] ?: 0
            act(s, event)
            compose.waitUntil(5000) { event in s.exploration.uiState.value.completedEvents }
            dismiss(s)
            assertEquals(before + 1, s.sessionStore.state.value.inventory[item])
            assertTrue(prose(s).contains("empty"))
            assertFalse(s.exploration.uiState.value.actions.any { it.name == label })
            val ui = s.exploration.uiState.value
            val plan = requireNotNull(buildInlineActionPlan(prose(s), ui.actions, ui.actionHints, ui.currentRoom, ui.roomState))
            assertFalse(plan.segments.any { plan.description.substring(it.start, it.end).contains(label, true) })
            val saved = s.sessionStore.state.value
            assertTrue(s.saveManager.saveGame(9, saved))
            assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
            assertFalse(s.exploration.uiState.value.actions.any { it.name == label })
            assertTrue(prose(s).contains("empty"))
            assertEquals(saved.inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun restoredDockingNarrativeAppearsAndDoesNotReplayItsReward() = runWith { s ->
        restore(s, seed(s, "orbital_executive_dock").copy(
            activeQuests = setOf("w5_mq21"),
            questTasksCompleted = mapOf("w5_mq21" to setOf("approach_halo", "shoot_down_fighters", "force_dock"))
        ))
        val before = s.sessionStore.state.value.playerXp
        act(s, "w5_mq21_hack_airlock")
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
        val shown = mutableListOf<String>()
        // Arrival salvage can be queued before the completion aftermath; read both.
        repeat(20) {
            s.exploration.uiState.value.narrationPrompt?.let { prompt ->
                shown += prompt.message
                compose.runOnIdle { s.exploration.dismissNarration() }; compose.waitForIdle()
            }
        }
        assertTrue(shown.joinToString("\n"), shown.any { it.contains("Gh0st breaks Thorne's old cipher") })
        assertTrue("w5_mq21" in s.sessionStore.state.value.completedQuests)
        assertTrue("w5_mq22" in s.sessionStore.state.value.activeQuests)
        assertEquals(before + 1000, s.sessionStore.state.value.playerXp)
        dismiss(s)
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == "sealed airlock" }
        assertTrue(action.completedInspection)
        compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
        assertEquals(before + 1000, s.sessionStore.state.value.playerXp)
    }

    @Test fun anchorAndRefusalOrdersChangeProseOnlyWhenTheActualCrossingIsOpened() = runWith { s ->
        for (order in listOf(listOf("zeke", "ghost", "orion", "nova"), listOf("nova", "orion", "ghost", "zeke"))) {
            restore(s, seed(s, "source_memory_bridge").copy(activeQuests = setOf("w6_mq28")))
            for (anchor in order) { act(s, "w6_mq28_anchor_$anchor"); dismiss(s) }
            assertTrue(prose(s).contains("All four hold"))
            assertNotEquals(true, s.sessionStore.state.value.roomStates["source_memory_bridge"]?.get("bridge_built"))
            act(s, "w6_mq28_build_bridge"); dismiss(s)
            assertTrue(prose(s).contains("solid crossing"))
            assertEquals(true, s.sessionStore.state.value.roomStates["source_memory_bridge"]?.get("bridge_built"))
        }
        for (order in listOf(listOf("jed", "astra", "foundry"), listOf("foundry", "astra", "jed"))) {
            restore(s, seed(s, "source_memory_stair").copy(activeQuests = setOf("w6_mq29")))
            for (revision in order) { act(s, "w6_mq29_refuse_${revision}_revision"); dismiss(s) }
            assertTrue("$order: ${prose(s)} / ${s.sessionStore.state.value.roomStates}", prose(s).contains("steadied enough to climb"))
            assertNotEquals(true, s.sessionStore.state.value.roomStates["source_memory_stair"]?.get("ascent_open"))
            act(s, "w6_mq29_climb_stair"); dismiss(s)
            assertTrue(prose(s).contains("eastern ascent open"))
            assertEquals(true, s.sessionStore.state.value.roomStates["source_memory_stair"]?.get("ascent_open"))
        }
    }

    @Test fun finalFocusStillRequiresBothBossesAndAftermathSurvivesRestore() = runWith { s ->
        for (second in listOf(false, true)) {
            val tasks = setOf("confront_vale", "defeat_vale") + if (second) setOf("defeat_god_form") else emptySet()
            val receipts = setOf("w6_mq30_defeat_vale") + if (second) setOf("w6_mq30_defeat_god") else emptySet()
            restore(s, seed(s, "source_center").copy(activeQuests = setOf("w6_mq30"),
                questTasksCompleted = mapOf("w6_mq30" to tasks), completedEvents = receipts))
            assertTrue(prose(s).contains(if (second) "Key answers" else "remaining form waits"))
            val focus = s.exploration.uiState.value.actions.filterIsInstance<TuningPuzzleAction>().single()
            compose.runOnIdle { s.exploration.onActionSelected(focus) }; compose.waitForIdle()
            assertEquals(second, s.exploration.uiState.value.tuningPuzzle != null)
            assertFalse("ms_game_complete" in s.sessionStore.state.value.completedMilestones)
            if (second) compose.runOnIdle { s.exploration.dismissTuningPuzzle() }
            dismiss(s)
        }
    }
}
