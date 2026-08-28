package com.example.starborn.feature.exploration.ui.menu

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FieldMenuDesignTest {
    @Test
    fun fieldMenuUsesStableSystemPalette() {
        assertEquals(Color(0xFF02070E), FieldMenuDesign.shell)
        assertEquals(Color(0xFF7FE6FF), FieldMenuDesign.cyan)
        assertEquals(Color(0xFFFFC857), FieldMenuDesign.gold)
        assertFalse(MenuDetailKind.entries.isEmpty())
    }
}
