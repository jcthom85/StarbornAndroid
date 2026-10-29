package com.example.starborn.feature.combat.presentation

import com.example.starborn.domain.model.Skill

/** Damage power is a percentage multiplier; healing power is a base HP amount. */
fun abilityPowerLabel(skill: Skill, momentum: Int = 0): String {
    if (skill.basePower <= 0) return "Utility"
    val healing = skill.type.equals("heal", true) || skill.combatTags.orEmpty().any { it.equals("heal", true) }
    val support = skill.type.equals("support", true) || skill.combatTags.orEmpty().any {
        it.equals("support", true) || it.equals("buff", true)
    }
    if (support && !healing) return "Utility"
    val multiplier = when (momentum.coerceIn(0, 3)) { 1 -> 1.25; 2 -> 1.5; 3 -> 1.8; else -> 1.0 }
    val base = skill.basePower
    val boosted = (base * multiplier).toInt()
    return if (healing) {
        if (momentum > 0) "$base HP base healing ($boosted overcharged)" else "$base HP base healing"
    } else {
        if (momentum > 0) "$base% attack scaling ($boosted% overcharged)" else "$base% attack scaling"
    }
}
