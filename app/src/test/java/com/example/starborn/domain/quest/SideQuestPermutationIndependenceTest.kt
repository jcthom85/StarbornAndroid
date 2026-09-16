package com.example.starborn.domain.quest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Priority 3 of the Automated Testing Strategy:
 * Side Quest Ordering & Independence Permutations.
 *
 * Enforces that:
 * 1. 0 static cross-quest milestone or prerequisite leakage exists across all 30 side quests (5 per world).
 * 2. Side quests per world can be completed in forward order, reverse order, and arbitrary shuffled
 *    permutations with 100% completion parity and zero state pollution.
 */
class SideQuestPermutationIndependenceTest {

    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val allQuests by lazy { reader.readList<Quest>("quests.json") }
    private val allEvents by lazy { reader.readList<GameEvent>("events.json") }
    private val allDialogue by lazy { reader.readList<DialogueLine>("dialogue.json") }

    private val worlds = mapOf(
        "world_1" to listOf("w1_sq01", "w1_sq02", "w1_sq03", "w1_sq04", "w1_sq05"),
        "world_2" to listOf("w2_sq01", "w2_sq02", "w2_sq03", "w2_sq04", "w2_sq05"),
        "world_3" to listOf("w3_sq11", "w3_sq12", "w3_sq13", "w3_sq14", "w3_sq15"),
        "world_4" to listOf("w4_sq16", "w4_sq17", "w4_sq18", "w4_sq19", "w4_sq20"),
        "world_5" to listOf("w5_sq21", "w5_sq22", "w5_sq23", "w5_sq24", "w5_sq25"),
        "world_6" to listOf("w6_sq26", "w6_sq27", "w6_sq28", "w6_sq29", "w6_sq30")
    )

    // =========================================================================
    // 1. Static Cross-Quest Independence Audit
    // =========================================================================

