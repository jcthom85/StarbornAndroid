package com.example.starborn.domain.quest

import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Priority 2 of the Automated Testing Strategy:
 * Interactive Quest Branch Coverage across all four major divergent side quests:
 *
 * 1. w3_sq14 (Corporate Espionage): Publish ledger vs weaponize for Phase Rounds
 * 2. w4_sq19 (Quality Control): Release units for Harden vs line defenders for Rapid Capacitor
 * 3. w5_sq24 (Ghost in the Shell): Data Shield vs Guardian Covenant
 * 4. w6_sq27 (The HR Record): Leave review vs broadcast revocation
 */
class InteractiveQuestBranchTest {

    // =========================================================================
    // 1. Corporate Espionage (w3_sq14)
    // =========================================================================

    @Test
    fun corporateEspionage_branchA_leakLedgerToWorkerTerminals() {
        val harness = createHarness()

        // Start quest
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_start"))
        var state = harness.store.state.value
        assertTrue("Quest w3_sq14 should be active", state.activeQuests.contains("w3_sq14"))

        // Complete prerequisites
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_copy_concierge_key"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_steal_ledger"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w3_ledger_stolen"))

        // Choose Branch A: Leak ledger
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_leak_ledger"))

        state = harness.store.state.value
        assertTrue("Quest w3_sq14 should be completed", state.completedQuests.contains("w3_sq14"))
        assertTrue("Milestone ms_w3_blackmail_unlocked should be set", state.completedMilestones.contains("ms_w3_blackmail_unlocked"))
        assertFalse("Milestone ms_w3_ledger_weaponized should NOT be set", state.completedMilestones.contains("ms_w3_ledger_weaponized"))
        assertEquals("Should receive encrypted_ledger", 1, state.inventory["encrypted_ledger"] ?: 0)
        assertEquals("Should NOT receive phase_rounds", 0, state.inventory["phase_rounds"] ?: 0)

        // Verify mutual exclusivity: Branch B cannot be executed afterwards
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_weaponize_ledger"))
        val finalState = harness.store.state.value
        assertFalse("Branch B should be blocked after Branch A is chosen", finalState.completedMilestones.contains("ms_w3_ledger_weaponized"))
        assertEquals("Phase rounds should still not be granted", 0, finalState.inventory["phase_rounds"] ?: 0)
    }

