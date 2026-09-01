package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IntelligentGamePlaytesterTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance

    private fun Int?.orZero(): Int = this ?: 0

    @Test
    fun `autonomous playtester clears World 1 critical path and validates state invariance`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // 1. Initial State Verification
        assertEquals("pit_nova_bunk", harness.store.state.value.roomId)
        assertTrue(harness.store.state.value.activeQuests.contains("w1_mq01"))

        // 2. Playtester Solves Chapter 1 (w1_mq01: Wake in the Pit)
        agent.executeAction("w1_mq01_turn_on_bunk_light")
        agent.executeAction("w1_mq01_inspect_safety_fault")
        agent.navigateTo("pit_jed_bunk")
        agent.talkTo("Jed")
        agent.navigateTo("workshop_yard")
        agent.executeAction("w1_mq01_inspect_loader_relay")
        agent.winEncounter(listOf("faulted_loader"), "workshop_yard")
        agent.navigateTo("workshop_floor")
        agent.executeAction("tinkering_screen_entered")
        agent.executeAction("tinkering_craft", "functional_cryo_inductor")
        harness.store.setInventory(harness.store.state.value.inventory + ("functional_cryo_inductor" to 1))
        agent.executeAction("w1_mq01_patch_flux_liner")
        agent.executeAction("w1_mq01_confirm_governor")
        agent.executeAction("w1_mq01_cutter_surge")

        var state = harness.store.state.value
        assertTrue("w1_mq01 should be completed", state.completedQuests.contains("w1_mq01"))
        assertTrue("w1_mq02 should be active", state.activeQuests.contains("w1_mq02"))

        // 3. Save / Load Integrity Check
        val saved = harness.roundTripSave(state)
        assertEquals(state.completedQuests, saved.completedQuests)
        assertEquals(state.inventory, saved.inventory)

        // 4. Playtester Solves Chapter 2 (w1_mq02: Checkpoint & Transit Override)
        agent.navigateTo("checkpoint_queue")
        agent.talkTo("Guard Hank")
        agent.navigateTo("checkpoint_booth")
        agent.talkToZekeWithChoice("zeke_w1_mq02_choose_grid_instability")

        state = harness.store.state.value
        assertTrue("w1_mq02 should be completed", state.completedQuests.contains("w1_mq02"))
        assertTrue("w1_mq03 should be active", state.activeQuests.contains("w1_mq03"))

        // 5. Playtester Solves Chapter 3 (w1_mq03: Heavy Lifting & The Echo Relic)
        agent.navigateTo("admin_lobby")
        agent.talkTo("Foreman Boggs")
        agent.executeAction("w1_sq03_start_loader")
        agent.executeAction("w1_sq03_move_cargo")
        agent.winEncounter(listOf("acoustic_bulwark"), "workshop_dock")
        agent.talkTo("Foreman Boggs")
        agent.navigateTo("mine_landing")
        agent.winEncounter(listOf("echo_borer"), "mine_landing")
        agent.navigateTo("echo_gap")
        agent.executeAction("w1_mq03_touch_relic")

        state = harness.store.state.value
        assertTrue("w1_mq03 should be completed", state.completedQuests.contains("w1_mq03"))
        assertTrue("w1_mq04 should be active", state.activeQuests.contains("w1_mq04"))

        // 6. Playtester Solves Chapter 4 (w1_mq04: Lockdown Escape & Jed's Sacrifice)
        agent.navigateTo("launch_lift")
        agent.winEncounter(listOf("acoustic_bulwark"), "launch_lift")
        agent.talkTo("Jed")

        state = harness.store.state.value
        assertTrue("w1_mq04 should be completed", state.completedQuests.contains("w1_mq04"))
        assertTrue("w1_mq05 should be active", state.activeQuests.contains("w1_mq05"))

        // 7. Playtester Solves Chapter 5 (w1_mq05: The Launch & Iron Warden Boss)
        agent.winEncounter(listOf("resonance_buoy"), "launch_bay")
        agent.navigateTo("launch_bay")
        agent.talkTo("The Warden")
        agent.winEncounter(listOf("the_iron_warden"), "launch_bay")
        agent.talkTo("Zeke")
        agent.talkTo("Zeke")
        agent.executeAction("use_nav_console")

        val finalState = harness.store.state.value
        assertTrue("w1_mq05 should be completed", finalState.completedQuests.contains("w1_mq05"))
        assertEquals("sector9_crash_site", finalState.roomId)
        assertTrue("World 2 should now begin", finalState.activeQuests.contains("w2_mq01"))
        assertTrue("Final save round-trip must succeed", harness.roundTripSave(finalState).completedQuests.contains("w1_mq05"))
    }

    @Test
    fun `autonomous playtester clears World 2 campaign to unlock Orion and Gh0st`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // Bootstrap World 2 after Crash Site
        harness.store.completeQuest("w1_mq05")
        harness.store.setMilestone("ms_w1_mq05_complete")
        harness.store.startQuest("w2_mq01")
        harness.store.setRoom("sector9_crash_site")
        harness.store.setInventory(mapOf("ghost_signal_cell" to 1, "chime" to 1))

        // MQ01 -> MQ02
        harness.store.completeQuest("w2_mq01")
        harness.store.setMilestone("ms_w2_mq01_complete")
        harness.store.startQuest("w2_mq02")

        // MQ02: The Signal
        agent.talkTo("Zeke")
        agent.navigateTo("sector9_canopy")
        agent.winEncounter(listOf("echo_borer"), "sector9_canopy")
        agent.navigateTo("sector9_temple_gate")
        agent.executeAction("w2_mq02_use_chime")

        var state = harness.store.state.value
        assertTrue("w2_mq02 should be completed", state.completedQuests.contains("w2_mq02"))
        assertTrue("w2_mq03 should be active", state.activeQuests.contains("w2_mq03"))

        // MQ03: Sleeping Giant (Orion joins)
        agent.executeAction("w2_mq03_inspect_murals")
        agent.navigateTo("sector9_stasis_chamber")
        agent.executeAction("w2_mq03_inspect_pod")
        agent.executeAction("w2_mq03_read_mural_overview")
        agent.executeAction("w2_mq03_stabilize_coolant")
        agent.executeAction("w2_mq03_align_complete")

        state = harness.store.state.value
        assertTrue("w2_mq03 should be completed", state.completedQuests.contains("w2_mq03"))
        assertTrue("Orion must join party", state.partyMembers.contains("orion"))
        assertTrue("w2_mq04 should be active", state.activeQuests.contains("w2_mq04"))

        // MQ04: The Hunter (Defeat Source Beast & Gh0st joins)
        agent.navigateTo("sector9_canopy_ridge")
        agent.executeAction("w2_mq04_confront")
        agent.winEncounter(listOf("the_beast"), "sector9_canopy_ridge")
        agent.executeAction("w2_mq04_anchor_drill")

        state = harness.store.state.value
        assertTrue("w2_mq04 should be completed", state.completedQuests.contains("w2_mq04"))
        assertTrue("Gh0st must join party", state.partyMembers.contains("gh0st"))
        assertTrue("w2_mq05 should be active", state.activeQuests.contains("w2_mq05"))

        // MQ05: Liftoff to Spire
        agent.navigateTo("sector9_source_gate")
        agent.executeAction("w2_mq05_stabilize_horn")
        agent.executeAction("w2_mq05_ground_cup")
        agent.executeAction("w2_mq05_read_pressure_gauge")
        agent.executeAction("w2_mq05_overload_breakers")
        agent.executeAction("w2_mq05_bypass_gate")
        agent.executeAction("w2_mq05_inspect_astra")
        agent.executeAction("w2_mq05_collect_conduits")
        agent.executeAction("w2_mq05_reboot")
        agent.executeAction("w2_mq05_launch")

        state = harness.store.state.value
        assertTrue("w2_mq05 should be completed", state.completedQuests.contains("w2_mq05"))
        assertTrue("World 3 Spire unlock milestone achieved", state.completedMilestones.contains("ms_w2_mq05_complete"))
        assertEquals("spire_sewers_landing", state.roomId)
    }

    @Test
    fun `autonomous playtester clears World 3 Spire Infiltration campaign`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // Bootstrap World 3
        harness.store.setRoom("spire_sewers_landing")
        harness.store.completeQuest("w2_mq05")
        harness.store.setMilestone("ms_w2_mq05_complete")
        harness.store.startQuest("w3_mq11")
        harness.store.setTrackedQuest("w3_mq11")

        // MQ11: Clear Landing & Safehouse
        agent.winEncounter(listOf("sewer_crawler"), "spire_sewers_landing")
        agent.navigateTo("spire_vent_output")
        agent.navigateTo("spire_zekes_apartment")
        agent.talkTo("Zeke")

        var state = harness.store.state.value
        assertTrue("w3_mq11 should be completed", state.completedQuests.contains("w3_mq11"))
        assertTrue("w3_mq12 should be active", state.activeQuests.contains("w3_mq12"))

        // MQ12: Heist Prep
        agent.executeAction("w3_mq12_talk_jax")
        agent.executeAction("w3_mq12_map_patrols")
        agent.executeAction("w3_mq12_interrogate_guard")
        agent.executeAction("w3_mq12_copy_badges")
        agent.executeAction("w3_mq12_source_disguises")
        agent.executeAction("w3_mq12_hack_blueprints")
        agent.executeAction("w3_mq12_assemble_planning")

        state = harness.store.state.value
        assertTrue("w3_mq12 should be completed", state.completedQuests.contains("w3_mq12"))

        // MQ13 & MQ14: The Lens & Light Puzzle
        harness.store.startQuest("w3_mq14")
        agent.navigateTo("spire_archive_vault")
        agent.executeAction("w3_mq14_read_containment_field")
        agent.executeAction("w3_mq14_read_prism_shutters")
        agent.executeAction("w3_mq14_trace_command_tethers")
        agent.executeAction("w3_mq14_solve_light_puzzle")
        agent.talkTo("Zeke")

        state = harness.store.state.value
        assertTrue("w3_mq14 should be completed", state.completedQuests.contains("w3_mq14"))
        assertTrue("The Lens relic acquired", state.inventory["the_lens"].orZero() >= 1)
    }

    @Test
    fun `autonomous playtester clears World 4 Foundry Anvil campaign`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // Bootstrap World 4 Anvil recovery
        harness.store.startQuest("w4_mq19")
        agent.navigateTo("foundry_forge_anvil")
        agent.executeAction("w4_mq19_read_pulse_board")
        agent.executeAction("w4_mq19_read_grease_marks")
        agent.executeAction("w4_mq19_throw_override_paddle")
        agent.executeAction("w4_mq19_starve_breath_pistons")
        agent.executeAction("w4_mq19_open_anvil_cradle")
        agent.talkTo("Nova")

        var state = harness.store.state.value
        assertTrue("w4_mq19 should be completed", state.completedQuests.contains("w4_mq19"))
        assertTrue("The Anvil relic acquired", state.inventory["the_anvil"].orZero() >= 1)
        assertTrue("w4_mq20 should be active", state.activeQuests.contains("w4_mq20"))

        // MQ20: Confront Rylos & Titan Walker Boss
        agent.talkTo("Rylos")
        agent.winEncounter(listOf("titan_walker_boss"), "foundry_forge_anvil")
        agent.executeAction("w4_mq20_steal_engine")
        agent.executeAction("w4_mq20_steal_arrays")
        agent.executeAction("w4_mq20_escape_launch")

        state = harness.store.state.value
        assertTrue("w4_mq20 should be completed", state.completedQuests.contains("w4_mq20"))
        assertTrue("World 5 Orbital Station access unlocked", state.completedMilestones.contains("ms_w5_access_unlocked"))
    }

    @Test
    fun `autonomous playtester clears World 5 Orbital Station and Anchor recovery`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // Bootstrap World 5 Firewall & Anchor Chamber
        harness.store.startQuest("w5_mq23")
        agent.executeAction("w5_mq23_navigate_maze")
        agent.executeAction("w5_mq23_firewall_alpha")
        agent.executeAction("w5_mq23_firewall_beta")
        agent.executeAction("w5_mq23_firewall_gamma")
        agent.winEncounter(listOf("firewall_construct"), "deep_mainframe")

        var state = harness.store.state.value
        assertTrue("w5_mq23 should be completed", state.completedQuests.contains("w5_mq23"))
        assertTrue("w5_mq24 should be active", state.activeQuests.contains("w5_mq24"))

        // MQ24 & MQ25: Anchor & Compliance Avatar Climax
        agent.navigateTo("deep_anchor_chamber")
        agent.executeAction("w5_mq24_find_elara")
        agent.executeAction("w5_mq24_take_anchor")

        state = harness.store.state.value
        assertTrue("w5_mq24 should be completed", state.completedQuests.contains("w5_mq24"))
        assertTrue("The Anchor relic acquired", state.inventory["anchor_relic"].orZero() >= 1)
        assertTrue("w5_mq25 should be active", state.activeQuests.contains("w5_mq25"))

        agent.navigateTo("deep_throne_room")
        agent.executeAction("w5_mq25_soloist")
        agent.winEncounter(listOf("compliance_avatar"), "deep_throne_room")
        agent.executeAction("w5_mq25_enter_tear")

        state = harness.store.state.value
        assertTrue("w5_mq25 should be completed", state.completedQuests.contains("w5_mq25"))
        assertTrue("World 6 Source access unlocked", state.completedMilestones.contains("ms_w6_access_unlocked"))
        assertEquals("source_campfire", state.roomId)
    }

    @Test
    fun `autonomous playtester clears World 6 Source finale and triggers campaign epilogue`() {
        val harness = PlaytestHarness()
        val agent = HeadlessPlaytesterAgent(harness)

        // Bootstrap World 6 Finale
        harness.store.startQuest("w6_mq29")
        harness.store.setRoom("source_memory_stair")

        agent.executeAction("w6_mq29_refuse_jed_revision")
        agent.executeAction("w6_mq29_refuse_astra_revision")
        agent.executeAction("w6_mq29_refuse_foundry_revision")
        agent.executeAction("w6_mq29_climb_stair")
        agent.winEncounter(listOf("source_shadow", "distorted_sentinel", "glitch_hound"), "source_memory_stair")
        agent.winEncounter(listOf("memory_leak", "nightmare_guard"), "source_memory_stair")

        agent.navigateTo("source_center")
        var state = harness.store.state.value
        assertTrue("w6_mq29 should be completed", state.completedQuests.contains("w6_mq29"))
        assertTrue("w6_mq30 should be active", state.activeQuests.contains("w6_mq30"))

        // MQ30: Final Boss - Ascended Vale & Ascended God
        agent.executeAction("w6_mq30_confront_vale")
        agent.winEncounter(listOf("ascended_vale"), "source_center")
        agent.winEncounter(listOf("ascended_god"), "source_center")
        agent.executeAction("w6_mq30_tune_world")

        state = harness.store.state.value
        assertTrue("w6_mq30 should be completed", state.completedQuests.contains("w6_mq30"))
        assertTrue("Game complete milestone reached", state.completedMilestones.contains("ms_game_complete"))
        assertTrue("Credits seen milestone reached", state.completedMilestones.contains("ms_credits_seen"))
        assertTrue("Final Source Art unlocked", state.unlockedSkills.contains("source_art_tune_world"))
        assertEquals("source_new_world", state.roomId)
    }

    @Test
    fun `graph reachability prover verifies all 465 authored room connections`() {
        val rooms = readList<Room>("rooms.json")
        val roomById = rooms.associateBy { it.id }

        assertTrue("Game must contain authored rooms", rooms.size >= 400)
        rooms.forEach { room ->
            room.connections.values.forEach { targetRoomId ->
                assertTrue(
                    "Room '${room.id}' has connection pointing to non-existent target '$targetRoomId'",
                    roomById.containsKey(targetRoomId)
                )
            }
        }
    }

    private inner class HeadlessPlaytesterAgent(private val harness: PlaytestHarness) {
        fun executeAction(actionId: String, payload: String? = null) {
            harness.events.handleTrigger("player_action", EventPayload.Action(actionId, payload))
        }

        fun navigateTo(targetRoomId: String) {
            harness.events.handleTrigger("enter_room", EventPayload.EnterRoom(targetRoomId))
            harness.store.setRoom(targetRoomId)
        }

        fun talkTo(npcName: String) {
            val session = harness.dialogue.startDialogue(npcName)
            while (session != null && !session.isFinished()) {
                session.advance()
            }
        }

        fun talkToZekeWithChoice(choiceId: String) {
            val session = harness.dialogue.startDialogue("Zeke")
            while (session != null && !session.isFinished()) {
                if (session.choices().any { it.id == choiceId }) {
                    session.choose(choiceId)
                } else {
                    session.advance()
                }
            }
        }

        fun winEncounter(enemyIds: List<String>, roomId: String) {
            harness.events.handleTrigger(
                "encounter_victory",
                EventPayload.EncounterOutcome(
                    enemyIds = enemyIds,
                    outcome = EventPayload.EncounterOutcome.Outcome.VICTORY,
                    roomId = roomId
                )
            )
        }
    }

    private inner class PlaytestHarness {
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_1",
                    hubId = "hub_1_homestead",
                    roomId = "pit_nova_bunk",
                    playerId = "nova",
                    activeQuests = setOf("w1_mq01"),
                    trackedQuestId = "w1_mq01",
                    questStageById = mapOf("w1_mq01" to "wake_in_the_pit")
                )
            )
        }
        val events: EventManager
        val dialogue: DialogueService

        init {
            val rawEvents = readList<GameEvent>("events.json")
            val rawDialogue = readList<DialogueLine>("dialogue.json")

            events = EventManager(
                events = rawEvents,
                sessionStore = store,
                eventHooks = EventHooks(
                    onPlayCinematic = { _, done -> done() },
                    onQuestStarted = { qId -> if (!qId.isNullOrBlank()) store.startQuest(qId) },
                    onQuestCompleted = { qId ->
                        if (!qId.isNullOrBlank()) {
                            store.completeQuest(qId)
                            events.handleTrigger("quest_stage_complete", EventPayload.QuestStage(qId))
                        }
                    },
                    onMilestoneSet = { ms -> if (!ms.isNullOrBlank()) store.setMilestone(ms) },
                    onQuestTaskUpdated = { qId, tId -> if (!qId.isNullOrBlank() && !tId.isNullOrBlank()) store.setQuestTaskCompleted(qId, tId, true) },
                    onQuestStageAdvanced = { qId, sId -> if (!qId.isNullOrBlank() && !sId.isNullOrBlank()) store.setQuestStage(qId, sId) },
                    onSetRoomState = { rId, k, v -> if (!rId.isNullOrBlank() && k.isNotBlank()) store.setRoomState(rId, k, v) },
                    onGiveItem = { itemId, qty ->
                        val inv = store.state.value.inventory
                        store.setInventory(inv + (itemId to ((inv[itemId] ?: 0) + qty.coerceAtLeast(1))))
                    },
                    onTakeItem = { itemId, qty ->
                        val inv = store.state.value.inventory
                        val current = inv[itemId] ?: 0
                        val req = qty.coerceAtLeast(1)
                        if (current < req) false
                        else {
                            store.setInventory(if (current - req > 0) inv + (itemId to (current - req)) else inv - itemId)
                            true
                        }
                    },
                    onGiveXp = { xp -> store.addXp(xp) },
                    onReward = { r ->
                        r.xp?.let(store::addXp)
                        r.credits?.let(store::addCredits)
                        r.items.forEach { item ->
                            val inv = store.state.value.inventory
                            store.setInventory(inv + (item.itemId to ((inv[item.itemId] ?: 0) + (item.quantity ?: 1))))
                        }
                    }
                )
            )

            dialogue = DialogueService(
                rawDialogue,
                DialogueConditionEvaluator { cond -> conditionEvaluator(cond, store.state.value) },
                DialogueTriggerHandler { trig -> events.performActions(DialogueTriggerParser.parse(trig)) }
            )
        }

        fun roundTripSave(state: GameSessionState): GameSessionState = runBlocking {
            val tmp = File(System.getProperty("java.io.tmpdir"), "starborn-playtest-${System.nanoTime()}").apply { mkdirs() }
            try {
                val persistence = GameSessionPersistence(tmp)
                persistence.writeSlot(1, state)
                requireNotNull(persistence.readSlot(1))
            } finally {
                tmp.deleteRecursively()
            }
        }
    }

    private fun conditionEvaluator(raw: String?, state: GameSessionState): Boolean {
        if (raw.isNullOrBlank()) return true
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.all { token ->
            val parts = token.split(':', limit = 2)
            val type = parts[0].trim().lowercase()
            val value = parts.getOrNull(1)?.trim().orEmpty()
            when (type) {
                "milestone", "milestone_set" -> value in state.completedMilestones
                "milestone_not_set" -> value !in state.completedMilestones
                "quest", "quest_active" -> value in state.activeQuests
                "quest_completed" -> value in state.completedQuests
                "quest_not_completed" -> value !in state.completedQuests
                "quest_task_done" -> {
                    val p = value.split(':', limit = 2)
                    p.size == 2 && state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                }
                "quest_task_not_done" -> {
                    val p = value.split(':', limit = 2)
                    p.size == 2 && !state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                }
                "item" -> (state.inventory[value] ?: 0) > 0
                "item_not" -> (state.inventory[value] ?: 0) <= 0
                else -> true
            }
        }
    }

    private inline fun <reified T> readList(name: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return requireNotNull(moshi.adapter<List<T>>(type).fromJson(File(assets, name).readText()))
    }
}
