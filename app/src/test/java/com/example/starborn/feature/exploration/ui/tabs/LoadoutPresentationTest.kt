package com.example.starborn.feature.exploration.ui.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadoutPresentationTest {
    @Test fun `missing equipment explains why mods cannot be used`() {
        assertEquals("Equip a weapon to use mods.", emptyModSlotHint("weapon", true, true))
        assertEquals("Equip armor to use mods.", emptyModSlotHint("armor", true, true))
    }

    @Test fun `equipped or non mod slots do not show missing equipment hint`() {
        assertNull(emptyModSlotHint("weapon", true, false))
        assertNull(emptyModSlotHint("armor", true, false))
        assertNull(emptyModSlotHint("accessory", false, true))
        assertNull(emptyModSlotHint("snack", false, false))
    }
}
