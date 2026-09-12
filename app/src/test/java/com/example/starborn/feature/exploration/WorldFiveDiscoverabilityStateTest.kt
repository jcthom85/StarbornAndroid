package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.ui.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.*
import org.junit.Test
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.migrateOpeningNarrativeState

class WorldFiveDiscoverabilityStateTest {
    @Test fun `remaining orbital rooms preserve actionable prose and retire completed events after restore`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val events = reader.readList<GameEvent>("events.json")
        val rooms = reader.readList<Room>("rooms.json").filter { it.id in setOf(
            "orbital_server_farm", "orbital_service_shaft", "orbital_solarium",
            "orbital_surveillance_pit", "orbital_zero_g_junction") }
        var usable = 0
        rooms.forEach { room ->
            val keys = (room.descriptionVariants.flatMap { it.requiresMilestones } +
                room.actions.flatMap { action ->
                    events.find { it.trigger.action == action["action_event"] && action["action_event"] != null }
                        ?.conditions.orEmpty().mapNotNull { it.milestone }
                }).distinct()
            for (mask in 0 until (1 shl keys.size)) {
                val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filterNot { it["type"] == "fishing" }.forEach { action ->
                    assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                    val name = action["action_event"] as? String
                    if (name == null) {
                        assertTrue(prose.contains(action["name"] as String, true))
                    } else {
                        val event = events.single { it.trigger.action == name }
                        for (retired in listOf(false, true)) {
                            val seed = GameSessionStore().apply {
                                milestones.forEach(::setMilestone)
                                event.conditions.orEmpty().forEach { c ->
                                    when (c.type) {
                                        "quest_active" -> startQuest(requireNotNull(c.questId))
                                        "quest_task_done" -> setQuestTaskCompleted(requireNotNull(c.questId), requireNotNull(c.taskId), true)
                                    }
                                }
                                if (retired) event.conditions.orEmpty().forEach { c ->
                                    when (c.type) {
                                        "quest_task_not_done" -> setQuestTaskCompleted(requireNotNull(c.questId), requireNotNull(c.taskId), true)
                                        "quest_not_completed" -> completeQuest(requireNotNull(c.questId))
                                    }
                                }
                            }.state.value
                            val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                            val milestoneReady = event.conditions.orEmpty().all { c -> when (c.type) {
                                "milestone_set" -> c.milestone in milestones
                                "milestone_not_set" -> c.milestone !in milestones
                                else -> true
                            } }
                            val taskRetired = retired && event.conditions.orEmpty().any {
                                it.type in setOf("quest_task_not_done", "quest_not_completed")
                            }
                            var executions = 0
                            val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            assertEquals("$name $milestones retired=$retired", if (milestoneReady && !taskRetired) 1 else 0, executions)
                            if (executions == 1) {
                                usable++
                                val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                if (milestones.containsAll(required)) assertTrue("${room.id}: ${action["name"]}", prose.contains(action["name"] as String, true))
                                store.restore(store.state.value.migrateOpeningNarrativeState())
                                manager.handleTrigger("player_action", EventPayload.Action(name))
                                assertEquals("Replay $name", 1, executions)
                            }
                        }
                    }
                }
            }
        }
        assertTrue(usable > 50)
    }
    @Test fun `concourse mirror and security references respect independent action gates and event history`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json").filter {
            it.id in setOf("orbital_grand_concourse", "orbital_mirror_walk", "orbital_security_hub")
        }
        val events = reader.readList<GameEvent>("events.json")
        val keys = listOf("ms_w5_corporate_insight_unlocked", "ms_w5_solar_maintenance_complete",
            "ms_w5_security_hub_cleared", "ms_w5_redactions_found",
            "ms_w5_director_logs_decrypted", "ms_w5_false_sun_traced")
        for (mask in 0 until 64) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            rooms.forEach { room ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.forEach { action ->
                    val name = action["action_event"] as? String
                    if (name == null) {
                        assertTrue(prose.contains(action["name"] as String, true))
                    } else for (completed in listOf(false, true)) {
                        val quest = if (name.startsWith("w5_sq21")) "w5_sq21" else "w5_sq22"
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (completed) completeQuest(quest)
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val eventEligible = !completed && when (name) {
                            "w5_sq21_director_logs" -> true
                            "w5_sq21_publish_logs" -> keys[4] in milestones
                            "w5_sq21_find_redactions" -> keys[3] !in milestones
                            "w5_sq22_trace_false_sun" -> keys[5] !in milestones
                            "w5_sq22_realign_mirrors" -> keys[5] in milestones
                            else -> error(name)
                        }
                        var executions = 0
                        val manager = EventManager(listOf(events.single { it.trigger.action == name }), store,
                            EventHooks(onEventCompleted = { executions++ }))
                        manager.handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("$name mask=$mask completed=$completed", if (eventEligible) 1 else 0, executions)
                        val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                        if (eventEligible && milestones.containsAll(required)) {
                            assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                            assertTrue("${room.id}: ${action["name"]} $milestones", prose.contains(action["name"] as String, true))
                        }
                        if (eventEligible) {
                            store.restore(store.state.value.migrateOpeningNarrativeState())
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            assertEquals("Restored replay $name", 1, executions)
                        }
                    }
                }
            }
        }
    }
    @Test fun `dock and gallery retain usable references across independent pressure and quest progress`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json").filter {
            it.id in setOf("orbital_airlock_gallery", "orbital_executive_dock")
        }
        val events = reader.readList<GameEvent>("events.json")
        val keys = listOf("ms_w5_fighter_screen_broken", "ms_w5_vacuum_seal_complete",
            "ms_w5_mq21_complete", "ms_w5_pressure_loss_mapped", "ms_w5_breaches_sealed")
        for (mask in 0 until 32) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            rooms.forEach { room ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.forEach { action ->
                    assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                    val name = action["action_event"] as? String
                    if (name == null) {
                        assertTrue(prose.contains(action["name"] as String, true))
                    } else for (stateMask in 0 until 8) {
                        val ready = stateMask and 1 != 0
                        val done = stateMask and 2 != 0
                        val completed = stateMask and 4 != 0
                        val seed = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (ready) {
                                startQuest("w5_mq21")
                                setQuestTaskCompleted("w5_mq21", "shoot_down_fighters", true)
                                setQuestTaskCompleted("w5_mq21", "force_dock", true)
                            }
                            // Each main action gets its own independent completion state.
                            if (name == "w5_mq21_force_dock") setQuestTaskCompleted("w5_mq21", "force_dock", done)
                            if (name == "w5_mq21_hack_airlock") setQuestTaskCompleted("w5_mq21", "hack_airlock", done)
                            if (completed) completeQuest("w5_sq23")
                        }.state.value
                        val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                        val expected = when (name) {
                            "w5_mq21_force_dock", "w5_mq21_hack_airlock" -> ready && !done
                            "w5_sq23_map_pressure_loss" -> !completed && keys[3] !in milestones
                            "w5_sq23_vacuum_seal" -> !completed && keys[3] in milestones
                            "w5_sq23_reopen_dock" -> !completed && keys[4] in milestones
                            else -> error("Unexpected event $name")
                        }
                        var executions = 0
                        val manager = EventManager(listOf(events.single { it.trigger.action == name }), store,
                            EventHooks(onEventCompleted = { executions++ }))
                        manager.handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("$name milestones=$mask tasks=$stateMask", if (expected) 1 else 0, executions)
                        if (expected) {
                            assertTrue("${room.id}: ${action["name"]} $milestones", prose.contains(action["name"] as String, true))
                            val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                            assertTrue(milestones.containsAll(required))
                            manager.handleTrigger("player_action", EventPayload.Action(name))
                            // Event completion history also retires sealing before quest completion.
                            assertEquals(name, 1, executions)
                        }
                    }
                }
            }
        }
    }
    @Test fun `deep station references survive independent milestones and event task retirement`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val events = reader.readList<GameEvent>("events.json")
        val ids = setOf("deep_mainframe_nave", "deep_sysadmin_nest", "deep_tear", "deep_throne_room")
        var usableStates = 0
        reader.readList<Room>("rooms.json").filter { it.id in ids }.forEach { room ->
            val keys = room.descriptionVariants.flatMap { it.requiresMilestones }.distinct()
            for (mask in 0 until (1 shl keys.size)) {
                val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.forEach { action ->
                    assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                    val eventName = action["action_event"] as? String
                    if (eventName == null) {
                        assertTrue(prose.contains(action["name"] as String, true))
                    } else {
                        val event = events.single { it.trigger.action == eventName }
                        for (stateMask in 0 until 8) {
                            val ready = stateMask and 1 != 0
                            val retired = stateMask and 2 != 0
                            val completed = stateMask and 4 != 0
                            val seed = GameSessionStore().apply {
                                milestones.forEach(::setMilestone)
                                event.conditions.orEmpty().forEach { c ->
                                    when (c.type) {
                                        "quest_active" -> if (ready) startQuest(requireNotNull(c.questId))
                                        "quest_task_done" -> if (ready) setQuestTaskCompleted(requireNotNull(c.questId), requireNotNull(c.taskId), true)
                                        "quest_task_not_done" -> if (retired) setQuestTaskCompleted(requireNotNull(c.questId), requireNotNull(c.taskId), true)
                                        "quest_not_completed" -> if (completed) completeQuest(requireNotNull(c.questId))
                                    }
                                }
                            }.state.value
                            val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                            val hasPrerequisites = event.conditions.orEmpty().any { it.type in setOf("quest_active", "quest_task_done") }
                            val checksCompletion = event.conditions.orEmpty().any { it.type == "quest_not_completed" }
                            val expected = (!hasPrerequisites || ready) && !retired && (!checksCompletion || !completed)
                            var executions = 0
                            val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
                            manager.handleTrigger("player_action", EventPayload.Action(eventName))
                            assertEquals("$eventName milestones=$mask tasks=$stateMask", if (expected) 1 else 0, executions)
                            if (expected) {
                                usableStates++
                                assertTrue("${room.id}: ${action["name"]} $milestones", prose.contains(action["name"] as String, true))
                                manager.handleTrigger("player_action", EventPayload.Action(eventName))
                                assertEquals("Replay $eventName", 1, executions)
                            }
                        }
                    }
                }
            }
        }
        assertEquals(48, usableStates)
    }
    @Test fun `firewall partial saves retain usable references and completed tasks prevent replay`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val events = reader.readList<GameEvent>("events.json")
        listOf("alpha", "beta", "gamma").forEachIndexed { index, node ->
            val room = rooms.single { it.id == "deep_firewall_$node" }
            val action = room.actions.single()
            val event = events.single { it.id == "w5_mq23_firewall_$node" }
            val prerequisite = listOf("navigate_maze", "firewall_alpha", "firewall_beta")[index]
            for (mask in 0 until 32) {
                val active = mask and 1 != 0
                val ready = mask and 2 != 0
                val done = mask and 4 != 0
                val seed = GameSessionStore().apply {
                    if (active) startQuest("w5_mq23")
                    if (ready) setQuestTaskCompleted("w5_mq23", prerequisite, true)
                    if (done) setQuestTaskCompleted("w5_mq23", "firewall_$node", true)
                    if (mask and 8 != 0) setMilestone("ms_w5_firewall_${node}_down")
                    if (mask and 16 != 0) setMilestone("ms_w5_mq23_complete")
                }.state.value
                val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
                assertEquals(seed.questTasksCompleted, store.state.value.questTasksCompleted)
                assertEquals(seed.completedMilestones, store.state.value.completedMilestones)
                val milestones = store.state.value.completedMilestones
                assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                var executions = 0
                val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
                manager.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_$node"))
                val usable = active && ready && !done
                assertEquals("$node mask=$mask", if (usable) 1 else 0, executions)
                if (usable) {
                    assertTrue("$node mask=$mask", requireNotNull(resolveRoomDescription(
                        room, emptyMap(), milestones, false
                    )).contains(action["name"] as String, ignoreCase = true))
                    manager.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_$node"))
                    assertEquals("Replay $node mask=$mask", 1, executions)
                }
            }
        }
    }
    @Test fun `graviton puzzle stays discoverable across independent anchor progress and retires when solved`() {
        val rooms = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance).readList<Room>("rooms.json")
        val room = rooms.single { it.id == "deep_anchor_chamber" }
        val action = room.actions.single { it["name"] == "graviton well" }
        val keys = listOf("ms_w5_elara_found", "ms_w5_mq24_complete", "ms_relic_w5_graviton_solved")
        for (mask in 0 until 8) {
            val milestones = keys.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
            val visible = narrativeActionVisible(action, emptyMap(), emptyMap(), milestones)
            assertEquals(milestones.toString(), "ms_relic_w5_graviton_solved" !in milestones, visible)
            if (visible) assertTrue(milestones.toString(), requireNotNull(
                resolveRoomDescription(room, emptyMap(), milestones, false)
            ).contains("graviton well", ignoreCase = true))
        }
        val gallery = rooms.single { it.id == "deep_anchor_chamber_scale_04" }
        val route = gallery.actions.single { it["name"] == "scratched route" }
        assertTrue(narrativeActionVisible(route, emptyMap(), emptyMap(), emptySet()))
        assertTrue(requireNotNull(resolveRoomDescription(gallery, emptyMap(), emptySet(), false))
            .contains("scratched route", ignoreCase = true))
    }
}
