package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Hub6CriticalFlowTest {

    @Test
    fun assemblyLineMainQuestProgressionsEndToEnd() {
        val harness = Hub6Harness()

        harness.store.startQuest("w4_mq17")
        harness.store.setQuestTaskCompleted("w4_mq17", "access_phantom_terminal", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq17_regroup_springs"))

        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq17"))
        assertTrue(state.activeQuests.contains("w4_mq18"))
        assertEquals("foundry_conveyor_belt", state.roomId)

        val floor = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(floor)
        assertEquals("gh0st_w4_mq18_floor_intro", floor?.current()?.id)
        floor?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq18"].orEmpty().contains("navigate_conveyors"))

        val prototypes = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(prototypes)
        assertEquals("gh0st_w4_mq18_prototypes", prototypes?.current()?.id)
        prototypes?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w4_mq18"].orEmpty().contains("defeat_prototypes"))
        assertEquals("matrix_overload", state.questStageById["w4_mq18"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq18_overload_matrix"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq18"))
        assertTrue(state.activeQuests.contains("w4_mq19"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("foundry_forge_anvil"))
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq19"].orEmpty().contains("reach_core_chamber"))

        val puzzle = harness.dialogue.startDialogue("Orion")
        assertNotNull(puzzle)
        assertEquals("orion_w4_mq19_puzzle_intro", puzzle?.current()?.id)
        puzzle?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq19"].orEmpty().contains("solve_conveyor_puzzle").not())
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq19_read_pulse_board"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq19_read_grease_marks"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq19_throw_override_paddle"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq19_starve_breath_pistons"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq19_open_anvil_cradle"))
        assertEquals("claim_anvil", harness.store.state.value.questStageById["w4_mq19"])

        val anvil = harness.dialogue.startDialogue("Nova")
        assertNotNull(anvil)
        assertEquals("nova_w4_mq19_anvil_intro", anvil?.current()?.id)
        anvil?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq19"))
        assertTrue(state.inventory["the_anvil"].orZero() >= 1)
        assertTrue(state.unlockedSkills.contains("source_art_construct"))
        assertTrue(state.activeQuests.contains("w4_mq20"))

        val rylos = harness.dialogue.startDialogue("Rylos")
        assertNotNull(rylos)
        assertEquals("rylos_w4_mq20_intro", rylos?.current()?.id)
        rylos?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq20"].orEmpty().contains("confront_rylos"))

        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("titan_walker_boss"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w4_mq20"].orEmpty().contains("defeat_titan_walker"))
        assertEquals("steal_and_escape", state.questStageById["w4_mq20"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq20_steal_engine"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq20_steal_arrays"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq20_escape_launch"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq20"))
        assertTrue(state.completedMilestones.contains("ms_w4_mq20_complete"))
        assertTrue(state.completedMilestones.contains("ms_w5_access_unlocked"))
        assertTrue(state.inventory["deep_core_engine"].orZero() >= 1)
        assertTrue(state.inventory["phase_cutter_arrays"].orZero() >= 1)
    }

    @Test
    fun assemblyLineSideQuestFlows() {
        val harness = Hub6Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq17_trace_ping"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq17_find_worker"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq17_vent_safe_route"))
        assertTrue(harness.store.state.value.completedQuests.contains("w4_sq17"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq18_stop_intake"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq18_salvage_mechs"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq18_build_bypass"))
        assertTrue(harness.store.state.value.completedQuests.contains("w4_sq18"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_read_rejection_codes"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_reprogram_units"))
        assertTrue(harness.store.state.value.completedQuests.contains("w4_sq19").not())
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_release_units"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_sq19"))
        assertTrue(state.unlockedSkills.contains("gh0st_harden"))
        assertTrue(state.completedMilestones.contains("ms_w4_quality_control_complete"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq20_map_hazards"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq20_survive_waves"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq20_steal_overclock"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_sq20"))
        assertTrue(state.inventory["thermal_clip"].orZero() >= 1)
        assertTrue(state.completedMilestones.contains("ms_w4_overclocked_complete"))
        assertTrue(state.questTasksCompleted["w4_sq19"].orEmpty().containsAll(listOf("read_rejection_codes", "resolve_units", "release_units")))
        assertTrue(state.questTasksCompleted["w4_sq20"].orEmpty().containsAll(listOf("map_hazards", "survive_waves", "steal_overclock")))
        assertTrue(state.questTasksCompleted["w4_sq17"].orEmpty().containsAll(listOf("trace_ping", "find_worker", "vent_safe_route")))
        assertTrue(state.questTasksCompleted["w4_sq18"].orEmpty().containsAll(listOf("stop_intake", "salvage_mechs", "build_bypass")))
    }

    private class Hub6Harness(initialState: GameSessionState? = null) {
        val store = GameSessionStore()
        val events: EventManager
        val dialogue: DialogueService

        init {
            initialState?.let(store::restore)
            events = EventManager(
                events = loadEvents(),
                sessionStore = store,
                eventHooks = EventHooks(
                    onQuestTaskUpdated = { questId, taskId ->
                        if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                            store.setQuestTaskCompleted(questId, taskId, true)
                        }
                    },
                    onQuestStageAdvanced = { questId, stageId ->
                        if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                            store.setQuestStage(questId, stageId)
                        }
                    },
                    onGiveItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val next = current + (itemId to (current[itemId].orZero() + quantity.coerceAtLeast(1)))
                        store.setInventory(next)
                    },
                    onQuestCompleted = ::handleQuestCompleted
                )
            )
            dialogue = DialogueService(
                loadDialogue(),
                DialogueConditionEvaluator { condition -> conditionMet(condition, store.state.value) },
                DialogueTriggerHandler { trigger -> events.performActions(DialogueTriggerParser.parse(trigger)) }
            )
        }

        private fun handleQuestCompleted(questId: String?) {
            if (!questId.isNullOrBlank()) {
                events.handleTrigger("quest_stage_complete", EventPayload.QuestStage(questId))
            }
        }
    }

    private companion object {
        private val moshi = MoshiProvider.instance

        private fun conditionMet(raw: String?, state: GameSessionState): Boolean {
            if (raw.isNullOrBlank()) return true
            return raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .all { token ->
                    val parts = token.split(':', limit = 2)
                    val type = parts[0].trim().lowercase()
                    val value = parts.getOrNull(1)?.trim().orEmpty()
                    when (type) {
                        "milestone", "milestone_set" -> value in state.completedMilestones
                        "milestone_not_set" -> value !in state.completedMilestones
                        "quest", "quest_active" -> value in state.activeQuests
                        "quest_not_started" -> value !in state.activeQuests &&
                            value !in state.completedQuests &&
                            value !in state.failedQuests
                        "quest_completed" -> value in state.completedQuests
                        "quest_not_completed" -> value !in state.completedQuests
                        "quest_stage" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId != null && stageId != null &&
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
                        }
                        "quest_task_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId != null && taskId != null &&
                                state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "quest_task_not_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId == null || taskId == null ||
                                !state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "item" -> value in state.inventory && state.inventory[value].orZero() > 0
                        "item_not" -> value !in state.inventory || state.inventory[value].orZero() <= 0
                        else -> true
                    }
                }
        }

        private fun parseQuestPair(raw: String): Pair<String?, String?> {
            val parts = raw.split(':', limit = 2)
            val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
            val value = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
            return questId to value
        }

        private fun Int?.orZero(): Int = this ?: 0

        private fun loadDialogue(): List<DialogueLine> {
            val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
            val adapter = moshi.adapter<List<DialogueLine>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/dialogue.json").readText()))
        }

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            val adapter = moshi.adapter<List<GameEvent>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText()))
        }
    }
}

private fun DialogueSession.advanceUntilFinished() {
    while (!isFinished()) {
        advance()
    }
}
