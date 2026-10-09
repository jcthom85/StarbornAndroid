package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShopPricing(
    @Json(name = "buy_markdown")
    val buyMarkdown: Double? = null,
    @Json(name = "sell_markup")
    val sellMarkup: Double? = null
)

@JsonClass(generateAdapter = true)
data class ShopSellsRules(
    @Json(name = "types_in")
    val typesIn: List<String> = emptyList(),
    @Json(name = "subtypes_in")
    val subtypesIn: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShopSells(
    val items: List<String> = emptyList(),
    val rules: ShopSellsRules = ShopSellsRules(),
    val gates: Map<String, ShopGate> = emptyMap(),
    @Json(name = "rotation_pool")
    val rotationPool: List<String> = emptyList(),
    @Json(name = "rotation_size")
    val rotationSize: Int? = null,
    @Json(name = "rotation_seed")
    val rotationSeed: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopGate(
    val milestones: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShopBuys(
    @Json(name = "accept_types")
    val acceptTypes: List<String> = emptyList(),
    val blacklist: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShopDefinition(
    val id: String,
    val name: String,
    val portrait: String? = null,
    val greeting: String? = null,
    @Json(name = "vo_cue")
    val voCue: String? = null,
    val pricing: ShopPricing? = null,
    val sells: ShopSells = ShopSells(),
    val buys: ShopBuys? = null,
    val dialogue: ShopDialogue? = null
)

@JsonClass(generateAdapter = true)
data class ShopDialogue(
    val preface: List<ShopDialogueLine> = emptyList(),
    val smalltalk: List<ShopDialogueTopic> = emptyList(),
    @Json(name = "trade_label")
    val tradeLabel: String? = null,
    @Json(name = "leave_label")
    val leaveLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopDialogueLine(
    val speaker: String? = null,
    val text: String,
    @Json(name = "voice")
    val voiceCue: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopDialogueTopic(
    val id: String,
    val label: String,
    val response: List<ShopDialogueLine> = emptyList(),
    @Json(name = "voice")
    val voiceCue: String? = null
)
