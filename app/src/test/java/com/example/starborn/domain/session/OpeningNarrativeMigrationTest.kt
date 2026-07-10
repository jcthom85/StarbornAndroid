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
}
