package com.example.starborn.feature.arcade.domain

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArcadeServiceTest {
    private lateinit var store: GameSessionStore
    private lateinit var inventory: InventoryService
    private lateinit var service: ArcadeService

    @Before
    fun setUp() {
        val ids = listOf(
            ArcadeIds.TOKEN_ITEM_ID,
            ArcadeIds.LOGIC_BOARD, ArcadeIds.REPAIRED_CORE, "scrap_metal", "wiring_bundle",
            "miner_gyro_mod", "drill_bit_mod", ArcadeIds.CANOPY_OPTIC_BOARD, ArcadeIds.REPAIRED_CANOPY_CORE,
            "herb", "beast_meat", "tideglass_delight", "bioluminescent_lure",
            ArcadeIds.SPIRE_LOGIC_BOARD, ArcadeIds.REPAIRED_SPIRE_CORE, "circuit_board", "nano_filament",
            "phase_rounds", "cyber_visor",
            ArcadeIds.SLAG_LOGIC_BOARD, ArcadeIds.REPAIRED_SLAG_CORE, "pure_iron", "foundry_crucible_lining",
            "thermal_solder_gun",
            ArcadeIds.ORBITAL_LOGIC_BOARD, ArcadeIds.REPAIRED_ORBITAL_CORE, "composite_plate",
            "plasma_lens_mod", "orbital_deflector_matrix",
            ArcadeIds.HARMONIC_LOGIC_BOARD, ArcadeIds.REPAIRED_HARMONIC_CORE, "astral_thread",
            "harmonic_resonator_tome", "source_cadence_crown"
        )
        val catalog = object : ItemCatalog {
            private val items = ids.associateWith { id -> Item(id, id, type = "misc") }
            override fun load() = Unit
            override fun findItem(idOrAlias: String): Item? = items[idOrAlias]
        }
        store = GameSessionStore()
        inventory = InventoryService(catalog).apply { loadItems() }
        service = ArcadeService(store, inventory)
    }

    @Test
    fun discoveryIsIdempotent() {
        assertTrue(service.discoverDeepMine())
        assertFalse(service.discoverDeepMine())
        assertEquals(1, inventory.snapshot()[ArcadeIds.LOGIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_RECIPE in store.state.value.learnedSchematics)
    }

    @Test
    fun canopyHopperDiscoveryAndRewardsAreClaimedOnce() {
        assertTrue(service.discoverCanopyHopper())
        assertFalse(service.discoverCanopyHopper())
        assertEquals(1, inventory.snapshot()[ArcadeIds.CANOPY_OPTIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_CANOPY_RECIPE in store.state.value.learnedSchematics)

        service.submitCanopyHopperScore(26_000)
        val credits = store.state.value.playerCredits
        assertEquals(500, credits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.CANOPY_HOPPER).claimedTiers)
        assertEquals(1, inventory.snapshot()["bioluminescent_lure"])
    }

    @Test
    fun spireInfiltratorDiscoveryAndRewardsAreClaimedOnce() {
        assertTrue(service.discoverSpireInfiltrator())
        assertFalse(service.discoverSpireInfiltrator())
        assertEquals(1, inventory.snapshot()[ArcadeIds.SPIRE_LOGIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_SPIRE_RECIPE in store.state.value.learnedSchematics)

        service.submitSpireInfiltratorScore(36_000)
        val credits = store.state.value.playerCredits
        assertEquals(750, credits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.SPIRE_INFILTRATOR).claimedTiers)
        assertEquals(1, inventory.snapshot()["cyber_visor"])
        assertEquals(1, inventory.snapshot()["phase_rounds"])
    }

    @Test
    fun slagCatcherDiscoveryAndRewardsAreClaimedOnce() {
        assertTrue(service.discoverSlagCatcher())
        assertFalse(service.discoverSlagCatcher())
        assertEquals(1, inventory.snapshot()[ArcadeIds.SLAG_LOGIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_SLAG_RECIPE in store.state.value.learnedSchematics)

        service.submitSlagCatcherScore(31_000)
        val credits = store.state.value.playerCredits
        assertEquals(850, credits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.SLAG_CATCHER).claimedTiers)
        assertEquals(1, inventory.snapshot()["thermal_solder_gun"])
        assertEquals(1, inventory.snapshot()["foundry_crucible_lining"])
    }

    @Test
    fun orbitalDefenseDiscoveryAndRewardsAreClaimedOnce() {
        assertTrue(service.discoverOrbitalDefense())
        assertFalse(service.discoverOrbitalDefense())
        assertEquals(1, inventory.snapshot()[ArcadeIds.ORBITAL_LOGIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_ORBITAL_RECIPE in store.state.value.learnedSchematics)

        service.submitOrbitalDefenseScore(41_000)
        val credits = store.state.value.playerCredits
        assertEquals(1000, credits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.ORBITAL_DEFENSE).claimedTiers)
        assertEquals(1, inventory.snapshot()["orbital_deflector_matrix"])
        assertEquals(1, inventory.snapshot()["plasma_lens_mod"])
    }

    @Test
    fun harmonicPulseDiscoveryAndRewardsAreClaimedOnce() {
        assertTrue(service.discoverHarmonicPulse())
        assertFalse(service.discoverHarmonicPulse())
        assertEquals(1, inventory.snapshot()[ArcadeIds.HARMONIC_LOGIC_BOARD])
        assertTrue(ArcadeIds.REPAIR_HARMONIC_RECIPE in store.state.value.learnedSchematics)

        service.submitHarmonicPulseScore(52_000)
        val credits = store.state.value.playerCredits
        assertEquals(1500, credits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.HARMONIC_PULSE).claimedTiers)
        assertEquals(1, inventory.snapshot()["source_cadence_crown"])
        assertEquals(1, inventory.snapshot()["harmonic_resonator_tome"])
    }

    @Test
    fun restoringAllCabinets_triggersMasterMilestone() {
        inventory.addItem(ArcadeIds.REPAIRED_CORE)
        inventory.addItem(ArcadeIds.REPAIRED_CANOPY_CORE)
        inventory.addItem(ArcadeIds.REPAIRED_SPIRE_CORE)
        inventory.addItem(ArcadeIds.REPAIRED_SLAG_CORE)
        inventory.addItem(ArcadeIds.REPAIRED_ORBITAL_CORE)
        inventory.addItem(ArcadeIds.REPAIRED_HARMONIC_CORE)

        service.completeDeepMineRepair()
        service.completeCanopyHopperRepair()
        service.completeSpireInfiltratorRepair()
        service.completeSlagCatcherRepair()
        service.completeOrbitalDefenseRepair()
        assertFalse("ms_all_arcade_cabinets_restored" in store.state.value.completedMilestones)

        service.completeHarmonicPulseRepair()
        assertTrue("ms_all_arcade_cabinets_restored" in store.state.value.completedMilestones)
    }

    @Test
    fun scoreRewardsAreCumulativeAndClaimedOnce() {
        service.submitDeepMineScore(31_000)
        val creditsAfterFirst = store.state.value.playerCredits
        val inventoryAfterFirst = inventory.snapshot()

        service.submitDeepMineScore(35_000)

        assertEquals(500, creditsAfterFirst)
        assertEquals(creditsAfterFirst, store.state.value.playerCredits)
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.DEEP_MINE).claimedTiers)
        assertEquals(2, service.progress(ArcadeIds.DEEP_MINE).playCount)
    }

    @Test
    fun tokensAreAwardedOnScoreSubmission() {
        val before = inventory.count(ArcadeIds.TOKEN_ITEM_ID)
        service.submitDeepMineScore(15_000)
        val after = inventory.count(ArcadeIds.TOKEN_ITEM_ID)
        assertTrue("Tokens should be awarded for Bronze + Silver tiers + participation", after > before)
    }
}
