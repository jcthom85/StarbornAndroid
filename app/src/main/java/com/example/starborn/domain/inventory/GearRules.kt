package com.example.starborn.domain.inventory

import com.example.starborn.domain.model.Equipment
import java.util.Locale

object GearRules {
    val equipSlots: List<String> = listOf("weapon", "armor", "accessory", "snack")

    private val weaponTypesByCharacter: Map<String, String> = mapOf(
        "nova" to "gun",
        "zeke" to "glove",
        "orion" to "jewel",
        "gh0st" to "sword",
        "ollie" to "slingshot"
    )
    private val armorTypesByCharacter: Map<String, String> = mapOf(
        "nova" to "armor_nova",
        "zeke" to "armor_zeke",
        "orion" to "armor_orion",
        "gh0st" to "armor_gh0st",
        "ollie" to "armor_ollie"
    )
    private val weaponTypes: Set<String> = weaponTypesByCharacter.values.toSet()
    private val charactersByWeaponType: Map<String, String> =
        weaponTypesByCharacter.entries.associate { (characterId, type) -> type to characterId }
    private val armorTypes: Set<String> = armorTypesByCharacter.values.toSet()
    private val charactersByArmorType: Map<String, String> =
        armorTypesByCharacter.entries.associate { (characterId, type) -> type to characterId }

    private fun normalize(raw: String?): String? =
        raw
            ?.trim()
            ?.lowercase(Locale.getDefault())

    fun allowedWeaponTypeFor(characterId: String?): String? =
        normalize(characterId)?.let { weaponTypesByCharacter[it] }

    fun isWeaponType(type: String?): Boolean = normalize(type)?.let { weaponTypes.contains(it) } == true

    fun characterForWeaponType(type: String?): String? =
        normalize(type)?.let { charactersByWeaponType[it] }

    fun allowedArmorTypeFor(characterId: String?): String? =
        normalize(characterId)?.let { armorTypesByCharacter[it] }

    fun isArmorType(type: String?): Boolean = normalize(type)?.let { armorTypes.contains(it) } == true

    fun characterForArmorType(type: String?): String? =
        normalize(type)?.let { charactersByArmorType[it] }

    private fun resolveSlotAndWeaponType(
        equipment: Equipment?,
        itemTypeHint: String?
    ): Pair<String, String?>? {
        val normalizedSlot = normalize(equipment?.slot)
        val normalizedType = normalize(itemTypeHint)
        val slot = when {
            normalizedSlot != null && equipSlots.contains(normalizedSlot) -> normalizedSlot
            normalizedType != null && equipSlots.contains(normalizedType) -> normalizedType
            normalizedType != null && isWeaponType(normalizedType) -> "weapon"
            else -> null
        } ?: return null

        val weaponType = when {
            slot != "weapon" -> null
            normalize(equipment?.weaponType) != null -> normalize(equipment?.weaponType)
            normalizedType != null && isWeaponType(normalizedType) -> normalizedType
            else -> null
        }
        return slot to weaponType
    }

    fun matchesSlot(
        equipment: Equipment?,
        slotId: String,
        characterId: String?,
        itemTypeHint: String? = null
    ): Boolean {
        val normalizedSlot = normalize(slotId) ?: return false
        val (itemSlot, itemWeaponType) = resolveSlotAndWeaponType(equipment, itemTypeHint) ?: return false
        if (itemSlot != normalizedSlot) return false

        val normalizedType = normalize(itemTypeHint)
        if (normalizedSlot != "weapon") {
            if (normalizedType != null && equipSlots.contains(normalizedType) && normalizedType != normalizedSlot) return false
            if (normalizedSlot == "armor") {
                val expectedArmorType = allowedArmorTypeFor(characterId) ?: return true
                val armorType = normalizedType?.takeIf { isArmorType(it) } ?: return false
                return armorType == expectedArmorType
            }
            return true
        }

        val expectedWeaponType = allowedWeaponTypeFor(characterId) ?: return true
        val typeToCheck = itemWeaponType ?: normalizedType
        return typeToCheck == expectedWeaponType
    }
}
