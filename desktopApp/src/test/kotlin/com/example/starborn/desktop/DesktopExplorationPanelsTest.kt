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

    @Test fun messHallKeepsItsInspectionsInProseAndTablesAreOnlyScenery() {
        val directory = Files.createTempDirectory("starborn-mess-hall-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            services.sessionStore.restore(services.sessionStore.state.value.copy(roomId = "pit_mess"))
            compose.waitUntil(10000) { services.exploration.uiState.value.currentRoom?.id == "pit_mess" }
            val ui = services.exploration.uiState.value
            assertFalse(ui.actions.any { it is RestStopAction || it.name == "tables" })
            val plan = requireNotNull(buildInlineActionPlan(ui.currentRoom!!.description, ui.actions, ui.actionHints, ui.currentRoom))
            val links = plan.segments.mapNotNull { (it.target as? InlineActionTarget.Room)?.action?.name }
            assertTrue("broken Hyperion cabinet" in links)
            assertTrue("vintage mystery monitor" in links)
            assertTrue(contextualRoomTargets(ui, plan, false).none { it is InlineActionTarget.Room })
            assertTrue(unmatchedRoomTargets(ui, plan, false).any { it is InlineActionTarget.Room && it.action.name == "monitor" })
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(1280.dp, 800.dp).testTag("mess-hall-stage")) {
                    DesktopExplorationPanels(services, ui, portraitBackdropLayout(Size(1280f, 800f), Size(1088f, 1920f)),
                        ui.currentRoom!!.description, plan, false, false) {}
                }
            } }
            compose.onNodeWithTag("exploration-context").assertDoesNotExist()
            compose.onAllNodesWithContentDescription("broken Hyperion cabinet", useUnmergedTree = true).onFirst().assertExists()
            compose.onAllNodesWithContentDescription("vintage mystery monitor", useUnmergedTree = true).onFirst().assertExists()
            val capture = File("build/reports/desktop/screenshots/mess-hall-clean.png").apply { parentFile.mkdirs() }
            ImageIO.write(compose.onNodeWithTag("mess-hall-stage").captureToImage().asAwtImage(), "png", capture)
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                roomId = "pit_nova_bunk",
                roomStates = services.sessionStore.state.value.roomStates + ("pit_nova_bunk" to mapOf("light_on" to true, "dark" to false))
            ))
            compose.waitUntil(10000) { services.exploration.uiState.value.currentRoom?.id == "pit_nova_bunk" }
            assertTrue(services.exploration.uiState.value.actions.any { it is RestStopAction })
        } finally { services.close(); directory.deleteRecursively() }
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
                    val gap = objective.top - minimap.bottom
                    assertTrue("Minimap and objective need visible background between them for $label (was $gap)", gap >= 19f)
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
            compose.onNodeWithTag("hud-key-legend").assertDoesNotExist()
            compose.onNodeWithText("Menu [Esc]").assertDoesNotExist()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun longNarrativeScrollsToLockedFallbackAndSpecialExit() {
        val directory = Files.createTempDirectory("starborn-long-narrative-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            val locked = GenericAction("sealed locker", "generic", actionEvent = "unlock_locker")
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
            compose.onNodeWithText("Sealed locker").assertDoesNotExist()
            compose.onNodeWithText("Actions").assertIsDisplayed()
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
            compose.onNodeWithText("locker").assertDoesNotExist()
            compose.waitUntil(15_000) { !services.exploration.uiState.value.isLoading }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
