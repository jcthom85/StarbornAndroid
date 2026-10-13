package com.example.starborn.domain.fishing

fun FishingRod.handlingSummary(): String {
    val speed = (((fishingPower - 1.0) / 2.05).coerceIn(0.0, 1.0) * 18).toInt()
    val resistance = (((stability - 1.0) / 1.8).coerceIn(0.0, 1.0) * 30).toInt()
    return if (speed == 0 && resistance == 0) "Starter handling - release before surges"
    else "$speed% faster reeling - $resistance% more tension resistance; still release before surges"
}

fun FishingLure.catchSummary(zone: FishingZone?, displayName: (String) -> String): String = buildString {
    append("Favors: ${attracts.joinToString { displayName(it) }.ifBlank { "General fish" }}")
    if ((zoneBonuses[zone?.id] ?: 0) > 0 && zone?.catches?.any { it.itemId in attracts } == true) append(" - Local match")
    if (rarityBonus > 0) append(" - +${(rarityBonus * 100).toInt()}% rare catch weight")
}
