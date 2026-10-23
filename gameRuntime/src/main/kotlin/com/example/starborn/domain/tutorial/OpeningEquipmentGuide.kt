package com.example.starborn.domain.tutorial

import com.example.starborn.domain.session.GameSessionState

/** Only newly granted opening kits opt in; existing saves retain their equipment. */
object OpeningEquipmentGuide {
    const val MANUAL_KIT = "ms_w1_manual_starter_equipment"
    const val OFFER = "opening_gear_offer"
    const val TUTORIAL = "gear_equip_basics"

    fun shouldAutoEquip(state: GameSessionState, owner: String, itemId: String): Boolean =
        !(owner.equals("nova", true) && itemId in setOf("mining_pistol", "nova_flux_liner") &&
            MANUAL_KIT in state.completedMilestones && "ms_w1_mq01_jed_talked" !in state.completedMilestones)

    fun hasKit(state: GameSessionState): Boolean =
        "ms_w1_mq01_jed_talked" in state.completedMilestones &&
            ("mining_pistol" in state.unlockedWeapons || state.inventory.getOrDefault("mining_pistol", 0) > 0) &&
            ("nova_flux_liner" in state.unlockedArmors || state.inventory.getOrDefault("nova_flux_liner", 0) > 0)

    fun isEquipped(state: GameSessionState): Boolean =
        !state.equippedWeapons["nova"].isNullOrBlank() && !state.equippedArmors["nova"].isNullOrBlank()

    fun needsGuidance(state: GameSessionState): Boolean =
        hasKit(state) && TUTORIAL !in state.tutorialCompleted && !isEquipped(state)

    fun instruction(state: GameSessionState): String = instruction(!state.equippedWeapons["nova"].isNullOrBlank())

    fun instruction(hasWeapon: Boolean): String = if (!hasWeapon)
        "Select Weapon under Equipment, then choose Mining Pistol. Next, select Armor and choose Flux Liner."
    else "Weapon equipped. Select Armor under Equipment, then choose Flux Liner to finish getting ready."
}