    @Test
    fun corporateEspionage_branchB_weaponizeLedgerForPhaseRounds() {
        val harness = createHarness()

        // Start quest
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_start"))
        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w3_sq14"))

        // Complete prerequisites
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_copy_concierge_key"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_steal_ledger"))
        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w3_ledger_stolen"))

        // Choose Branch B: Weaponize ledger
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_weaponize_ledger"))

        state = harness.store.state.value
        assertTrue("Quest w3_sq14 should be completed", state.completedQuests.contains("w3_sq14"))
        assertTrue("Milestone ms_w3_ledger_weaponized should be set", state.completedMilestones.contains("ms_w3_ledger_weaponized"))
        assertFalse("Milestone ms_w3_blackmail_unlocked should NOT be set", state.completedMilestones.contains("ms_w3_blackmail_unlocked"))
        assertEquals("Should receive phase_rounds", 1, state.inventory["phase_rounds"] ?: 0)
        assertEquals("Should NOT receive encrypted_ledger", 0, state.inventory["encrypted_ledger"] ?: 0)

        // Verify mutual exclusivity: Branch A cannot be executed afterwards
        harness.events.handleTrigger("player_action", EventPayload.Action("w3_sq14_leak_ledger"))
        val finalState = harness.store.state.value
        assertFalse("Branch A should be blocked after Branch B is chosen", finalState.completedMilestones.contains("ms_w3_blackmail_unlocked"))
        assertEquals("Encrypted ledger should still not be in inventory", 0, finalState.inventory["encrypted_ledger"] ?: 0)
    }

    // =========================================================================
    // 2. Quality Control (w4_sq19)
    // =========================================================================

    @Test
    fun qualityControl_branchA_releaseUnitsIntoWorkerTunnels() {
        val harness = createHarness()

        // Resolve / Spare defective units
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_reprogram_units"))
        var state = harness.store.state.value
        assertTrue("Quest w4_sq19 should be active", state.activeQuests.contains("w4_sq19"))
        assertTrue(state.completedMilestones.contains("ms_w4_defective_units_spared"))

        // Choose Branch A: Release units
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_release_units"))

        state = harness.store.state.value
        assertTrue("Quest w4_sq19 should be completed", state.completedQuests.contains("w4_sq19"))
        assertTrue("Milestone ms_w4_quality_control_complete should be set", state.completedMilestones.contains("ms_w4_quality_control_complete"))
        assertFalse("Milestone ms_w4_units_defend_workers should NOT be set", state.completedMilestones.contains("ms_w4_units_defend_workers"))
        assertTrue("Skill gh0st_harden should be unlocked", state.unlockedSkills.contains("gh0st_harden"))
        assertEquals("Should NOT receive rapid_capacitor", 0, state.inventory["rapid_capacitor"] ?: 0)

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_defend_workers"))
        val finalState = harness.store.state.value
        assertFalse("Branch B should be blocked after Branch A is chosen", finalState.completedMilestones.contains("ms_w4_units_defend_workers"))
        assertEquals("Rapid capacitor should still not be granted", 0, finalState.inventory["rapid_capacitor"] ?: 0)
    }

    @Test
    fun qualityControl_branchB_lineDefendersForRapidCapacitor() {
        val harness = createHarness()

        // Resolve / Spare defective units
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_reprogram_units"))
        var state = harness.store.state.value
        assertTrue("Quest w4_sq19 should be active", state.activeQuests.contains("w4_sq19"))
        assertTrue(state.completedMilestones.contains("ms_w4_defective_units_spared"))

        // Choose Branch B: Defend workers
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_defend_workers"))

        state = harness.store.state.value
        assertTrue("Quest w4_sq19 should be completed", state.completedQuests.contains("w4_sq19"))
        assertTrue("Milestone ms_w4_units_defend_workers should be set", state.completedMilestones.contains("ms_w4_units_defend_workers"))
        assertFalse("Milestone ms_w4_quality_control_complete should NOT be set", state.completedMilestones.contains("ms_w4_quality_control_complete"))
        assertEquals("Should receive rapid_capacitor", 1, state.inventory["rapid_capacitor"] ?: 0)
        assertFalse("Skill gh0st_harden should NOT be unlocked via Branch B", state.unlockedSkills.contains("gh0st_harden"))

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w4_sq19_release_units"))
        val finalState = harness.store.state.value
        assertFalse("Branch A should be blocked after Branch B is chosen", finalState.completedMilestones.contains("ms_w4_quality_control_complete"))
        assertFalse("gh0st_harden should still not be unlocked", finalState.unlockedSkills.contains("gh0st_harden"))
    }

    // =========================================================================
    // 3. Ghost in the Shell (w5_sq24)
    // =========================================================================

    @Test
    fun ghostInTheShell_branchA_restoreIndependentGuardianDataShield() {
        val harness = createHarness()

        // Recover purged backup
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_recover_backup"))
        var state = harness.store.state.value
        assertTrue("Quest w5_sq24 should be active", state.activeQuests.contains("w5_sq24"))
        assertTrue(state.completedMilestones.contains("ms_w5_backup_recovered"))

        // Choose Branch A: Restore independent guardian (Data Shield)
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_restore_guardian"))

        state = harness.store.state.value
        assertTrue("Quest w5_sq24 should be completed", state.completedQuests.contains("w5_sq24"))
        assertTrue("Milestone ms_w5_data_shield_unlocked should be set", state.completedMilestones.contains("ms_w5_data_shield_unlocked"))
        assertFalse("Milestone ms_w5_guardian_covenant should NOT be set", state.completedMilestones.contains("ms_w5_guardian_covenant"))
        assertTrue("Skill data_shield should be unlocked", state.unlockedSkills.contains("data_shield"))
        assertFalse("Skill zeke_guardian_covenant should NOT be unlocked", state.unlockedSkills.contains("zeke_guardian_covenant"))

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_bind_guardian"))
        val finalState = harness.store.state.value
        assertFalse("Branch B should be blocked after Branch A is chosen", finalState.completedMilestones.contains("ms_w5_guardian_covenant"))
        assertFalse("zeke_guardian_covenant should still not be unlocked", finalState.unlockedSkills.contains("zeke_guardian_covenant"))
    }

    @Test
    fun ghostInTheShell_branchB_crewCovenantGuardianCovenant() {
        val harness = createHarness()

        // Recover purged backup
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_recover_backup"))
        var state = harness.store.state.value
        assertTrue("Quest w5_sq24 should be active", state.activeQuests.contains("w5_sq24"))
        assertTrue(state.completedMilestones.contains("ms_w5_backup_recovered"))

        // Choose Branch B: Bind guardian with crew covenant
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_bind_guardian"))

        state = harness.store.state.value
        assertTrue("Quest w5_sq24 should be completed", state.completedQuests.contains("w5_sq24"))
        assertTrue("Milestone ms_w5_guardian_covenant should be set", state.completedMilestones.contains("ms_w5_guardian_covenant"))
        assertFalse("Milestone ms_w5_data_shield_unlocked should NOT be set", state.completedMilestones.contains("ms_w5_data_shield_unlocked"))
        assertTrue("Skill zeke_guardian_covenant should be unlocked", state.unlockedSkills.contains("zeke_guardian_covenant"))
        assertFalse("Skill data_shield should NOT be unlocked via Branch B", state.unlockedSkills.contains("data_shield"))

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w5_sq24_restore_guardian"))
        val finalState = harness.store.state.value
        assertFalse("Branch A should be blocked after Branch B is chosen", finalState.completedMilestones.contains("ms_w5_data_shield_unlocked"))
        assertFalse("data_shield should still not be unlocked", finalState.unlockedSkills.contains("data_shield"))
    }

    // =========================================================================
    // 4. The HR Record (w6_sq27)
    // =========================================================================

    @Test
    fun theHRRecord_branchA_leaveReviewRoomDeleteBackups() {
        val harness = createHarness()

        // Expose backups and revoke authorization
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_delete_record"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_revoke_record"))
        var state = harness.store.state.value
        assertTrue("Quest w6_sq27 should be active", state.activeQuests.contains("w6_sq27"))
        assertTrue(state.completedMilestones.contains("ms_w6_hr_revoked"))

        // Choose Branch A: Leave review loop (delete backups)
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_leave_review"))

        state = harness.store.state.value
        assertTrue("Quest w6_sq27 should be completed", state.completedQuests.contains("w6_sq27"))
        assertTrue("Milestone ms_w6_unshackled_unlocked should be set", state.completedMilestones.contains("ms_w6_unshackled_unlocked"))
        assertFalse("Milestone ms_w6_workers_unshackled should NOT be set", state.completedMilestones.contains("ms_w6_workers_unshackled"))
        assertTrue("Skill zeke_unshackled should be unlocked", state.unlockedSkills.contains("zeke_unshackled"))

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_free_workers"))
        val finalState = harness.store.state.value
        assertFalse("Branch B should be blocked after Branch A is chosen", finalState.completedMilestones.contains("ms_w6_workers_unshackled"))
    }

    @Test
    fun theHRRecord_branchB_broadcastRevocationFreeAllWorkers() {
        val harness = createHarness()

        // Expose backups and revoke authorization
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_delete_record"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_revoke_record"))
        var state = harness.store.state.value
        assertTrue("Quest w6_sq27 should be active", state.activeQuests.contains("w6_sq27"))
        assertTrue(state.completedMilestones.contains("ms_w6_hr_revoked"))

        // Choose Branch B: Broadcast revocation across all worker files
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_free_workers"))

        state = harness.store.state.value
        assertTrue("Quest w6_sq27 should be completed", state.completedQuests.contains("w6_sq27"))
        assertTrue("Milestone ms_w6_workers_unshackled should be set", state.completedMilestones.contains("ms_w6_workers_unshackled"))
        assertTrue("Skill zeke_unshackled should be unlocked", state.unlockedSkills.contains("zeke_unshackled"))

        // Verify mutual exclusivity
        harness.events.handleTrigger("player_action", EventPayload.Action("w6_sq27_leave_review"))
        val finalState = harness.store.state.value
        assertFalse("Branch A should be blocked after Branch B is chosen", finalState.completedMilestones.contains("ms_w6_unshackled_unlocked"))
    }

    // =========================================================================
    // Test Harness
    // =========================================================================

    private class BranchTestHarness {
        val store = GameSessionStore()
        val events: EventManager

        init {
            events = EventManager(
                events = loadEvents(),
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

    private companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        private fun createHarness(): BranchTestHarness = BranchTestHarness()

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            val adapter = moshi.adapter<List<GameEvent>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText()))
        }
    }
}
