package com.example.starborn.domain.fishing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FishingData(
    val meta: FishingMeta? = null,
    val rods: List<FishingRod> = emptyList(),
    val lures: List<FishingLure> = emptyList(),
    val zones: Map<String, List<FishingCatchDefinition>> = emptyMap(),
    @Json(name = "fish_behaviors")
    val fishBehaviors: Map<String, FishBehaviorDefinition> = emptyMap(),
    @Json(name = "minigame_rules")
    val minigameRules: Map<String, FishingMinigameRule> = emptyMap(),
    @Json(name = "victory_screen")
    val victoryScreen: VictoryScreenConfig? = null
)

@JsonClass(generateAdapter = true)
data class FishingMeta(
    val version: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class FishingRod(
    val id: String,
    val name: String,
    val description: String? = null,
    @Json(name = "fishing_power")
    val fishingPower: Double = 1.0,
    val stability: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class FishingLure(
    val id: String,
    val name: String,
    val description: String? = null,
    @Json(name = "rarity_bonus")
    val rarityBonus: Double = 0.0,
    val attracts: List<String> = emptyList(),
    @Json(name = "zone_bonus")
    val zoneBonuses: Map<String, Int> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class FishingCatchDefinition(
    @Json(name = "item")
    val itemId: String,
    val weight: Int = 1,
    val rarity: FishingRarity = FishingRarity.COMMON,
    @Json(name = "behavior_id")
    val behaviorId: String? = null
)

@JsonClass(generateAdapter = true)
data class FishBehaviorDefinition(
    val pattern: FishPattern = FishPattern.SINE,
    @Json(name = "base_pull")
    val basePull: Double = 0.5,
    @Json(name = "burst_pull")
    val burstPull: Double = 0.0,
    val stamina: Double = 10.0
)

@JsonClass(generateAdapter = true)
data class FishingMinigameRule(
    @Json(name = "target_size")
    val targetSize: Double = 0.3,
    @Json(name = "bar_speed")
    val barSpeed: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class VictoryScreenConfig(
    @Json(name = "show_on_success")
    val showOnSuccess: Boolean = true,
    @Json(name = "show_on_failure")
    val showOnFailure: Boolean = false,
    val contents: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FishingZone(
    val id: String,
    val name: String,
    val catches: List<FishingCatchDefinition> = emptyList()
)

enum class FishingRarity {
    @Json(name = "junk")
    JUNK,

    @Json(name = "common")
    COMMON,

    @Json(name = "uncommon")
    UNCOMMON,

    @Json(name = "rare")
    RARE,

    @Json(name = "epic")
    EPIC
}

enum class FishPattern {
    @Json(name = "sine")
    SINE,
    @Json(name = "linear")
    LINEAR,
    @Json(name = "burst")
    BURST
}
