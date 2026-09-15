package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import org.junit.Assert.*
import org.junit.Test

/** Regression checks for authored World 1 progression. */
class WorldOneProgressionAuditTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val events = reader.readList<GameEvent>("events.json")

    private fun launchState() = GameSessionStore().apply {
        startQuest("w1_mq05")
        setQuestStage("w1_mq05", "launch_pod")
        setRoom("launch_pod")
    }

    @Test fun `early console tap stays retryable after restore`() {
        val store = launchState()
        val event = events.single { it.id == "w1_mq05_use_nav_console" }
        val room = reader.readList<Room>("rooms.json").single { it.id == "launch_pod" }
        val action = room.actions.single { it["action_event"] == "use_nav_console" }
        assertNull(action["requires_milestone"])
        var launches = 0
        val hooks = EventHooks(onPlayCinematic = { _, _ -> launches++ })
        EventManager(listOf(event), store, hooks).handleTrigger("player_action", EventPayload.Action("use_nav_console"))
        assertFalse("Locked feedback must not consume the launch event",
            event.id in store.state.value.completedEvents)
        store.setMilestone("ms_w1_chime_spliced")
        val restored = GameSessionStore().apply { restore(store.state.value) }
        EventManager(listOf(event), restored, hooks).handleTrigger("player_action", EventPayload.Action("use_nav_console"))
        assertEquals(1, launches)
    }

    @Test fun `launch with chime first reaches world two and grants once`() {
        val store = launchState().apply { setMilestone("ms_w1_chime_spliced") }
        var xp = 0
        val manager = EventManager(events, store, EventHooks(
            onGiveXp = { xp += it },
            onQuestTaskUpdated = { quest, task ->
                if (quest != null && task != null) store.setQuestTaskCompleted(quest, task, true)
            }
        ))
        manager.handleTrigger("player_action", EventPayload.Action("use_nav_console"))
        assertTrue("w1_mq05" in store.state.value.completedQuests)
        assertTrue("w2_mq01" in store.state.value.activeQuests)
        assertEquals("sector9_crash_site", store.state.value.roomId)
        assertEquals(250, xp)
        manager.handleTrigger("player_action", EventPayload.Action("use_nav_console"))
        assertEquals(250, xp)
    }

    @Test fun `cutter test refuses missing parts without consuming event then permits retry`() {
        val store = GameSessionStore().apply {
            startQuest("w1_mq01")
            listOf("use_tinkering_table", "patch_flux_liner", "confirm_governor_bypass").forEach {
                setQuestTaskCompleted("w1_mq01", it, true)
            }
        }
        val event = events.single { it.id == "w1_mq01_cutter_surge" }
        var scenes = 0
        val manager = EventManager(listOf(event), store, EventHooks(onPlayCinematic = { _, _ -> scenes++ }))
        manager.handleTrigger("player_action", EventPayload.Action("w1_mq01_cutter_surge"))
        assertFalse(event.id in store.state.value.completedEvents)
        assertEquals(0, scenes)
        store.setInventory(mapOf("functional_cryo_inductor" to 1))
        manager.handleTrigger("player_action", EventPayload.Action("w1_mq01_cutter_surge"))
        assertEquals(1, scenes)
    }

    @Test fun `relic cinematic resumes followup after restore`() {
        val store = GameSessionStore().apply {
            startQuest("w1_mq03")
            setRoom("echo_heart")
        }
        val event = events.single { it.id == "w1_mq03_touch_relic" }
        EventManager(listOf(event), store, EventHooks(onPlayCinematic = { _, _ -> }))
            .handleTrigger("player_action", EventPayload.Action("w1_mq03_touch_relic"))
        assertTrue("scene_relic_sync" in store.state.value.pendingEventCinematics)
        val restored = GameSessionStore().apply { restore(store.state.value) }
        var forks = 0
        val manager = EventManager(listOf(event), restored, EventHooks(onGiveItem = { id, qty ->
            if (id == "tuning_fork") forks += qty
        }))
        manager.resumePendingCinematics()
        manager.resumePendingCinematics()
        assertEquals(1, forks)
        assertTrue("w1_mq03" in restored.state.value.completedQuests)
        assertTrue(restored.state.value.pendingEventCinematics.isEmpty())
    }

    @Test fun `missing chime and failed consumption cannot grant splice milestone`() {
        val dialogue = org.json.JSONArray(java.io.File(
            listOf(java.io.File("app/src/main/assets"), java.io.File("src/main/assets")).first { it.isDirectory },
            "dialogue.json"
        ).readText())
        val line = (0 until dialogue.length()).map { dialogue.getJSONObject(it) }
            .single { it.getString("id") == "zeke_w1_mq05_pod_core_4" }
        val actions = com.example.starborn.domain.dialogue.DialogueTriggerParser.parse(line.getString("trigger"))
        val store = launchState()
        var attemptedTake = false
        val manager = EventManager(events, store, EventHooks(onTakeItem = { _, _ -> attemptedTake = true; false }))
        manager.performActions(actions)
        assertFalse(attemptedTake)
        store.setInventory(mapOf("ghost_signal_cell" to 1))
        manager.performActions(actions)
        assertTrue(attemptedTake)
        assertFalse("Failed resource consumption must stop the splice reward",
            "ms_w1_chime_spliced" in store.state.value.completedMilestones)
    }

    @Test fun `migration reopens stuck launch but preserves pending and completed launches`() {
        val state = launchState().state.value.copy(completedEvents = setOf("w1_mq05_use_nav_console"))
        assertFalse("w1_mq05_use_nav_console" in state.migrateOpeningNarrativeState().completedEvents)
        assertTrue("w1_mq05_use_nav_console" in state.copy(pendingEventCinematics = setOf("scene_launch_crash"))
            .migrateOpeningNarrativeState().completedEvents)
        assertTrue("w1_mq05_use_nav_console" in state.copy(completedQuests = setOf("w1_mq05"))
            .migrateOpeningNarrativeState().completedEvents)
    }

    @Test fun `sold opening part is restored once and parts are unsellable`() {
        val store = GameSessionStore().apply {
            startQuest("w1_mq01")
            setQuestTaskCompleted("w1_mq01", "equip_starter_gear", true)
        }
        val migrated = store.state.value.migrateOpeningNarrativeState()
        assertEquals(1, migrated.inventory["cryo_inductor"])
        assertEquals(migrated, migrated.migrateOpeningNarrativeState())
        val items = reader.readList<com.example.starborn.domain.model.Item>("items.json")
        listOf("cryo_inductor", "functional_cryo_inductor").forEach { id ->
            assertTrue(items.single { it.id == id }.unsellable)
        }
    }

    @Test fun `completed fork migration consumes at most one spare coil across reloads`() {
        val state = com.example.starborn.domain.session.GameSessionState(
            completedQuests = setOf("w1_mq03"), inventory = mapOf("functional_cryo_inductor" to 3))
        val once = state.migrateOpeningNarrativeState()
        assertEquals(2, once.inventory["functional_cryo_inductor"])
        assertEquals(once, once.migrateOpeningNarrativeState())
    }
}
