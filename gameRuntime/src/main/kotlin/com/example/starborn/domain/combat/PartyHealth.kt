package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.session.GameSessionState

/** Shared resting/combat HP ceiling, including equipment and passive vitality. */
object PartyHealth {
    fun maxHp(player: Player, state: GameSessionState, item: (String) -> Item?,
              nodes: Map<String, SkillTreeNode>): Int {
        val owner = player.id.trim().lowercase()
        var vitality = player.vitality
        var bonus = state.activeMealBuff?.hpBonus ?: 0
        state.unlockedSkills.filter { it.startsWith("${player.id}_") }.forEach { id ->
            val effect = nodes[id]?.effect
            if (effect?.type == "buff" && effect.buffType?.trim()?.lowercase() in setOf("vit", "vitality")) {
                vitality += effect?.value ?: 0
            }
        }
        val weapon = state.equippedWeapons[owner]?.trim()?.takeIf { it.isNotBlank() }
        val armor = state.equippedArmors[owner]?.trim()?.takeIf { it.isNotBlank() }
        val slots = listOf("weapon", "armor", "accessory", "snack", "weapon_mod1", "weapon_mod2", "armor_mod1", "armor_mod2")
        slots.forEach { slot ->
            if (slot.startsWith("weapon_") && weapon == null) return@forEach
            if (slot.startsWith("armor_") && armor == null) return@forEach
            val id = when (slot) {
                "weapon" -> weapon
                "armor" -> armor
                else -> state.equippedItems.entries.firstOrNull { it.key.equals("$owner:$slot", true) && it.value.isNotBlank() }?.value
                    ?: state.equippedItems.entries.firstOrNull { it.key.equals(slot, true) && it.value.isNotBlank() }?.value
            } ?: return@forEach
            val equipment = item(id)?.equipment ?: return@forEach
            bonus += equipment.hpBonus ?: 0
            equipment.statMods.orEmpty().forEach { (key, value) ->
                if (key.trim().lowercase() in setOf("vit", "vitality")) vitality += value
            }
        }
        return CombatFormulas.maxHp(player.hp, vitality.coerceAtLeast(0)) + bonus
    }
}
