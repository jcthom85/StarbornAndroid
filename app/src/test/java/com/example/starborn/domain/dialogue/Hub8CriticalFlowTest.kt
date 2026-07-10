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

class Hub8CriticalFlowTest {

    @Test
    fun deepRingMainQuestProgressionsEndToEnd() {
        val harness = Hub8Harness()

        harness.store.startQuest("w5_mq22")
        harness.store.setQuestTaskCompleted("w5_mq22", "access_mainframe", true)
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq22_find_thorne"))
        assertTrue(harness.store.state.value.activeQuests.contains("w5_mq23"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_alpha"))
        assertTrue(harness.store.state.value.questTasksCompleted["w5_mq23"].orEmpty().contains("firewall_alpha").not())

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq23_navigate_maze"))
        var state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w5_mq23"].orEmpty().contains("navigate_maze"))
        assertTrue(state.completedMilestones.contains("ms_w5_server_maze_mapped"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_alpha"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_beta"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_gamma"))
        assertEquals("construct", harness.store.state.value.questStageById["w5_mq23"])

        harness.winEncounter("firewall_construct")
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_mq23"))
        assertTrue(state.activeQuests.contains("w5_mq24"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("deep_anchor_chamber"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq24_find_elara"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq24_take_anchor"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_mq24"))
        assertTrue(state.inventory["anchor_relic"].orZero() >= 1)
        assertTrue(state.unlockedSkills.contains("source_art_stasis"))
        assertTrue(state.activeQuests.contains("w5_mq25"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("deep_throne_room"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq25_soloist"))
        assertEquals("critical_escape", harness.store.state.value.questStageById["w5_mq25"])

        harness.winEncounter("compliance_avatar")
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_mq25_enter_tear"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_mq25"))
        assertTrue(state.completedMilestones.contains("ms_w5_mq25_complete"))
        assertTrue(state.completedMilestones.contains("ms_w6_access_unlocked"))
        assertEquals("source_campfire", state.roomId)
    }

    @Test
    fun deepRingSideQuestFlows() {
        val harness = Hub8Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_realign_mirrors"))
        assertTrue(harness.store.state.value.completedQuests.contains("w5_sq22").not())

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_trace_false_sun"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_realign_mirrors"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq22_restore_gardens"))
        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq22"))
        assertTrue(state.unlockedSkills.contains("orion_sunbeam"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq23_vacuum_seal"))
        assertTrue(harness.store.state.value.completedQuests.contains("w5_sq23").not())

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

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq25_find_keycard"))
        state = harness.store.state.value
        assertTrue(state.inventory["sysadmin_keycard"].orZero() >= 1)
        assertEquals("armory", state.questStageById["w5_sq25"])

        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq25_open_armory"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w5_sq25"))
        assertTrue(state.inventory["void_clip"].orZero() >= 1)
    }

    private class Hub8Harness {
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
            return requireNotNull(
                moshi.adapter<List<GameEvent>>(type)
                    .fromJson(File("src/main/assets/events.json").readText())
            )
        }

        private fun Int?.orZero(): Int = this ?: 0
    }
}
