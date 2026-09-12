package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import com.example.starborn.feature.exploration.ui.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.*
import org.junit.Test

class WorldOneDiscoverabilityStateTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)

    @Test fun `dark and partial states retain references for executable interactions`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val events = reader.readList<GameEvent>("events.json")
        data class Case(val room: String, val label: String, val state: GameSessionState, val dark: Boolean = false)
        val fault = GameSessionState(activeQuests = setOf("w1_mq01"),
            completedMilestones = setOf("ms_w1_mq01_fault_visible"))
        val cases = listOf(
            Case("pit_nova_bunk", "scorched conduit", fault),
            Case("pit_nova_bunk", "scorched conduit", fault, true),
            Case("pit_storage", "loose floor panel", GameSessionState(), true),
            Case("server_backup", "archive terminal", GameSessionState(), true),
            Case("workshop_basement", "components chest", GameSessionState(), true),
            Case("workshop_yard", "loader diagnostic strip", GameSessionState(
                activeQuests = setOf("w1_mq01"),
                completedMilestones = setOf("ms_w1_mq01_safety_fault_inspected"),
                roomStates = mapOf("workshop_yard" to mapOf("loader_cleared" to true)))),
            Case("medbay_vents", "toxic blockage", GameSessionState(
                activeQuests = setOf("w1_sq02"),
                questTasksCompleted = mapOf("w1_sq02" to setOf("reroute_airflow")),
                roomStates = mapOf("medbay_vents" to mapOf("toxic_blockage_cleared" to true)))),
            Case("server_hub", "terminal", GameSessionState(
                activeQuests = setOf("w1_sq04"),
                questTasksCompleted = mapOf("w1_sq04" to setOf("restore_protocol_spoof")),
                roomStates = mapOf("server_hub" to mapOf("override_applied" to true))))
        )
        cases.forEach { case ->
            val room = rooms.getValue(case.room)
            val state = case.state.migrateOpeningNarrativeState()
            val flags = state.roomStates[case.room].orEmpty()
            val action = room.actions.single { it["name"] == case.label }
            assertTrue(case.toString(), narrativeActionVisible(action, emptyMap(), flags, state.completedMilestones))
            val eventName = action["action_event"] as String
            val matching = events.filter { it.trigger.action == eventName }
            assertTrue(matching.isNotEmpty())
            val store = GameSessionStore().apply { restore(state) }
            var executed = false
            EventManager(matching, store, EventHooks(onEventCompleted = { executed = true }))
                .handleTrigger("player_action", EventPayload.Action(eventName))
            assertTrue("Event must execute for $case", executed)
            val prose = requireNotNull(resolveRoomDescription(room, flags, state.completedMilestones, case.dark))
            assertTrue("Missing reference for $case", prose.contains(case.label, true))
        }
    }
    @Test fun `shunt datapad is named whenever power and quest gates expose it`() {
        val room = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
            .readList<Room>("rooms.json").single { it.id == "mine_shunt" }
        val action = room.actions.single { it["name"] == "crew datapad" }
        for (powered in listOf(false, true)) for (completed in listOf(false, true)) {
            val milestones = setOfNotNull(
                "ms_mine_power_on".takeIf { powered },
                "ms_w1_sq05_completed".takeIf { completed })
            val visible = narrativeActionVisible(action, emptyMap(), emptyMap(), milestones)
            assertEquals(powered && !completed, visible)
            // Generator power selects the lit description in production.
            val prose = requireNotNull(resolveRoomDescription(room, emptyMap(), milestones, !powered))
            if (visible) assertTrue(prose.contains("crew datapad", true))
        }
        assertFalse(room.description.contains("crew datapad", true))
    }
}
