package com.example.starborn.domain.inventory

import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState

data class ModLocation(val characterId: String, val slotId: String) {
    val key get() = "${characterId.lowercase()}:${slotId.lowercase()}"
    val label get() = "${characterId.replaceFirstChar { it.uppercase() }} / ${slotId.replace('_', ' ')}"
}
data class ModTransferRequest(val target: ModLocation, val itemId: String, val sources: List<ModLocation>)

sealed interface ModEquipResult {
    data object Success : ModEquipResult
    data class Rejected(val message: String) : ModEquipResult
    data class TransferRequired(val sources: List<ModLocation>) : ModEquipResult
}

data class ModSlotComparison(
    val target: ModLocation,
    val equippedItemId: String?,
    val equippedItemName: String?,
    val statDeltas: Map<String, Int>,
    val effectiveElementBefore: String?,
    val effectiveElementAfter: String?,
    val blockedReason: String?,
    val movingFrom: ModLocation? = null
)

object ModEquipmentRules {
    val slots = listOf("weapon_mod1", "weapon_mod2", "armor_mod1", "armor_mod2")
    private val explicitWeaponOnlyModIds = setOf(
        "ergonomic_grip",
        "thermite_core_mod",
        "magma_induction_core_mod"
    )
    private val explicitArmorOnlyModIds = setOf(
        "relic_mod_thermal_aegis",
        "relic_mod_graviton_matrix",
        "adaptive_padding",
        "astral_weave_mod"
    )

    fun isWeaponOnlyMod(itemId: String, equipment: Equipment?): Boolean {
        val id = itemId.trim().lowercase()
        if (id in explicitWeaponOnlyModIds) return true
        val eq = equipment ?: return false
        return !eq.attackElement.isNullOrBlank() || !eq.statusOnHit.isNullOrBlank()
    }

    fun isArmorOnlyMod(itemId: String, equipment: Equipment?): Boolean {
        val id = itemId.trim().lowercase()
        if (id in explicitArmorOnlyModIds) return true
        val eq = equipment ?: return false
        return (eq.statMods?.get("defense") ?: 0) > 0
    }

    fun isWeaponOnlyMod(item: Item): Boolean = isWeaponOnlyMod(item.id, item.equipment)

    fun isArmorOnlyMod(item: Item): Boolean = isArmorOnlyMod(item.id, item.equipment)

    fun slotCompatibilityError(slotId: String, itemId: String, equipment: Equipment?): String? {
        val slot = slotId.trim().lowercase()
        return when {
            slot.startsWith("weapon_") && isArmorOnlyMod(itemId, equipment) ->
                "Armor plating and defensive mods can only be installed in armor mod slots."
            slot.startsWith("armor_") && isWeaponOnlyMod(itemId, equipment) ->
                "Weapon payload and strike mods can only be installed in weapon mod slots."
            else -> null
        }
    }

    fun slotCompatibilityError(slotId: String, item: Item): String? =
        slotCompatibilityError(slotId, item.id, item.equipment)

    fun locations(state: GameSessionState, itemId: String): List<ModLocation> = state.equippedItems.mapNotNull { (key, value) ->
        val slot = key.substringAfter(':')
        if (':' in key && slot in slots && value.equals(itemId, true)) ModLocation(key.substringBefore(':'), slot) else null
    }

    fun blockedReason(state: GameSessionState, target: ModLocation, item: Item? = null): String? {
        val owner = target.characterId.lowercase()
        val slot = target.slotId.lowercase()
        val party = state.partyMembers.ifEmpty { listOfNotNull(state.playerId) }
        return when {
            slot !in slots -> "Choose a weapon or armor modification slot."
            owner !in party.map { it.lowercase() } || owner == "ollie" -> "That character cannot equip this mod."
            !GearRules.isModSlotUnlocked(slot, state.completedMilestones) -> "This slot is locked by story progress."
            slot.startsWith("weapon_") && state.equippedWeapons[owner].isNullOrBlank() -> "Equip a weapon first."
            slot.startsWith("armor_") && state.equippedArmors[owner].isNullOrBlank() -> "Equip armor first."
            item != null -> slotCompatibilityError(slot, item)
            else -> null
        }
    }

