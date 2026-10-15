package com.example.starborn

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.feature.exploration.ui.TutorialBanner
import com.example.starborn.feature.combat.ui.components.CombatTutorialOverlay
import com.example.starborn.feature.combat.presentation.CombatTutorialCopy
import com.example.starborn.feature.combat.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TutorialDismissalInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explorationTutorialBodyAndButtonEachAcknowledgeOnce() {
        var acknowledged = 0
        compose.setContent { MaterialTheme {
            TutorialBanner(TutorialEntry("test", "exploration", "Tutorial body"), { acknowledged++ })
        } }
        compose.onNodeWithText("Tutorial body").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, acknowledged) }
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertEquals(2, acknowledged) }
    }

    @Test fun combatExplanationsAdvanceFromBodyAndButtonOnce() {
        var acknowledged = 0
        var step by mutableStateOf(CombatTutorialStep.BRIEF)
        compose.setContent { MaterialTheme {
            CombatTutorialOverlay(CombatTutorialState(step, "enemy"), null, false, { acknowledged++ }, {})
        } }
        listOf(CombatTutorialStep.BRIEF, CombatTutorialStep.BLOCKED_EXPLANATION, CombatTutorialStep.SUCCESS).forEachIndexed { index, next ->
            compose.runOnIdle { step = next }
            val state = CombatTutorialState(next, "enemy")
            compose.onNodeWithText(CombatTutorialCopy.message(state)).performTouchInput { click() }
            compose.runOnIdle { assertEquals(index * 2 + 1, acknowledged) }
            compose.onNodeWithText(CombatTutorialCopy.continueLabel(state)).performClick()
            compose.runOnIdle { assertEquals(index * 2 + 2, acknowledged) }
        }
    }

    @Test fun actionRequiredCombatHintCannotBeDismissedByTapping() {
        var acknowledged = 0
        val state = CombatTutorialState(CombatTutorialStep.CHOOSE_SKILLS, "enemy")
        compose.setContent { MaterialTheme {
            CombatTutorialOverlay(state, null, false, { acknowledged++ }, {})
        } }
        compose.onNodeWithText(CombatTutorialCopy.message(state)).performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, acknowledged) }
    }
}
