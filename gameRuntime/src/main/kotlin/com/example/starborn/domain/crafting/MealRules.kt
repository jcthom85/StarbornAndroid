package com.example.starborn.domain.crafting

import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.ActiveMealBuff

/** Percentage bonuses use percentage points, matching CombatFormulas. */
object MealRules {
    const val BATTLES = 3

    fun statLabel(stat: String): String = when (stat.lowercase()) {
        "spd", "speed" -> "Speed"
        "foc", "focus" -> "Focus"
        "crit", "crit_rate", "crit_chance" -> "Crit chance"
        "acc", "accuracy" -> "Accuracy"
        "eva", "evasion" -> "Evasion"
        "resist", "status_resist" -> "Status resistance"
        else -> stat.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun bonusLabel(stat: String, value: Int): String {
        val percent = stat.lowercase() in setOf("crit", "crit_rate", "crit_chance", "acc", "accuracy", "eva", "evasion", "resist", "status_resist")
        return "${statLabel(stat)} +$value${if (percent) "%" else ""}"
    }

    fun chefDescription(id: String): String = when (id) {
        "nova" -> "+3 Focus"
        "zeke" -> "+25 Max HP · +3 Stability"
        "gh0st" -> "+2 Speed · +10% Status resistance"
        "orion" -> "+4% Crit chance"
        else -> "No serving bonus"
    }

    fun buff(item: Item, chef: String?): ActiveMealBuff {
        val effects = item.effect?.buffs.orEmpty() + listOfNotNull(item.effect?.singleBuff)
        fun bonus(vararg names: String) = effects.filter { effect -> names.any { effect.stat.equals(it, true) } }.sumOf { it.value }
        return ActiveMealBuff(
            recipeId = item.id, recipeName = item.name, chefId = chef, remainingEncounters = BATTLES,
            hpBonus = bonus("hp", "max_hp") + if (chef == "zeke") 25 else 0,
            speedBonus = bonus("speed", "spd") + if (chef == "gh0st") 2 else 0,
            focusBonus = bonus("focus", "foc") + if (chef == "nova") 3 else 0,
            critBonus = bonus("crit", "crit_chance", "crit_rate").toDouble() + if (chef == "orion") 4.0 else 0.0,
            stabilityBonus = bonus("stability") + if (chef == "zeke") 3 else 0,
            statusResistBonus = bonus("resist", "status_resist") + if (chef == "gh0st") 10 else 0,
            strengthBonus = bonus("strength", "str"), defenseBonus = bonus("defense", "def"),
            agilityBonus = bonus("agility", "agi"), luckBonus = bonus("luck", "lck"),
            accuracyBonus = bonus("accuracy", "acc"), evasionBonus = bonus("evasion", "eva")
        )
    }

    fun summary(buff: ActiveMealBuff): String = listOfNotNull(
        buff.hpBonus.takeIf { it != 0 }?.let { "Max HP +$it" },
        buff.strengthBonus.takeIf { it != 0 }?.let { "Strength +$it" },
        buff.defenseBonus.takeIf { it != 0 }?.let { "Defense +$it" },
        buff.agilityBonus.takeIf { it != 0 }?.let { "Agility +$it" },
        buff.focusBonus.takeIf { it != 0 }?.let { "Focus +$it" },
        buff.speedBonus.takeIf { it != 0 }?.let { "Speed +$it" },
        buff.luckBonus.takeIf { it != 0 }?.let { "Luck +$it" },
        buff.accuracyBonus.takeIf { it != 0 }?.let { "Accuracy +$it%" },
        buff.evasionBonus.takeIf { it != 0 }?.let { "Evasion +$it%" },
        buff.critBonus.takeIf { it != 0.0 }?.let { "Crit +${it.toInt()}%" },
        buff.stabilityBonus.takeIf { it != 0 }?.let { "Stability +$it" },
        buff.statusResistBonus.takeIf { it != 0 }?.let { "Status resistance +$it%" }
    ).joinToString(" · ").ifBlank { "No stat bonus" }
}
