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
import org.junit.Assert.assertTrue
import org.junit.Test

class Hub1CriticalFlowTest {

    @Test
    fun wakeUpCallCompletesFromBunkToJedToTinkering() {
        val harness = Hub1Harness()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_nova_bunk"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_mq01_check_bunk"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_shaft"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("workshop_floor"))

        val jed = harness.dialogue.startDialogue("Jed")
        assertEquals("jed_w1_mq01_intro_1", jed?.current()?.id)
        jed?.advanceUntilFinished()

        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_screen_entered"))

        val state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq01"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq01_complete"))
        assertTrue(state.questTasksCompleted["w1_mq01"].orEmpty().contains("use_tinkering_table"))
        assertTrue(state.inventory["ration_pack"].orZero() >= 1)
    }

    @Test
    fun heavyLiftingStartsAtDockAndCompletesOnRiotGuardVictory() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val bogs = harness.dialogue.startDialogue("Foreman Bogs")
        assertEquals("bogs_w1_sq03_intro_1", bogs?.current()?.id)
        bogs?.advanceUntilFinished()

        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq03_start_loader"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq03_move_cargo"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("riot_guard"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq03"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq03"))
        assertTrue(state.completedMilestones.contains("ms_w1_guardbreak_trained"))
        assertTrue(completedTasks.contains("talk_to_bogs"))
        assertTrue(completedTasks.contains("start_loader"))
        assertTrue(completedTasks.contains("move_cargo"))
        assertTrue(completedTasks.contains("learn_hydraulic_kick"))
        assertTrue(completedTasks.contains("break_training_shield"))
        assertTrue(completedTasks.contains("rescue_trapped_workers"))
    }

    @Test
    fun scavengersStashMakesTradeRowPlayable() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val scrapper = harness.dialogue.startDialogue("Scrapper")
        assertEquals("scrapper_w1_sq01_intro_1", scrapper?.current()?.id)
        scrapper?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("trade_stash"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq01_open_rebel_cache"))

        val turnIn = harness.dialogue.startDialogue("Scrapper")
        assertEquals("scrapper_w1_sq01_turnin_1", turnIn?.current()?.id)
        turnIn?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq01"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq01"))
        assertTrue(state.completedMilestones.contains("ms_w1_sq01_complete"))
        assertTrue(completedTasks.contains("talk_to_scrapper"))
        assertTrue(completedTasks.contains("find_hidden_stash"))
        assertTrue(completedTasks.contains("open_rebel_cache"))
        assertTrue(completedTasks.contains("return_cache_proof"))
        assertTrue(state.inventory["pulse_grenade"].orZero() >= 2)
    }

    @Test
    fun systemFlushMakesMedBayPlayable() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val doc = harness.dialogue.startDialogue("Doc")
        assertEquals("doc_w1_sq02_intro_1", doc?.current()?.id)
        doc?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("medbay_vents"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq02_patch_vent"))

        val turnIn = harness.dialogue.startDialogue("Doc")
        assertEquals("doc_w1_sq02_turnin_1", turnIn?.current()?.id)
        turnIn?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq02"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq02"))
        assertTrue(state.completedMilestones.contains("ms_w1_sq02_complete"))
        assertTrue(completedTasks.contains("talk_to_doc"))
        assertTrue(completedTasks.contains("enter_ventilation_hub"))
        assertTrue(completedTasks.contains("clear_toxic_blockage"))
        assertTrue(completedTasks.contains("return_to_doc"))
        assertTrue(state.inventory["mod_corrosive_rounds"].orZero() >= 1)
    }

    @Test
    fun paperworkDenialZekeOverrideAndBadgeGateProgress() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("checkpoint_queue"))
        val hank = harness.dialogue.startDialogue("Guard Hank")
        assertEquals("hank_w1_mq02_denial_1", hank?.current()?.id)
        hank?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("checkpoint_booth"))
        val zeke = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq02_override_1", zeke?.current()?.id)
        zeke?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_mq02"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_mq02"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq02_complete"))
        assertTrue(completedTasks.contains("approach_admin_gate"))
        assertTrue(completedTasks.contains("request_clearance"))
        assertTrue(completedTasks.contains("meet_zeke"))
        assertTrue(completedTasks.contains("spoof_liability_form"))
        assertTrue(completedTasks.contains("receive_mine_access_badge"))
        assertTrue(state.inventory["mine_access_badge"].orZero() >= 1)
    }

    private class Hub1Harness {
        val store = GameSessionStore()
        val events = EventManager(
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
                onGiveXp = { amount ->
                    store.addXp(amount)
                }
            )
        )
        val dialogue = DialogueService(
            loadDialogue(),
            DialogueConditionEvaluator { condition -> conditionMet(condition, store.state.value) },
            DialogueTriggerHandler { trigger -> events.performActions(DialogueTriggerParser.parse(trigger)) }
        )
    }

    private fun DialogueSession.advanceUntilFinished() {
        while (!isFinished()) {
            advance()
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
                        else -> true
                    }
                }
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
