package com.example.starborn.domain.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningNarrativeMigrationTest {
    @Test
    fun earlyWardenEvidenceRepairsOnlyPhysicalAndQuestProgressWithoutRewards() {
        val old = GameSessionState(roomId = "launch_bay", activeQuests = setOf("w1_mq05"),
            questStageById = mapOf("w1_mq05" to "defeat_warden"), playerXp = 77, playerCredits = 123,
            playerAp = 8, inventory = mapOf("ghost_signal_cell" to 1, "medkit" to 2),
            roomStates = mapOf("launch_bay" to mapOf("encounter_cleared:the_iron_warden" to true)))
        val changed = old.migrateOpeningNarrativeState()
        assertTrue("ms_w1_warden_defeated" in changed.completedMilestones)
        assertTrue(changed.roomStates["launch_bay"]?.get("warden_defeated") == true)
        assertTrue(changed.questStageById["w1_mq05"] == "launch_pod")
        assertTrue("defeat_the_warden" in changed.questTasksCompleted["w1_mq05"].orEmpty())
        assertTrue(changed.inventory == old.inventory && changed.playerXp == old.playerXp &&
            changed.playerCredits == old.playerCredits && changed.playerAp == old.playerAp)
        assertFalse("ms_w1_chime_spliced" in changed.completedMilestones)
        assertFalse("w1_mq05" in changed.completedQuests)
        assertTrue(changed == changed.migrateOpeningNarrativeState())
        val unproven = old.copy(roomStates = mapOf("launch_bay" to mapOf("encounter_cleared:echo_borer" to true)))
            .migrateOpeningNarrativeState()
        assertFalse("ms_w1_warden_defeated" in unproven.completedMilestones)
        assertTrue(unproven.questStageById["w1_mq05"] == "defeat_warden")
        val beforeHandoff = old.copy(activeQuests = setOf("w1_mq04"), questStageById = emptyMap())
            .migrateOpeningNarrativeState()
        assertFalse("w1_mq05" in beforeHandoff.activeQuests)
        assertTrue(beforeHandoff.questTasksCompleted["w1_mq05"].isNullOrEmpty())
    }

    @Test
    fun legacyRailSaveResumesAtPodControlsWithoutStartingTheLaunch() {
        val old = GameSessionState(worldId = "world_1", hubId = "hub_2_logistics", roomId = "launch_rail",
            activeQuests = setOf("w1_mq05"), inventory = mapOf("ghost_signal_cell" to 1), playerAp = 8)
        val changed = old.migrateOpeningNarrativeState()
        assertTrue(changed.roomId == "launch_pod")
        assertTrue(changed.inventory == old.inventory && changed.playerAp == old.playerAp)
        assertFalse("w1_mq05" in changed.completedQuests)
        assertFalse("ms_w1_chime_spliced" in changed.completedMilestones)
        assertTrue(changed == changed.migrateOpeningNarrativeState())
    }

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
