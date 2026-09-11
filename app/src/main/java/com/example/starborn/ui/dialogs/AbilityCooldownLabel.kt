package com.example.starborn.ui.dialogs

internal fun abilityCooldownLabel(baseCooldown: Int, remaining: Int, momentum: Int): String {
    fun turns(count: Int) = "$count turn${if (count == 1) "" else "s"}"
    if (remaining > 0) return "Ready in ${turns(remaining)}"
    val effective = (baseCooldown - if (momentum >= 2) 1 else 0).coerceAtLeast(0)
    val label = if (effective == 0) "No cooldown" else "${turns(effective)} after use"
    return if (baseCooldown > 0 && momentum >= 2) "$label (Overcharge)" else label
}
