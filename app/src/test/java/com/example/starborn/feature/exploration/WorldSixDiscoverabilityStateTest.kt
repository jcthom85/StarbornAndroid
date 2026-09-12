package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Quest
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
    @Test fun `world six completion events exclusively own successor handoffs`() {
        val events = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
            .readList<GameEvent>("events.json")
        val handoffs = mapOf(
            "w6_mq26_reassemble" to ("w6_mq26" to "w6_mq27"),
            "w6_mq27_reach_elevator" to ("w6_mq27" to "w6_mq28"),
            "w6_mq28_reach_singularity" to ("w6_mq28" to "w6_mq29"),
            "w6_mq29_reach_center" to ("w6_mq29" to "w6_mq30")
        )
        handoffs.forEach { (eventId, pair) ->
            val event = events.single { it.id == eventId }
            assertEquals(1, event.actions.count { it.type == "complete_quest" && it.questId == pair.first })
            assertEquals(1, event.actions.count { it.type == "start_quest" && it.questId == pair.second })
            assertEquals(1, event.actions.count { it.type == "track_quest" && it.questId == pair.second })
            assertFalse(events.any { other ->
                other.trigger.type == "quest_stage_complete" && other.trigger.questId == pair.first &&
                    other.actions.any { it.type == "start_quest" && it.questId == pair.second }
            })
        }
    }

    @Test fun `quest terminology names authored world six rooms and actions`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val quests = reader.readList<Quest>("quests.json").associateBy { it.id }
        fun tasks(id: String) = quests.getValue(id).stages.flatMap { it.tasks }.associateBy { it.id }
        fun assertTerms(quest: String, task: String, room: String, vararg terms: String) {
            val text = tasks(quest).getValue(task).text
            assertTrue("$quest/$task missing room $room", text.contains(rooms.getValue(room).title, true))
            terms.forEach { assertTrue("$quest/$task missing $it", text.contains(it, true)) }
        }

        assertTerms("w6_mq26", "reassemble_team", "source_campfire", "song-fire")
        assertTerms("w6_mq27", "evade_manager", "source_echo_patrol", "manager patrol")
        assertTerms("w6_mq28", "build_bridge", "source_memory_bridge", "unfinished bridge", "Zeke's anchor")
        assertTerms("w6_mq28", "final_banter", "source_memory_bridge_span", "campfire promise")
        assertTerms("w6_mq28", "reach_singularity", "source_memory_threshold", "singularity threshold")
        assertTerms("w6_sq26", "find_tool_marks", "source_orion_chorus", "crooked tool marks")
        assertTerms("w6_sq26", "talk_to_jed", "source_echo_workbench", "jed echo")
        assertTerms("w6_sq26", "carry_legacy", "source_echo_patrol", "open shift gate")
        assertTerms("w6_sq27", "find_backups", "source_zeke_nightmare", "backup lattice")
        assertTerms("w6_sq27", "delete_record", "source_zeke_review_loop", "authorization chain")
        assertTerms("w6_sq27", "leave_review", "source_zeke_break_room", "unauthorized exit", "worker broadcast")
        assertTerms("w6_sq28", "isolate_voice", "source_gh0st_nightmare", "buried voice")
        assertTerms("w6_sq28", "break_command_layer", "source_gh0st_kill_suite", "command carrier")
        assertTerms("w6_sq28", "recover_song", "source_gh0st_elara_signal", "pure audio log")
        assertTerms("w6_mq29", "climb_stair", "source_memory_stair", "Jed's door", "Astra wreckage", "Foundry catwalk")
        assertTerms("w6_mq30", "use_key", "source_center", "shared tuning focus")
        assertTerms("w6_mq30", "tune_world", "source_center", "shared tuning focus")
        assertTerms("w6_sq29", "trace_names", "source_memory_threshold", "names beneath the light")
        assertTerms("w6_sq29", "find_grave", "source_memory_stair", "aethel grave")
        assertTerms("w6_sq29", "carry_chorus", "source_memory_fragments", "answering chorus")
        assertTerms("w6_sq30", "identify_timeline", "source_spire_thought", "wreck timeline")
        assertTerms("w6_sq30", "recover_hull", "source_spire_archive", "future astra hull")
        assertTerms("w6_sq30", "fit_hull", "source_spire_arena", "empty armor socket")
    }

    @Test fun `remaining Source references preserve partial progress and intentional epilogue replay`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val events = reader.readList<GameEvent>("events.json")
        val ids = setOf("source_zeke_break_room", "source_orion_chorus", "source_spire_thought",
            "source_spire_archive", "source_spire_arena", "source_new_world", "source_new_world_node_scale_final")
        var cases = 0
        rooms.filter { it.id in ids }.forEach { room ->
            val roomEvents = room.actions.mapNotNull { action ->
                (action["action_event"] as? String)?.let { name -> events.single { it.trigger.action == name } }
            }
            val keys = (room.descriptionVariants.flatMap { it.requiresMilestones } +
                roomEvents.flatMap { it.conditions.orEmpty().mapNotNull { c -> c.milestone } }).distinct()
            for (mask in 0 until (1 shl keys.size)) {
                val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { it["action_event"] != null }.forEach { action ->
                    val name = action["action_event"] as String
                    val event = roomEvents.single { it.trigger.action == name }
                    for (completed in listOf(false, true)) {
                        cases++
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (completed) event.conditions.orEmpty().filter { it.type == "quest_not_completed" }
                                .forEach { completeQuest(requireNotNull(it.questId)) }
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val expected = event.conditions.orEmpty().all { c -> when (c.type) {
                            "milestone_set" -> c.milestone in milestones
                            "milestone_not_set" -> c.milestone !in milestones
                            "quest_not_completed" -> !completed
                            else -> error("Unhandled condition ${c.type}")
                        } }
                        var executions = 0
                        val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
                        manager.handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("$name $milestones completed=$completed", if (expected) 1 else 0, executions)
                        if (expected) {
                            assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                            assertTrue(prose.contains(action["name"] as String, true))
                            store.restore(store.state.value.migrateOpeningNarrativeState())
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            val repeatable = name in setOf("w6_replay_epilogue", "source_board_astra")
                            assertEquals("Replay $name", if (repeatable) 2 else 1, executions)
                        }
                    }
                }
            }
        }
        assertEquals(42, cases)
        val inspectIds = setOf("source_zeke_nightmare_node_scale_01", "source_orion_nightmare_node_scale_01",
            "source_spire_thought_node_scale_01", "source_new_world_node_scale_01", "source_new_world_node_scale_03")
        rooms.filter { it.id in inspectIds }.forEach { room ->
            val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), emptySet(), false))
            room.actions.forEach { assertTrue(prose.contains(it["name"] as String, true)) }
        }
    }
    @Test fun `memory references survive quest retirement and independent Aethel milestones`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val events = reader.readList<GameEvent>("events.json")
        val references = mapOf("source_memory_bridge_span" to listOf("campfire promise", "memory altar box"),
            "source_memory_threshold" to listOf("singularity threshold", "names beneath the light"),
            "source_memory_stair" to listOf("aethel grave"), "source_memory_fragments" to listOf("answering chorus"))
        val keys = listOf("ms_w6_aethel_names_traced", "ms_w6_aethel_grave_tuned", "ms_w6_ancestral_grace_unlocked")
        for (mask in 0 until 8) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            references.forEach { (id, labels) ->
                val room = rooms.single { it.id == id }
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { it["name"] in labels }.forEach { action ->
                    val name = action["action_event"] as String
                    val event = events.single { it.trigger.action == name }
                    for (bits in 0 until 8) {
                        val ready = bits and 1 != 0
                        val done = bits and 2 != 0
                        val completed = bits and 4 != 0
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (ready) {
                                startQuest("w6_mq28")
                                setQuestTaskCompleted("w6_mq28", "build_bridge", true)
                                setQuestTaskCompleted("w6_mq28", "final_banter", true)
                            }
                            if (name == "w6_mq28_final_banter") setQuestTaskCompleted("w6_mq28", "final_banter", done)
                            if (name == "w6_mq28_reach_singularity") setQuestTaskCompleted("w6_mq28", "reach_singularity", done)
                            if (completed) completeQuest("w6_sq29")
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val expected = when (name) {
                            "w6_mq28_final_banter", "w6_mq28_reach_singularity" -> ready && !done
                            "w6_sq29_aethel_grave" -> !completed
                            "w6_sq29_tune_grave" -> keys[0] in milestones && keys[1] !in milestones
                            "w6_sq29_carry_chorus" -> keys[1] in milestones && !completed
                            "find_vhs_tape_10" -> true
                            else -> error(name)
                        }
                        var executions = 0
                        val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
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
        listOf("source_memory_bridge_node_scale_01" to "tideglass footprints",
            "source_memory_stair_node_scale_01" to "miner's motto carving").forEach { (id, label) ->
            assertTrue(requireNotNull(resolveRoomDescription(rooms.single { it.id == id }, emptyMap(), emptySet(), false)).contains(label, true))
        }
    }
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
