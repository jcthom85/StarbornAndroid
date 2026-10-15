package com.example.starborn.desktop

import com.example.starborn.desktop.ui.combatActionState
import org.junit.Assert.*
import org.junit.Test

class DesktopCombatActionStateTest {
    @Test fun choosingAndExecutingAreDifferentStates() {
        assertEquals("READY", combatActionState(true, false, false, true))
        assertEquals("CHOOSING", combatActionState(true, false, true, true))
        assertEquals("ACTING", combatActionState(true, true, true, true))
        assertEquals("DOWN", combatActionState(false, true, true, true))
        assertNull(combatActionState(true, false, false, false))
    }
}
