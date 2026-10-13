package com.example.starborn.domain.combat

import com.example.starborn.domain.crafting.MealRules
import com.example.starborn.domain.model.Item

/** Player-facing wording shared by touch and desktop combat. */
object CombatClarity {
    const val SKILL_GUIDANCE = "Using an ability spends one action. Cooldowns count this character's turns. Abilities use their own crit and Overcharge rules."
    fun turns(count: Int): String = "${count.coerceAtLeast(0)} ${if (count == 1) "turn" else "turns"} for this character"

    fun skillCooldownLabel(baseCooldown: Int, remaining: Int, momentum: Int): String {
        fun countLabel(count: Int) = "$count turn${if (count == 1) "" else "s"}"
        if (remaining > 0) return "Ready in ${countLabel(remaining)}"
        val effective = (baseCooldown - if (momentum >= 2) 1 else 0).coerceAtLeast(0)
        val label = if (effective == 0) "No cooldown" else "${countLabel(effective)} after use"
        return if (baseCooldown > 0 && momentum >= 2) "$label (Overcharge)" else label
    }

    fun buffLabel(buff: ActiveBuff): String = "${bonus(buff.effect.stat, buff.effect.value)} · ${turns(buff.remainingTurns)} left"

    fun compactBuffLabel(buff: ActiveBuff): String =
        "${compactBuffAmount(buff)} (${buff.remainingTurns.coerceAtLeast(0)}t)"

    fun compactBuffAmount(buff: ActiveBuff): String = bonus(buff.effect.stat, buff.effect.value).replace("Weapon crit chance", "Crit")

    private fun bonus(stat: String, value: Int): String = if (stat.lowercase() in setOf("crit_rate", "crit", "crit_chance"))
        "Weapon crit chance ${if (value >= 0) "+" else ""}$value%" else MealRules.bonusLabel(stat, value)

    fun snackPreview(item: Item?, remainingCooldown: Int): String {
        if (item == null) return "Equip a snack in the party menu to use its ability."
        val effect = item.effect ?: return "This snack has no combat ability."
        val effects = buildList {
            effect.restoreHp?.takeIf { it > 0 }?.let { add("Restores $it HP") }
            effect.damage?.takeIf { it > 0 }?.let { add("Deals $it damage") }
            (effect.buffs.orEmpty() + listOfNotNull(effect.singleBuff)).forEach {
                add("${bonus(it.stat, it.value)} (${turns((it.duration ?: 2).coerceAtLeast(1))})")
            }
        }
        return listOfNotNull(
            effects.joinToString("; ").ifBlank { "No active effect" } + ".",
            "Target: ${if (effect.target?.lowercase() in setOf("party", "all_allies")) "all living allies" else "self"}. Spends one action; the snack is not consumed.",
            if (remainingCooldown > 0) "Ready in ${turns(remainingCooldown)}." else "Ready. Cooldown: ${turns(effect.cooldown ?: 5)}.",
            when (item.id) {
                "comet_gummies" -> "Best before basic weapon attacks; skills use their own crit rules."
                "pulse_citrus" -> "Best early in a longer fight, when faster turns can repay the setup action."
                "mineral_trail_mix" -> "Use before heavy incoming damage. Equipped HP applies without activation."
                else -> null
            }
        ).joinToString("\n")
    }
}
