package com.example.starborn.data.repository

import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import java.util.Locale

class ItemRepository(
    private val dataSource: ItemAssetDataSource
) : ItemCatalog {
    private val itemsById: MutableMap<String, Item> = mutableMapOf()
    private val aliasMap: MutableMap<String, String> = mutableMapOf()
    private val whitespaceRegex = Regex("\\s+")
    private val punctuationRegex = Regex("[^a-z0-9_]")

    override fun load() {
        val items = dataSource.loadItems()
        itemsById.clear()
        aliasMap.clear()
        items.forEach { item ->
            val id = item.id.lowercase(Locale.getDefault())
            itemsById[id] = item
            registerAlias(item.id, item.id)
            item.aliases.forEach { alias -> registerAlias(alias, item.id) }
            registerAlias(item.name, item.id)
        }
    }

    fun allItems(): List<Item> = itemsById.values.toList()

    override fun findItem(idOrAlias: String): Item? {
        val normalized = idOrAlias.trim().lowercase(Locale.getDefault())
        if (normalized.isEmpty()) return null
        val candidates = buildList {
            add(normalized)
            add(whitespaceRegex.replace(normalized, "_"))
            add(normalized.replace('-', '_'))
            add(punctuationRegex.replace(normalized, ""))
            if (normalized.contains('_')) {
                add(normalized.replace('_', ' '))
            }
        }.mapNotNull { key -> key.takeIf { it.isNotBlank() } }
            .distinct()
        candidates.forEach { key ->
            itemsById[key]?.let { return it }
            aliasMap[key]?.let { id -> itemsById[id]?.let { return it } }
        }
        return null
    }

    private fun registerAlias(raw: String?, itemId: String) {
        if (raw.isNullOrBlank()) return
        val lower = raw.trim().lowercase(Locale.getDefault())
        if (lower.isEmpty()) return
        val variants = listOf(
            lower,
            whitespaceRegex.replace(lower, "_"),
            lower.replace('-', '_'),
            punctuationRegex.replace(lower, "")
        )
        variants.forEach { variant ->
            if (variant.isNotBlank()) {
                aliasMap[variant] = itemId
            }
        }
    }
}
