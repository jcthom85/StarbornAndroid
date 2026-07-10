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

class Hub7CriticalFlowTest {

    @Test
    fun orbitalRingMainQuestProgressionsEndToEnd() {
        val harness = Hub7Harness()

        harness.store.startQuest("w4_mq20")
        harness.store.setQuestTaskCompleted("w4_mq20", "steal_engine", true)
        harness.store.setQuestTaskCompleted("w4_mq20", "steal_arrays", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_mq20_escape_launch"))

        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w4_mq20"))
        assertTrue(state.activeQuests.contains("w5_mq21"))
        assertEquals("orbital_executive_dock", state.roomId)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("orbital_executive_dock"))
        assertTrue(harness.store.state.value.questTasksCompleted["w5_mq21"].orEmpty().contains("approach_halo"))

        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("orbital_fighter"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        assertEquals("forced_docking", harness.store.state.value.questStageById["w5_mq21"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq21_force_dock"))
        val airlock = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(airlock)
        assertEquals("gh0st_w5_mq21_airlock", airlock?.current()?.id)
        airlock?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_mq21"))
        assertTrue(state.completedMilestones.contains("ms_w5_mq21_complete"))
        assertTrue(state.activeQuests.contains("w5_mq22"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq22_cross_solarium"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("compliance_officer", "null_g_drone"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        assertEquals("zero_g_descent", harness.store.state.value.questStageById["w5_mq22"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq22_traverse_shaft"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq22_access_mainframe"))
        val thorne = harness.dialogue.startDialogue("Thorne")
        assertNotNull(thorne)
        assertEquals("thorne_w5_mq22_intro", thorne?.current()?.id)
        thorne?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_mq22"))
        assertTrue(state.completedMilestones.contains("ms_w5_mq22_complete"))
        assertTrue(state.inventory["grav_boots"].orZero() >= 1)
    }

    @Test
    fun orbitalRingSideQuestFlows() {
        val harness = Hub7Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq21_find_redactions"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq21_director_logs"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq21_publish_logs"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq21"))
        assertTrue(state.unlockedSkills.contains("corporate_insight"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_trace_false_sun"))
        val maintenance = harness.dialogue.startDialogue("Maintenance Bot")
        assertNotNull(maintenance)
        maintenance?.advanceUntilFinished()
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_restore_gardens"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq22"))
        assertTrue(state.unlockedSkills.contains("orion_sunbeam"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq23_map_pressure_loss"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq23_vacuum_seal"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq23_reopen_dock"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq23"))
        assertTrue(state.inventory["mag_boots"].orZero() >= 1)

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_trace_purge"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_recover_backup"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_restore_guardian"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq24"))
        assertTrue(state.unlockedSkills.contains("data_shield"))
        assertTrue(state.questTasksCompleted["w5_sq21"].orEmpty().containsAll(listOf("find_redactions", "read_logs", "publish_logs")))
        assertTrue(state.questTasksCompleted["w5_sq22"].orEmpty().containsAll(listOf("trace_false_sun", "realign_mirrors", "restore_gardens")))
        assertTrue(state.questTasksCompleted["w5_sq23"].orEmpty().containsAll(listOf("map_pressure_loss", "seal_breaches", "reopen_dock")))
        assertTrue(state.questTasksCompleted["w5_sq24"].orEmpty().containsAll(listOf("trace_purge", "recover_backup", "restore_guardian")))
    }

    private class Hub7Harness {
        val store = GameSessionStore()
        val events: EventManager
        val dialogue: DialogueService

        init {
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
                        store.setInventory(current + (itemId to (current[itemId].orZero() + quantity.coerceAtLeast(1))))
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
            return raw.split(',').map(String::trim).filter(String::isNotEmpty).all { token ->
                val parts = token.split(':', limit = 2)
                val type = parts[0].lowercase()
                val value = parts.getOrNull(1).orEmpty()
                when (type) {
                    "milestone", "milestone_set" -> value in state.completedMilestones
                    "milestone_not_set" -> value !in state.completedMilestones
                    "quest", "quest_active" -> value in state.activeQuests
                    "quest_not_started" -> value !in state.activeQuests && value !in state.completedQuests && value !in state.failedQuests
                    "quest_completed" -> value in state.completedQuests
                    "quest_not_completed" -> value !in state.completedQuests
                    "quest_task_done" -> parseQuestPair(value).let { (questId, taskId) ->
                        questId != null && taskId != null && state.questTasksCompleted[questId].orEmpty().contains(taskId)
                    }
                    "quest_task_not_done" -> parseQuestPair(value).let { (questId, taskId) ->
                        questId == null || taskId == null || !state.questTasksCompleted[questId].orEmpty().contains(taskId)
                    }
                    else -> true
                }
            }
        }

        private fun parseQuestPair(raw: String): Pair<String?, String?> {
            val parts = raw.split(':', limit = 2)
            return parts.getOrNull(0)?.takeIf(String::isNotBlank) to parts.getOrNull(1)?.takeIf(String::isNotBlank)
        }

        private fun Int?.orZero(): Int = this ?: 0

        private fun loadDialogue(): List<DialogueLine> {
            val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
            return requireNotNull(moshi.adapter<List<DialogueLine>>(type).fromJson(File("src/main/assets/dialogue.json").readText()))
        }

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            return requireNotNull(moshi.adapter<List<GameEvent>>(type).fromJson(File("src/main/assets/events.json").readText()))
        }
    }
}

private fun DialogueSession.advanceUntilFinished() {
    while (!isFinished()) advance()
}
