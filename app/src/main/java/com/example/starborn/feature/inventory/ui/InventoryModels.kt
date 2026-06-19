package com.example.starborn.feature.inventory.ui

import androidx.annotation.DrawableRes
import com.example.starborn.R
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import java.util.Locale
import kotlin.math.roundToInt

internal const val CATEGORY_ALL = "all"
internal const val CATEGORY_CONSUMABLES = "consumables"
internal const val CATEGORY_EQUIPMENT = "equipment"
internal const val CATEGORY_CRAFTING = "crafting"
internal const val CATEGORY_KEY_ITEMS = "key_items"
internal const val CATEGORY_OTHER = "other"
internal val EQUIP_SLOTS = GearRules.equipSlots

enum class InventoryTab { SUPPLIES, GEAR, KEY_ITEMS }

data class InventoryLaunchOptions(
    val initialTab: InventoryTab? = null,
    val focusSlot: String? = null,
    val initialCharacterId: String? = null
)

data class WeaponOption(
    val id: String,
    val item: Item
)

data class ArmorOption(
    val id: String,
    val item: Item
)

data class InventoryHealPulse(
    val id: Long,
    val amount: Int
)

internal fun Item.categoryKey(): String {
    categoryOverride?.let { return it.lowercase(Locale.getDefault()) }
    if (equipment != null) return CATEGORY_EQUIPMENT
    val normalized = type.lowercase(Locale.getDefault())
    if (GearRules.isWeaponType(normalized)) return CATEGORY_EQUIPMENT
    val idLower = id.lowercase(Locale.getDefault())
    val nameLower = name.lowercase(Locale.getDefault())
    val looksBrokenComponent = idLower.startsWith("broken_") || nameLower.contains("broken ")
    return when (normalized) {
        "consumable", "medicine", "food", "drink", "tonic" -> CATEGORY_CONSUMABLES
        "weapon", "armor", "shield", "accessory", "gear", "snack" -> CATEGORY_EQUIPMENT
        "material", "ingredient", "component", "resource" -> CATEGORY_CRAFTING
        // Treat broken quest components as crafting parts so they appear under Supplies -> Crafting.
        "misc" -> if (looksBrokenComponent) CATEGORY_CRAFTING else CATEGORY_OTHER
        else -> CATEGORY_OTHER
    }
}

internal fun categoryLabel(key: String): String = when (key) {
    CATEGORY_ALL -> "All"
    CATEGORY_CONSUMABLES -> "Consumables"
    CATEGORY_EQUIPMENT -> "Equipment"
    CATEGORY_CRAFTING -> "Crafting"
    CATEGORY_KEY_ITEMS -> "Key Items"
    CATEGORY_OTHER -> "Other"
    else -> key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@DrawableRes
internal fun itemIconRes(item: Item?): Int {
    if (item == null) return R.drawable.item_icon_generic
    val normalizedType = item.type.lowercase(Locale.getDefault())
    val name = item.name.lowercase(Locale.getDefault())
    return when {
        normalizedType == "mod" || item.equipment?.slot?.equals("mod", true) == true ->
            R.drawable.item_icon_material
        normalizedType.contains("food") || listOf("stew", "salad", "ramen", "cake").any { name.contains(it) } ->
            R.drawable.item_icon_food
        normalizedType in setOf("consumable", "medicine", "tonic", "drink") ->
            R.drawable.item_icon_consumable
        normalizedType.contains("fish") ->
            R.drawable.item_icon_fish
        normalizedType.contains("ingredient") || normalizedType.contains("material") ->
            R.drawable.item_icon_ingredient
        normalizedType.contains("fishing") ->
            R.drawable.item_icon_fishing
        normalizedType.contains("lure") ->
            R.drawable.item_icon_lure
        normalizedType in setOf("weapon", "armor", "accessory", "gear", "snack") ||
            GearRules.isWeaponType(normalizedType) || item.equipment != null -> {
            val slot = item.equipment?.slot?.lowercase(Locale.getDefault())
                ?: normalizedType.takeIf { EQUIP_SLOTS.contains(it) }
                ?: if (GearRules.isWeaponType(normalizedType)) "weapon" else null
            when {
                slot == "snack" -> R.drawable.item_icon_food
                slot == "armor" && name.contains("glove") -> R.drawable.item_icon_gloves
                slot == "armor" && name.contains("pendant") -> R.drawable.item_icon_pendant
                slot == "armor" -> R.drawable.item_icon_armor
                slot == "accessory" -> R.drawable.item_icon_accessory
                slot == "weapon" && (name.contains("gun") || name.contains("pistol") || name.contains("rifle")) ->
                    R.drawable.item_icon_gun
                slot == "weapon" -> R.drawable.item_icon_sword
                else -> R.drawable.item_icon_generic
            }
        }
        else -> R.drawable.item_icon_generic
    }
}

internal fun slotIconRes(slot: String): Int {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> R.drawable.item_icon_sword
        "armor" -> R.drawable.item_icon_armor
        "accessory" -> R.drawable.item_icon_accessory
        "snack" -> R.drawable.item_icon_food
        else -> R.drawable.item_icon_generic
    }
}

