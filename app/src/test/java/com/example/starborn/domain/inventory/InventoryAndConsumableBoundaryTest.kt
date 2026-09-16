package com.example.starborn.domain.inventory

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.ActiveMealBuff
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates Vector D of the Automated Testing Strategy:
 * Inventory & Consumable Boundary Edge Cases.
 *
 * Enforces that:
 * 1. Duplicate meal buff applications (e.g. consuming multiple meals in a row) do not stack
 *    stats infinitely, but cleanly replace the active meal buff and reset the 3-encounter timer.
 * 2. Meal buff encounter lifecycle decrements correctly from 3 -> 2 -> 1 -> 0, clearing at 0.
 * 3. Atomic inventory transactions: consumeItems() fails atomically without partial deduction
 *    when any required ingredient is deficient.
 * 4. Quantity boundary guards: zero and negative inputs are ignored; removing excess clamps to 0
 *    and purges the inventory entry.
 * 5. Quest item grants retain all items without truncation or capacity dropping.
 */
class InventoryAndConsumableBoundaryTest {

    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val rawItems by lazy { reader.readList<Item>("items.json") }

    private lateinit var itemCatalog: ItemCatalog
    private lateinit var inventoryService: InventoryService
    private lateinit var sessionStore: GameSessionStore

    @Before
    fun setup() {
        itemCatalog = object : ItemCatalog {
            private val items = rawItems.associateBy { it.id }.toMutableMap()
            override fun load() {}
            override fun findItem(idOrAlias: String): Item? = items[idOrAlias]
        }

        inventoryService = InventoryService(itemCatalog)
        sessionStore = GameSessionStore()
    }

    // =========================================================================
    // 1. Meal Buff Stacking Prevention & Replacement
    // =========================================================================

    @Test
    fun mealBuff_multipleApplicationsInARow_doesNotStackInfinitely_replacesCleanly() {
        val buff1 = ActiveMealBuff(
            recipeId = "starbar_stew",
            recipeName = "Starbar Stew",
            chefId = "zeke",
            remainingEncounters = 3,
            hpBonus = 50,
            stabilityBonus = 5
        )

        val buff2 = ActiveMealBuff(
            recipeId = "spire_roast",
            recipeName = "Spire Roast",
            chefId = "nova",
            remainingEncounters = 3,
            focusBonus = 20,
            critBonus = 0.10
        )

        // Apply first meal
        sessionStore.applyMealBuff(buff1)
        var state = sessionStore.state.value
        assertEquals("starbar_stew", state.activeMealBuff?.recipeId)
        assertEquals(50, state.activeMealBuff?.hpBonus)
        assertEquals(0, state.activeMealBuff?.focusBonus)

        // Apply second meal directly on top
        sessionStore.applyMealBuff(buff2)
        state = sessionStore.state.value

        // Must replace, NOT stack (hpBonus should not become 50 + 0, focusBonus should be 20)
        assertEquals("spire_roast", state.activeMealBuff?.recipeId)
        assertEquals("Spire Roast", state.activeMealBuff?.recipeName)
        assertEquals(0, state.activeMealBuff?.hpBonus)
        assertEquals(20, state.activeMealBuff?.focusBonus)
        assertEquals(0.10, state.activeMealBuff?.critBonus ?: 0.0, 0.001)
        assertEquals(3, state.activeMealBuff?.remainingEncounters)
    }

    // =========================================================================
    // 2. Meal Buff Encounter Lifecycle & Expiry
    // =========================================================================

