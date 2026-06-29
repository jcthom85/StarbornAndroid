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

class Hub5CriticalFlowTest {

    @Test
    fun foundrySlagPitsMainQuestProgressionsEndToEnd() {
        val harness = Hub5Harness()

        harness.store.startQuest("w3_mq15")
        harness.store.setQuestTaskCompleted("w3_mq15", "defeat_administrator", true)
        harness.store.setQuestTaskCompleted("w3_mq15", "scan_shield_gap", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq15_launch_astra"))

        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_mq15"))
        assertTrue(state.activeQuests.contains("w4_mq16"))
        assertEquals("foundry_slag_landing", state.roomId)

        val gh0stLanding = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(gh0stLanding)
        assertEquals("gh0st_w4_landing_intro", gh0stLanding?.current()?.id)
        gh0stLanding?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq16"].orEmpty().contains("land_shelf"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq16_cross_slag_river"))
        assertTrue(harness.store.state.value.questTasksCompleted["w4_mq16"].orEmpty().contains("cross_slag_river"))

        val zekeVents = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zekeVents)
        assertEquals("zeke_w4_vents_intro", zekeVents?.current()?.id)
        zekeVents?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w4_mq16"].orEmpty().contains("hack_cooling_vents"))
        assertEquals("open_airlock", state.questStageById["w4_mq16"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq16_enter_airlock"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq16"))
        assertTrue(state.completedMilestones.contains("ms_w4_mq16_complete"))
        assertTrue(state.inventory["heat_liner"].orZero() >= 1)
        assertTrue(state.activeQuests.contains("w4_mq17"))

        val phantom = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(phantom)
        assertEquals("gh0st_w4_phantom_intro", phantom?.current()?.id)
        phantom?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w4_mq17"].orEmpty().contains("access_phantom_terminal"))
        assertEquals("cooling_springs", state.questStageById["w4_mq17"])

        val springs = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(springs)
        assertEquals("gh0st_w4_springs_intro", springs?.current()?.id)
        springs?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq17"))
        assertTrue(state.completedMilestones.contains("ms_w4_mq17_complete"))
        assertTrue(state.inventory["gh0st_override_key"].orZero() >= 1)
        assertTrue(state.unlockedSkills.contains("gh0st_phase_counter"))
    }

    @Test
    fun foundrySideQuestFlows() {
        val harness = Hub5Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq16_destroy_crate_alpha"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq16_destroy_crate_beta"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq16_destroy_crate_gamma"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_sq16"))
        assertTrue(state.inventory["explosive_tip"].orZero() >= 1)
        assertTrue(state.completedMilestones.contains("ms_w4_sabotage_complete"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq17_find_worker"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_sq17"))
        assertTrue(state.inventory["coolant_system"].orZero() >= 1)
        assertTrue(state.completedMilestones.contains("ms_w4_lost_worker_complete"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq18_salvage_mechs"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_sq18"))
        assertTrue(state.completedMilestones.contains("ms_w4_scavenger_unlocked"))
    }

    private class Hub5Harness(initialState: GameSessionState? = null) {
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
