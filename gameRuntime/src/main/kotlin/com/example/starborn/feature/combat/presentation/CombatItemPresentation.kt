package com.example.starborn.feature.combat.presentation

import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import java.util.Locale

/** Android's battle item eligibility and target picker policy. */
object CombatItemPresentation {
    fun isUsable(entry: InventoryEntry): Boolean {
        if (entry.quantity <= 0) return false
        val item = entry.item
        if (item.type.equals("snack", true) || item.equipment?.slot.equals("snack", true)) return false
        val effect = item.effect ?: return false
        return (effect.restoreHp ?: 0) > 0 || (effect.damage ?: 0) > 0 ||
            effect.singleBuff != null || !effect.buffs.isNullOrEmpty()
    }

    fun targetRequirement(entry: InventoryEntry): TargetRequirement {
        val effect = entry.item.effect ?: return TargetRequirement.NONE
        return when (effect.target?.lowercase(Locale.ROOT)) {
            "enemy", "single_enemy" -> TargetRequirement.ENEMY
            "ally", "single_ally" -> TargetRequirement.ALLY
            "any" -> TargetRequirement.ANY
            "self", "party", "all_allies", "enemy_group", "all_enemies" -> TargetRequirement.NONE
            else -> when {
                (effect.damage ?: 0) > 0 -> TargetRequirement.ENEMY
                (effect.restoreHp ?: 0) > 0 || effect.singleBuff != null || !effect.buffs.isNullOrEmpty() -> TargetRequirement.ALLY
                else -> TargetRequirement.NONE
            }
        }
    }
}
