package com.example.starborn.data.assets

import com.example.starborn.domain.model.Item

class ItemAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadItems(): List<Item> = assetReader.readList("items.json")
}
