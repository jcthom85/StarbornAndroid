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

class WorldFourDiscoverabilityStateTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)

    @Test fun `overlook forge controls and reject bay retain inspection references`() {
        val labels = mapOf(
            "foundry_obsidian_overlook" to listOf("foundry basin", "shield window telemetry"),
            "foundry_forge_control_alcove" to listOf("hazard cycle map", "overclock profile"),
            "foundry_forge_scale_01" to listOf("bellows valve"),
            "foundry_power_core_scale_04" to listOf("blue pipes", "coolant choir"),
            "foundry_reject_bay" to listOf("rejected droid"))
        val events = reader.readList<GameEvent>("events.json")
        reader.readList<Room>("rooms.json").filter { it.id in labels }.forEach { room ->
            val states = listOf(emptySet<String>()) + room.descriptionVariants.map { it.requiresMilestones.toSet() }
            states.forEach { milestones ->
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                labels.getValue(room.id).forEach { label ->
                    val action = room.actions.single { it["name"] == label }
                    assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                    assertTrue("${room.id}: $label", prose.contains(label, true))
                    val name = action["action_event"] as? String
                    if (name != null) {
                        val store = GameSessionStore().apply {
                            milestones.forEach(::setMilestone)
                            if (label == "overclock profile") setMilestone("ms_w4_hazard_trial_cleared")
                        }
                        val matching = events.filter { it.trigger.action == name }
                        assertTrue(matching.isNotEmpty())
                        var executed = false
                        EventManager(matching, store, EventHooks(onEventCompleted = { executed = true }))
                            .handleTrigger("player_action", EventPayload.Action(name))
                        assertTrue("Expected executable $name", executed)
                    }
                }
            }
        }
    }

    @Test fun `conveyor and conditioning descriptions retain interactions across independent milestones`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val events = reader.readList<GameEvent>("events.json")
        val keys = listOf("ms_w4_matrix_overloaded", "ms_w4_prototypes_defeated",
            "ms_w4_conveyors_timed", "ms_w4_pulse_board_read",
            "ms_w4_rejection_codes_read", "ms_w4_defective_units_spared")
        val ids = listOf("foundry_conveyor_belt", "foundry_conditioning_chamber", "foundry_conveyor_belt_scale_01")
        var executions = 0
        for (mask in 0 until 64) {
            val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            ids.forEach { id ->
                val room = rooms.getValue(id)
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { narrativeActionVisible(it, emptyMap(), emptyMap(), milestones) }.forEach { action ->
                    assertTrue("Missing ${action["name"]} in $id with $milestones",
                        prose.contains(action["name"] as String, true))
                    val name = action["action_event"] as? String ?: return@forEach
                    if (id == "foundry_conveyor_belt_scale_01") return@forEach
                    val store = GameSessionStore().apply {
                        milestones.forEach(::setMilestone)
                        startQuest("w4_mq18")
                        // Prove the conditioning action with its prerequisite task;
                        // conveyor timing is tested with that task still unfinished.
                        if (id == "foundry_conditioning_chamber")
                            setQuestTaskCompleted("w4_mq18", "navigate_conveyors", true)
                    }
                    // Generic milestone prerequisites are enforced by selection before dispatch.
                    val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                    if (!milestones.containsAll(required)) return@forEach
                    val matching = events.filter { it.trigger.action == name }
                    assertTrue(matching.isNotEmpty())
                    var executed = false
                    EventManager(matching, store, EventHooks(onEventCompleted = { executed = true }))
                        .handleTrigger("player_action", EventPayload.Action(name))
                    assertTrue("Event blocked unexpectedly: $name with $milestones", executed)
                    executions++
                }
            }
        }
        assertTrue(executions > 128)
    }
}
