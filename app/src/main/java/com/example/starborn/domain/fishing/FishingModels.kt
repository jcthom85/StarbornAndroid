package com.example.starborn.domain.fishing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FishingData(
    val rods: List<FishingRod> = emptyList(),
    val lures: List<FishingLure> = emptyList(),
    val zones: List<FishingZone> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FishingRod(
    val id: String,
    val name: String,
    val description: String? = null,
    val power: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class FishingLure(
    val id: String,
    val name: String,
    val description: String? = null,
    @Json(name = "rarity_bonus")
    val rarityBonus: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class FishingZone(
    val id: String,
    val name: String,
    @Json(name = "minigame_rules")
    val minigameRules: MinigameRules = MinigameRules(),
    val catches: List<FishingCatchDefinition> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MinigameRules(
    val speed: Double = 1.0,
    @Json(name = "success_window")
    val successWindow: Double = 0.3,
    @Json(name = "perfect_window")
    val perfectWindow: Double = 0.1
)

@JsonClass(generateAdapter = true)
data class FishingCatchDefinition(
    @Json(name = "item_id")
    val itemId: String,
    val weight: Int = 1,
    @Json(name = "min_quantity")
    val minQuantity: Int = 1,
    @Json(name = "max_quantity")
    val maxQuantity: Int = 1
)