    fun equip(state: GameSessionState, target: ModLocation, item: Item?, source: ModLocation?): Pair<GameSessionState, ModEquipResult> {
        if (target.slotId.lowercase() !in slots) return state to ModEquipResult.Rejected("Invalid mod slot.")
        if (item == null) return state.copy(equippedItems = state.equippedItems - target.key) to ModEquipResult.Success
        blockedReason(state, target)?.let { return state to ModEquipResult.Rejected(it) }
        if (item.equipment?.slot?.equals("mod", true) != true && !item.type.equals("mod", true))
            return state to ModEquipResult.Rejected("This item is not a mod.")
        slotCompatibilityError(target.slotId, item)?.let { return state to ModEquipResult.Rejected(it) }
        val owned = state.inventory[item.id] ?: 0
        if (owned <= 0) return state to ModEquipResult.Rejected("You don't own this mod.")
        val assigned = locations(state, item.id)
        if (assigned.size > owned) return state to ModEquipResult.Rejected("Equipped copies exceed ownership. Unequip a copy first.")
        if (state.equippedItems[target.key] == item.id) return state to ModEquipResult.Success
        val sameOwner = assigned.filter { it.characterId.equals(target.characterId, true) }
        if (source == null && sameOwner.isNotEmpty()) return state to ModEquipResult.TransferRequired(sameOwner)
        if (source == null && assigned.size >= owned) return state to ModEquipResult.TransferRequired(assigned)
        if (source != null && assigned.none { it.key == source.key })
            return state to ModEquipResult.Rejected("That copy is no longer equipped there.")
        if (source != null && sameOwner.any { it.key != source.key })
            return state to ModEquipResult.Rejected("A character can use only one copy of each mod type.")
        val updated = state.equippedItems.toMutableMap()
        source?.let { updated.remove(it.key) }
        updated[target.key] = item.id
        return state.copy(equippedItems = updated) to ModEquipResult.Success
    }

    /** Same precedence used by combat: weapon element, then the first weapon mod element. */
    fun resolveAttackElement(weaponElement: String?, modElements: List<String?>): String? =
        (listOf(weaponElement) + modElements).firstNotNullOfOrNull { it?.trim()?.lowercase()?.takeIf(String::isNotBlank) }

    fun effectiveElement(state: GameSessionState, owner: String, catalog: (String) -> Item?): String? =
        resolveAttackElement(state.equippedWeapons[owner]?.let(catalog)?.equipment?.attackElement,
            listOf("weapon_mod1", "weapon_mod2").map { slot ->
                (state.equippedItems["$owner:$slot"] ?: state.equippedItems[slot])?.let(catalog)?.equipment?.attackElement
            })

    fun reconcile(state: GameSessionState, original: GameSessionState): GameSessionState {
        val party = state.partyMembers.ifEmpty { listOfNotNull(state.playerId) }.map { it.lowercase() }
        val bindings = state.equippedItems.filterKeys { it.substringAfter(':') in slots }
            .entries.sortedWith(compareBy<Map.Entry<String, String>>(
                { if (original.equippedItems.containsKey(it.key)) 0 else 1 },
                { party.indexOf(it.key.substringBefore(':')).let { n -> if (n < 0) Int.MAX_VALUE else n } },
                { slots.indexOf(it.key.substringAfter(':')) }))
        val updated = state.equippedItems.filterKeys { it.substringAfter(':') !in slots && it.substringAfter(':') != "mod" }.toMutableMap()
        val counts = mutableMapOf<String, Int>()
        val ownerTypes = mutableSetOf<Pair<String, String>>()
        for ((key, id) in bindings) {
            val target = ModLocation(key.substringBefore(':'), key.substringAfter(':'))
            if (blockedReason(state, target) != null || (counts[id] ?: 0) >= (state.inventory[id] ?: 0) ||
                !ownerTypes.add(target.characterId to id)) continue
            updated[key] = id
            counts[id] = (counts[id] ?: 0) + 1
        }
        return state.copy(equippedItems = updated)
    }
}
