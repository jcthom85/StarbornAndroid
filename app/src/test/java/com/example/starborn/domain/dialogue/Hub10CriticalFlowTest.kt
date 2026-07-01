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

class Hub10CriticalFlowTest {

    @Test
    fun singularityMainQuestCompletesCampaignAndCredits() {
        val harness = Hub10Harness()

        harness.store.startQuest("w6_mq28")
        harness.store.setQuestTaskCompleted("w6_mq28", "final_banter", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq28_reach_singularity"))

        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w6_mq29"))
        assertEquals("source_memory_stair", state.roomId)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("source_memory_stair"))
        assertTrue(harness.store.state.value.questTasksCompleted["w6_mq29"].orEmpty().contains("climb_stair").not())

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq29_refuse_jed_revision"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq29_refuse_astra_revision"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq29_refuse_foundry_revision"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq29_climb_stair"))
        assertTrue(harness.store.state.value.questTasksCompleted["w6_mq29"].orEmpty().contains("climb_stair"))
        harness.winEncounter("source_shadow", "distorted_sentinel", "glitch_hound")
        assertEquals("spire_arena", harness.store.state.value.questStageById["w6_mq29"])

        harness.winEncounter("memory_leak", "nightmare_guard")
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("source_center"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w6_mq29"))
        assertTrue(state.activeQuests.contains("w6_mq30"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq30_confront_vale"))
        harness.winEncounter("ascended_vale")
        assertEquals("singularity", harness.store.state.value.questStageById["w6_mq30"])

        harness.winEncounter("ascended_god")
        assertEquals("final_note", harness.store.state.value.questStageById["w6_mq30"])
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_mq30_tune_world"))

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w6_mq30"))
        assertTrue(state.completedMilestones.contains("ms_w6_mq30_complete"))
        assertTrue(state.completedMilestones.contains("ms_game_complete"))
        assertTrue(state.completedMilestones.contains("ms_credits_seen"))
        assertTrue(state.unlockedSkills.contains("source_art_tune_world"))
        assertEquals("source_new_world", state.roomId)
        assertEquals(listOf("scene_w6_final_note", "scene_w6_epilogue_credits"), harness.playedCinematics)
    }

    @Test
    fun singularitySideQuestRewardsResolve() {
        val harness = Hub10Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq29_aethel_grave"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq30_final_scavenge"))

        val state = harness.store.state.value
        assertTrue(state.completedQuests.containsAll(listOf("w6_sq29", "w6_sq30")))
        assertTrue(state.unlockedSkills.contains("orion_ancestral_grace"))
        assertTrue(state.inventory["starborn_mod"].orZero() >= 1)
    }

    private class Hub10Harness {
        val store = GameSessionStore()
        val playedCinematics = mutableListOf<String>()
        val events: EventManager

        init {
            events = EventManager(
                events = loadEvents(),
                sessionStore = store,
                eventHooks = EventHooks(
                    onPlayCinematic = { sceneId, done ->
                        playedCinematics += sceneId
                        done()
                    },
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

        fun winEncounter(vararg enemyIds: String) {
            events.handleTrigger(
                "encounter_victory",
                EventPayload.EncounterOutcome(
                    enemyIds = enemyIds.toList(),
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
