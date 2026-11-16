package com.example.starborn.feature.combat.presentation

import com.example.starborn.domain.model.Skill

fun isHealingSkill(skill: Skill): Boolean =
    skill.type.equals("heal", true) || skill.combatTags.orEmpty().any { it.equals("heal", true) }

fun isPureSupportSkill(skill: Skill): Boolean {
    val healing = isHealingSkill(skill)
    val support = skill.type.equals("support", true) || skill.combatTags.orEmpty().any {
        it.equals("support", true) || it.equals("buff", true) || it.equals("defend", true)
    }
    return support && !healing
}

private fun overchargedPower(skill: Skill, momentum: Int): Int {
    val multiplier = when (momentum.coerceIn(0, 3)) {
        1 -> 1.25
        2 -> 1.5
        3 -> 1.8
        else -> 1.0
    }
    return (skill.basePower * multiplier).toInt()
}

/** Damage power is a percentage multiplier; healing power is a base HP amount. */
fun abilityPowerLabel(skill: Skill, momentum: Int = 0): String {
    if (skill.basePower <= 0 || isPureSupportSkill(skill)) return "Utility"
    val base = skill.basePower
    val boosted = overchargedPower(skill, momentum)
    return if (isHealingSkill(skill)) {
        if (momentum > 0) "$base HP base healing ($boosted overcharged)" else "$base HP base healing"
    } else {
        if (momentum > 0) "$base% attack scaling ($boosted% overcharged)" else "$base% attack scaling"
    }
}

fun abilityCompactPowerLabel(skill: Skill, momentum: Int = 0): String? {
    if (skill.basePower <= 0 || isPureSupportSkill(skill)) return null
    val boosted = overchargedPower(skill, momentum)
    return when {
        isHealingSkill(skill) && momentum > 0 -> "⚡ Heal $boosted HP"
        isHealingSkill(skill) -> "Heal $boosted HP"
        momentum >= 3 -> "⚡ $boosted% · Crit"
        momentum > 0 -> "⚡ $boosted% Power"
        else -> "$boosted% Power"
    }
}
