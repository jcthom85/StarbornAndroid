package com.example.starborn.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.*
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopPresentationParityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun roomTextRoutesActionsNpcsEnemiesAndPreventsLockedActions() {
        val action = GenericAction("console", "inspect")
        val text = "console Jed sentinel locked"
        val plan = InlineActionPlan(text, listOf(
            InlineActionSegment("action", InlineActionTarget.Room(action), 0, 7, false),
            InlineActionSegment("npc", InlineActionTarget.Npc("Jed"), 8, 11, false),
            InlineActionSegment("enemy", InlineActionTarget.Enemy("sentinel", "sentinel"), 12, 20, false),
            InlineActionSegment("locked", InlineActionTarget.Room(GenericAction("locked", "inspect")), 21, 27, true)))
        var pickedAction: RoomAction? = null
        var pickedNpc: String? = null
        var pickedEnemy: String? = null
        compose.setContent { DesktopStarbornTheme {
            RoomDescription(plan, text, false, Color.White, { pickedAction = it },
                { pickedNpc = it }, { pickedEnemy = it }, Color.Cyan)
        } }
        compose.onNodeWithContentDescription("console", useUnmergedTree = true).performClick()
        assertEquals(action, pickedAction)
        compose.onNodeWithContentDescription("Jed", useUnmergedTree = true).performClick()
        assertEquals("Jed", pickedNpc)
        compose.onNodeWithContentDescription("sentinel", useUnmergedTree = true).performClick()
        assertEquals("sentinel", pickedEnemy)
        compose.onNodeWithContentDescription("locked", useUnmergedTree = true).assertIsNotEnabled().performClick()
        assertEquals(action, pickedAction)
    }

    @Test fun authoredDialogueRevealsTextPlaysMurmursAndRoutesVoiceAndChoices() {
        val directory = Files.createTempDirectory("starborn-dialogue-parity-").toFile()
        val services = DesktopAppServices(directory)
        val revealRequest = mutableIntStateOf(0)
        var revealed = false
        var murmurs = 0
        var voice: String? = null
        var choice: String? = null
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme {
                DesktopAuthoredDialogueOverlay(services,
                    DialogueUi(DialogueLine("fixture", "Jed", "Check the conduit before leaving."), null, "jed_voice"),
                    listOf(DialogueChoiceUi("accept", "I'll check it.")), {}, { choice = it },
                    { voice = it }, onPlayMurmur = { murmurs++ }, onRevealFinished = { revealed = true },
                    revealAllRequest = revealRequest.intValue)
            } }
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
            assertTrue(murmurs > 0)
            compose.runOnIdle { revealRequest.intValue++ }
            compose.mainClock.advanceTimeBy(32)
            compose.waitForIdle()
            assertTrue(revealed)
            compose.onNodeWithText("Voice").performClick()
            assertEquals("jed_voice", voice)
            compose.onNodeWithText("I'll check it.", substring = true).performClick()
            assertEquals("accept", choice)
        } finally { services.close(); directory.deleteRecursively() }
    }
}
