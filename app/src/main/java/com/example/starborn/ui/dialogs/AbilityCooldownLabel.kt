package com.example.starborn.ui.dialogs

internal fun abilityCooldownLabel(baseCooldown: Int, remaining: Int, momentum: Int): String {
    return com.example.starborn.domain.combat.CombatClarity.skillCooldownLabel(baseCooldown, remaining, momentum)
}
