package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

@JsonClass(generateAdapter = true)
data class Item(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val type: String,
    @Json(name = "category_override")
    val categoryOverride: String? = null,
    val value: Int = 0,
    @Json(name = "buy_price")
    val buyPrice: Int? = null,
    val rarity: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val equipment: Equipment? = null,
    val effect: ItemEffect? = null,
    val unsellable: Boolean = false,
    // Optional base used before the shop's markdown; purchase pricing is unchanged.
    @Json(name = "resale_value")
    val resaleValue: Int? = null
) {
    fun resolvedIconAssetPath(): String? =
        icon?.trim()?.takeIf { it.isNotEmpty() } ?: itemIconAssetPath(id)
}

fun itemIconAssetPath(itemId: String?): String? =
    itemId?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotEmpty() }
        ?.let { "images/items/$it.webp" }

enum class SupplyCategory(val label: String) {
    FOOD("Food"),
    FIELD("Field"),
    CRAFT("Craft")
}

private val FOOD_CONSUMABLE_IDS = setOf(
    "tideglass_delight", "black_coffee", "ellies_signature_sampler", "latte",
    "ration_pack", "spicy_ramen", "celestial_fillet", "chronos_stew",
    "ration_soup", "glowfish_broth", "resonance_carp_stew", "chime_minnow_broth",
    "stellarium_eel_skewer", "frequency_tetra_consomme", "foundry_smoked_brisket",
    "orbital_glimmer_tonic", "source_astral_feast", "gagas_skillet_biscuits"
)

fun classifySupplyCategory(itemId: String, itemType: String): SupplyCategory {
    val type = itemType.trim().lowercase(Locale.ROOT)
    val id = itemId.trim().lowercase(Locale.ROOT)
    return when {
        type == "snack" || id in FOOD_CONSUMABLE_IDS ||
            (type == "consumable" && (id.contains("stew") || id.contains("broth") || id.contains("ramen") ||
                id.contains("delight") || id.contains("brisket") || id.contains("feast") ||
                id.contains("fillet") || id.contains("tonic") || id.contains("consomme") ||
                id.contains("skewer") || id.contains("soup") || id.contains("biscuit") ||
                id.contains("coffee") || id.contains("latte") || id.contains("ration"))) -> SupplyCategory.FOOD
        type == "consumable" || type == "medicine" -> SupplyCategory.FIELD
        else -> SupplyCategory.CRAFT
    }
}

@JsonClass(generateAdapter = true)
data class Equipment(
    val slot: String,
    @Json(name = "weapon_type")
    val weaponType: String? = null,
    @Json(name = "damage_min")
    val damageMin: Int? = null,
    @Json(name = "damage_max")
    val damageMax: Int? = null,
    @Json(name = "attack_style")
    val attackStyle: String? = null,
    @Json(name = "attack_power_mult")
    val attackPowerMultiplier: Double? = null,
    @Json(name = "attack_charge_turns")
    val attackChargeTurns: Int? = null,
    @Json(name = "attack_splash_mult")
    val attackSplashMultiplier: Double? = null,
    @Json(name = "attack_element")
    val attackElement: String? = null,
    @Json(name = "status_on_hit")
    val statusOnHit: String? = null,
    @Json(name = "status_chance")
    val statusChance: Double? = null,
    @Json(name = "defense")
    val defense: Int? = null,
    @Json(name = "hp_bonus")
    val hpBonus: Int? = null,
    val accuracy: Double? = null,
    @Json(name = "crit_rate")
    val critRate: Double? = null,
    @Json(name = "stat_mods")
    val statMods: Map<String, Int>? = null,
    @Json(name = "passive_id")
    val passiveId: String? = null,
    @Json(name = "passive_text")
    val passiveText: String? = null
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
    val damage: Int? = null,
    @Json(name = "learn_schematic")
    val learnSchematic: String? = null,
    @Json(name = "buff_stat")
    val singleBuff: BuffEffect? = null,
    val buffs: List<BuffEffect>? = null,
    val cooldown: Int? = null,
    @Json(name = "uses_per_battle")
    val usesPerBattle: Int? = null,
    val cleanses: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BuffEffect(
    val stat: String,
    val value: Int,
    val duration: Int? = null
)
