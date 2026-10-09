package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.*
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopExplorationPanelsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun panelBreakpointsRespectImageGeometryAndDensity() {
        val image = Size(1088f, 1920f)
        assertTrue(hasThreePanelSpace(portraitBackdropLayout(Size(1920f, 1080f), image), 1f))
        assertTrue(hasThreePanelSpace(portraitBackdropLayout(Size(1280f, 800f), image), 1f))
        assertTrue(hasThreePanelSpace(portraitBackdropLayout(Size(2560f, 1080f), image), 1f))
        assertFalse(hasThreePanelSpace(portraitBackdropLayout(Size(480f, 768f), image), 1f))
        assertFalse(hasThreePanelSpace(portraitBackdropLayout(Size(1280f, 800f), image), 2f))
    }

    @Test fun fallbackTargetsKeepUnmentionedInteractionsAndDeduplicate() {
        val represented = GenericAction("console", "inspect")
        val remaining = GenericAction("locker", "inspect")
        val service = ShopAction("Shop", "fixture-shop")
        val ui = ExplorationUiState(actions = listOf(represented, represented, remaining, service),
            npcs = listOf("Jed", "Mara", "Mara"), enemies = listOf("sentinel", "rat", "rat"))
        val plan = InlineActionPlan("console Jed sentinel", listOf(
            InlineActionSegment("console", InlineActionTarget.Room(represented), 0, 7, false),
            InlineActionSegment("Jed", InlineActionTarget.Npc("Jed"), 8, 11, false),
            InlineActionSegment("sentinel", InlineActionTarget.Enemy("sentinel", "sentinel"), 12, 20, false)))
        assertEquals(listOf(InlineActionTarget.Room(remaining), InlineActionTarget.Room(service),
            InlineActionTarget.Npc("Mara"), InlineActionTarget.Enemy("rat", "rat")), unmatchedRoomTargets(ui, plan, false))
        assertEquals(listOf(InlineActionTarget.Room(remaining), InlineActionTarget.Room(service)), unmatchedRoomTargets(ui, plan, true))
        assertTrue(unmatchedRoomTargets(ExplorationUiState(), null, false).isEmpty())
    }

    @Test fun fourRatiosShowNarrativeAndStatusWithoutCoveringWideArtwork() {
        val directory = Files.createTempDirectory("starborn-three-panel-").toFile()
        val services = DesktopAppServices(directory)
        val dimensions = mutableStateOf(Size(1024f, 576f))
        var selectedTab: DesktopMenuTab? = null
        try {
            assertTrue(services.startNewGame())
            val room = services.worldDataSource.loadRooms().first { it.id == "pit_nova_bunk" }
            val action = GenericAction("locker", "inspect")
            val fixture = ExplorationUiState(currentRoom = room, actions = listOf(action), groundItems = mapOf("medkit" to 2),
                availableConnections = mapOf("north" to "fixture-north", "west" to "fixture-west", "east" to null), blockedDirections = setOf("north"),
                minimap = MinimapUiState(listOf(
                    MinimapCellUi("here", 0, 0, 0, 0, true, true, true, false, setOf("north"), mapOf("north" to "next")),
                    MinimapCellUi("next", 0, 1, 0, 1, false, true, false, false, emptySet(), mapOf("south" to "here")))),
                questLogActive = listOf(QuestSummaryUi("fixture", "Restore the conduit", "Restore power.", null, null, emptyList(), false, 0, 1, "Inspect the door control panel.")),
                trackedQuestId = "fixture",
                partyStatus = PartyStatusUi(listOf("nova", "zeke", "orion", "gh0st").map { id ->
                    PartyMemberStatusUi(id, id.replaceFirstChar { it.uppercase() }, 1, 0f, "0 XP", "100 / 100 HP", 1f, "images/characters/${id}_portrait.png", emptyList())
                }))
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(dimensions.value.width.dp, dimensions.value.height.dp).testTag("stage")) {
                    DesktopPortraitBackdrop(rememberDesktopAssetPainter(room.backgroundImage, services.assetProvider), room.title) { layout ->
                        DesktopExplorationPanels(services, fixture, layout, room.description, null, false, false) { selectedTab = it }
                    }
                }
            } }
            compose.waitForIdle()
            val screenshots = File("build/reports/desktop/screenshots/three-panel").apply { mkdirs() }
            listOf("16x9" to Size(1024f, 576f), "16x10" to Size(1024f, 640f), "ultrawide" to Size(1008f, 432f), "narrow" to Size(480f, 768f)).forEach { (label, size) ->
                compose.runOnIdle { dimensions.value = size }
                if (label == "narrow") {
                    compose.onNodeWithText("Room", useUnmergedTree = true).performClick()
                }
                compose.onNodeWithTag("exploration-narrative").assertExists()
                compose.onNodeWithTag("travel-east").assertDoesNotExist()
                compose.onNodeWithContentDescription("Travel north, locked").assertIsNotEnabled()
                if (label != "narrow") {
                    val narrative = compose.onNodeWithTag("exploration-narrative").fetchSemanticsNode().boundsInRoot
                    val status = compose.onNodeWithTag("exploration-status").fetchSemanticsNode().boundsInRoot
                    val geometry = portraitBackdropLayout(size, Size(1088f, 1920f))
                    assertTrue(narrative.right <= geometry.center.left)
                    assertTrue(status.left >= geometry.center.right)
                    assertTrue("Narrative must not stretch into a full-height shell", narrative.height <= (size.height - 40f) * .6f + 1f)
                    val minimap = compose.onNodeWithTag("hud-minimap").fetchSemanticsNode().boundsInRoot
                    val objective = compose.onNodeWithTag("hud-objective").fetchSemanticsNode().boundsInRoot
                    assertTrue("Minimap and objective need visible background between them", objective.top - minimap.bottom >= 19f)
                }
                ImageIO.write(compose.onNodeWithTag("stage").captureToImage().asAwtImage(), "png", File(screenshots, "$label.png"))
            }
            compose.onNodeWithText("Room", useUnmergedTree = true).performClick()
            compose.onNodeWithTag("exploration-narrative").assertDoesNotExist()
            compose.onNodeWithText("Status", useUnmergedTree = true).performClick()
            compose.onNodeWithContentDescription("Area minimap. Open full map").performClick()
            assertEquals(DesktopMenuTab.MAP, selectedTab)
            compose.onNodeWithContentDescription("Open tracked objective in Journal").performScrollTo().performClick()
            assertEquals(DesktopMenuTab.JOURNAL, selectedTab)
            val mapBounds = compose.onNodeWithTag("hud-minimap").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithText("Gh0st").performScrollTo().assertIsDisplayed()
            assertEquals("Scrolling party details must not move the minimap", mapBounds, compose.onNodeWithTag("hud-minimap").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithText("Menu [Esc]").performClick()
            assertEquals(DesktopMenuTab.STATS, selectedTab)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun longNarrativeScrollsToLockedFallbackAndSpecialExit() {
        val directory = Files.createTempDirectory("starborn-long-narrative-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            val locked = GenericAction("sealed locker", "inspect")
            val room = services.worldDataSource.loadRooms().first { it.id == "pit_nova_bunk" }
            val ui = ExplorationUiState(currentRoom = room, actions = listOf(locked), actionHints = mapOf(locked.actionKey() to ActionHintUi(true, "Requires a key")),
                availableConnections = mapOf("up" to "upper-level"))
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(1024.dp, 576.dp)) {
                    DesktopExplorationPanels(services, ui, portraitBackdropLayout(Size(1024f, 576f), Size(1088f, 1920f)),
                        "A long room description. ".repeat(100), null, false, false) {}
                }
            } }
            val titleBounds = compose.onNodeWithText(room.title).fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("narrative-body").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10_000f) }
            compose.onNodeWithText(room.title).assertIsDisplayed()
            assertEquals(titleBounds, compose.onNodeWithText(room.title).fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithText("sealed locker").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText("up").performScrollTo().assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("OBJECTIVE").assertDoesNotExist()
            compose.onNodeWithText("PARTY").assertDoesNotExist()
            compose.waitUntil(15_000) { !services.exploration.uiState.value.isLoading }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun darkAndBlockedStateHideEntitiesLootMapAndDisableNavigation() {
        val directory = Files.createTempDirectory("starborn-dark-panels-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            val ui = ExplorationUiState(actions = listOf(GenericAction("locker", "inspect")), npcs = listOf("Mara"), enemies = listOf("rat"),
                groundItems = mapOf("medkit" to 2), availableConnections = mapOf("west" to "next"))
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(1024.dp, 576.dp)) {
                    DesktopExplorationPanels(services, ui, portraitBackdropLayout(Size(1024f, 576f), Size(1088f, 1920f)),
                        "A dark room.", null, true, true) {}
                }
            } }
            compose.onNodeWithText("Dark room").assertExists()
            compose.onNodeWithText("Talk to Mara").assertDoesNotExist()
            compose.onNodeWithText("Engage rat").assertDoesNotExist()
            compose.onAllNodesWithText("Collect", substring = true).assertCountEquals(0)
            compose.onNodeWithTag("travel-west").assertIsNotEnabled()
            compose.onNodeWithText("locker").assertIsNotEnabled()
            compose.waitUntil(15_000) { !services.exploration.uiState.value.isLoading }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
