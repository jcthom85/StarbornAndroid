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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Hub4CriticalFlowTest {

    @Test
    fun upperCityMainQuestProgressionsEndToEnd() {
        val harness = Hub4Harness()

        harness.store.completeQuest("w3_mq12")
        harness.store.setMilestone("ms_w3_mq12_complete")
        harness.events.handleTrigger("quest_stage_complete", EventPayload.QuestStage("w3_mq12"))

        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w3_mq13"))
        assertEquals("spire_laundry_service", state.roomId)

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq13_disable_sensors"))
        assertFalse(harness.store.state.value.questTasksCompleted["w3_mq13"].orEmpty().contains("disable_sensors"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_skypark_dome"))
        val curator = harness.dialogue.startDialogue("Curator")
        assertNotNull(curator)
        assertEquals("curator_w3_mq13_intro", curator?.current()?.id)
        curator?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq13"].orEmpty().contains("blend_in"))

        val zeke = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zeke)
        assertEquals("zeke_w3_mq13_console_intro", zeke?.current()?.id)
        zeke?.advanceUntilFinished()
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq13"].orEmpty().contains("disable_sensors"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq13_enter_lobby"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_mq13"))
        assertTrue(state.completedMilestones.contains("ms_w3_mq13_complete"))
        assertTrue(state.activeQuests.contains("w3_mq14"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_prism_gallery"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_archive_vault"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq14"].orEmpty().contains("enter_archive"))

        val orion = harness.dialogue.startDialogue("Orion")
        assertNotNull(orion)
        assertEquals("orion_w3_mq14_puzzle_intro", orion?.current()?.id)
        orion?.advanceUntilFinished()
        state = harness.store.state.value
        assertFalse(state.questTasksCompleted["w3_mq14"].orEmpty().contains("solve_light_puzzle"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq14_solve_light_puzzle"))
        assertFalse(harness.store.state.value.questTasksCompleted["w3_mq14"].orEmpty().contains("solve_light_puzzle"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq14_read_containment_field"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq14_read_prism_shutters"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq14_trace_command_tethers"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w3_containment_field_read"))
        assertTrue(state.completedMilestones.contains("ms_w3_prism_shutters_read"))
        assertTrue(state.completedMilestones.contains("ms_w3_command_tethers_traced"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq14_solve_light_puzzle"))
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w3_mq14"].orEmpty().contains("solve_light_puzzle"))
        assertEquals("claim_lens", state.questStageById["w3_mq14"])

        val lensWarning = harness.dialogue.startDialogue("Zeke")
        assertNotNull(lensWarning)
        assertEquals("zeke_w3_mq14_lens_warning", lensWarning?.current()?.id)
        lensWarning?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_mq14"))
        assertTrue(state.completedMilestones.contains("ms_w3_mq14_complete"))
        assertTrue(state.inventory["the_lens"].orZero() >= 1)
        assertTrue(state.unlockedSkills.contains("source_art_scan"))
        assertTrue(state.activeQuests.contains("w3_mq15"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_scan_archive_tethers"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_scan_prism_alarm_chords"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_scan_drone_paths"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_scan_shield_gap"))
        assertTrue(harness.messages.any { it.contains("command tethers", ignoreCase = true) })
        assertTrue(harness.messages.any { it.contains("service route, drone alcove, roof", ignoreCase = true) })
        assertTrue(harness.messages.any { it.contains("drone nest", ignoreCase = true) })
        assertTrue(harness.messages.any { it.contains("Administrator's targeting lattice", ignoreCase = true) })

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_drone_test_alcove"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_landing_pad_roof"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq15"].orEmpty().contains("reach_landing_pad"))

        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("administrator_boss"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w3_mq15"].orEmpty().contains("defeat_administrator"))
        assertEquals("launch_to_foundry", state.questStageById["w3_mq15"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq15_launch_astra"))
        state = harness.store.state.value
        assertFalse(state.completedQuests.contains("w3_mq15"))
        assertFalse(state.roomId == "foundry_slag_landing")
        assertTrue(harness.messages.any { it.contains("scan the shield gap", ignoreCase = true) })

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_scan_shield_gap"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq15"].orEmpty().contains("scan_shield_gap"))
        assertTrue(harness.messages.any { it.contains("one launch window", ignoreCase = true) })

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq15_launch_astra"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_mq15"))
        assertTrue(state.completedMilestones.contains("ms_w3_mq15_complete"))
        assertTrue(state.completedMilestones.contains("ms_w4_access_unlocked"))
        assertEquals("foundry_slag_landing", state.roomId)
    }

    @Test
    fun upperCitySideQuestFlows() {
        val harness = Hub4Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_steal_ledger"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_sq14"))
        assertTrue(state.inventory["encrypted_ledger"].orZero() >= 1)
        assertTrue(state.completedMilestones.contains("ms_w3_blackmail_unlocked"))

        val terminal = harness.dialogue.startDialogue("Lab Terminal")
        assertNotNull(terminal)
        assertEquals("lab_terminal_w3_sq15_intro", terminal?.current()?.id)
        terminal?.advanceUntilFinished()
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_sq15"))
        assertTrue(state.inventory["phase_rounds"].orZero() >= 1)
        assertTrue(state.completedMilestones.contains("ms_w3_prototype_tested"))
    }

    private class Hub4Harness(initialState: GameSessionState? = null) {
        val store = GameSessionStore()
        val events: EventManager
        val dialogue: DialogueService
        val messages = mutableListOf<String>()

        init {
            initialState?.let(store::restore)
            events = EventManager(
                events = loadEvents(),
                sessionStore = store,
                eventHooks = EventHooks(
                    onMessage = { messages += it },
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
