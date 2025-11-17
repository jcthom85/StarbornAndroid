package com.example.starborn.data.assets

import com.example.starborn.domain.model.ShopBuys
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.ShopGate
import com.example.starborn.domain.model.ShopDialogue
import com.example.starborn.domain.model.ShopPricing
import com.example.starborn.domain.model.ShopSells
import com.example.starborn.domain.model.ShopSellsRules
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

class ShopAssetDataSource(
    private val assetReader: AssetJsonReader
) {

    fun loadShops(): List<ShopDefinition> {
        val rawMap: Map<String, ShopRaw> = assetReader.readMap("shops.json")
        if (rawMap.isEmpty()) return emptyList()
        return rawMap.map { (id, raw) -> raw.toDefinition(id) }
    }

    @JsonClass(generateAdapter = true)
    data class ShopRaw(
        val name: String,
        val portrait: String? = null,
        val greeting: String? = null,
        @Json(name = "vo_cue")
        val voCue: String? = null,
        val pricing: ShopPricing? = null,
        val sells: ShopSells = ShopSells(),
        val buys: ShopBuys? = null,
        val dialogue: ShopDialogue? = null
    ) {
        fun toDefinition(id: String): ShopDefinition = ShopDefinition(
            id = id,
            name = name,
            portrait = portrait,
            greeting = greeting,
            voCue = voCue,
            pricing = pricing,
            sells = sells,
            buys = buys,
            dialogue = dialogue
        )
    }
}
