package com.example.starborn.feature.exploration.ui.tabs

/** Empty base equipment cannot host mods; summarize instead of showing dead controls. */
internal fun emptyModSlotHint(slot: String, hasModSlots: Boolean, missingEquipment: Boolean): String? =
    if (hasModSlots && missingEquipment) "Equip ${if (slot == "armor") "armor" else "a weapon"} to use mods." else null
