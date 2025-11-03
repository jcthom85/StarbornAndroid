package com.example.starborn.domain.inventory

import com.example.starborn.domain.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventoryService(
    private val itemCatalog: ItemCatalog
) {
    private val items: MutableMap<String, InventoryEntry> = mutableMapOf()
    private val _state = MutableStateFlow<List<InventoryEntry>>(emptyList())
    val state: StateFlow<List<InventoryEntry>> = _state.asStateFlow()

    fun loadItems() {
        itemCatalog.load()
        _state.value = items.values.toList()
    }

    fun snapshot(): Map<String, Int> = items.mapValues { it.value.quantity }.filterValues { it > 0 }

    fun restore(entries: Map<String, Int>) {
        itemCatalog.load()
        items.clear()
        entries.forEach { (id, quantity) ->
            val normalizedQuantity = quantity.coerceAtLeast(0)
            if (normalizedQuantity <= 0) return@forEach
            val item = itemCatalog.findItem(id) ?: return@forEach
            items[id] = InventoryEntry(item = item, quantity = normalizedQuantity)
        }
        publish()
    }

    fun addItem(idOrAlias: String, quantity: Int = 1) {
        val item = itemCatalog.findItem(idOrAlias) ?: return
        val entry = items.getOrPut(item.id) { InventoryEntry(item, 0) }
        entry.quantity += quantity
        publish()
    }

    fun removeItem(idOrAlias: String, quantity: Int = 1) {
        val item = itemCatalog.findItem(idOrAlias) ?: return
        removeItemById(item.id, quantity)
    }

    fun hasItem(idOrAlias: String, quantity: Int = 1): Boolean {
        val item = itemCatalog.findItem(idOrAlias) ?: return false
        val entry = items[item.id] ?: return false
        return entry.quantity >= quantity
    }

    fun consumeItems(requirements: Map<String, Int>): Boolean {
        if (!requirements.all { hasItem(it.key, it.value) }) return false
        requirements.forEach { (id, qty) -> removeItem(id, qty) }
        return true
    }

    fun useItem(idOrAlias: String): ItemUseResult? {
        val item = itemCatalog.findItem(idOrAlias) ?: return null
        if (!hasItemDirect(item.id)) return null
        val effect = item.effect
        val result = when {
            effect == null -> ItemUseResult.None(item)
            (effect.restoreHp ?: 0) > 0 || (effect.restoreRp ?: 0) > 0 -> ItemUseResult.Restore(
                item = item,
                hp = effect.restoreHp ?: 0,
                rp = effect.restoreRp ?: 0
            )
            (effect.damage ?: 0) > 0 -> ItemUseResult.Damage(
                item = item,
                amount = effect.damage ?: 0
            )
            effect.learnSchematic != null -> ItemUseResult.LearnSchematic(
                item = item,
                schematicId = effect.learnSchematic
            )
            effect.singleBuff != null || !effect.buffs.isNullOrEmpty() -> {
                val buffs = buildList {
                    effect.singleBuff?.let { add(it) }
                    effect.buffs?.let { addAll(it) }
                }
                ItemUseResult.Buff(item = item, buffs = buffs)
            }
            else -> ItemUseResult.None(item)
        }
        removeItemById(item.id, 1)
        return result
    }

    private fun publish() {
        _state.value = items.values.sortedBy { it.item.name }.toList()
    }

    fun itemDisplayName(idOrAlias: String): String {
        return itemCatalog.findItem(idOrAlias)?.name ?: idOrAlias
    }

    private fun hasItemDirect(itemId: String): Boolean =
        items[itemId]?.quantity?.let { it > 0 } ?: false

    private fun removeItemById(itemId: String, quantity: Int) {
        val entry = items[itemId] ?: return
        entry.quantity = (entry.quantity - quantity).coerceAtLeast(0)
        if (entry.quantity == 0) {
            items.remove(itemId)
        }
        publish()
    }
}

data class InventoryEntry(
    val item: Item,
    var quantity: Int
)

sealed interface ItemUseResult {
    val item: Item

    data class None(override val item: Item) : ItemUseResult
    data class Restore(override val item: Item, val hp: Int, val rp: Int) : ItemUseResult
    data class Damage(override val item: Item, val amount: Int) : ItemUseResult
    data class Buff(override val item: Item, val buffs: List<com.example.starborn.domain.model.BuffEffect>) : ItemUseResult
    data class LearnSchematic(override val item: Item, val schematicId: String) : ItemUseResult
}

interface ItemCatalog {
    fun load()
    fun findItem(idOrAlias: String): Item?
}
