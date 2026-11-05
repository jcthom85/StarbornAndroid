package com.example.starborn.domain.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningNarrativeMigrationTest {
    @Test fun sourceNavigationMigrationUsesReceiptsAndLegacyLocationsWithoutInventingProgress() {
        val base = GameSessionState(worldId="world_6",hubId="hub_11_event_horizon",roomId="source_campfire",
            activeQuests=setOf("w6_mq28"), playerXp=71, playerAp=9, playerCredits=123, inventory=mapOf("key_relic" to 1))
        val flags = listOf("source_memory_bridge" to "bridge_built", "source_memory_threshold" to "crossing_open",
            "source_memory_stair" to "ascent_open", "source_spire_arena" to "center_path_open")
        val cases = listOf(
            Triple(base.copy(questTasksCompleted=mapOf("w6_mq28" to setOf("build_bridge"))),0,0),
            Triple(base.copy(completedEvents=setOf("w6_mq28_build_bridge")),0,0),
            Triple(base.copy(roomId="source_memory_bridge_span"),0,0),
            Triple(base.copy(activeQuests=setOf("w6_mq29")),0,1),
            Triple(base.copy(completedQuests=setOf("w6_mq28")),0,1),
            Triple(base.copy(completedEvents=setOf("w6_mq28_reach_singularity")),0,1),
            Triple(base.copy(roomId="source_memory_stair_node_scale_final"),0,1),
            Triple(base.copy(questTasksCompleted=mapOf("w6_mq29" to setOf("climb_stair"))),0,2),
            Triple(base.copy(completedEvents=setOf("w6_mq29_climb_stair")),0,2),
            Triple(base.copy(roomId="source_memory_fragments"),0,2),
            Triple(base.copy(visitedNodes=setOf("source_spire_thought_node")),0,2),
            Triple(base.copy(questTasksCompleted=mapOf("w6_mq29" to setOf("defeat_memory_guard"))),0,3),
            Triple(base.copy(activeQuests=setOf("w6_mq30")),0,3),
            Triple(base.copy(roomId="source_center_node_scale_02"),0,3),
            Triple(base.copy(roomId="source_new_world_node_scale_final"),0,3))
        for ((old,first,last) in cases) {
            val changed=old.migrateOpeningNarrativeState()
            for (i in first..last) assertTrue("$old needs ${flags[i]}",changed.roomStates[flags[i].first]?.get(flags[i].second)==true)
            if (old.roomId=="source_new_world_node_scale_final") assertTrue(changed.roomStates["source_center"]?.get("ending_complete")==true)
            assertTrue(changed.questTasksCompleted==old.questTasksCompleted && changed.completedQuests==old.completedQuests && changed.activeQuests==old.activeQuests)
            assertTrue(changed.completedMilestones==old.completedMilestones && changed.inventory==old.inventory && changed.playerXp==old.playerXp && changed.playerAp==old.playerAp && changed.playerCredits==old.playerCredits)
            assertTrue(changed.roomId==old.roomId && changed==changed.migrateOpeningNarrativeState())
        }
        for (old in listOf(base,base.copy(completedMilestones=setOf("ms_w6_bridge_zeke_anchor","ms_w6_bridge_ghost_anchor","ms_w6_bridge_orion_anchor","ms_w6_bridge_nova_anchor")),
            base.copy(questTasksCompleted=mapOf("w6_mq26" to setOf("rescue_zeke","rescue_gh0st","rescue_orion"))),
            base.copy(completedQuests=setOf("w6_mq26","w6_mq27")))) {
            val changed=old.migrateOpeningNarrativeState()
            flags.forEach { (room,key) -> assertFalse(changed.roomStates[room]?.get(key)==true) }
        }
    }

    @Test fun anchorEntranceMigrationPreservesBossProgressAndLegacyOccupantsWithoutPayouts() {
        val base = GameSessionState(worldId = "world_5", hubId = "hub_10_deep_ring", roomId = "orbital_server_farm",
            playerXp = 71, playerAp = 9, playerCredits = 123, inventory = mapOf("sysadmin_keycard" to 1))
        for (old in listOf(base.copy(completedQuests = setOf("w5_mq23")),
            base.copy(completedMilestones = setOf("ms_w5_mq23_complete")), base.copy(activeQuests = setOf("w5_mq24")),
            base.copy(completedMilestones = setOf("ms_w5_mq24_complete")),
            base.copy(completedEvents = setOf("w5_mq23_defeat_construct")),
            base.copy(questTasksCompleted = mapOf("w5_mq23" to setOf("defeat_construct"))),
            base.copy(questTasksCompleted = mapOf("w5_mq24" to setOf("enter_chamber"))),
            base.copy(completedQuests = setOf("w5_mq25")), base.copy(roomId = "deep_anchor_chamber"),
            base.copy(roomId = "deep_throne_room_scale_03"), base.copy(visitedNodes = setOf("deep_tear")))) {
            val changed = old.migrateOpeningNarrativeState()
            assertTrue(changed.roomStates["deep_anchor_chamber"]?.get("firewall_access_open") == true)
            assertTrue(changed.completedQuests == old.completedQuests && changed.activeQuests == old.activeQuests)
            assertTrue(changed.playerXp == old.playerXp && changed.playerAp == old.playerAp && changed.playerCredits == old.playerCredits && changed.inventory == old.inventory)
            assertTrue(changed.roomId == old.roomId && changed.questTasksCompleted == old.questTasksCompleted)
            assertTrue(changed == changed.migrateOpeningNarrativeState())
        }
        for (old in listOf(base, base.copy(activeQuests = setOf("w5_mq23")),
            base.copy(completedMilestones = setOf("ms_w5_firewall_gamma_down")), base.copy(inventory = mapOf("anchor_relic" to 1)))) {
            assertFalse(old.migrateOpeningNarrativeState().roomStates["deep_anchor_chamber"]?.get("firewall_access_open") == true)
        }
    }

    @Test fun foundryReturnRouteBackfillsFromStoryEvidenceWithoutMovingOrPayingAgain() {
        val base = GameSessionState(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_pipe_gallery",
            playerXp = 77, playerCredits = 123, playerAp = 8, inventory = mapOf("medkit" to 2))
        val proven = listOf(
            base.copy(completedQuests = setOf("w4_mq17")),
            base.copy(completedMilestones = setOf("ms_w4_mq17_complete")),
            base.copy(completedMilestones = setOf("ms_w4_springs_regrouped")),
            base.copy(questTasksCompleted = mapOf("w4_mq17" to setOf("regroup_springs"))),
            base.copy(activeQuests = setOf("w4_mq18")),
            base.copy(completedQuests = setOf("w4_mq20"))
        )
        for (old in proven) {
            val restored = old.migrateOpeningNarrativeState()
            assertTrue(restored.roomStates["foundry_pipe_gallery"]?.get("assembly_route_open") == true)
            assertTrue(restored.inventory == old.inventory && restored.playerXp == old.playerXp &&
                restored.playerCredits == old.playerCredits && restored.playerAp == old.playerAp)
            assertTrue(restored.roomId == old.roomId && restored.hubId == old.hubId && restored.worldId == old.worldId)
            assertTrue(restored.completedQuests == old.completedQuests && restored.activeQuests == old.activeQuests)
            assertTrue(restored == restored.migrateOpeningNarrativeState())
        }
        for (old in listOf(base, base.copy(activeQuests = setOf("w4_mq17")),
            base.copy(completedMilestones = setOf("ms_w4_phantom_records_found")),
            base.copy(inventory = mapOf("gh0st_override_key" to 1)))) {
            assertFalse(old.migrateOpeningNarrativeState().roomStates["foundry_pipe_gallery"]?.get("assembly_route_open") == true)
        }
    }

    @Test fun tideglassRepairUsesEventReceiptsWithoutGrantingRewards() {
        val old = GameSessionState(activeQuests = setOf("w2_sq03"),
            completedEvents = setOf("w2_sq03_visit"), inventory = mapOf("source_resin" to 1), playerXp = 17)
        val repaired = old.migrateOpeningNarrativeState()
        assertTrue(repaired.questTasksCompleted["w2_sq03"] == setOf("visit_beach"))
        assertTrue(repaired.inventory == old.inventory && repaired.playerXp == old.playerXp)
        assertTrue(repaired == repaired.migrateOpeningNarrativeState())
        val withoutEvidence = old.copy(completedEvents = emptySet()).migrateOpeningNarrativeState()
        assertTrue(withoutEvidence.questTasksCompleted["w2_sq03"].isNullOrEmpty())
        val gathered = old.copy(completedEvents = setOf("w2_sq03_gather")).migrateOpeningNarrativeState()
        assertTrue("collect_food" in gathered.questTasksCompleted["w2_sq03"].orEmpty())
        assertTrue("ms_w2_tideglass_gathered" in gathered.completedMilestones)
        assertTrue(gathered.inventory == old.inventory)
    }

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
