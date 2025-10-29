package com.example.starborn.data.repository

import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item

class ItemRepository(
    private val dataSource: ItemAssetDataSource
) : ItemCatalog {
    private val itemsById: MutableMap<String, Item> = mutableMapOf()
    private val aliasMap: MutableMap<String, String> = mutableMapOf()

    override fun load() {
        val items = dataSource.loadItems()
        itemsById.clear()
        aliasMap.clear()
        items.forEach { item ->
            itemsById[item.id] = item
            aliasMap[item.id.lowercase()] = item.id
            item.aliases.forEach { alias ->
                aliasMap[alias.lowercase()] = item.id
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
