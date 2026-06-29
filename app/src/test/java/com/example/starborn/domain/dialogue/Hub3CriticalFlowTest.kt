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

class Hub3CriticalFlowTest {

    @Test
    fun world3MainQuestProgressionsEndToEnd() {
        val harness = Hub3Harness()

        // Setup World 3 start state (transitioned from World 2 launch)
        harness.store.setRoom("spire_sewers_landing")
        harness.store.completeQuest("w2_mq05")
        harness.store.setMilestone("ms_w2_mq05_complete")
        harness.store.startQuest("w3_mq11")
        harness.store.setTrackedQuest("w3_mq11")

        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w3_mq11"))
        assertEquals("spire_sewers_landing", state.roomId)

        // --- MQ11: Homecoming ---
        // 1. Defeat sewer crawler at landing pad
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("sewer_crawler"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq11"].orEmpty().contains("clear_landing"))

        // 2. Reach Vent Output
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_vent_output"))
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w3_mq11"].orEmpty().contains("reach_vent"))
        // Verify stage advanced to find_safehouse
        assertEquals("find_safehouse", state.questStageById["w3_mq11"])

        // 3. Find Zeke's apartment
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("spire_zekes_apartment"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq11"].orEmpty().contains("find_apartment"))

        // 4. Talk to Zeke to establish shield
        val zekeSafehouse = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zekeSafehouse)
        assertEquals("zeke_w3_mq11_safehouse_intro", zekeSafehouse?.current()?.id)

        // Advance to Orion shield trigger
        zekeSafehouse?.advance()
        assertEquals("zeke_w3_mq11_safehouse_next", zekeSafehouse?.current()?.id)
        zekeSafehouse?.advance()
        assertEquals("zeke_w3_mq11_safehouse_end", zekeSafehouse?.current()?.id)

        // Ending dialogue triggers player_action:w3_mq11_establish_shield
        zekeSafehouse?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w3_mq11"].orEmpty().contains("establish_shield"))
        assertTrue(state.completedQuests.contains("w3_mq11"))
        assertTrue(state.completedMilestones.contains("ms_w3_mq11_complete"))
        assertTrue(state.activeQuests.contains("w3_mq12"))

        // --- MQ12: The Plan ---
        // 1. Talk to Jax
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_talk_jax"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("talk_jax"))

        // Workstation blueprints should stay locked until the heist prep pieces are gathered.
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_hack_blueprints"))
        assertFalse(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("hack_blueprints"))

        // 2. Study patrol timing at the underrail platform
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_map_patrols"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("map_patrols"))

        // 3. Pull security records at the plaza kiosk
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_interrogate_guard"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("interrogate_guard"))

        // 4. Copy service badge patterns at the elevator service gate
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_copy_badges"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("copy_badges"))

        // 5. Source disguise materials at the safehouse roof
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_source_disguises"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("source_disguises"))

        // 6. Hack blueprints at workstation
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_hack_blueprints"))
        assertTrue(harness.store.state.value.questTasksCompleted["w3_mq12"].orEmpty().contains("hack_blueprints"))

        // 7. Assemble at planning table
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_mq12_assemble_planning"))
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w3_mq12"].orEmpty().contains("assemble_table"))
        assertTrue(state.completedQuests.contains("w3_mq12"))
        assertTrue(state.completedMilestones.contains("ms_w3_mq12_complete"))
    }

    @Test
    fun world3SideQuestsFlows() {
        val harness = Hub3Harness()
        harness.store.completeQuest("w3_mq11")

        // --- SQ11: Neon Fix (Jax) ---
        // Inspect sign to trigger quest accept
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq11_inspect_sign"))
        assertTrue(harness.store.state.value.activeQuests.contains("w3_sq11"))

        // Give player the sign core
        harness.store.setInventory(mapOf("neon_sign_core" to 1))

        // Install core
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq11_install_core"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_sq11"))
        assertTrue(state.completedMilestones.contains("ms_w3_streetwise_unlocked"))

        // --- SQ12: Cold Case (Gh0st) ---
        harness.store.startQuest("w3_sq12")

        // Access terminal
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq12_access_terminal"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_sq12"))
        assertTrue(state.completedMilestones.contains("ms_w3_ghost_skill_unlocked"))

        // --- SQ13: Night Market Run (Mika) ---
        harness.store.startQuest("w3_sq13")

        // Fix lanterns
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq13_fix_market_lights"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w3_sq13"))
        assertTrue(state.completedMilestones.contains("ms_w3_market_lit"))
        assertTrue(state.inventory["neon_band"].orZero() >= 1)
    }

    private class Hub3Harness(initialState: GameSessionState? = null) {
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
                    onSystemTutorial = { _, _, _, done -> done() },
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
                    onTakeItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val available = current[itemId].orZero()
                        val requested = quantity.coerceAtLeast(1)
                        if (available < requested) {
                            false
                        } else {
                            val remaining = available - requested
                            val next = if (remaining > 0) {
                                current + (itemId to remaining)
                            } else {
                                current - itemId
                            }
                            store.setInventory(next)
                            true
                        }
                    },
                    onGiveXp = { amount -> store.addXp(amount) },
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
                        "milestone" -> value in state.completedMilestones
                        "milestone_set" -> value in state.completedMilestones
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
                        "quest_stage_not" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId == null || stageId == null ||
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
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