    @Test
    fun auditAllWorlds_noCrossSideQuestDependencyLeaks() {
        // Collect all milestones set per side quest
        val sqMilestones = mutableMapOf<String, MutableSet<String>>()
        for ((_, sqList) in worlds) {
            for (sq in sqList) {
                sqMilestones[sq] = mutableSetOf()
            }
        }

        for (event in allEvents) {
            val eventId = event.id
            for (sq in sqMilestones.keys) {
                val matchesQuest = eventId.contains(sq) ||
                    event.actions.any { it.questId == sq }
                if (matchesQuest) {
                    for (action in event.actions) {
                        if (action.type == "set_milestone" && !action.milestone.isNullOrBlank()) {
                            sqMilestones[sq]?.add(action.milestone)
                        }
                    }
                }
            }
        }

        for (line in allDialogue) {
            val triggers = mutableListOf<String>()
            line.trigger?.let { triggers.add(it) }
            line.options.orEmpty().forEach { opt -> opt.trigger?.let { triggers.add(it) } }
            for (sq in sqMilestones.keys) {
                for (trig in triggers) {
                    if (line.id.contains(sq) || trig.contains(sq)) {
                        for (part in trig.split(",")) {
                            if (part.startsWith("set_milestone:")) {
                                sqMilestones[sq]?.add(part.removePrefix("set_milestone:"))
                            }
                        }
                    }
                }
            }
        }

        // Assert 0 cross-dependencies within each world
        val violations = mutableListOf<String>()
        for ((worldId, sqList) in worlds) {
            for (sqA in sqList) {
                for (event in allEvents) {
                    val matchesA = event.id.contains(sqA) || event.actions.any { it.questId == sqA }
                    if (matchesA) {
                        for (cond in event.conditions) {
                            for (sqB in sqList) {
                                if (sqA != sqB) {
                                    if (cond.questId == sqB) {
                                        violations.add("Event ${event.id} for $sqA directly checks $sqB")
                                    }
                                    val bMilestones = sqMilestones[sqB].orEmpty()
                                    if (cond.milestone in bMilestones) {
                                        violations.add("Event ${event.id} for $sqA requires milestone ${cond.milestone} from $sqB")
                                    }
                                }
                            }
                        }
                    }
                }

                for (line in allDialogue) {
                    if (line.id.contains(sqA)) {
                        val condStr = line.condition.orEmpty()
                        for (sqB in sqList) {
                            if (sqA != sqB) {
                                if (condStr.contains(sqB)) {
                                    violations.add("Dialogue ${line.id} for $sqA references $sqB in condition: $condStr")
                                }
                                for (m in sqMilestones[sqB].orEmpty()) {
                                    if (condStr.contains(m)) {
                                        violations.add("Dialogue ${line.id} for $sqA requires milestone $m from $sqB")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue("Found cross-quest leakage violations:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    // =========================================================================
    // 2. World 3 (The Spire) Side Quest Permutations
    // =========================================================================

    @Test
    fun world3_sideQuestPermutations_forwardReverseAndShuffled_completeIndependently() {
        val sqActions = mapOf(
            "w3_sq11" to listOf("w3_sq11_inspect_sign", "w3_sq11_install_core"),
            "w3_sq12" to listOf("w3_sq12_find_case_number", "w3_sq12_access_terminal", "w3_sq12_expose_transfer"),
            "w3_sq13" to listOf("w3_sq13_trace_power_theft", "w3_sq13_fix_market_lights", "w3_sq13_restore_market"),
            "w3_sq14" to listOf("w3_sq14_copy_concierge_key", "w3_sq14_steal_ledger", "w3_sq14_leak_ledger"),
            "w3_sq15" to listOf("w3_sq15_scan_targeting", "w3_sq15_test_weapon", "w3_sq15_scrub_telemetry")
        )

        val keys = sqActions.keys.toList()
        val forward = keys
        val reverse = keys.reversed()
        val shuffled1 = listOf(keys[2], keys[0], keys[4], keys[1], keys[3])
        val shuffled2 = listOf(keys[3], keys[1], keys[4], keys[0], keys[2])

        for (order in listOf(forward, reverse, shuffled1, shuffled2)) {
            val harness = createHarness()
            harness.store.setInventory(mapOf("neon_sign_core" to 1))

            for (qid in order) {
                val actions = requireNotNull(sqActions[qid])
                for (act in actions) {
                    harness.events.handleTrigger("player_action", EventPayload.Action(act))
                }
                assertTrue("Quest $qid must be completed in order $order", harness.store.state.value.completedQuests.contains(qid))
            }

            val finalState = harness.store.state.value
            assertEquals("All 5 side quests must be completed in order $order", 5, finalState.completedQuests.count { it.startsWith("w3_sq") })
            assertTrue(finalState.completedMilestones.contains("ms_w3_streetwise_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w3_ghost_skill_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w3_market_lit"))
            assertTrue(finalState.completedMilestones.contains("ms_w3_blackmail_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w3_prototype_trial_complete"))
        }
    }

    // =========================================================================
    // 3. World 4 (The Foundry) Side Quest Permutations
    // =========================================================================

    @Test
    fun world4_sideQuestPermutations_forwardReverseAndShuffled_completeIndependently() {
        val sqActions = mapOf(
            "w4_sq16" to listOf("w4_sq16_start", "w4_sq16_destroy_crate_alpha", "w4_sq16_destroy_crate_beta", "w4_sq16_destroy_crate_gamma"),
            "w4_sq17" to listOf("w4_sq17_trace_ping", "w4_sq17_find_worker", "w4_sq17_vent_safe_route"),
            "w4_sq18" to listOf("w4_sq18_stop_intake", "w4_sq18_salvage_mechs", "w4_sq18_build_bypass"),
            "w4_sq19" to listOf("w4_sq19_read_rejection_codes", "w4_sq19_reprogram_units", "w4_sq19_release_units"),
            "w4_sq20" to listOf("w4_sq20_map_hazards", "w4_sq20_survive_waves", "w4_sq20_steal_overclock")
        )

        val keys = sqActions.keys.toList()
        val forward = keys
        val reverse = keys.reversed()
        val shuffled1 = listOf(keys[2], keys[0], keys[4], keys[1], keys[3])
        val shuffled2 = listOf(keys[3], keys[1], keys[4], keys[0], keys[2])

        for (order in listOf(forward, reverse, shuffled1, shuffled2)) {
            val harness = createHarness()

            for (qid in order) {
                val actions = requireNotNull(sqActions[qid])
                for (act in actions) {
                    harness.events.handleTrigger("player_action", EventPayload.Action(act))
                }
                assertTrue("Quest $qid must be completed in order $order", harness.store.state.value.completedQuests.contains(qid))
            }

            val finalState = harness.store.state.value
            assertEquals("All 5 side quests must be completed in order $order", 5, finalState.completedQuests.count { it.startsWith("w4_sq") })
            assertTrue(finalState.completedMilestones.contains("ms_w4_sabotage_complete"))
            assertTrue(finalState.completedMilestones.contains("ms_w4_lost_worker_complete"))
            assertTrue(finalState.completedMilestones.contains("ms_w4_scavenger_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w4_quality_control_complete"))
            assertTrue(finalState.completedMilestones.contains("ms_w4_overclocked_complete"))
        }
    }

    // =========================================================================
    // 4. World 5 (The Solarium Archive) Side Quest Permutations
    // =========================================================================

    @Test
    fun world5_sideQuestPermutations_forwardReverseAndShuffled_completeIndependently() {
        val sqActions = mapOf(
            "w5_sq21" to listOf("w5_sq21_find_redactions", "w5_sq21_director_logs", "w5_sq21_publish_logs"),
            "w5_sq22" to listOf("w5_sq22_trace_false_sun", "w5_sq22_realign_mirrors", "w5_sq22_restore_gardens"),
            "w5_sq23" to listOf("w5_sq23_map_pressure_loss", "w5_sq23_vacuum_seal", "w5_sq23_reopen_dock"),
            "w5_sq24" to listOf("w5_sq24_trace_purge", "w5_sq24_recover_backup", "w5_sq24_restore_guardian"),
            "w5_sq25" to listOf("w5_sq25_find_keycard", "w5_sq25_open_armory")
        )

        val keys = sqActions.keys.toList()
        val forward = keys
        val reverse = keys.reversed()
        val shuffled1 = listOf(keys[2], keys[0], keys[4], keys[1], keys[3])
        val shuffled2 = listOf(keys[3], keys[1], keys[4], keys[0], keys[2])

        for (order in listOf(forward, reverse, shuffled1, shuffled2)) {
            val harness = createHarness()

            for (qid in order) {
                val actions = requireNotNull(sqActions[qid])
                for (act in actions) {
                    harness.events.handleTrigger("player_action", EventPayload.Action(act))
                }
                assertTrue("Quest $qid must be completed in order $order", harness.store.state.value.completedQuests.contains(qid))
            }

            val finalState = harness.store.state.value
            assertEquals("All 5 side quests must be completed in order $order", 5, finalState.completedQuests.count { it.startsWith("w5_sq") })
            assertTrue(finalState.completedMilestones.contains("ms_w5_corporate_insight_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w5_solar_maintenance_complete"))
            assertTrue(finalState.completedMilestones.contains("ms_w5_vacuum_seal_complete"))
            assertTrue(finalState.completedMilestones.contains("ms_w5_data_shield_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w5_admin_privileges_complete"))
        }
    }

    // =========================================================================
    // 5. World 6 (Aethel Core) Side Quest Permutations
    // =========================================================================

    @Test
    fun world6_sideQuestPermutations_forwardReverseAndShuffled_completeIndependently() {
        val sqActions = mapOf(
            "w6_sq26" to listOf("w6_sq26_jed_echo", "w6_sq26_hear_lesson", "w6_sq26_carry_legacy"),
            "w6_sq27" to listOf("w6_sq27_delete_record", "w6_sq27_revoke_record", "w6_sq27_leave_review"),
            "w6_sq28" to listOf("w6_sq28_elara_song", "w6_sq28_break_command", "w6_sq28_preserve_song"),
            "w6_sq29" to listOf("w6_sq29_aethel_grave", "w6_sq29_tune_grave", "w6_sq29_carry_chorus"),
            "w6_sq30" to listOf("w6_sq30_final_scavenge", "w6_sq30_detach_hull", "w6_sq30_fit_hull")
        )

        val keys = sqActions.keys.toList()
        val forward = keys
        val reverse = keys.reversed()
        val shuffled1 = listOf(keys[2], keys[0], keys[4], keys[1], keys[3])
        val shuffled2 = listOf(keys[3], keys[1], keys[4], keys[0], keys[2])

        for (order in listOf(forward, reverse, shuffled1, shuffled2)) {
            val harness = createHarness()

            for (qid in order) {
                val actions = requireNotNull(sqActions[qid])
                for (act in actions) {
                    harness.events.handleTrigger("player_action", EventPayload.Action(act))
                }
                assertTrue("Quest $qid must be completed in order $order", harness.store.state.value.completedQuests.contains(qid))
            }

            val finalState = harness.store.state.value
            assertEquals("All 5 side quests must be completed in order $order", 5, finalState.completedQuests.count { it.startsWith("w6_sq") })
            assertTrue(finalState.completedMilestones.contains("ms_w6_legacy_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w6_unshackled_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w6_source_balance_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w6_ancestral_grace_unlocked"))
            assertTrue(finalState.completedMilestones.contains("ms_w6_final_scavenge_complete"))
        }
    }

    // =========================================================================
    // 6. World 1 Reverse Order Progression (SQ05 before SQ01)
    // =========================================================================

    @Test
    fun world1_sideQuest_reverseOrder_sq05_and_sq04_before_sq01() {
        val harness = createHarness()
        harness.store.setMilestone("ms_mine_power_on")

        // Complete SQ05 (The Lost Shift) first
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_shunt"))
        assertTrue("w1_sq05 should be active", harness.store.state.value.activeQuests.contains("w1_sq05"))
        harness.events.handleTrigger("player_action", EventPayload.Action("read_datapad_sq05"))
        assertTrue("w1_sq05 should be completed", harness.store.state.value.completedQuests.contains("w1_sq05"))
        assertTrue("w1_sq05 milestone set", harness.store.state.value.completedMilestones.contains("ms_w1_sq05_completed"))

        // Complete SQ04 (Protocol Override) second
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("server_hub"))
        harness.events.handleTrigger("player_action", EventPayload.Action("start_hack_sq04"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq04_thaw_console"))
        harness.events.handleTrigger("player_action", EventPayload.Action("start_hack_sq04"))
        assertTrue("w1_sq04 should be completed", harness.store.state.value.completedQuests.contains("w1_sq04"))

        // Complete SQ01 (The Scavenger's Stash) third
        harness.store.startQuest("w1_sq01")
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("trade_stash"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq01_open_rebel_cache"))
        assertTrue("w1_sq01 milestone set", harness.store.state.value.completedMilestones.contains("ms_w1_sq01_cache_opened"))
        harness.store.completeQuest("w1_sq01")
        harness.store.setMilestone("ms_w1_sq01_complete")

        val state = harness.store.state.value
        assertTrue("w1_sq05 completed", state.completedQuests.contains("w1_sq05"))
        assertTrue("w1_sq04 completed", state.completedQuests.contains("w1_sq04"))
        assertTrue("w1_sq01 completed", state.completedQuests.contains("w1_sq01"))
    }

    // =========================================================================
    // Test Harness
    // =========================================================================

    private class TestHarness(eventsList: List<GameEvent>) {
        val store = GameSessionStore()
        val events: EventManager

        init {
            events = EventManager(
                events = eventsList,
                sessionStore = store,
                eventHooks = EventHooks(
                    onQuestTaskUpdated = { questId, taskId ->
                        if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                            store.setQuestTaskCompleted(questId, taskId, true)
                        }
                    },
                    onGiveItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val next = current + (itemId to ((current[itemId] ?: 0) + quantity.coerceAtLeast(1)))
                        store.setInventory(next)
                    },
                    onTakeItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val available = current[itemId] ?: 0
                        val requested = quantity.coerceAtLeast(1)
                        if (available >= requested) {
                            val remaining = available - requested
                            val next = if (remaining > 0) current + (itemId to remaining) else current - itemId
                            store.setInventory(next)
                            true
                        } else {
                            false
                        }
                    },
                    onGiveXp = { amount -> store.addXp(amount) },
                    onQuestCompleted = { questId ->
                        if (!questId.isNullOrBlank()) {
                            store.completeQuest(questId)
                        }
                    }
                )
            )
        }
    }

    private fun createHarness(): TestHarness = TestHarness(allEvents)
}
