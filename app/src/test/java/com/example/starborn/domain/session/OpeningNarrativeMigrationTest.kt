package com.example.starborn.domain.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningNarrativeMigrationTest {
    @Test
    fun migratesDiscardedOpeningProgressToGroundedEquivalent() {
        val migrated = GameSessionState(
            completedMilestones = setOf(
                "ms_w1_mq01_resonance_visible",
                "ms_w1_mq01_resonance_traced",
                "ms_w1_mq01_loader_read"
            ),
            milestoneHistory = listOf("ms_w1_mq01_resonance_visible"),
            questTasksCompleted = mapOf("w1_mq01" to setOf("turn_on_bunk_light", "trace_resonance")),
            roomStates = mapOf(
                "pit_nova_bunk" to mapOf(
                    "light_on" to true,
                    "resonance_visible" to true,
                    "resonance_investigated" to true
                )
            )
        ).migrateOpeningNarrativeState()

        assertTrue("ms_w1_mq01_fault_visible" in migrated.completedMilestones)
        assertTrue("ms_w1_mq01_safety_fault_inspected" in migrated.completedMilestones)
        assertTrue("ms_w1_mq01_loader_inspected" in migrated.completedMilestones)
        assertFalse(migrated.completedMilestones.any { "resonance" in it })
        assertTrue("inspect_safety_fault" in migrated.questTasksCompleted["w1_mq01"].orEmpty())
        assertTrue("inspect_loader_relay" in migrated.questTasksCompleted["w1_mq01"].orEmpty())
        assertTrue(migrated.roomStates["pit_nova_bunk"]?.get("fault_visible") == true)
        assertTrue(migrated.roomStates["pit_nova_bunk"]?.get("conduit_isolated") == true)
    }

    @Test
    fun backfillsCompletedForkHandshakeWithoutDuplicatingRewards() {
        val legacy = GameSessionState(
            completedQuests = setOf("w1_mq01", "w1_mq03"),
            inventory = mapOf(
                "functional_cryo_inductor" to 1,
                "nova_flux_liner" to 1,
                "tuning_fork" to 1
            )
        )

        val once = legacy.migrateOpeningNarrativeState()
        val twice = once.migrateOpeningNarrativeState()

        assertTrue("ms_w1_mq01_cutter_surge" in twice.completedMilestones)
        assertTrue("ms_w1_mq03_echo_marked" in twice.completedMilestones)
        assertTrue("ms_w1_mq03_liner_ground_spent" in twice.completedMilestones)
        assertTrue(twice.roomStates["echo_heart"]?.get("relic_synced") == true)
        assertTrue(twice.inventory["functional_cryo_inductor"] == null)
        assertTrue(twice.inventory["nova_flux_liner"] == 1)
        assertTrue(twice.inventory["tuning_fork"] == 1)
        assertTrue(once == twice)
    }

    @Test
    fun derivesWorldTwoRidgeAndBridgeStateIdempotently() {
        val legacy = GameSessionState(
            completedQuests = setOf("w2_mq03", "w2_mq04"),
            activeQuests = setOf("w2_mq05"),
            questTasksCompleted = mapOf(
                "w2_mq04" to setOf("confront_stalker", "defeat_the_beast"),
                "w2_mq05" to setOf("reboot_bridge_relic")
            ),
            inventory = mapOf("ghost_signal_cell" to 1)
        )

        val once = legacy.migrateOpeningNarrativeState()
        val twice = once.migrateOpeningNarrativeState()

        assertTrue("ms_w2_bridge_recovered" in twice.completedMilestones)
        assertTrue("ms_w2_bridge_installed" in twice.completedMilestones)
        assertTrue("ms_w2_link_unlocked" in twice.completedMilestones)
        assertTrue("nova_link" in twice.unlockedSkills)
        assertTrue(twice.inventory["bridge_relic"] == null)
        assertTrue(twice.inventory["ghost_signal_cell"] == 1)
        assertTrue(twice.roomStates["sector9_canopy_ridge"]?.get("hunter_confronted") == true)
        assertTrue(twice.roomStates["sector9_canopy_ridge"]?.get("beast_defeated") == true)
        assertTrue(twice.roomStates["sector9_canopy_ridge"]?.get("anchor_drill_complete") == true)
        assertTrue(once == twice)
    }
}
