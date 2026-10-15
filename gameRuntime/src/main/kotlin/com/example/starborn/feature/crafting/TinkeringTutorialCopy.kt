package com.example.starborn.feature.crafting

/** Input-neutral instructions shared by the menu and station tutorial presentations. */
fun tinkeringTutorialInstruction(step: TinkeringTutorialStep, buildLabel: String = "Synthesize"): String = when (step) {
    TinkeringTutorialStep.SLOT_BASE -> "Select the Cryo-Inductor as the base item. It becomes the item you will repair."
    TinkeringTutorialStep.SLOT_COMPONENT -> "Select Scrap Metal to fill Component 1."
    TinkeringTutorialStep.SYNTHESIZE -> "Select $buildLabel to repair the Cryo-Inductor. This restores the cold loop and unlocks Cryo Vent."
    TinkeringTutorialStep.COMPLETE -> "Repair complete. Cryo Vent is now available in combat."
}
