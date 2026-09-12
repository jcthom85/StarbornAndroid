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
import com.example.starborn.feature.exploration.ui.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.*
import org.junit.Test

class WorldFourCoolingDiscoverabilityTest {
    @Test fun `cooling and core variants preserve usable references in partial milestone states`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val events = reader.readList<GameEvent>("events.json")
        val ids = setOf("foundry_cooling_springs", "foundry_engine_service_ring", "foundry_escape_catwalk",
            "foundry_forge_anvil", "foundry_power_core", "foundry_service_airlock",
            "foundry_slag_landing", "foundry_slag_river", "foundry_slag_stepping_stones",
            "foundry_titan_dock", "foundry_waste_intake")
        var executions = 0
        reader.readList<Room>("rooms.json").filter { it.id in ids }.forEach { room ->
            val keys = (room.descriptionVariants.flatMap { it.requiresMilestones + it.forbiddenMilestones } +
                room.actions.flatMap { (it["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty() +
                    listOfNotNull(it["requires_milestone_not_set"] as? String) }).distinct()
            for (mask in 0 until (1 shl keys.size)) {
                val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { it["type"] != "fishing" && narrativeActionVisible(it, emptyMap(), emptyMap(), milestones) }
                    .forEach actionLoop@ { action ->
                        val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                        if (!milestones.containsAll(required)) return@actionLoop
                        assertTrue("${room.id}: ${action["name"]} $milestones", prose.contains(action["name"] as String, true))
                        val name = action["action_event"] as? String ?: return@actionLoop
                        val event = events.single { it.trigger.action == name }
                        val blockedByMilestone = event.conditions.orEmpty().any {
                            it.type == "milestone_not_set" && it.milestone in milestones
                        }
                        val store = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            // Seed only authored positive prerequisites; completion tasks remain absent.
                            event.conditions.orEmpty().forEach { c ->
                                when (c.type) {
                                    "quest_active" -> startQuest(requireNotNull(c.questId))
                                    "quest_task_done" -> setQuestTaskCompleted(requireNotNull(c.questId), requireNotNull(c.taskId), true)
                                }
                            }
                        }
                        var executed = false
                        EventManager(listOf(event), store, EventHooks(onEventCompleted = { executed = true }))
                            .handleTrigger("player_action", EventPayload.Action(name))
                        assertEquals("Execution of $name with $milestones", !blockedByMilestone, executed)
                        if (executed) executions++
                    }
            }
        }
        assertTrue(executions > 100)
    }
    @Test fun `cooling and decon prose names visible actions`() {
        val rooms = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
            .readList<Room>("rooms.json")
        rooms.filter { it.id in setOf("foundry_cooling_springs", "foundry_decon_chamber") }.forEach { room ->
            val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), emptySet(), false))
            room.actions.filter {
                narrativeActionVisible(it, emptyMap(), emptyMap(), emptySet()) &&
                    (it["requires_milestones"] as? List<*>)?.isEmpty() != false
            }
                .filterNot { it["type"] == "fishing" }
                .forEach { assertTrue("${room.id}: ${it["name"]}", prose.contains(it["name"] as String, true)) }
        }
    }
}