    @Test
    fun mealBuff_encounterProgression_decrementsUntilExpiry_andClears() {
        val buff = ActiveMealBuff(
            recipeId = "canopy_chowder",
            recipeName = "Canopy Chowder",
            chefId = "orion",
            remainingEncounters = 3,
            hpBonus = 30
        )
        sessionStore.applyMealBuff(buff)

        // Encounter 1 ends
        var active = sessionStore.decrementMealBuffEncounter()
        assertNotNull(active)
        assertEquals(2, active?.remainingEncounters)
        assertEquals(2, sessionStore.state.value.activeMealBuff?.remainingEncounters)

        // Encounter 2 ends
        active = sessionStore.decrementMealBuffEncounter()
        assertNotNull(active)
        assertEquals(1, active?.remainingEncounters)
        assertEquals(1, sessionStore.state.value.activeMealBuff?.remainingEncounters)

        // Encounter 3 ends -> Expiration
        active = sessionStore.decrementMealBuffEncounter()
        assertNull("Buff must expire and return null when remaining encounters reaches 0", active)
        assertNull("Active buff in session store must be cleared to null", sessionStore.state.value.activeMealBuff)
    }

    // =========================================================================
    // 3. Inventory Atomic Transactions (All-or-Nothing Consumption)
    // =========================================================================

    @Test
    fun inventory_consumeItems_atomicRollbackOnPartialDeficit() {
        inventoryService.addItem("copper_wire", 5)
        inventoryService.addItem("quantum_core", 1)
        // Note: flux_gel is NOT added (0 possessed)

        val requirements = mapOf(
            "copper_wire" to 3,
            "quantum_core" to 1,
            "flux_gel" to 2
        )

        // Attempt consumption with missing flux_gel
        val consumed = inventoryService.consumeItems(requirements)
        assertFalse("consumeItems must return false when any requirement is deficient", consumed)

        // Assert atomic integrity: neither copper_wire nor quantum_core was deducted!
        val snapshot = inventoryService.snapshot()
        assertEquals(5, snapshot["copper_wire"])
        assertEquals(1, snapshot["quantum_core"])
        assertFalse(snapshot.containsKey("flux_gel"))
    }

    // =========================================================================
    // 4. Quantity Boundary Guards & Zero-Clamping
    // =========================================================================

    @Test
    fun inventory_zeroAndNegativeQuantityGuards_doesNotCorruptInventory() {
        // Zero addition has no effect
        inventoryService.addItem("medkit", 0)
        assertFalse("Adding 0 must not create entry", inventoryService.hasItem("medkit"))

        // Add 3 medkits
        inventoryService.addItem("medkit", 3)
        assertTrue(inventoryService.hasItem("medkit", 3))

        // Remove more than available
        inventoryService.removeItem("medkit", 10)
        assertFalse("Item must be removed from inventory when depleted", inventoryService.hasItem("medkit"))
        assertEquals("Depleted item must not appear in snapshot with negative count", 0, inventoryService.snapshot().getOrDefault("medkit", 0))

        // Restore negative or zero entries
        inventoryService.restore(mapOf("repair_patch" to -5, "herb" to 0, "beast_meat" to 4))
        val snapshot = inventoryService.snapshot()
        assertFalse("Negative restore entry must be ignored", snapshot.containsKey("repair_patch"))
        assertFalse("Zero restore entry must be ignored", snapshot.containsKey("herb"))
        assertEquals(4, snapshot["beast_meat"])
    }

    // =========================================================================
    // 5. High-Volume Inventory Capacity
    // =========================================================================

    @Test
    fun questItemGrants_neverDropWhenInventoryHasManyItems() {
        // Populate inventory with 100 distinct items
        val largeInventory = (1..100).associate { "item_slot_$it" to it * 2 }
        inventoryService.restore(largeInventory)
        assertEquals(100, inventoryService.snapshot().size)

        // Grant key quest items
        inventoryService.addItem("mine_access_badge", 1)
        inventoryService.addItem("encrypted_ledger", 1)
        inventoryService.addItem("sysadmin_keycard", 1)

        val snapshot = inventoryService.snapshot()
        assertEquals(103, snapshot.size)
        assertEquals(1, snapshot["mine_access_badge"])
        assertEquals(1, snapshot["encrypted_ledger"])
        assertEquals(1, snapshot["sysadmin_keycard"])
    }
}
