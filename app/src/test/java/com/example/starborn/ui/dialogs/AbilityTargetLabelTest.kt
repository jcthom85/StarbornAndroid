package com.example.starborn.ui.dialogs

import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import org.junit.Assert.assertEquals
import org.junit.Test

class AbilityTargetLabelTest {
    @Test fun labelsDescribeSelectionWithoutGuessingEffectRecipients() {
        assertEquals("Choose one enemy", abilityTargetLabel(TargetRequirement.ENEMY))
        assertEquals("Choose one ally", abilityTargetLabel(TargetRequirement.ALLY))
        assertEquals("Choose any combatant", abilityTargetLabel(TargetRequirement.ANY))
        assertEquals("Automatic - no selection", abilityTargetLabel(TargetRequirement.NONE))
    }
}
