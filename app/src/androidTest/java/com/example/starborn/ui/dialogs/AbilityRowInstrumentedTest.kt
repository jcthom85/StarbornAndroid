package com.example.starborn.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.Skill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AbilityRowInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun statusBlockedRowAndRecovery() = verifyStatusRow(1f)
    @Test fun statusBlockedRowAndRecoveryAtDoubleTextSize() = verifyStatusRow(2f)

    private fun verifyStatusRow(fontScale: Float) {
        val blocked = mutableStateOf(true)
        var uses = 0
        var details = 0
        val skill = Skill("nova_arc_tether", "Arc Tether", "nova", "active",
            basePower = 80, cooldown = 2, description = "Shock ability", scaling = "agility")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(270.dp)) {
                        AbilityRow(skill = skill, cooldownRemaining = 0,
                            canUse = !blocked.value,
                            unavailableReason = if (blocked.value) "Abilities blocked by status" else null,
                            highlighted = false, accent = Color.Cyan, border = Color.Cyan,
                            highContrastMode = false,
                            onDetails = { details++ }, onUse = { uses++ })
                    }
                }
            }
        }
        compose.onNodeWithText("Abilities blocked by status").assertIsDisplayed()
        if (fontScale >= 1.5f) {
            val explanationBounds = compose.onNodeWithText("Abilities blocked by status")
                .fetchSemanticsNode().boundsInRoot
            val useBounds = compose.onNodeWithText("Use").fetchSemanticsNode().boundsInRoot
            assertTrue("Large-text controls must sit below the explanation", useBounds.top >= explanationBounds.bottom)
        }
        compose.onNodeWithText("Use").assertIsDisplayed().assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, uses) }
        compose.onNodeWithContentDescription("Details for Arc Tether")
            .assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, details)
            blocked.value = false
        }
        compose.onNodeWithText("Abilities blocked by status").assertDoesNotExist()
        compose.onNodeWithText("Use").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, uses) }
    }
}