internal fun isModItem(item: Item): Boolean {
    val normalizedType = item.type.lowercase(Locale.getDefault())
    return normalizedType == "mod" || item.equipment?.slot?.equals("mod", true) == true
}

internal fun filterGearOptions(
    entries: List<InventoryEntry>,
    slotId: String?,
    characterId: String?
): List<InventoryEntry> {
    val normalizedSlot = slotId?.trim()?.lowercase(Locale.getDefault()) ?: return emptyList()
    return entries.filter { entry ->
        GearRules.matchesSlot(
            equipment = entry.item.equipment,
            slotId = normalizedSlot,
            characterId = characterId,
            itemTypeHint = entry.item.type
        )
    }
}

internal fun formatSigned(value: Int): String =
    if (value >= 0) "+$value" else value.toString()

internal fun formatSignedDecimal(value: Double): String {
    val formatted = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
    return if (value >= 0) "+$formatted" else formatted
}

internal fun formatMultiplier(value: Double): String {
    val formatted = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
    return "x$formatted"
}

internal fun formatPercent(value: Double): String {
    val percent = if (value <= 1.0) value * 100.0 else value
    val rounded = percent.roundToInt()
    return "$rounded%"
}

internal fun slotDisplayName(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
        .ifBlank { raw }

internal fun String.humanizeId(): String =
    split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }

fun formatDamageLabel(equipment: Equipment): String? {
    val damageMin = equipment.damageMin
    val damageMax = equipment.damageMax
    return when {
        damageMin == null && damageMax == null -> null
        damageMin != null && damageMax != null && damageMin != damageMax -> "${damageMin}–${damageMax}"
        damageMin != null -> damageMin.toString()
        else -> damageMax.toString()
    }
}

fun averageDamageValue(equipment: Equipment): Int? {
    val values = listOfNotNull(equipment.damageMin, equipment.damageMax)
    if (values.isEmpty()) return null
    return values.average().roundToInt()
}

fun primaryStatSummary(item: Item): String? {
    val equipment = item.equipment ?: return null
    formatDamageLabel(equipment)?.let { return "DMG $it" }
    equipment.defense?.let { return "DEF ${it}" }
    equipment.hpBonus?.let { return "HP ${formatSigned(it)}" }
    equipment.accuracy?.let { return "ACC ${formatSignedDecimal(it)}" }
    equipment.critRate?.let { return "CRIT ${formatSignedDecimal(it)}" }
    equipment.statMods?.entries?.firstOrNull()?.let { (stat, value) ->
        return "${stat.uppercase(Locale.getDefault())} ${formatSigned(value)}"
    }
    return null
}

fun buildWeaponOptions(
    unlockedWeapons: Set<String>,
    characterId: String?,
    resolveWeaponItem: (String) -> Item?
): List<WeaponOption> {
    val expectedType = GearRules.allowedWeaponTypeFor(characterId)
    val options = unlockedWeapons.mapNotNull { rawId ->
        val item = resolveWeaponItem(rawId) ?: return@mapNotNull null
        val weaponType = weaponTypeFor(item)
        if (expectedType != null && weaponType != null && weaponType != expectedType) return@mapNotNull null
        if (expectedType != null && weaponType == null && item.equipment?.slot?.equals("weapon", true) != true) {
            return@mapNotNull null
        }
        WeaponOption(id = item.id, item = item)
    }
    return options
        .distinctBy { it.id }
        .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
}

