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

    override fun load() {
        val items = dataSource.loadItems()
        itemsById.clear()
        aliasMap.clear()
        val whitespaceRegex = Regex("\\s+")
        items.forEach { item ->
            itemsById[item.id] = item
            aliasMap[item.id.lowercase(Locale.getDefault())] = item.id
            item.aliases.forEach { alias ->
                aliasMap[alias.lowercase(Locale.getDefault())] = item.id
            }
            val name = item.name.trim()
            if (name.isNotBlank()) {
                val lower = name.lowercase(Locale.getDefault())
                aliasMap[lower] = item.id
                val underscored = lower.replace(whitespaceRegex, "_")
                aliasMap[underscored] = item.id
            }
        }
    }

    fun allItems(): List<Item> = itemsById.values.toList()

    override fun findItem(idOrAlias: String): Item? {
        val key = idOrAlias.lowercase()
        val id = itemsById[key]?.id ?: aliasMap[key]
        return id?.let { itemsById[it] }
    }
}
