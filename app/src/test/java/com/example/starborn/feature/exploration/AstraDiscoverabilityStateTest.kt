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

class AstraDiscoverabilityStateTest {
    @Test fun `arcade progression never removes archive or relic references and archive remains repeatable`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json")
        val room = rooms.single { it.id == "astra_common_room" }
        val event = reader.readList<GameEvent>("events.json").single { it.id == "astra_open_tape_deck" }
        val keys = (1..6).map { "ms_arcade_cabinet_0${it}_repaired" } + "ms_all_arcade_cabinets_restored"
        val descriptions = mutableSetOf<String>()
        for (mask in 0 until 128) {
            val seed = GameSessionStore().apply {
                keys.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.forEach(::setMilestone)
            }.state.value
            val store = GameSessionStore().apply { restore(seed.migrateOpeningNarrativeState()) }
            val milestones = store.state.value.completedMilestones
            val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, false))
            descriptions.add(prose)
            room.actions.filter { it["type"] == "generic" }.forEach { action ->
                assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                assertTrue("${action["name"]} mask=$mask", prose.contains(action["name"] as String, true))
            }
            var executions = 0
            val manager = EventManager(listOf(event), store, EventHooks(onEventCompleted = { executions++ }))
            manager.handleTrigger("player_action", EventPayload.Action(event.id))
            store.restore(store.state.value.migrateOpeningNarrativeState())
            manager.handleTrigger("player_action", EventPayload.Action(event.id))
            assertEquals("Archive replay mask=$mask", 2, executions)
        }
        assertEquals(5, descriptions.size)
        val cargo = rooms.single { it.id == "astra_cargo_bay" }
        val prose = requireNotNull(resolveRoomDescription(cargo, emptyMap(), emptySet(), false))
        val actions = cargo.actions.map { com.example.starborn.domain.model.GenericAction(it["name"] as String, "generic") }
        val plan = requireNotNull(com.example.starborn.feature.exploration.ui.buildInlineActionPlan(prose, actions, emptyMap(), cargo))
        assertTrue(plan.description.contains("repair bench"))
        assertFalse(plan.description.contains("[action:"))
        assertEquals(actions.toSet(), plan.segments.mapNotNull {
            (it.target as? com.example.starborn.feature.exploration.ui.hud.InlineActionTarget.Room)?.action
        }.toSet())
    }
}
