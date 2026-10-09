package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.hub.presentation.*
import com.example.starborn.domain.node.NodeProgressionEvaluator
import com.example.starborn.domain.node.NodeVisibility
import com.example.starborn.feature.hub.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DesktopHubRedesignTest {
    @get:Rule val compose = createComposeRule()

    @Test fun anchorsFollowOriginalImageBoundsAndSharedLayoutsCoverHubs() {
        val services = DesktopAppServices(Files.createTempDirectory("starborn-hub-layout").toFile())
        try {
            services.worldDataSource.loadHubs().forEach { assertTrue("Missing layout ${it.id}", HubMapLayouts.all.containsKey(it.id)) }
            for ((w,h) in listOf(1440f to 900f,1280f to 720f,900f to 650f,2560f to 1080f)) {
                val layout = portraitBackdropLayout(Size(w,h),Size(1080f,1920f))
                val anchor = hubAnchor(layout.center.width,layout.center.height,.35f,.55f)
                assertEquals(layout.center.width*.35f,anchor.x,.001f)
                assertEquals(layout.center.height*.55f,anchor.y,.001f)
                assertEquals(9f/16f,layout.center.width/layout.center.height,.001f)
            }
        } finally { services.close() }
    }

    @Test fun controllerRetainsSelectionAndSuppliesAuthoredTrackedQuestAndLock(): Unit = runBlocking {
        val services = DesktopAppServices(Files.createTempDirectory("starborn-hub-state").toFile())
        check(services.startDebugScenario("tut_npc_dialogue"))
        val runtime = HubController(services.worldDataSource,services.questRepository,services.sessionStore,parentScope=this)
        try {
            val state=withTimeout(15000) { runtime.uiState.first { !it.isLoading && it.nodes.isNotEmpty() } }
            val evaluator=NodeProgressionEvaluator()
            val expected=services.worldDataSource.loadHubNodes().filter { it.hubId==state.hub!!.id &&
                evaluator.evaluate(it,services.sessionStore.state.value).visibility!=NodeVisibility.HIDDEN }.map { it.id }.toSet()
            assertEquals(expected,state.nodes.filter { it.special==null }.map { it.id }.toSet())
            val chosen=state.nodes.last()
            runtime.selectNode(chosen.id)
            services.sessionStore.startQuest("w6_mq26",track=true)
            val updated=withTimeout(15000) { runtime.uiState.first { it.trackedQuest?.id=="w6_mq26" } }
            assertEquals(chosen.id,updated.selectedNodeId)
            assertEquals(services.questRepository.questById("w6_mq26")!!.title,updated.trackedQuest!!.title)
            updated.nodes.firstOrNull { !it.canEnter }?.let { locked ->
                var travelled=false
                runtime.enterNode(locked.id) { travelled=true }
                assertFalse(travelled)
                assertEquals(locked.title,runtime.uiState.value.lockedPrompt!!.title)
                runtime.dismissLockedPrompt();assertNull(runtime.uiState.value.lockedPrompt)
            }
            val available=updated.nodes.first { it.canEnter && it.special==null }
            var travelled=false
            runtime.enterNode(available.id) { travelled=true }
            assertTrue(travelled)
            assertEquals(available.entryRoom,services.sessionStore.state.value.roomId)
        } finally { runtime.close();services.close() }
    }

    @Test fun astraTravelAndDisembarkUseSharedRoutes(): Unit = runBlocking {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-astra").toFile())
        check(services.startDebugScenario("tut_npc_dialogue"))
        services.sessionStore.setMilestone(HubController.ASTRA_UNLOCK_MILESTONE)
        val original=services.sessionStore.state.value
        val runtime=HubController(services.worldDataSource,services.questRepository,services.sessionStore,parentScope=this)
        try {
            withTimeout(15000) { runtime.uiState.first { it.nodes.any { n -> n.id=="astra_access" } } }
            runtime.enterNode("astra_access") {}
            assertEquals("hub_astra",services.sessionStore.state.value.hubId)
            withTimeout(15000) { runtime.uiState.first { it.nodes.any { n -> n.id=="astra_disembark" } } }
            runtime.enterNode("astra_disembark") {}
            assertEquals(original.hubId,services.sessionStore.state.value.hubId)
        } finally { runtime.close();services.close() }
    }

    @Test fun unlockRevealWaitsForSelection(): Unit = runBlocking {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-reveal").toFile())
        check(services.startDebugScenario("tut_npc_dialogue"))
        services.sessionStore.setHub("hub_2_logistics")
        services.sessionStore.setMilestone("ms_w1_mq03_bogs_talked")
        services.sessionStore.setRoomState("admin_elevator","mine_map_reveal_pending",true)
        val runtime=HubController(services.worldDataSource,services.questRepository,services.sessionStore,parentScope=this)
        try {
            val state=withTimeout(15000) { runtime.uiState.first { !it.isLoading && it.hub?.id=="hub_2_logistics" } }
            assertEquals("deep_mine",state.newlyUnlockedNodeId)
            assertEquals("deep_mine",state.selectedNodeId)
            delay(100)
            assertEquals("deep_mine",runtime.uiState.value.newlyUnlockedNodeId)
            runtime.selectNode("deep_mine")
            assertNull(runtime.uiState.value.newlyUnlockedNodeId)
            assertEquals(true,services.sessionStore.state.value.roomStates["admin_elevator"]?.get("mine_map_reveal_seen"))
            assertEquals(false,services.sessionStore.state.value.roomStates["admin_elevator"]?.get("mine_map_reveal_pending"))
        } finally { runtime.close();services.close() }
    }

    @Test fun lockedDestinationShowsOriginalReasonAndDisablesTravel() {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-card").toFile())
        try {
            val node=HubNodeUi("locked","The Gate","gate",.5f,.5f,220f,false,
                description="Authored description",lockedPreview="Authored preview",canEnter=false,lockReason="Authored lock reason")
            compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
                Box(Modifier.size(340.dp,500.dp)) { HubDestinationCard(node,androidx.compose.ui.graphics.Color.Cyan) { error("Locked destination entered") } }
            } } }
            compose.onNodeWithText("Authored lock reason").assertExists()
            compose.onNodeWithText("Enter").assertIsNotEnabled()
        } finally { services.close() }
    }

    @Test fun missingArtworkRetainsKeyboardSelectableDestination() {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-fallback").toFile())
        try {
            val hub=services.worldDataSource.loadHubs().first()
            val node=HubNodeUi("missing","Original title","entry",.5f,.5f,220f,true,iconPath="missing-artwork.png")
            var selected:String?=null
            var entered=false
            compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
                Box(Modifier.size(500.dp,850.dp)) {
                    DesktopHubMapScene(services,HubUiState(isLoading=false,hub=hub,nodes=listOf(node)),androidx.compose.ui.graphics.Color.Cyan,
                        { selected=it }, { entered=true })
                }
            } } }
            compose.onNodeWithTag("hub-node-missing").performClick()
            compose.runOnIdle { assertEquals("missing",selected) }
            compose.onNodeWithTag("hub-node-missing").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            compose.onNodeWithTag("hub-node-missing").performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertTrue(entered) }
        } finally { services.close() }
    }

    @Test fun nameplatesStaySmallWhileTargetsGrowAndStatusWordsStayOffMap() {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-nameplates").toFile())
        try {
            val hub=services.worldDataSource.loadHubs().first()
            val node=HubNodeUi("label","Med-Bay","entry",.5f,.5f,220f,true,completed=true)
            var large by mutableStateOf(false)
            compose.setContent { DesktopStarbornTheme {
                CompositionLocalProvider(LocalExplorationSettings provides com.example.starborn.data.local.UserSettings(largeTouchTargets=large)) {
                    Box(Modifier.size(500.dp,850.dp)) {
                        DesktopHubMapScene(services,HubUiState(isLoading=false,hub=hub,nodes=listOf(node)),androidx.compose.ui.graphics.Color.Cyan,{}, {})
                    }
                }
            } }
            val target=compose.onNodeWithTag("hub-node-label").fetchSemanticsNode().boundsInRoot
            val plate=compose.onNodeWithTag("hub-nameplate-label",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertTrue(target.height>=44f)
            assertTrue(plate.height<target.height)
            compose.onNodeWithText("Completed").assertDoesNotExist()
            compose.runOnIdle { large=true }
            val bigger=compose.onNodeWithTag("hub-node-label").fetchSemanticsNode().boundsInRoot
            assertTrue(bigger.height>=56f)
        } finally { services.close() }
    }

    @Test fun hoveringArtworkHighlightsItsNameplateAndDoesNotMoveLabels() {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-hover").toFile())
        try {
            val hub=services.worldDataSource.loadHubs().first()
            val source=services.worldDataSource.loadHubNodes().first { it.id=="med_bay" }
            val node=HubNodeUi(source.id,source.title,source.entryRoom,.5f,.5f,220f,true,iconPath=source.iconImage)
            var selection by mutableStateOf<String?>(null)
            compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
                Box(Modifier.size(900.dp,650.dp)) {
                    DesktopHubMapScene(services,HubUiState(isLoading=false,hub=hub,nodes=listOf(node),selectedNodeId=selection),
                        androidx.compose.ui.graphics.Color.Cyan,{ selection=it },{})
                }
            } } }
            compose.waitUntil(15000) { compose.onAllNodesWithTag("hub-art-med_bay").fetchSemanticsNodes().isNotEmpty() }
            val before=compose.onNodeWithTag("hub-node-med_bay").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("hub-art-med_bay").performMouseInput { moveTo(center) }
            compose.onNodeWithTag("hub-node-med_bay").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Hovered"))
            compose.onNodeWithTag("hub-node-med_bay").performClick()
            val after=compose.onNodeWithTag("hub-node-med_bay").fetchSemanticsNode().boundsInRoot
            assertEquals(before,after)
        } finally { services.close() }
    }

    @Test fun compactRegionDrawerAndEscapeAreReachable() {
        val services=DesktopAppServices(Files.createTempDirectory("starborn-hub-keys").toFile())
        check(services.startDebugScenario("tut_npc_dialogue"))
        try {
            var returned=false
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(900.dp,650.dp)) {
                DesktopHubScreen(services,{}, { returned=true },{})
            } } }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Region").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Region").performClick()
            compose.onNodeWithText("Close").assertExists()
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.onNodeWithText("Close").assertDoesNotExist()
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.runOnIdle { assertTrue(returned) }
        } finally { services.close() }
    }
}
