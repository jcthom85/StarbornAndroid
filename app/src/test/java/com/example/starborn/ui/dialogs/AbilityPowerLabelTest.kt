package com.example.starborn.ui.dialogs

import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.presentation.abilityCompactPowerLabel
import com.example.starborn.feature.combat.presentation.abilityPowerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    @Test fun compactPowerLabelFormatsNormalAndOverchargedTiers() {
        assertEquals("80% Power", abilityCompactPowerLabel(skill("active", 80), 0))
        assertEquals("⚡ 100% Power", abilityCompactPowerLabel(skill("active", 80), 1))
        assertEquals("⚡ 144% · Crit", abilityCompactPowerLabel(skill("active", 80), 3))
        assertEquals("Heal 55 HP", abilityCompactPowerLabel(skill("heal", 55), 0))
        assertEquals("⚡ Heal 99 HP", abilityCompactPowerLabel(skill("heal", 55), 3))
        assertNull(abilityCompactPowerLabel(skill("support", 55), 3))
        assertNull(abilityCompactPowerLabel(skill("active", 0), 3))
    }
}
