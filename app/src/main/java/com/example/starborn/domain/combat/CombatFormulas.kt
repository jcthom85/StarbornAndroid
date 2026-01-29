package com.example.starborn.domain.combat

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object CombatFormulas {

    const val BASE_ACCURACY = 95.0
    const val BASE_EVASION = 5.0
    const val BASE_CRIT = 5.0

    const val ATK_STR_MULT = 2.0
    const val DEF_VIT_MULT = 1.5
    const val HP_PER_VIT = 10
    const val SPD_AGI_MULT = 1.2

    const val EVA_PER_AGI = 0.15
    const val ACC_PER_FOCUS = 0.20
    const val CRIT_PER_FOCUS = 0.15
    const val CRIT_PER_LUCK = 0.10
    const val RES_PER_FOCUS = 0.10
    const val SKILL_FOCUS_MULT = 0.01

    const val CRIT_DAMAGE_MULT = 2.0

    fun maxHp(baseHp: Int, vitality: Int): Int = baseHp + vitality * HP_PER_VIT

    fun attackPower(strength: Int): Int = floor(strength * ATK_STR_MULT).toInt()

    fun defensePower(vitality: Int): Int = floor(vitality * DEF_VIT_MULT).toInt()

    fun speed(baseSpeed: Int, agility: Int): Double = baseSpeed + agility * SPD_AGI_MULT

    fun accuracy(focus: Int): Double = BASE_ACCURACY + focus * ACC_PER_FOCUS

    fun evasion(agility: Int): Double = BASE_EVASION + agility * EVA_PER_AGI

    fun critChance(focus: Int, luck: Int = 0): Double =
        BASE_CRIT + focus * CRIT_PER_FOCUS + luck * CRIT_PER_LUCK

    fun generalResistance(focus: Int): Int = max(0, (focus * RES_PER_FOCUS).toInt())

    fun skillPotencyMultiplier(focus: Int): Double = 1.0 + focus * SKILL_FOCUS_MULT

    fun stabilityForTier(maxHp: Int, tier: String?): Int {
        val multiplier = when (tier?.trim()?.lowercase()) {
            "boss" -> 0.95
            "elite" -> 0.85
            "mini" -> 0.75
            else -> 0.6
        }
        return (maxHp * multiplier).roundToInt().coerceAtLeast(1)
    }
}
