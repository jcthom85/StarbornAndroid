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

class WorldThreeDiscoverabilityStateTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val ids = setOf(
            "spire_signal_stairwell",
            "spire_zekes_apartment",
            "spire_night_market",
            "spire_lantern_bridge",
            "spire_security_kiosk",
            "spire_uniform_sorting",
            "spire_service_lift",
            "spire_archive_vault",
            "spire_prism_gallery",
            "spire_drone_test_alcove",
            "spire_landing_pad_roof",
            "spire_night_market_scale_01",
            "spire_skypark_scale_02"
    )
    private val scans = setOf("w3_scan_archive_tethers", "w3_scan_prism_alarm_chords", "w3_scan_drone_paths")

    @Test fun `reviewed rooms name visible actions across milestone combinations`() {
        reader.readList<Room>("rooms.json").filter { it.id in ids }.forEach { room ->
            val keys = (room.descriptionVariants.flatMap { it.requiresMilestones + it.forbiddenMilestones } +
                room.actions.flatMap { action ->
                    listOfNotNull(action["requires_milestone_not_set"] as? String) +
                        (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                }).distinct()
            assertTrue("Bounded milestone matrix", keys.size < 12)
            for (mask in 0 until (1 shl keys.size)) {
                val milestones = keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
                room.actions.filter { narrativeActionVisible(it, emptyMap(), emptyMap(), milestones) }
                    .filterNot { it["type"] == "rest_stop" }
                    .forEach { action ->
                        val gatedScan = action["action_event"] in scans && "ms_w3_mq14_complete" !in milestones
                        if (!gatedScan) assertTrue("Missing ${action["name"]} in ${room.id} with $milestones",
                            prose.contains(action["name"] as String, true))
                    }
            }
        }
    }

    @Test fun `scan omissions before Lens milestone are blocked and completion prose exposes them`() {
        val events = reader.readList<GameEvent>("events.json")
        val rooms = reader.readList<Room>("rooms.json")
        scans.forEach { name ->
            val room = rooms.single { r -> r.actions.any { it["action_event"] == name } }
            val action = room.actions.single { it["action_event"] == name }
            val matching = events.filter { it.trigger.action == name }
            assertTrue(matching.isNotEmpty())
            val store = GameSessionStore()
            var count = 0
            val manager = EventManager(matching, store, EventHooks(onEventCompleted = { count++ }))
            manager.handleTrigger("player_action", EventPayload.Action(name))
            assertEquals(0, count)
            store.setMilestone("ms_w3_mq14_complete")
            manager.handleTrigger("player_action", EventPayload.Action(name))
            assertEquals(1, count)
            assertTrue(requireNotNull(resolveRoomDescription(room, emptyMap(),
                store.state.value.completedMilestones, false)).contains(action["name"] as String, true))
        }
    }
}
