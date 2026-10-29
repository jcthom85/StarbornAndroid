package com.example.starborn.ui.dialogs

import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.presentation.abilityPowerLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class AbilityPowerLabelTest {
    private fun skill(type: String, power: Int) = Skill(id = "test", name = "Test", character = "nova",
        type = type, basePower = power, cooldown = 2, description = "Test")
    @Test fun damagePowerDescribesTheMultiplierRatherThanPromisingFlatDamage() {
        assertEquals("80% attack scaling", abilityPowerLabel(skill("active", 80)))
        assertEquals("80% attack scaling (100% overcharged)", abilityPowerLabel(skill("active", 80), 1))
    }
    @Test fun healingAndUtilityUseTheirActualUnits() {
        assertEquals("55 HP base healing", abilityPowerLabel(skill("heal", 55)))
        assertEquals("Utility", abilityPowerLabel(skill("support", 55)))
        assertEquals("Utility", abilityPowerLabel(skill("active", 0)))
    }
}
