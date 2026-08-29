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
            ArcadeIds.LOGIC_BOARD, ArcadeIds.REPAIRED_CORE, "scrap_metal", "wiring_bundle",
            "miner_gyro_mod", "drill_bit_mod"
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
    fun scoreRewardsAreCumulativeAndClaimedOnce() {
        service.submitDeepMineScore(31_000)
        val creditsAfterFirst = store.state.value.playerCredits
        val inventoryAfterFirst = inventory.snapshot()

        service.submitDeepMineScore(35_000)

        assertEquals(500, creditsAfterFirst)
        assertEquals(creditsAfterFirst, store.state.value.playerCredits)
        assertEquals(inventoryAfterFirst, inventory.snapshot())
        assertEquals(setOf("BRONZE", "SILVER", "GOLD"), service.progress(ArcadeIds.DEEP_MINE).claimedTiers)
        assertEquals(2, service.progress(ArcadeIds.DEEP_MINE).playCount)
    }
}
