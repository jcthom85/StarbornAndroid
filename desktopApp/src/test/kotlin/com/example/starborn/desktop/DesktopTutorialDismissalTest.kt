package com.example.starborn.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.feature.combat.presentation.CombatTutorialCopy
import com.example.starborn.feature.combat.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopTutorialDismissalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explorationTutorialBodyAndButtonEachAcknowledgeExactlyOnce() {
        val directory = Files.createTempDirectory("tutorial-dismissal-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            var acknowledged = 0
            val prompt = TutorialPrompt(TutorialEntry("test", "exploration", "Tutorial body", mapOf("action_label" to "Continue")))
            compose.setContent { DesktopStarbornTheme(services) {
                DesktopExplorationPromptBanner(services, prompt, { acknowledged++ })
            } }
            compose.onNodeWithText("Tutorial body").performTouchInput { click() }
            compose.runOnIdle { assertEquals(1, acknowledged) }
            compose.onNodeWithText("Continue").performClick()
            compose.runOnIdle { assertEquals(2, acknowledged) }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun eachCombatExplanationAdvancesFromBodyOrButtonOnce() {
        var acknowledged = 0
        var step by mutableStateOf(CombatTutorialStep.BRIEF)
        compose.setContent { MaterialTheme {
            DesktopCombatTutorialOverlay(CombatTutorialState(step, "enemy"), Color.Cyan, false, { acknowledged++ }, {})
        } }
        listOf(CombatTutorialStep.BRIEF, CombatTutorialStep.BLOCKED_EXPLANATION, CombatTutorialStep.SUCCESS).forEachIndexed { index, next ->
            compose.runOnIdle { step = next }
            val state = CombatTutorialState(next, "enemy")
            compose.onNodeWithText(CombatTutorialCopy.message(state).adaptInputVocabularyForDesktop(DesktopKeyBindings())).performTouchInput { click() }
            compose.runOnIdle { assertEquals(index * 2 + 1, acknowledged) }
            compose.onNodeWithText(CombatTutorialCopy.continueLabel(state)).performClick()
            compose.runOnIdle { assertEquals(index * 2 + 2, acknowledged) }
        }
    }

    @Test fun actionRequiredCombatHintCannotBeDismissedByTapping() {
        var acknowledged = 0
        val state = CombatTutorialState(CombatTutorialStep.CHOOSE_SKILLS, "enemy")
        compose.setContent { MaterialTheme {
            DesktopCombatTutorialOverlay(state, Color.Cyan, false, { acknowledged++ }, {})
        } }
        compose.onNodeWithText(CombatTutorialCopy.message(state).adaptInputVocabularyForDesktop(DesktopKeyBindings())).performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, acknowledged) }
    }
}
