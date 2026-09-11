package com.example.starborn.ui.dialogs

import com.example.starborn.feature.combat.viewmodel.TargetRequirement

/** Describes the selection step, not the recipients of every secondary effect. */
internal fun abilityTargetLabel(requirement: TargetRequirement): String = when (requirement) {
    TargetRequirement.ENEMY -> "Choose one enemy"
    TargetRequirement.ALLY -> "Choose one ally"
    TargetRequirement.ANY -> "Choose any combatant"
    TargetRequirement.NONE -> "Automatic - no selection"
}
