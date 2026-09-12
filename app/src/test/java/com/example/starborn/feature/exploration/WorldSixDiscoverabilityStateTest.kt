package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import com.example.starborn.feature.exploration.ui.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.*
import org.junit.Test

class WorldSixDiscoverabilityStateTest {
    @Test fun `finale and Gh0st references remain reachable in independent milestone and task states`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val events = reader.readList<GameEvent>("events.json")
        val keys = listOf("ms_w6_mq30_complete", "ms_w6_mq26_complete", "ms_w6_source_balance_unlocked",
            "ms_w6_elara_voice_isolated", "ms_w6_elara_commands_broken")
        val ids = setOf("source_center", "source_gh0st_nightmare", "source_gh0st_kill_suite", "source_gh0st_elara_signal")
        for (mask in 0 until 32) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            rooms.filter { it.id in ids }.forEach { room ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.forEach { action ->
                    // The tuning action uses its puzzle's success event for action hints.
                    val name = action["action_event"] as? String ?: "w6_mq30_tune_world"
                    for (bits in 0 until 8) {
                        val ready = bits and 1 != 0
                        val done = bits and 2 != 0
                        val complete = bits and 4 != 0
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (ready) {
                                startQuest("w6_mq30")
                                setQuestTaskCompleted("w6_mq30", "defeat_god_form", true)
                            }
                            if (done) {
                                setQuestTaskCompleted("w6_mq30", "confront_vale", true)
                                setQuestTaskCompleted("w6_mq30", "tune_world", true)
                            }
                            if (complete) completeQuest("w6_sq28")
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val expected = when (name) {
                            "w6_mq30_confront_vale", "w6_mq30_tune_world" -> ready && !done
                            "w6_sq28_elara_song" -> !complete
                            "w6_sq28_break_command" -> keys[3] in milestones && keys[4] !in milestones
                            "w6_sq28_preserve_song" -> keys[4] in milestones && !complete
                            else -> error(name)
                        }
                        var executions = 0
                        val manager = EventManager(listOf(events.single { it.id == name }), store,
                            EventHooks(onEventCompleted = { executions++ }))
                        manager.handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("$name $mask $bits", if (expected) 1 else 0, executions)
                        if (expected) {
                            assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                            assertTrue(prose.contains(action["name"] as String, true))
                            store.restore(store.state.value.migrateOpeningNarrativeState())
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            assertEquals("Replay $name", 1, executions)
                        }
                    }
                }
            }
        }
        val well = rooms.single { it.id == "source_gh0st_nightmare_node_scale_01" }
        assertTrue(requireNotNull(resolveRoomDescription(well, emptyMap(), emptySet(), false)).contains("phantom dogtags", true))
    }
    @Test fun `Jed legacy references survive independent milestones and retired tasks`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val events = reader.readList<GameEvent>("events.json")
        val keys = listOf("ms_w6_jed_marks_found", "ms_w6_jed_lesson_heard", "ms_w6_legacy_unlocked")
        for (mask in 0 until 8) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            rooms.filter { it.id in setOf("source_echo_workbench", "source_echo_patrol") }.forEach { room ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { it["type"] == "generic" }.forEach { action ->
                    val name = action["action_event"] as String
                    for (bits in 0 until 8) {
                        val ready = bits and 1 != 0
                        val done = bits and 2 != 0
                        val complete = bits and 4 != 0
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (ready) {
                                startQuest("w6_mq27")
                                setQuestTaskCompleted("w6_mq27", "enter_mines", true)
                            }
                            if (done) setQuestTaskCompleted("w6_mq27", "evade_manager", true)
                            if (complete) completeQuest("w6_sq26")
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val expected = when (name) {
                            "w6_mq27_evade_manager" -> ready && !done
                            "w6_sq26_hear_lesson" -> keys[0] in milestones && keys[1] !in milestones
                            "w6_sq26_carry_legacy" -> keys[1] in milestones && !complete
                            else -> error(name)
                        }
                        var executions = 0
                        val manager = EventManager(listOf(events.single { it.trigger.action == name }), store,
                            EventHooks(onEventCompleted = { executions++ }))
                        manager.handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("$name mask=$mask bits=$bits", if (expected) 1 else 0, executions)
                        if (expected) {
                            assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                            assertTrue(prose.contains(action["name"] as String, true))
                            store.restore(store.state.value.migrateOpeningNarrativeState())
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            assertEquals("Replay $name", 1, executions)
                        }
                    }
                }
            }
        }
        rooms.filter { it.id in setOf("source_echo_mines_node_scale_01", "source_center_node_scale_01") }
            .forEach { room ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), emptySet(), false))
                room.actions.forEach { assertTrue(prose.contains(it["name"] as String, true)) }
            }
    }
}
