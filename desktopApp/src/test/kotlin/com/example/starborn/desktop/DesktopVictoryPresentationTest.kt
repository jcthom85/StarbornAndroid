package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.ui.*
import com.example.starborn.data.local.UserSettings
import com.example.starborn.domain.leveling.*
import com.example.starborn.navigation.CombatResultPayload
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class DesktopVictoryPresentationTest {
    @get:Rule val compose = createComposeRule()

    private fun settled(label: String) {
        compose.waitUntil(5_000) {
            compose.mainClock.advanceTimeBy(100)
            val nodes = compose.onAllNodesWithText(label).fetchSemanticsNodes()
            nodes.isNotEmpty() && !nodes.first().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
    }
    private fun capture(name: String) {
        val folder = File("build/reports/desktop/screenshots/victory-headings").apply { mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(folder, "$name.png"))
    }
    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-victory-ui").toFile()
        val services = DesktopAppServices(directory)
        try { block(services) } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun spoilsThenLevelUpsAndOnlyOneCompletion() = withServices { services ->
        val levels = listOf(LevelUpSummary("nova", "Nova", 1, 2, listOf(SkillUnlockSummary("spark", "Spark"))),
            LevelUpSummary("zeke", "Zeke", 1, 3, emptyList()))
        val payload = CombatResultPayload(CombatResultPayload.Outcome.VICTORY, rewardXp = 120,
            rewardAp = 3, rewardCredits = 40, rewardItems = mapOf("medkit" to 2), levelUps = levels)
        val before = services.inventoryService.snapshot()
        var completions = 0
        compose.setContent { DesktopStarbornTheme { MaterialTheme(typography = desktopStarbornTypography(services)) {
            DesktopVictoryDialog(services, payload, { "Medkit" }, emptyMap(), false, { completions++ })
        } } }
        settled("Next")
        compose.onNodeWithText("Spoils Recovered").assertExists()
        compose.onNodeWithText("+120 XP").assertExists()
        compose.onNodeWithText("Medkit").assertExists()
        compose.onNodeWithText("Nova").assertDoesNotExist()
        capture("spoils")
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Level Up!").assertExists()
        compose.onNodeWithText("LEVEL 2").assertExists()
        compose.onNodeWithText("Spark").assertExists()
        assertEquals(0, completions)
        settled("Continue")
        capture("level-ups")
        compose.onNodeWithText("Continue").performClick()
        compose.onRoot().performKeyInput { pressKey(Key.Enter); pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, completions); assertEquals(before, services.inventoryService.snapshot()) }
    }

    @Test fun emptySpoilsUsesAndroidTextAndSkipsLevelUps() = withServices { services ->
        var completions = 0
        compose.setContent { DesktopStarbornTheme {
            DesktopVictoryDialog(services, CombatResultPayload(CombatResultPayload.Outcome.VICTORY,
                rewardXp = -1, rewardItems = mapOf("not-loot" to 0)), { it }, emptyMap(), true, { completions++ }, largeTouchTargets = true)
        } }
        settled("Continue")
        compose.onNodeWithText("No spoils collected.").assertExists()
        compose.onNodeWithText("Next").assertDoesNotExist()
        compose.onRoot().performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, completions)
        assertTrue(compose.onNodeWithText("Continue").fetchSemanticsNode().boundsInRoot.height >= 56f)
        capture("empty-high-contrast")
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun shortWindowScrollsLootAndKeepsContinueVisible() = withServices { services ->
        val payload = CombatResultPayload(CombatResultPayload.Outcome.VICTORY, rewardXp = 30,
            rewardAp = 2, rewardCredits = 6, rewardItems = (1..20).associate { "item-%02d".format(it) to 1 })
        compose.setContent { DesktopStarbornTheme { Box(Modifier.size(500.dp, 320.dp)) {
            DesktopVictoryDialog(services, payload, { it }, emptyMap(), false, {})
        } } }
        settled("Continue")
        compose.onNodeWithText("Continue").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("item-20"))
        compose.onNodeWithText("item-20").assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsDisplayed()
        capture("short-window-loot")
    }

    @Test fun roomHeadingWrapsAndUsesBolderGameTypography() = withServices { services ->
        val compact = mutableStateOf(false)
        val title = "The Very Long Authored Room Title"
        compose.setContent { DesktopStarbornTheme { MaterialTheme(typography = desktopStarbornTypography(services)) {
            Column(Modifier.width(320.dp).padding(16.dp)) {
                DesktopRoomHeading(title, Color(0xFFFF922B), compact.value)
                DesktopCombatReadyPrompt(Color(0xFFFF922B), true,
                    UserSettings(disableScreenshake = true, disableFlashes = true))
            }
        } } }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("exploration-room-title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(36.sp, results.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.ExtraBold, results.single().layoutInput.style.fontWeight)
        assertTrue(results.single().lineCount > 1)
        assertFalse(results.single().hasVisualOverflow)
        compose.onNodeWithText("Select a ready character").assertIsDisplayed()
        capture("room-heading-and-ready")
        compose.runOnIdle { compact.value = true }
        results.clear()
        compose.onNodeWithTag("exploration-room-title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(30.sp, results.single().layoutInput.style.fontSize)
    }
    @Test fun roomHeadingStaysFixedWhenDescriptionScrolls() = withServices { services ->
        services.startNewGame()
        val room = services.roomDefinitions.getValue("pit_nova_bunk")
        val ui = com.example.starborn.feature.exploration.viewmodel.ExplorationUiState(currentRoom = room)
        compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
            Box(Modifier.size(1024.dp, 640.dp)) {
                DesktopExplorationPanels(services, ui,
                    portraitBackdropLayout(androidx.compose.ui.geometry.Size(1024f, 640f), androidx.compose.ui.geometry.Size(1088f, 1920f)),
                    "An authored room description. ".repeat(100), null, false, false) {}
            }
        } } }
        val bounds = compose.onNodeWithTag("exploration-room-title").fetchSemanticsNode().boundsInRoot
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasTestTag("narrative-body")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10_000f) }
        compose.onNodeWithTag("exploration-room-title").assertIsDisplayed()
        assertEquals(bounds, compose.onNodeWithTag("exploration-room-title").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun darkRoomKeepsItsAuthoredTitleConcealed() = withServices { services ->
        services.startNewGame()
        val room = services.roomDefinitions.getValue("pit_nova_bunk")
        val ui = mutableStateOf(com.example.starborn.feature.exploration.viewmodel.ExplorationUiState(currentRoom = room.copy(revealTitleWhenDark = false)))
        compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
            Box(Modifier.size(1024.dp, 640.dp)) {
                DesktopExplorationPanels(services, ui.value,
                    portraitBackdropLayout(androidx.compose.ui.geometry.Size(1024f, 640f), androidx.compose.ui.geometry.Size(1088f, 1920f)),
                    "A dark room.", null, true, false) {}
            }
        } } }
        compose.onNodeWithTag("exploration-room-title").assertTextEquals("Dark room")
        compose.onNodeWithText(room.title).assertDoesNotExist()
        compose.runOnIdle { ui.value = ui.value.copy(currentRoom = room.copy(revealTitleWhenDark = true)) }
        compose.onNodeWithTag("exploration-room-title").assertTextEquals(room.title)
    }

}
