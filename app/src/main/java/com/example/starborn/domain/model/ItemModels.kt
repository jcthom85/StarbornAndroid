package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Item(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val type: String,
    val value: Int = 0,
    @Json(name = "buy_price")
    val buyPrice: Int? = null,
    val rarity: String? = null,
    val description: String? = null,
    val equipment: Equipment? = null,
    val effect: ItemEffect? = null,
    val unsellable: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Equipment(
    val slot: String,
    @Json(name = "weapon_type")
    val weaponType: String? = null,
    @Json(name = "damage_min")
    val damageMin: Int? = null,
    @Json(name = "damage_max")
    val damageMax: Int? = null,
    @Json(name = "defense")
    val defense: Int? = null,
    @Json(name = "hp_bonus")
    val hpBonus: Int? = null,
    @Json(name = "stat_mods")
    val statMods: Map<String, Int>? = null
)

@JsonClass(generateAdapter = true)
data class ItemEffect(
    val type: String? = null,
    val amount: Int? = null,
    val duration: Int? = null,
    @Json(name = "status")
    val status: String? = null,
    val target: String? = null,
    @Json(name = "restore_hp")
    val restoreHp: Int? = null,
    @Json(name = "restore_rp")
    val restoreRp: Int? = null,
    val damage: Int? = null,
    @Json(name = "learn_schematic")
    val learnSchematic: String? = null,
    @Json(name = "buff_stat")
    val singleBuff: BuffEffect? = null,
    val buffs: List<BuffEffect>? = null
)

@JsonClass(generateAdapter = true)
data class BuffEffect(
    val stat: String,
    val value: Int,
    val duration: Int? = null
)
