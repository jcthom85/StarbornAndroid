package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.exploration.presentation.resolveRoomDescription
import org.junit.Assert.*
import org.junit.Test

class Worlds456ReadinessRegressionTest {
    private class Harness {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val store = GameSessionStore()
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        })
        val events = reader.readList<GameEvent>("events.json")
        val started = mutableListOf<String>()
        val messages = mutableListOf<String>()
        val manager = EventManager(events, store, EventHooks(
            onStartDialogue = { started += it }, onMessage = { messages += it },
            onQuestTaskUpdated = { q, t -> if (q != null && t != null) store.setQuestTaskCompleted(q, t, true) },
            onQuestStageAdvanced = { q, stage -> if (q != null && stage != null) store.setQuestStage(q, stage) },
            onGiveItem = { id, n -> inventory.addItem(id, n) }, onGiveXp = store::addXp
        ))
        val dialogue = DialogueService(reader.readList<DialogueLine>("dialogue.json"),
            { isDialogueConditionMet(it, store.state.value, inventory) },
            { manager.performActions(DialogueTriggerParser.parse(it)) })
        fun act(id: String) = manager.handleTrigger("player_action", EventPayload.Action(id))
        fun requestedDialogue(): DialogueSession = requireNotNull(dialogue.startDialogueAt(started.last().removePrefix("line:")))
        fun prismReady() {
            store.startQuest("w3_mq14")
            store.setQuestTaskCompleted("w3_mq14", "enter_archive", true)
            listOf("w3_mq14_read_containment_field", "w3_mq14_read_prism_shutters", "w3_mq14_trace_command_tethers").forEach(::act)
        }
    }

    @Test fun overclockCalibrationRequiresTheMapAndPaysOnlyAfterRecovery() {
        val h = Harness()
        h.act("w4_sq20_survive_waves")
        assertFalse("ms_w4_hazard_trial_cleared" in h.store.state.value.completedMilestones)
        h.act("w4_sq20_map_hazards")
        h.act("w4_sq20_steal_overclock")
        assertFalse("w4_sq20" in h.store.state.value.completedQuests)
        h.act("w4_sq20_survive_waves")
        assertTrue("ms_w4_hazard_trial_cleared" in h.store.state.value.completedMilestones)
        h.act("w4_sq20_steal_overclock")
        assertEquals(450, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["thermal_clip"])
        h.act("w4_sq20_steal_overclock")
        assertEquals(450, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["thermal_clip"])
    }

    @Test fun allTwentyEightLateReturnShortcutsWorkWithLegacyReceipts() {
        val h = Harness()
        val shortcuts = h.events.filter { it.id.startsWith("warp_") && it.id.endsWith("_to_entry") &&
            listOf("warp_foundry_", "warp_orbital_", "warp_deep_", "warp_source_").any(it.id::startsWith) }
        assertEquals(28, shortcuts.size)
        shortcuts.forEach { event ->
            h.store.markEventCompleted(event.id)
            repeat(2) {
                h.store.setRoom("source_center")
                h.act(requireNotNull(event.trigger.action))
                assertEquals(event.actions.single { it.type == "warp" }.roomId, h.store.state.value.roomId)
                assertTrue(h.manager.canHandleTrigger("player_action", EventPayload.Action(requireNotNull(event.trigger.action))))
            }
        }
    }

    @Test fun displayNameFallbackPreservesNormalConditionalEntrySelection() {
        val h = Harness()
        val npcs = h.reader.readList<Npc>("npcs.json").associateBy { it.id }
        val thorne = npcs.getValue("thorne")
        val speaker = h.dialogue.resolveNpcSpeaker(thorne.name, thorne.interactions.first().dialogueId)
        assertEquals("Thorne", speaker)
        h.store.startQuest("w5_mq22")
        assertNull(h.dialogue.startDialogue(speaker))
        h.store.setQuestTaskCompleted("w5_mq22", "access_mainframe", true)
        assertEquals("thorne_w5_mq22_intro", h.dialogue.startDialogue(speaker)?.current()?.id)
        assertEquals("Nova", h.dialogue.resolveNpcSpeaker("Nova", "thorne_w5_mq22_intro"))
        assertEquals("Unknown", h.dialogue.resolveNpcSpeaker("Unknown", "missing"))
    }

    @Test fun authoredSceneOpenersRequireTheirPhysicalRoomAndDoNotAwardOnOpen() {
        val fixtures = listOf(
            Triple("w4_mq17_access_phantom_terminal", "foundry_waste_intake", "w4_mq17"),
            Triple("w4_mq17_regroup_springs", "foundry_cooling_springs", "w4_mq17"),
            Triple("w4_mq20_confront_rylos", "foundry_titan_dock", "w4_mq20"),
            Triple("w5_mq22_find_thorne", "deep_mainframe_nave", "w5_mq22"),
            Triple("w5_mq24_find_elara", "deep_anchor_chamber", "w5_mq24"),
            Triple("w5_mq25_soloist", "deep_throne_room", "w5_mq25"),
            Triple("w6_mq30_confront_vale", "source_center", "w6_mq30")
        )
        fixtures.forEach { (event, room, quest) ->
            val h = Harness(); h.store.startQuest(quest)
            when (event) {
                "w4_mq17_regroup_springs" -> h.store.setQuestTaskCompleted(quest, "access_phantom_terminal", true)
                "w5_mq22_find_thorne" -> h.store.setQuestTaskCompleted(quest, "access_mainframe", true)
                "w5_mq24_find_elara" -> h.store.setQuestTaskCompleted(quest, "enter_chamber", true)
                "w5_mq25_soloist" -> h.store.setQuestTaskCompleted(quest, "reach_throne", true)
            }
            h.store.setRoom("unrelated_room"); h.act(event + "_scene"); assertTrue(h.started.isEmpty())
            h.store.setRoom(room)
            val before = h.store.state.value
            h.act(event + "_scene")
            assertEquals(1, h.started.size)
            assertEquals(before.playerXp, h.store.state.value.playerXp)
            assertEquals(before.questTasksCompleted, h.store.state.value.questTasksCompleted)
            val session = h.requestedDialogue()
            assertNotNull(session.current())
            while (!session.isFinished()) session.advance()
            assertNotEquals(before.questTasksCompleted, h.store.state.value.questTasksCompleted)
        }
    }
}
