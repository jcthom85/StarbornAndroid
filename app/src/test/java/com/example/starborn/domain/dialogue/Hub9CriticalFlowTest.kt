package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Hub9CriticalFlowTest {

    @Test
    fun eventHorizonMainQuestProgressionsEndToEnd() {
        val harness = Hub9Harness()

        harness.store.startQuest("w5_mq25")
        harness.store.setQuestTaskCompleted("w5_mq25", "defeat_avatar", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq25_enter_tear"))

        var state = harness.store.state.value
        assertEquals("source_campfire", state.roomId)
        assertTrue(state.activeQuests.contains("w6_mq26"))

        harness.winEncounter("manager_projection")
        harness.winEncounter("endless_war_echo")
        harness.winEncounter("silent_shore_wraith")
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w6_mq26"].orEmpty().containsAll(listOf("rescue_zeke", "rescue_gh0st", "rescue_orion")))

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq26_reassemble"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w6_mq26"))
        assertTrue(state.inventory["key_relic"].orZero() >= 1)
        assertTrue(state.activeQuests.contains("w6_mq27"))
        assertEquals("source_echo_mines", state.roomId)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("source_echo_mines"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq27_evade_manager"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("source_echo_elevator"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w6_mq27"))
        assertTrue(state.activeQuests.contains("w6_mq28"))
        assertEquals("source_memory_bridge", state.roomId)

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq28_build_bridge"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq28_final_banter"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq28_reach_singularity"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w6_mq28"))
        assertTrue(state.completedMilestones.contains("ms_w6_mq28_complete"))
        assertEquals("source_memory_stair", state.roomId)
    }

    @Test
    fun eventHorizonClosureSideQuests() {
        val harness = Hub9Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq26_jed_echo"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_delete_record"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq28_elara_song"))

        val state = harness.store.state.value
        assertTrue(state.completedQuests.containsAll(listOf("w6_sq26", "w6_sq27", "w6_sq28")))
        assertTrue(state.unlockedSkills.containsAll(listOf("nova_legacy", "zeke_unshackled", "gh0st_source_balance")))
    }

    private class Hub9Harness {
        val store = GameSessionStore()
        val events: EventManager

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
                        val inventory = store.state.value.inventory
                        store.setInventory(inventory + (itemId to (inventory[itemId].orZero() + quantity.coerceAtLeast(1))))
                    },
                    onQuestCompleted = ::handleQuestCompleted
                )
            )
        }

        fun winEncounter(enemyId: String) {
            events.handleTrigger(
                "encounter_victory",
                EventPayload.EncounterOutcome(
                    enemyIds = listOf(enemyId),
                    outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
                )
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

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            return requireNotNull(moshi.adapter<List<GameEvent>>(type).fromJson(File("src/main/assets/events.json").readText()))
        }

        private fun Int?.orZero(): Int = this ?: 0
    }
}
