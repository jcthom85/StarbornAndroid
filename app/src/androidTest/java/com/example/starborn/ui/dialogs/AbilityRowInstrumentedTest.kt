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

    @Test fun overchargedAndCooldownCardsShowTacticalChipsAndCountdown() {
        val arc = Skill(
            id = "nova_arc_tether",
            name = "Arc Tether",
            character = "nova",
            type = "active",
            basePower = 80,
            cooldown = 2,
            description = "Lashes one target with a high-voltage tether.",
            combatTags = listOf("shock"),
            scaling = "agility",
            statusApplications = listOf("shock")
        )
        val plasma = Skill(
            id = "nova_plasma_burst",
            name = "Plasma Burst",
            character = "nova",
            type = "active",
            basePower = 90,
            cooldown = 3,
            description = "Detonates superheated plasma across all foes.",
            combatTags = listOf("burn", "aoe"),
            scaling = "agility"
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(340.dp)) {
                    androidx.compose.foundation.layout.Column {
                        AbilityRow(
                            skill = arc,
                            cooldownRemaining = 0,
                            canUse = true,
                            unavailableReason = null,
                            highlighted = false,
                            momentum = 3,
                            accent = Color(0xFF7BE4FF),
                            border = Color(0xFF5CCBE8),
                            highContrastMode = false,
                            onDetails = {},
                            onUse = {},
                            targetScopeLabel = "One enemy"
                        )
                        AbilityRow(
                            skill = plasma,
                            cooldownRemaining = 2,
                            canUse = false,
                            unavailableReason = "On cooldown",
                            highlighted = false,
                            momentum = 0,
                            accent = Color(0xFF7BE4FF),
                            border = Color(0xFF5CCBE8),
                            highContrastMode = false,
                            onDetails = {},
                            onUse = {},
                            targetScopeLabel = "All enemies"
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("⚡ 144% · Crit").assertIsDisplayed()
        compose.onNodeWithText("One enemy").assertIsDisplayed()
        compose.onNodeWithText("Use").assertIsEnabled()
        compose.onNodeWithText("On cooldown").assertIsDisplayed()
        compose.onNodeWithText("2").assertIsDisplayed().assertIsNotEnabled()
        val arcTitleBounds = compose.onNodeWithText("Arc Tether").fetchSemanticsNode().boundsInRoot
        val useBounds = compose.onNodeWithText("Use").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Use button ($useBounds) must sit below the skill title ($arcTitleBounds) for Maestro selectors",
            useBounds.top >= arcTitleBounds.bottom - 2f
        )
    }

    @Test fun novaLinkCardRetainsHealsPartyAndRegenLabel() {
        val link = Skill(
            id = "nova_link",
            name = "Link",
            character = "nova",
            type = "support",
            basePower = 90,
            cooldown = 4,
            description = "Restore party HP and apply Regen through the Bridge ground.",
            combatTags = listOf("source", "heal", "support", "aoe"),
            scaling = "focus",
            statusApplications = listOf("regen"),
            targeting = "all_allies"
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(340.dp)) {
                    AbilityRow(
                        skill = link,
                        cooldownRemaining = 0,
                        canUse = true,
                        unavailableReason = null,
                        highlighted = false,
                        momentum = 0,
                        accent = Color(0xFF7BE4FF),
                        border = Color(0xFF5CCBE8),
                        highContrastMode = false,
                        onDetails = {},
                        onUse = {},
                        targetScopeLabel = "All allies"
                    )
                }
            }
        }
        compose.onNodeWithText("Heals party + Regen", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Heal 90 HP").assertIsDisplayed()
        compose.onNodeWithText("All allies").assertIsDisplayed()
    }
}
