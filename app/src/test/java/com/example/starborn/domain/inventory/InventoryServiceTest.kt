package com.example.starborn.domain.inventory

import com.example.starborn.domain.inventory.ItemUseResult.Buff
import com.example.starborn.domain.inventory.ItemUseResult.LearnSchematic
import com.example.starborn.domain.inventory.ItemUseResult.Restore
import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryServiceTest {

    private lateinit var inventoryService: InventoryService

    @Before
    fun setup() {
        val catalog = FakeItemCatalog(
            listOf(
                schematicItem(),
                medkitItem(),
                latteItem()
            )
        )
        inventoryService = InventoryService(catalog)
        inventoryService.loadItems()
    }

    @Test
    fun useItemReturnsNullWhenNotOwned() {
        val result = inventoryService.useItem("medkit_i")
        assertNull(result)
    }

    @Test
    fun useItemConsumesSchematicAndReturnsResult() {
        inventoryService.addItem("schematic_power_lens_1")

        val result = inventoryService.useItem("schematic_power_lens_1")

        assertNotNull(result)
        val schematicResult = result as LearnSchematic
        assertEquals("mod_power_lens_1", schematicResult.schematicId)
        assertTrue(inventoryService.state.value.isEmpty())
    }

    @Test
    fun useItemRestoresResources() {
        inventoryService.addItem("medkit_i")

        val result = inventoryService.useItem("medkit_i")

        assertNotNull(result)
        val restore = result as Restore
        assertEquals(50, restore.hp)
        assertEquals(0, restore.rp)
    }

    @Test
    fun useItemAppliesBuffs() {
        inventoryService.addItem("latte")

        val result = inventoryService.useItem("latte")

        assertNotNull(result)
        val buff = result as Buff
        assertEquals(2, buff.buffs.size)
        val stats = buff.buffs.map { it.stat }.toSet()
        assertTrue(stats.contains("spd"))
        assertTrue(stats.contains("focus"))
    }

    @Test
    fun addItemNotifiesListeners() {
        var notified = 0
        val listener: (String, Int) -> Unit = { id, qty ->
            if (id == "medkit_i" && qty == 2) {
                notified++
            }
        }
        inventoryService.addOnItemAddedListener(listener)

        inventoryService.addItem("medkit_i", 2)

        assertEquals(1, notified)
        inventoryService.removeOnItemAddedListener(listener)
    }
}

private class FakeItemCatalog(items: List<Item>) : ItemCatalog {
    private val entries: MutableMap<String, Item> = mutableMapOf()

    init {
        items.forEach { register(it) }
    }

    override fun load() {
        // no-op for tests
    }

    override fun findItem(idOrAlias: String): Item? = entries[idOrAlias.lowercase()]

    private fun register(item: Item) {
        entries[item.id.lowercase()] = item
        item.aliases.forEach { alias ->
            entries[alias.lowercase()] = item
        }
    }
}

private fun schematicItem() = Item(
    id = "schematic_power_lens_1",
    name = "Schematic: Power Lens Mk. I",
    type = "schematic",
    effect = ItemEffect(learnSchematic = "mod_power_lens_1")
)

private fun medkitItem() = Item(
    id = "medkit_i",
    name = "Medkit I",
    type = "consumable",
    effect = ItemEffect(restoreHp = 50, restoreRp = 0)
)

private fun latteItem() = Item(
    id = "latte",
    name = "Latte",
    type = "consumable",
    effect = ItemEffect(
        buffs = listOf(
            BuffEffect(stat = "spd", value = 5, duration = 1),
            BuffEffect(stat = "focus", value = 5, duration = 1)
        )
    )
)
