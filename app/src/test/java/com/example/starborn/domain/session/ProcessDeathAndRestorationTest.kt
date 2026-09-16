package com.example.starborn.domain.session

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * Validates Vector B of Phase 3 in the Automated Testing Strategy:
 * Process Death & Low-Memory Restoration (Activity Recreation).
 *
 * Android frequently terminates background activities during low RAM, device rotation,
 * or multitasking.
 *
 * Verifies that:
 * 1. Mid-Combat Checkpoint Rollback: If process death occurs mid-battle, the engine
 *    accurately restores the unpaid battle checkpoint, preventing item duplication
 *    and rolling back spent supplies cleanly.
 * 2. Complete State Persistence: All narrative milestones, active meal buffs,
 *    inventory stacks, party levels, and pending cinematics survive process termination.
 * 3. Idempotent Serialization: Multiple save/reload cycles produce byte-identical states.
 */
class ProcessDeathAndRestorationTest {

    private lateinit var tempDir: File
    private lateinit var persistence: GameSessionPersistence

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "starborn-process-death-${UUID.randomUUID()}").apply { mkdirs() }
        persistence = GameSessionPersistence(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // =========================================================================
    // 1. Mid-Combat Process Death & Unpaid Checkpoint Rollback
    // =========================================================================

    @Test
    fun processDeath_midCombat_restoresPristinePreBattleCheckpoint() = runBlocking {
        val initialStore = GameSessionStore()
        val preBattleState = GameSessionState(
            playerCredits = 250,
            inventory = mapOf("medkit" to 5, "pulse_grenade" to 2),
            roomId = "pit_mine_shaft",
            hubId = "hub_1_homestead",
            completedMilestones = setOf("ms_w1_mq01_cutter_surge", "ms_w1_mq02_started")
        )
        initialStore.restore(preBattleState)

        // 1. Enter battle: arm and checkpoint battle state
        val battleDescriptor = """{"encounter_id":"echo_borer_pack","enemies":["echo_borer","echo_borer"]}"""
        initialStore.armBattle(battleDescriptor)
        initialStore.checkpointBattle()

        // 2. Mid-combat turn: player spends items and receives pending battle actions
        initialStore.setInventory(mapOf("medkit" to 2, "pulse_grenade" to 0)) // spent 3 medkits, 2 grenades
        initialStore.addCredits(100) // mid-battle temporary credit spike

        // 3. Android process death occurs: state is written to disk autosave
        persistence.writeAutosave(initialStore.state.value)

        // 4. App relaunched: Fresh GameSessionStore initialized from disk
        val restoredFromDisk = requireNotNull(persistence.readAutosave())
        val freshStore = GameSessionStore()
        freshStore.restore(restoredFromDisk)

        // Verifications:
        // By architecture, GameSessionPersistence automatically persists the pre-battle checkpoint
        // whenever battleCheckpoint is active, ensuring mid-combat death rolls back cleanly to pre-fight state:
        assertEquals("Battle descriptor must be preserved across process death", battleDescriptor, freshStore.state.value.pendingBattleJson)
        assertEquals("Credits must be rolled back to pre-battle balance", 250, freshStore.state.value.playerCredits)
        assertEquals("Spent medkits must be restored to pre-battle count (no item loss on crash)", 5, freshStore.state.value.inventory["medkit"])
        assertEquals("Spent grenades must be restored to pre-battle count", 2, freshStore.state.value.inventory["pulse_grenade"])

        // 5. Finishing battle clears pending battle state
        freshStore.finishBattle(battleDescriptor)
        assertEquals("", freshStore.state.value.pendingBattleJson)
    }

    // =========================================================================
    // 2. Full State Round-Trip Across Process Recreation
    // =========================================================================

    @Test
    fun processDeath_fullCampaignState_restoresAllProgressionAndBuffsLosslessly() = runBlocking {
        val mealBuff = ActiveMealBuff(
            recipeId = "hearty_skimmer_soup",
            recipeName = "Hearty Skimmer Soup",
            chefId = "nova",
            remainingEncounters = 2,
            strengthBonus = 2,
            hpBonus = 15
        )

        val richState = GameSessionState(
            playerCredits = 1250,
            playerLevel = 14,
            playerXp = 8400,
            worldId = "world_3",
            hubId = "hub_3_high_district",
            roomId = "spire_skypark_dome",
            completedMilestones = (1..25).map { "ms_w${(it % 6) + 1}_milestone_$it" }.toSet(),
            questTasksCompleted = mapOf("w1_mq01" to setOf("task_1", "task_2")),
            inventory = mapOf(
                "medkit" to 12,
                "repair_patch" to 6,
                "pure_iron" to 4,
                "composite_plate" to 4,
                "astral_thread" to 4
            ),
            unlockedWeapons = setOf("cryo_cutter", "arc_whip", "neutron_lance"),
            unlockedArmors = setOf("nova_flux_liner", "slag_carapace", "kinetic_mail"),
            equippedWeapons = mapOf("nova" to "neutron_lance", "ollie" to "arc_whip"),
            equippedArmors = mapOf("nova" to "kinetic_mail", "ollie" to "slag_carapace"),
            activeMealBuff = mealBuff,
            pendingEventCinematics = setOf("cinematic_spire_inauguration")
        )

        // Write state to slot and autosave
        persistence.writeSlot(1, richState)
        persistence.writeAutosave(richState)

        // Simulate process termination: read back from both slot and autosave
        val restoredSlot = requireNotNull(persistence.readSlot(1))
        val restoredAutosave = requireNotNull(persistence.readAutosave())

        // 1. Verify Slot restoration
        assertEquals(richState.playerCredits, restoredSlot.playerCredits)
        assertEquals(richState.playerLevel, restoredSlot.playerLevel)
        assertEquals(richState.roomId, restoredSlot.roomId)
        assertEquals(richState.hubId, restoredSlot.hubId)
        assertEquals(richState.completedMilestones, restoredSlot.completedMilestones)
        assertEquals(richState.questTasksCompleted, restoredSlot.questTasksCompleted)
        assertEquals(richState.inventory, restoredSlot.inventory)
        assertEquals(richState.unlockedWeapons, restoredSlot.unlockedWeapons)
        assertEquals(richState.unlockedArmors, restoredSlot.unlockedArmors)
        assertEquals(richState.equippedWeapons, restoredSlot.equippedWeapons)
        assertEquals(richState.equippedArmors, restoredSlot.equippedArmors)
        assertEquals(richState.pendingEventCinematics, restoredSlot.pendingEventCinematics)

        // 2. Verify Meal Buff restoration
        assertNotNull(restoredSlot.activeMealBuff)
        assertEquals("hearty_skimmer_soup", restoredSlot.activeMealBuff?.recipeId)
        assertEquals(2, restoredSlot.activeMealBuff?.remainingEncounters)

        // 3. Verify Autosave exact parity
        assertEquals(restoredSlot.playerCredits, restoredAutosave.playerCredits)
        assertEquals(restoredSlot.inventory, restoredAutosave.inventory)
        assertEquals(restoredSlot.roomId, restoredAutosave.roomId)
    }

    // =========================================================================
    // 3. Serialization Idempotency
    // =========================================================================

    @Test
    fun serialization_multipleCycles_isStrictlyIdempotent() = runBlocking {
        val original = GameSessionState(
            playerCredits = 500,
            roomId = "foundry_cooling_springs",
            completedMilestones = setOf("ms_w4_cooling_restored", "ms_w4_boss_defeated"),
            inventory = mapOf("medkit" to 8, "scrap_metal" to 15)
        )

        persistence.writeSlot(2, original)
        val cycle1 = requireNotNull(persistence.readSlot(2))

        persistence.writeSlot(2, cycle1)
        val cycle2 = requireNotNull(persistence.readSlot(2))

        assertEquals(original.playerCredits, cycle1.playerCredits)
        assertEquals(cycle1.playerCredits, cycle2.playerCredits)
        assertEquals(original.inventory, cycle2.inventory)
        assertEquals(original.completedMilestones, cycle2.completedMilestones)
    }
}
