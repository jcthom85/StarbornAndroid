package com.example.starborn.domain.inventory

import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class InventoryService(
    private val itemCatalog: ItemCatalog
) {
    private val items: MutableMap<String, InventoryEntry> = mutableMapOf()
    private val _state = MutableStateFlow<List<InventoryEntry>>(emptyList())
    val state: StateFlow<List<InventoryEntry>> = _state.asStateFlow()
    private val itemAddedListeners: MutableSet<(String, Int) -> Unit> = mutableSetOf()

    fun loadItems() {
        itemCatalog.load()
        _state.value = items.values.toList()
    }

    fun snapshot(): Map<String, Int> = items.mapValues { it.value.quantity }.filterValues { it > 0 }

    private val placeholderItems: MutableMap<String, Item> = mutableMapOf()

    fun restore(entries: Map<String, Int>) {
        itemCatalog.load()
        items.clear()
        entries.forEach { (id, quantity) ->
            val normalizedQuantity = quantity.coerceAtLeast(0)
            if (normalizedQuantity <= 0) return@forEach
            val item = resolveItem(id) ?: return@forEach
            items[item.id] = InventoryEntry(item = item, quantity = normalizedQuantity)
        }
        publish()
    }

    fun addItem(idOrAlias: String, quantity: Int = 1) {
        if (quantity == 0) return
        val item = resolveItem(idOrAlias) ?: return
        val entry = items.getOrPut(item.id) { InventoryEntry(item, 0) }
        entry.quantity += quantity
        if (quantity > 0) {
            notifyItemAdded(item.id, quantity)
        }
        publish()
    }

    fun removeItem(idOrAlias: String, quantity: Int = 1) {
        val item = resolveItem(idOrAlias) ?: return
        removeItemById(item.id, quantity)
    }

    fun hasItem(idOrAlias: String, quantity: Int = 1): Boolean {
        val item = resolveItem(idOrAlias) ?: return false
        val entry = items[item.id] ?: return false
        return entry.quantity >= quantity
    }

    fun consumeItems(requirements: Map<String, Int>): Boolean {
        val resolved = mutableListOf<Pair<String, Int>>()
        for ((req, qty) in requirements) {
            val entry = findEntry(req) ?: return false
            if (entry.second.quantity < qty) return false
            resolved.add(entry.first to qty)
        }
        resolved.forEach { (id, qty) -> removeItemById(id, qty) }
        return true
    }

    fun useItem(idOrAlias: String): ItemUseResult? {
        val item = resolveItem(idOrAlias) ?: return null
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

    fun addOnItemAddedListener(listener: (String, Int) -> Unit) {
        itemAddedListeners.add(listener)
    }

    fun removeOnItemAddedListener(listener: (String, Int) -> Unit) {
        itemAddedListeners.remove(listener)
    }

    private fun notifyItemAdded(itemId: String, quantity: Int) {
        if (quantity <= 0) return
        itemAddedListeners.toList().forEach { it(itemId, quantity) }
    }

    fun itemDisplayName(idOrAlias: String): String {
        return resolveItem(idOrAlias)?.name ?: idOrAlias
    }

    fun itemDetail(idOrAlias: String): Item? = resolveItem(idOrAlias)

    /**
     * Returns a catalog-backed item for the provided id/alias, or null if not found.
     * Unlike itemDetail, this will not synthesize placeholders.
     */
    fun catalogItem(idOrAlias: String): Item? = itemCatalog.findItem(idOrAlias)

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

    private fun resolveItem(idOrAlias: String): Item? {
        val trimmed = idOrAlias.trim()
        if (trimmed.isEmpty()) return null
        val catalogItem = itemCatalog.findItem(trimmed)?.let { item ->
            val looksBroken = item.id.startsWith("broken_", ignoreCase = true) ||
                item.name.startsWith("Broken ", ignoreCase = true)
            if (looksBroken && item.categoryOverride == null && item.type.equals("misc", true)) {
                item.copy(categoryOverride = "crafting")
            } else item
        }
        if (catalogItem != null) return catalogItem
        val normalized = trimmed.lowercase(Locale.getDefault())
        placeholderItems[normalized]?.let { return it }
        val name = normalized.replace('_', ' ').replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }.ifBlank { trimmed }
        val inferredSlot = inferEquipmentSlot(normalized)
        val type = when {
            inferredSlot != null -> inferredSlot
            normalized.contains("key") -> "key_item"
            normalized.contains("quest") -> "quest"
            normalized.contains("schematic") -> "schematic"
            normalized.contains("recipe") -> "schematic"
            normalized.contains("medkit") || normalized.contains("potion") -> "consumable"
            normalized.contains("ingredient") || normalized.contains("pepper") ||
                normalized.contains("fish") || normalized.contains("meat") ||
                normalized.contains("lettuce") -> "ingredient"
            normalized.contains("scrap") || normalized.contains("wiring") ||
                normalized.contains("component") || normalized.contains("core") ||
                normalized.contains("cell") -> "component"
            else -> "misc"
        }
        val placeholder = Item(
            id = normalized,
            name = name,
            type = type,
            equipment = inferredSlot?.let { Equipment(slot = it) }
        )
        placeholderItems[normalized] = placeholder
        return placeholder
    }

    private fun findEntry(idOrName: String): Pair<String, InventoryEntry>? {
        val needle = idOrName.trim()
        if (needle.isEmpty()) return null
        val normalizedNeedle = normalizeToken(needle)
        return items.entries.firstOrNull { entry ->
            val item = entry.value.item
            item.id.equals(needle, ignoreCase = true) ||
                item.name.equals(needle, ignoreCase = true) ||
                item.aliases.any { it.equals(needle, ignoreCase = true) } ||
                normalizeToken(item.id) == normalizedNeedle ||
                normalizeToken(item.name) == normalizedNeedle ||
                item.aliases.any { normalizeToken(it) == normalizedNeedle }
        }?.let { it.key to it.value }
    }

    private fun inferEquipmentSlot(normalized: String): String? {
        val hasWeaponKeywords = listOf(
            "weapon", "sword", "blade", "blaster", "gun", "rifle", "pistol", "slingshot", "bow", "staff"
        ).any { normalized.contains(it) }
        if (hasWeaponKeywords) return "weapon"

        if (normalized.contains("shield") || normalized.contains("offhand")) {
            return "offhand"
        }

        val hasArmorKeywords = listOf(
            "armor", "armour", "vest", "jacket", "coat", "jumpsuit", "plates", "plating"
        ).any { normalized.contains(it) }
        if (hasArmorKeywords) return "armor"

        val hasAccessoryKeywords = listOf(
            "medal", "medallion", "amulet", "ring", "belt", "trinket", "charm", "watch", "bracelet"
        ).any { normalized.contains(it) }
        if (hasAccessoryKeywords) return "accessory"

        val hasSnackKeywords = listOf(
            "snack", "bar", "mix", "gummies", "cookie", "ration", "brew"
        ).any { normalized.contains(it) }
        return if (hasSnackKeywords) "snack" else null
    }

    private fun normalizeToken(raw: String): String =
        raw.trim().lowercase(Locale.getDefault()).replace("[^a-z0-9]+".toRegex(), "")
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

private val LOOT_ID_OVERRIDES = mapOf(
    "scrap" to "scrap_metal",
    "scrap metal" to "scrap_metal",
    "wiring" to "wiring_bundle"
)

fun normalizeLootItemId(rawId: String): String {
    val normalized = rawId.trim().lowercase(Locale.getDefault())
    return LOOT_ID_OVERRIDES[normalized] ?: rawId
}
