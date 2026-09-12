package com.example.starborn.feature.exploration

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import com.example.starborn.feature.exploration.ui.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.*
import org.junit.Test

class WorldTwoDiscoverabilityStateTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)

    @Test fun `completed ridge quest repairs absent flags but milestone alone does not`() {
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_canopy_ridge" }
        val milestones = setOf("ms_w2_mq03_complete", "ms_w2_mq04_complete")
        val complete = GameSessionState(completedQuests = setOf("w2_mq04"),
            completedMilestones = milestones).migrateOpeningNarrativeState()
        assertEquals(setOf("hunter_confronted", "beast_defeated", "anchor_drill_complete"),
            complete.roomStates.getValue(room.id).filterValues { it }.keys)
        assertEquals(listOf("Look at Shield"), room.actions.filter {
            narrativeActionVisible(it, emptyMap(), complete.roomStates.getValue(room.id), complete.completedMilestones)
        }.map { it["name"] })

        // A milestone is persisted independently of quest completion and room flags.
        val partial = GameSessionState(completedMilestones = milestones).migrateOpeningNarrativeState()
        assertTrue(partial.roomStates[room.id].isNullOrEmpty())
        val confront = room.actions.single { it["name"] == "Confront stalker" }
        assertTrue(narrativeActionVisible(confront, emptyMap(), emptyMap(), partial.completedMilestones))
        // Keep the compatibility variant: MQ04's milestone alone must not select
        // the completed-drill prose when the hunter flag is absent.
        assertNotEquals(room.description, room.descriptionVariants[1].description)
        assertEquals(room.descriptionVariants[1].description,
            resolveRoomDescription(room, emptyMap(), partial.completedMilestones, false))
        assertTrue(requireNotNull(resolveRoomDescription(room, emptyMap(), partial.completedMilestones, false))
            .contains("Confront stalker", true))
        val store = GameSessionStore().apply { restore(partial) }
        dispatch(store, "w2_mq04_confront")
        assertEquals(partial, store.state.value) // No active quest: visible does not mean usable.
        store.startQuest("w2_mq04")
        dispatch(store, "w2_mq04_confront")
        assertTrue("ms_w2_stalker_confronted" in store.state.value.completedMilestones)
        assertTrue("w2_mq04_confront" in store.state.value.completedEvents)
    }

    @Test fun `ridge task migration restores each corresponding flag independently`() {
        val pairs = listOf("confront_stalker" to "hunter_confronted",
            "defeat_the_beast" to "beast_defeated", "complete_anchor_drill" to "anchor_drill_complete")
        pairs.forEach { (task, flag) ->
            val migrated = GameSessionState(questTasksCompleted = mapOf("w2_mq04" to setOf(task)))
                .migrateOpeningNarrativeState()
            assertEquals(mapOf(flag to true), migrated.roomStates["sector9_canopy_ridge"])
        }
    }

    @Test fun `all emitter milestone combinations distinguish visibility from event usability`() {
        val rooms = reader.readList<Room>("rooms.json")
        listOf("sector9_gate_emitter_left" to "ms_w2_source_horn_stabilized",
            "sector9_gate_emitter_right" to "ms_w2_source_cup_grounded").forEach { (id, source) ->
            val room = rooms.single { it.id == id }
            val action = room.actions.single()
            val retired = action["requires_milestone_not_set"] as String
            for (hasSource in listOf(false, true)) for (hasRetired in listOf(false, true)) {
                val milestones = setOfNotNull(source.takeIf { hasSource }, retired.takeIf { hasRetired })
                val state = GameSessionState(activeQuests = setOf("w2_mq05"), completedMilestones = milestones)
                    .migrateOpeningNarrativeState()
                assertEquals(milestones, state.completedMilestones)
                assertEquals(!hasRetired, narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
                val store = GameSessionStore().apply { restore(state) }
                dispatch(store, action["action_event"] as String)
                if (hasSource) assertEquals(state, store.state.value)
                else assertTrue(store.state.value.completedMilestones.containsAll(setOf(source, retired)))
            }
        }
    }

    @Test fun `launch without inspection leaves console visible but its event unusable`() {
        val store = GameSessionStore().apply {
            startQuest("w2_mq05")
            setQuestTaskCompleted("w2_mq05", "reboot_bridge_relic", true)
        }
        dispatch(store, "w2_mq05_launch")
        val state = store.state.value.migrateOpeningNarrativeState()
        assertTrue("w2_mq05" in state.completedQuests)
        assertFalse("ms_w2_astra_inspected" in state.completedMilestones)
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_hangar_bay" }
        val console = room.actions.single { it["name"] == "dark console" }
        assertTrue(narrativeActionVisible(console, emptyMap(), emptyMap(), state.completedMilestones))
        assertFalse(requireNotNull(resolveRoomDescription(room, emptyMap(), state.completedMilestones, false))
            .contains("dark console", true))
        store.restore(state)
        dispatch(store, "w2_mq05_inspect_astra")
        assertEquals(state, store.state.value)
    }

    private fun dispatch(store: GameSessionStore, action: String) {
        val events = reader.readList<GameEvent>("events.json").filter { it.trigger.action == action }
        assertTrue("Missing shipped event for $action", events.isNotEmpty())
        EventManager(events, store).handleTrigger("player_action", EventPayload.Action(action))
    }

    @Test fun `ridge migrated partial states name every visible executable objective`() {
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_canopy_ridge" }
        val events = reader.readList<GameEvent>("events.json")
        val flags = listOf("hunter_confronted", "beast_defeated", "anchor_drill_complete")
        val tasks = listOf("confront_stalker", "defeat_the_beast", "complete_anchor_drill")
        val exercised = mutableSetOf<String>()
        // Absent, false and true flags; every task subset; independent milestones;
        // inactive, active and completed quest records, through the load migration.
        for (flagCode in 0 until 27) for (taskMask in 0 until 8)
        for (milestoneMask in 0 until 4) for (questMode in 0 until 3) {
            var code = flagCode
            val persisted = buildMap {
                flags.forEach { flag ->
                    val value = code % 3
                    code /= 3
                    if (value != 0) put(flag, value == 2)
                }
            }
            val state = GameSessionState(
                roomStates = mapOf(room.id to persisted),
                questTasksCompleted = mapOf("w2_mq04" to tasks.filterIndexed { i, _ -> taskMask and (1 shl i) != 0 }.toSet()),
                activeQuests = if (questMode == 1) setOf("w2_mq04") else emptySet(),
                completedQuests = if (questMode == 2) setOf("w2_mq04") else emptySet(),
                completedMilestones = setOfNotNull(
                    "ms_w2_mq03_complete".takeIf { milestoneMask and 1 != 0 },
                    "ms_w2_mq04_complete".takeIf { milestoneMask and 2 != 0 })
            ).migrateOpeningNarrativeState()
            val roomState = room.state.mapValues { it.value as Boolean } + state.roomStates.getValue(room.id)
            val prose = requireNotNull(resolveRoomDescription(room, roomState, state.completedMilestones, false))
            room.actions.filter { narrativeActionVisible(it, emptyMap(), roomState, state.completedMilestones) }
                .forEach { action ->
                    val eventName = action["action_event"] as? String ?: return@forEach
                    val store = GameSessionStore().apply { restore(state) }
                    var executed = false
                    EventManager(events.filter { it.trigger.action == eventName }, store, EventHooks(
                        onEventCompleted = { executed = true },
                        onSetRoomState = { id, key, value -> store.setRoomState(requireNotNull(id), key, value) },
                        onQuestTaskUpdated = { id, task -> store.setQuestTaskCompleted(requireNotNull(id), requireNotNull(task), true) }
                    )).handleTrigger("player_action", EventPayload.Action(eventName))
                    if (executed) {
                        exercised += eventName
                        assertTrue("Missing ${action["name"]}: flags=$persisted tasks=$taskMask milestones=$milestoneMask quest=$questMode",
                            prose.contains(action["name"] as String, true))
                    }
                }
        }
        assertEquals(setOf("w2_mq04_confront", "w2_mq04_beast_ambush", "w2_mq04_anchor_drill"), exercised)
    }

    @Test fun `ridge description priority covers every raw flag and milestone combination`() {
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_canopy_ridge" }
        val flags = listOf("hunter_confronted", "beast_defeated", "anchor_drill_complete")
        for (flagCode in 0 until 27) for (milestoneMask in 0 until 4) {
            var code = flagCode
            val state = buildMap {
                flags.forEach { key ->
                    val value = code % 3
                    code /= 3
                    if (value != 0) put(key, value == 2)
                }
            }
            val milestones = setOfNotNull(
                "ms_w2_mq03_complete".takeIf { milestoneMask and 1 != 0 },
                "ms_w2_mq04_complete".takeIf { milestoneMask and 2 != 0 })
            val prose = requireNotNull(resolveRoomDescription(room, state, milestones, false))
            room.actions.filter { narrativeActionVisible(it, emptyMap(), state, milestones) }.forEach {
                assertTrue("Missing ${it["name"]}: state=$state milestones=$milestones",
                    prose.contains(it["name"] as String, true))
            }
            // Compatibility descriptions must not advertise objectives their gates hide.
            room.descriptionVariants.take(3).filter { it.description == prose &&
                ("ms_w2_mq03_complete" in milestones || state["hunter_confronted"] == true) }.forEach { _ ->
                room.actions.filter { (it["name"] as String) != "Look at Shield" &&
                    prose.contains(it["name"] as String, true) }.forEach {
                    assertTrue(narrativeActionVisible(it, emptyMap(), state, milestones))
                }
            }
        }
    }

    @Test fun `ridge progressive states expose exactly the current objective and shield`() {
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_canopy_ridge" }
        val state = mutableMapOf<String, Boolean>()
        val milestones = mutableSetOf("ms_w2_mq03_complete")
        fun verify(objective: String?) {
            val visible = room.actions.filter { narrativeActionVisible(it, emptyMap(), state, milestones) }
            val expected = setOfNotNull("Look at Shield", objective)
            assertEquals(expected, visible.map { it["name"] }.toSet())
            val prose = requireNotNull(resolveRoomDescription(room, state, milestones, false))
            visible.forEach { action ->
                assertTrue("Visible ${action["name"]} must occur in selected ridge prose",
                    prose.contains(action["name"] as String, ignoreCase = true))
            }
        }
        // Seed the cumulative states from the authored encounter sequence; no combat simulation.
        verify("Confront stalker")
        state["hunter_confronted"] = true
        verify("Face the Beast")
        state["beast_defeated"] = true
        verify("Anchor Drill")
        state["anchor_drill_complete"] = true
        milestones += "ms_w2_mq04_complete"
        verify(null)
    }

    @Test fun `emitter events select completed prose and retire their actions together`() {
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val events = reader.readList<GameEvent>("events.json")
        listOf("sector9_gate_emitter_left", "sector9_gate_emitter_right").forEach { id ->
            val room = rooms.getValue(id)
            val action = room.actions.single()
            val eventName = action["action_event"] as String
            val store = GameSessionStore().apply { startQuest("w2_mq05") }
            assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), store.state.value.completedMilestones))
            EventManager(events.filter { it.trigger.action == eventName }, store)
                .handleTrigger("player_action", EventPayload.Action(eventName))
            val milestones = store.state.value.completedMilestones
            assertTrue((action["requires_milestone_not_set"] as String) in milestones)
            assertEquals(room.descriptionVariants.first().description,
                resolveRoomDescription(room, emptyMap(), milestones, false))
            assertFalse(narrativeActionVisible(action, emptyMap(), emptyMap(), milestones))
        }
    }
}
