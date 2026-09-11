package com.example.starborn.ui.dialogs

import org.junit.Assert.assertEquals
import org.junit.Test

class AbilityCooldownLabelTest {
    @Test fun activeCooldownIsNotReducedByCurrentMomentum() {
        assertEquals("Ready in 1 turn", abilityCooldownLabel(4, 1, 3))
        assertEquals("Ready in 2 turns", abilityCooldownLabel(4, 2, 0))
    }

    @Test fun availableAbilityDescribesCooldownAfterUse() {
        assertEquals("4 turns after use", abilityCooldownLabel(4, 0, 1))
        assertEquals("1 turn after use", abilityCooldownLabel(1, 0, 0))
        assertEquals("No cooldown", abilityCooldownLabel(0, 0, 3))
    }

    @Test fun overchargePreviewMatchesOneTurnRefund() {
        assertEquals("3 turns after use (Overcharge)", abilityCooldownLabel(4, 0, 2))
        assertEquals("3 turns after use (Overcharge)", abilityCooldownLabel(4, 0, 3))
        assertEquals("No cooldown (Overcharge)", abilityCooldownLabel(1, 0, 2))
    }
}
