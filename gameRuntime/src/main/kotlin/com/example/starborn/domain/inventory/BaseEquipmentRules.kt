package com.example.starborn.domain.inventory

import com.example.starborn.domain.session.GameSessionState

object BaseEquipmentRules {
    fun assignments(state: GameSessionState, slot: String): Map<String, String> =
        if (slot == "weapon") state.equippedWeapons else state.equippedArmors

    fun canEquip(owned: Int, equipped: Map<String, String>, owner: String, item: String): Boolean =
        owned > equipped.count { (character, id) -> character != owner && id == item }

    fun restore(state: GameSessionState): GameSessionState {
        val inventory = state.inventory.toMutableMap()
        if (!state.physicalGearInventory) {
            // The old Android inventory converted an acquired item into a single permanent unlock.
            val legacyGear = state.unlockedWeapons + state.unlockedArmors +
                state.equippedWeapons.values + state.equippedArmors.values
            legacyGear.forEach { item -> inventory[item] = inventory.getOrDefault(item, 0).coerceAtLeast(1) }
        }
        return withInventory(state, inventory).copy(
            physicalGearInventory = true,
            battleCheckpoint = state.battleCheckpoint?.let(::restore)
        )
    }

    fun withInventory(state: GameSessionState, inventory: Map<String, Int>): GameSessionState {
        fun retainOwned(equipped: Map<String, String>): Map<String, String> {
            val order = state.partyMembers + equipped.keys.sorted()
            val kept = mutableMapOf<String, String>()
            for (owner in order.distinct()) {
                val item = equipped[owner] ?: continue
                if (canEquip(inventory[item] ?: 0, kept, owner, item)) kept[owner] = item
            }
            return kept
        }
        return state.copy(
            inventory = inventory,
            equippedWeapons = retainOwned(state.equippedWeapons),
            equippedArmors = retainOwned(state.equippedArmors)
        )
    }
}
