package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.DialogueLine
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

    @Test fun `launch journal distinguishes Zeke handoff from console activation`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val tasks = reader.readList<Quest>("quests.json").single { it.id == "w1_mq05" }
            .stages.flatMap { it.tasks }.associateBy { it.id }
        assertEquals("launch_pod", rooms.getValue("launch_bay").connections["north"])
        val splice = tasks.getValue("splice_chime").text
        assertTrue(splice.contains(rooms.getValue("launch_bay").title))
        assertTrue(splice.contains(rooms.getValue("launch_pod").title))
        assertTrue(splice.contains("talk again"))
        val launch = tasks.getValue("launch_from_colony").text
        assertTrue(launch.contains("navigation console"))
        assertTrue(launch.contains(rooms.getValue("launch_pod").title))
        assertEquals("use_nav_console", rooms.getValue("launch_pod").actions
            .single { it["name"] == "navigation console" }["action_event"])
        val lines = reader.readList<DialogueLine>("dialogue.json").associateBy { it.id }
        assertTrue(lines.getValue("zeke_w1_mq05_defeat_warden_3").trigger.orEmpty()
            .contains("set_milestone:ms_w1_zeke_directed_to_pod"))
        assertTrue(lines.getValue("zeke_w1_mq05_pod_core_1").condition.orEmpty()
            .contains("milestone:ms_w1_zeke_directed_to_pod"))
        assertTrue(lines.getValue("zeke_w1_mq05_pod_core_4").trigger.orEmpty()
            .contains("set_milestone:ms_w1_chime_spliced"))
    }

    @Test fun `mine instructions distinguish elevator route from bulwark detour`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        assertEquals("admin_elevator", rooms.getValue("admin_lobby").connections["west"])
        assertEquals("mine_landing", rooms.getValue("admin_elevator").connections["north"])
        assertEquals("mine_checkpoint", rooms.getValue("mine_junction").connections["north"])
        assertEquals("mine_conveyor", rooms.getValue("mine_junction").connections["east"])
        val tasks = reader.readList<Quest>("quests.json").single { it.id == "w1_mq03" }
            .stages.flatMap { it.tasks }.associateBy { it.id }
        assertTrue(tasks.getValue("talk_to_bogs").text.contains(rooms.getValue("admin_lobby").title))
        assertTrue(tasks.getValue("use_deep_elevator").text.contains(rooms.getValue("admin_elevator").title))
        assertTrue(tasks.getValue("break_riot_guard").text.contains(rooms.getValue("mine_checkpoint").title))
        // Base prose is shared by authorized and unauthorized states; do not
        // announce successful authorization before the cross-room gate checks it.
        val elevator = rooms.getValue("admin_elevator")
        assertFalse(elevator.description.orEmpty().contains("shows green"))
        listOf(emptySet(), setOf("ms_w1_mq03_bogs_talked")).forEach { milestones ->
            val prose = requireNotNull(resolveRoomDescription(elevator, emptyMap(), milestones, false))
            listOf("warning signs", "scanner", "cables").forEach { assertTrue(prose.contains(it)) }
        }
    }

    @Test fun `clearance instructions name the hub destination and pre-badge booth route`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val node = reader.readList<HubNode>("hub_nodes.json").single { it.id == "admin_gate" }
        assertEquals("hub", node.entryPolicy)
        assertEquals("checkpoint_queue", node.entryRoom)
        assertTrue(node.unlockConditions.any { it.type == "quest_completed" && it.questId == "w1_mq01" })
        assertEquals("checkpoint_bay", rooms.getValue(node.entryRoom).connections["north"])
        assertEquals("checkpoint_booth", rooms.getValue("checkpoint_bay").connections["east"])
        val tasks = reader.readList<Quest>("quests.json").single { it.id == "w1_mq02" }
            .stages.flatMap { it.tasks }.associateBy { it.id }
        val approach = tasks.getValue("approach_admin_gate").text
        assertTrue(approach.contains(node.title))
        assertTrue(approach.contains("hub map"))
        assertTrue(tasks.getValue("meet_zeke").text.contains(rooms.getValue("checkpoint_booth").title))
        val directions = reader.readList<DialogueLine>("dialogue.json").filter {
            it.id in setOf("jed_w1_mq02_handoff_3", "jed_w1_mq02_reminder")
        }
        assertEquals(2, directions.size)
        directions.forEach {
            assertTrue(it.text.contains(node.title))
            assertTrue(it.text.contains("east"))
            assertTrue(it.text.contains(rooms.getValue("checkpoint_bay").title))
            assertTrue(it.text.contains(rooms.getValue("checkpoint_booth").title))
            assertFalse(it.text.contains("Admin window"))
        }
    }

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
