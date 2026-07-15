package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Hub2CriticalFlowTest {

    @Test
    fun world2MainQuestProgressionsEndToEnd() {
        val harness = Hub2Harness()

        // Setup World 2 start state (after MQ_01 completion)
        harness.store.completeQuest("w2_mq01")
        harness.store.setMilestone("ms_w2_mq01_complete")
        harness.store.startQuest("w2_mq02")
        harness.store.setTrackedQuest("w2_mq02")
        harness.store.setInventory(mapOf("ghost_signal_cell" to 1, "chime" to 1)) // start with Chime

        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w2_mq02"))

        // --- MQ02: The Signal ---
        // Talk to Zeke
        val zeke1 = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zeke1)
        assertEquals("zeke_w2_mq02_intro_1", zeke1?.current()?.id)
        zeke1?.advanceUntilFinished()

        // Clear canopy path (defeat echo-borer in sector9_canopy)
        harness.store.setRoom("sector9_canopy")
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("echo_borer"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq02"].orEmpty().contains("clear_canopy_path"))

        // Reach Temple Gate
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_temple_gate"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq02"].orEmpty().contains("locate_temple_gate"))

        // Use Chime on gate
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq02_use_chime"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_mq02"))
        assertTrue(state.questTasksCompleted["w2_mq02"].orEmpty().contains("insert_chime"))
        assertTrue(state.completedMilestones.contains("ms_w2_mq02_complete"))
        assertEquals("sector9_hall_of_echoes", state.roomId)
        assertTrue(state.activeQuests.contains("w2_mq03"))

        // --- MQ03: Sleeping Giant ---
        // Inspect murals in Hall of Echoes
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_inspect_murals"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq03"].orEmpty().contains("inspect_murals"))
        assertTrue(harness.store.state.value.completedMilestones.contains("ms_w2_murals_read"))

        // Enter Stasis Chamber
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_stasis_chamber"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq03"].orEmpty().contains("find_stasis_chamber"))

        // Zeke now hints at the environmental chain instead of presenting answer choices.
        val zekeHint = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zekeHint)
        assertEquals("zeke_w2_mq03_puzzle_hint_1", zekeHint?.current()?.id)
        assertTrue(zekeHint?.choices().orEmpty().isEmpty())

        // The ring console cannot complete until the chamber has been read and stabilized.
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_align_complete"))
        assertFalse(harness.store.state.value.questTasksCompleted["w2_mq03"].orEmpty().contains("align_stasis_rings"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_inspect_pod"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_read_mural_overview"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_stabilize_coolant"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w2_stasis_pod_read"))
        assertTrue(state.completedMilestones.contains("ms_w2_stasis_overview_read"))
        assertTrue(state.completedMilestones.contains("ms_w2_coolant_stabilized"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq03_align_complete"))

        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w2_mq03"].orEmpty().contains("align_stasis_rings"))
        assertTrue(state.questTasksCompleted["w2_mq03"].orEmpty().contains("talk_to_orion"))
        assertTrue(state.partyMembers.contains("orion"))
        assertTrue(state.completedQuests.contains("w2_mq03"))
        assertTrue(state.completedMilestones.contains("ms_w2_mq03_complete"))
        assertTrue(state.completedMilestones.contains("ms_w2_bridge_recovered"))
        assertTrue(state.inventory["bridge_relic"].orZero() == 1)
        assertTrue(state.activeQuests.contains("w2_mq04"))

        // --- MQ04: The Hunter ---
        // Enter Canopy Ridge
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_canopy_ridge"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq04"].orEmpty().contains("reach_canopy_ridge"))

        // Confront Gh0st
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq04_confront"))
        assertEquals("Gh0st", harness.startedDialogues.lastOrNull())
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq04"].orEmpty().contains("confront_stalker"))
        val ghostTalk = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(ghostTalk)
        assertEquals("ghost_default_talk", ghostTalk?.current()?.id)

        // Defeat mutated Source Beast
        harness.store.setRoom("sector9_canopy_ridge")
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("the_beast"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w2_mq04"].orEmpty().contains("defeat_the_beast"))
        assertTrue(state.partyMembers.contains("gh0st"))
        assertFalse(state.completedQuests.contains("w2_mq04"))
        assertFalse(state.completedMilestones.contains("ms_w2_mq04_complete"))
        assertFalse(state.unlockedSkills.contains("nova_link"))
        assertFalse(state.activeQuests.contains("w2_mq05"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq04_anchor_drill"))
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w2_mq04"].orEmpty().contains("complete_anchor_drill"))
        assertTrue(state.completedQuests.contains("w2_mq04"))
        assertTrue(state.completedMilestones.contains("ms_w2_mq04_complete"))
        assertTrue(state.completedMilestones.contains("ms_w2_link_unlocked"))
        assertTrue(state.unlockedSkills.contains("nova_link"))
        assertTrue(state.activeQuests.contains("w2_mq05"))

        // --- MQ05: Liftoff ---
        // Enter Source Gate room
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_source_gate"))

        // Orion now points to the gate hardware instead of offering a chord menu.
        val orionHint = harness.dialogue.startDialogue("Orion")
        assertNotNull(orionHint)
        assertEquals("orion_w2_mq05_gate_1", orionHint?.current()?.id)
        assertTrue(orionHint?.choices().orEmpty().isEmpty())

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_bypass_gate"))
        assertFalse(harness.store.state.value.questTasksCompleted["w2_mq05"].orEmpty().contains("bypass_source_gate"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_stabilize_horn"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_ground_cup"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_read_pressure_gauge"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_overload_breakers"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w2_source_horn_stabilized"))
        assertTrue(state.completedMilestones.contains("ms_w2_source_cup_grounded"))
        assertTrue(state.completedMilestones.contains("ms_w2_source_pressure_mapped"))
        assertTrue(state.completedMilestones.contains("ms_w2_source_breakers_starved"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_bypass_gate"))

        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w2_mq05"].orEmpty().contains("bypass_source_gate"))
        assertEquals("sector9_hangar_bay", state.roomId)

        // Inspect Astra
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_inspect_astra"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq05"].orEmpty().contains("inspect_astra"))

        // Collect power conduits from crash site
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_collect_conduits"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq05"].orEmpty().contains("collect_power_conduits"))

        // Reboot Bridge Relic
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_reboot"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_mq05"].orEmpty().contains("reboot_bridge_relic"))
        assertTrue(harness.store.state.value.completedMilestones.contains("ms_w2_bridge_installed"))
        assertTrue(harness.store.state.value.inventory["bridge_relic"] == null)
        assertTrue(harness.store.state.value.inventory["ghost_signal_cell"].orZero() == 1)

        // Launch ship (Astra) -> warps to the sky, complete quest, collisions, warps to admin lobby
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq05_launch"))
        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w2_mq05"].orEmpty().contains("launch_ship"))
        assertTrue(state.completedQuests.contains("w2_mq05"))
        assertTrue(state.completedMilestones.contains("ms_w2_mq05_complete"))
        assertEquals("spire_sewers_landing", state.roomId) // Transitioned to World 3!
        assertTrue(state.activeQuests.contains("w3_mq11"))
        assertEquals("w3_mq11", state.trackedQuestId)
    }

    @Test
    fun world2SideQuestsFlows() {
        val harness = Hub2Harness()
        harness.store.completeQuest("w2_mq01")

        // --- SQ01: Botanist (Zeke) ---
        val zekeIntro = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zekeIntro)
        assertEquals("zeke_w2_sq01_intro_1", zekeIntro?.current()?.id)
        
        // Accept side quest
        val zekeNext = zekeIntro?.choose("zeke_w2_sq01_accept")
        zekeIntro?.advanceUntilFinished()

        assertTrue(harness.store.state.value.activeQuests.contains("w2_sq01"))

        // Scan all 5 plants
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_ferns")) // flora 1
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_weeds")) // flora 2
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_moss"))  // flora 3
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_spores")) // flora 4
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_roots"))  // flora 5

        // Talk to Zeke to turn in
        val zekeTurnin = harness.dialogue.startDialogue("Zeke")
        assertNotNull(zekeTurnin)
        assertEquals("zeke_w2_sq01_turnin_1", zekeTurnin?.current()?.id)
        zekeTurnin?.advanceUntilFinished()

        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_sq01"))
        assertTrue(state.inventory["painkillers"].orZero() >= 2)

        // --- SQ02: Lost Patrol (triangulate three signals, then recover the cache) ---
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq02_trace_alpha"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq02_trace_beta"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq02_trace_gamma"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq02_recover_transceiver"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.none { it == "w2_sq02" })
        assertTrue(state.inventory["damaged_thermal_cutter"].orZero() >= 1)
        assertTrue(state.inventory["schematic_thermal_cutter"].orZero() >= 1)
        assertTrue(state.inventory["mod_corrosive_rounds"].orZero() >= 1)

        harness.store.setInventory(harness.store.state.value.inventory + ("thermal_cutter" to 1))
        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_craft", "thermal_cutter"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_sq02"))
        assertTrue(state.questTasksCompleted["w2_sq02"].orEmpty().contains("repair_thermal_cutter"))

        // --- SQ03: Tideglass Day (Orion) ---
        // Setup Orion in party
        harness.store.addPartyMember("orion")
        harness.store.completeQuest("w2_mq03") // complete MQ03 to unlock Orion dialogues

        val orionIntro = harness.dialogue.startDialogue("Orion")
        assertNotNull(orionIntro)
        assertEquals("orion_w2_sq03_intro_1", orionIntro?.current()?.id)
        orionIntro?.choose("orion_w2_sq03_accept")
        orionIntro?.advanceUntilFinished()

        assertTrue(harness.store.state.value.activeQuests.contains("w2_sq03"))

        // Visit beach
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_stream_pools"))
        assertTrue(harness.store.state.value.questTasksCompleted["w2_sq03"].orEmpty().contains("visit_beach"))

        // Gather tideglass
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq03_gather_tideglass"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.none { it == "w2_sq03" })
        assertTrue(state.inventory["schematic_source_resin"].orZero() >= 1)
        assertTrue(state.inventory["herb"].orZero() >= 1)
        assertTrue(state.inventory["beast_meat"].orZero() >= 1)

        harness.store.setInventory(harness.store.state.value.inventory + ("source_resin" to 1))
        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_craft", "source_resin"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_sq03"))
        assertTrue(state.questTasksCompleted["w2_sq03"].orEmpty().contains("craft_source_resin"))
        assertTrue(state.inventory["source_resin"].orZero() >= 1)
        assertTrue(state.inventory["painkillers"].orZero() >= 2) // additional painkillers

        // --- SQ04: Ancient Echoes (Orion mural tuning) ---
        val orionMuralIntro = harness.dialogue.startDialogue("Orion")
        assertNotNull(orionMuralIntro)
        assertEquals("orion_w2_sq04_intro_1", orionMuralIntro?.current()?.id)
        orionMuralIntro?.choose("orion_w2_sq04_accept")
        orionMuralIntro?.advanceUntilFinished()

        assertTrue(harness.store.state.value.activeQuests.contains("w2_sq04"))

        // Recover all three Sanctuary wing crystals before the mural can be tuned.
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq04_crystal_west"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq04_crystal_east"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq04_crystal_north"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w2_crystal_west_seated"))
        assertTrue(state.completedMilestones.contains("ms_w2_crystal_east_seated"))
        assertTrue(state.completedMilestones.contains("ms_w2_crystal_north_seated"))

        // Orion points the player to the resonator columns instead of offering answer choices.
        harness.store.setRoom("sector9_hall_of_echoes")
        val tuneMural = harness.dialogue.startDialogue("Orion")
        assertNotNull(tuneMural)
        assertEquals("orion_w2_sq04_tune_1", tuneMural?.current()?.id)
        assertTrue(tuneMural?.choices().orEmpty().isEmpty())
        tuneMural?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_sq04"))
        assertTrue(state.inventory["focus_conduit"].orZero() >= 1)

        // --- SQ05: Stolen Tech (Gh0st search vents) ---
        harness.store.addPartyMember("gh0st")
        harness.store.completeQuest("w2_mq04")

        val ghostIntro = harness.dialogue.startDialogue("Gh0st")
        assertNotNull(ghostIntro)
        assertEquals("ghost_w2_sq05_intro_1", ghostIntro?.current()?.id)
        ghostIntro?.choose("ghost_w2_sq05_accept")
        ghostIntro?.advanceUntilFinished()

        assertTrue(harness.store.state.value.activeQuests.contains("w2_sq05"))

        // Blind the security grid, pass its guard pattern, then search the tech alcove.
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq05_hack_grid"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq05_bypass_guards"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_sq05_search_vents"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.none { it == "w2_sq05" })
        assertTrue(state.inventory["dominion_transmitter_core"].orZero() >= 1)
        assertTrue(state.inventory["schematic_rapid_capacitor"].orZero() >= 1)

        harness.store.setInventory(harness.store.state.value.inventory + ("rapid_capacitor" to 1))
        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_craft", "rapid_capacitor"))
        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w2_sq05"))
        assertTrue(state.questTasksCompleted["w2_sq05"].orEmpty().contains("assemble_capacitor"))
        assertTrue(state.inventory["rapid_capacitor"].orZero() >= 1)
    }

    private class Hub2Harness(initialState: GameSessionState? = null) {
        val store = GameSessionStore()
        val events: EventManager
        val dialogue: DialogueService
        val messages = mutableListOf<String>()
        val startedDialogues = mutableListOf<String>()

        init {
            initialState?.let(store::restore)
            events = EventManager(
                events = loadEvents(),
                sessionStore = store,
                eventHooks = EventHooks(
                    onMessage = { messages += it },
                    onSystemTutorial = { _, _, _, done -> done() },
                    onQuestTaskUpdated = { questId, taskId ->
                        if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                            store.setQuestTaskCompleted(questId, taskId, true)
                        }
                    },
                    onQuestStageAdvanced = { questId, stageId ->
                        if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                            store.setQuestStage(questId, stageId)
                        }
                    },
                    onStartDialogue = { npcName -> startedDialogues += npcName },
                    onGiveItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val next = current + (itemId to (current[itemId].orZero() + quantity.coerceAtLeast(1)))
                        store.setInventory(next)
                    },
                    onTakeItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val available = current[itemId].orZero()
                        val requested = quantity.coerceAtLeast(1)
                        if (available < requested) {
                            false
                        } else {
                            val remaining = available - requested
                            val next = if (remaining > 0) {
                                current + (itemId to remaining)
                            } else {
                                current - itemId
                            }
                            store.setInventory(next)
                            true
                        }
                    },
                    onGiveXp = { amount -> store.addXp(amount) },
                    onQuestCompleted = ::handleQuestCompleted
                )
            )
            dialogue = DialogueService(
                loadDialogue(),
                DialogueConditionEvaluator { condition -> conditionMet(condition, store.state.value) },
                DialogueTriggerHandler { trigger -> events.performActions(DialogueTriggerParser.parse(trigger)) }
            )
        }

        private fun handleQuestCompleted(questId: String?) {
            if (!questId.isNullOrBlank()) {
                events.handleTrigger("quest_stage_complete", EventPayload.QuestStage(questId))
            }
        }
    }

    private companion object {
        private val moshi = MoshiProvider.instance

        private fun conditionMet(raw: String?, state: GameSessionState): Boolean {
            if (raw.isNullOrBlank()) return true
            return raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .all { token ->
                    val parts = token.split(':', limit = 2)
                    val type = parts[0].trim().lowercase()
                    val value = parts.getOrNull(1)?.trim().orEmpty()
                    when (type) {
                        "milestone" -> value in state.completedMilestones
                        "milestone_set" -> value in state.completedMilestones
                        "milestone_not_set" -> value !in state.completedMilestones
                        "quest", "quest_active" -> value in state.activeQuests
                        "quest_not_started" -> value !in state.activeQuests &&
                            value !in state.completedQuests &&
                            value !in state.failedQuests
                        "quest_completed" -> value in state.completedQuests
                        "quest_not_completed" -> value !in state.completedQuests
                        "quest_stage" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId != null && stageId != null &&
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
                        }
                        "quest_stage_not" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId == null || stageId == null ||
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
                        }
                        "quest_task_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId != null && taskId != null &&
                                state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "quest_task_not_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId == null || taskId == null ||
                                !state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "item" -> value in state.inventory && state.inventory[value].orZero() > 0
                        "item_not" -> value !in state.inventory || state.inventory[value].orZero() <= 0
                        else -> true
                    }
                }
        }

        private fun parseQuestPair(raw: String): Pair<String?, String?> {
            val parts = raw.split(':', limit = 2)
            val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
            val value = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
            return questId to value
        }

        private fun Int?.orZero(): Int = this ?: 0

        private fun loadDialogue(): List<DialogueLine> {
            val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
            val adapter = moshi.adapter<List<DialogueLine>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/dialogue.json").readText()))
        }

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            val adapter = moshi.adapter<List<GameEvent>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText()))
        }
    }
}

private fun DialogueSession.advanceUntilFinished() {
    while (!isFinished()) {
        advance()
    }
}
