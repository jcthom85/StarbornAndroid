package com.example.starborn.data.repository

import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.shop.ShopCatalog

class ShopRepository(
    private val dataSource: ShopAssetDataSource
) : ShopCatalog {
    private val shopsById: MutableMap<String, ShopDefinition> = mutableMapOf()

    fun load() {
        val shops = dataSource.loadShops()
        shopsById.clear()
        shops.forEach { shop ->
            shopsById[shop.id] = shop
        }
    }

    override fun shopById(id: String?): ShopDefinition? {
        if (id.isNullOrBlank()) return null
        return shopsById[id]
    }

    override fun allShops(): Collection<ShopDefinition> = shopsById.values
}