fun buildArmorOptions(
    unlockedArmors: Set<String>,
    characterId: String?,
    resolveArmorItem: (String) -> Item?
): List<ArmorOption> {
    val expectedType = GearRules.allowedArmorTypeFor(characterId)
    val options = unlockedArmors.mapNotNull { rawId ->
        val item = resolveArmorItem(rawId) ?: return@mapNotNull null
        if (!item.isArmorItem()) return@mapNotNull null
        val armorType = item.type.trim().lowercase(Locale.getDefault())
        if (expectedType != null) {
            if (!GearRules.isArmorType(armorType)) return@mapNotNull null
            if (armorType != expectedType) return@mapNotNull null
        }
        ArmorOption(id = item.id, item = item)
    }
    return options
        .distinctBy { it.id }
        .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
}

fun weaponTypeFor(item: Item): String? {
    val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
    if (!weaponType.isNullOrBlank()) return weaponType
    val normalizedType = item.type.trim().lowercase(Locale.getDefault())
    return normalizedType.takeIf { GearRules.isWeaponType(it) }
}

fun Item.isArmorItem(): Boolean {
    val normalizedType = type.trim().lowercase(Locale.getDefault())
    return normalizedType == "armor" || equipment?.slot?.equals("armor", ignoreCase = true) == true
}

fun weaponSummaryLine(item: Item): String? {
    val equipment = item.equipment ?: return null
    val parts = mutableListOf<String>()
    formatDamageLabel(equipment)?.let { parts += "DMG $it" }
    equipment.attackStyle?.let { parts += slotDisplayName(it) }
    equipment.attackElement?.let { parts += slotDisplayName(it) }
    return parts.joinToString(" | ").takeIf { it.isNotBlank() }
}

fun armorSummaryLine(item: Item): String? {
    val equipment = item.equipment ?: return null
    val parts = mutableListOf<String>()
    equipment.defense?.let { parts += "DEF ${formatSigned(it)}" }
    equipment.hpBonus?.let { parts += "HP ${formatSigned(it)}" }
    equipment.accuracy?.let { parts += "ACC ${formatSignedDecimal(it)}" }
    equipment.critRate?.let { parts += "CRIT ${formatSignedDecimal(it)}" }
    return parts.joinToString(" | ").takeIf { it.isNotBlank() }
}

fun weaponDetailRows(item: Item): List<Pair<String, String>> {
    val equipment = item.equipment ?: return emptyList()
    val rows = mutableListOf<Pair<String, String>>()
    formatDamageLabel(equipment)?.let { rows += "Damage" to it }
    equipment.attackStyle?.let { rows += "Style" to slotDisplayName(it) }
    equipment.attackPowerMultiplier?.let { rows += "Power" to formatMultiplier(it) }
    equipment.attackChargeTurns?.let { turns ->
        rows += "Charge" to "$turns turn${if (turns == 1) "" else "s"}"
    }
    equipment.attackSplashMultiplier?.let { rows += "Splash" to formatPercent(it) }
    equipment.attackElement?.let { rows += "Element" to slotDisplayName(it) }
    equipment.statusOnHit?.let { status ->
        val chanceLabel = equipment.statusChance?.let { formatPercent(it) }
        val label = chanceLabel?.let { "${slotDisplayName(status)} ($it)" } ?: slotDisplayName(status)
        rows += "Status" to label
    }
    equipment.accuracy?.let { rows += "Accuracy" to formatSignedDecimal(it) }
    equipment.critRate?.let { rows += "Crit Rate" to formatSignedDecimal(it) }
    return rows
}

fun armorDetailRows(item: Item): List<Pair<String, String>> {
    val equipment = item.equipment ?: return emptyList()
    val rows = mutableListOf<Pair<String, String>>()
    equipment.defense?.let { rows += "Defense" to formatSigned(it) }
    equipment.hpBonus?.let { rows += "HP Bonus" to formatSigned(it) }
    equipment.accuracy?.let { rows += "Accuracy" to formatSignedDecimal(it) }
    equipment.critRate?.let { rows += "Crit Rate" to formatSignedDecimal(it) }
    equipment.statMods?.forEach { (stat, value) ->
        rows += slotDisplayName(stat) to formatSigned(value)
    }
    return rows
}
