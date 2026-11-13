package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class World5PlaytestDirectFixesTest {
    @get:Rule val compose = createComposeRule()
    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world5-direct-fixes").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { s.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { compose.runOnIdle { s.close() }; dir.deleteRecursively() }
    }
    private fun restore(s: DesktopAppServices, room: String) {
        val node = s.worldDataSource.loadHubNodes().first { room in it.rooms }
        val hub = s.worldDataSource.loadHubs().single { it.id == node.hubId }
        compose.runOnIdle { s.sessionStore.restore(s.sessionStore.state.value.copy(
            worldId = hub.worldId, hubId = hub.id, roomId = room,
            activeQuests = emptySet(), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = emptySet(), roomStates = emptyMap(), questTasksCompleted = emptyMap(),
            learnedSchematics = emptySet(), arcadeProgress = emptyMap(), unlockedExits = emptySet(), visitedNodes = emptySet()
        )) }
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
        compose.waitForIdle()
        repeat(30) {
            if (s.exploration.uiState.value.activeDialogue != null) {
                compose.runOnIdle { s.exploration.advanceDialogue() }; compose.waitForIdle()
            }
        }
        dismissNarration(s)
        s.promptManager.discardMatching { true }
    }
    private fun dismissNarration(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }; compose.waitForIdle()
        }
    }

    @Test fun sixDiscoveriesAnnounceTheLearnedGameOnceAndStayReadOnlyAfterReload() = runWith { s ->
        val discoveries = s.worldDataSource.loadRooms().flatMap { room -> room.actions.filter { it["type"] == "arcade_discovery" }.map { room to it } }
        assertEquals(6, discoveries.size)
        for ((room, raw) in discoveries) {
            restore(s, room.id)
            val cabinet = raw["discovery_cabinet_id"] as String
            val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == raw["name"] }
            compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
            compose.waitUntil(5000) { s.sessionStore.state.value.arcadeProgress[cabinet]?.discovered == true }
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            dismissNarration(s)
            try {
                compose.waitUntil(5000) { s.exploration.uiState.value.prompt is ItemGrantedPrompt }
            } catch (e: Throwable) {
                val ui = s.exploration.uiState.value
                fail("$cabinet: black=${ui.forceBlackScreen}, narration=${ui.narrationPrompt}, announcement=${ui.eventAnnouncement}, dialogue=${ui.activeDialogue?.line?.id}, level=${ui.levelUpPrompt}, prompts=${s.promptManager.state.value}")
            }
            val prompt = s.exploration.uiState.value.prompt as ItemGrantedPrompt
            assertEquals("arcade_schematic", prompt.category)
            assertEquals("ARCADE SCHEMATIC ACQUIRED", prompt.announcementHeading)
            assertTrue(prompt.itemName.contains("repair schematic"))
            assertTrue(prompt.description!!.contains("after repair"))
            assertTrue(s.sessionStore.state.value.learnedSchematics.isNotEmpty())
            assertFalse(s.sessionStore.state.value.arcadeProgress[cabinet]!!.installed)
            val inventory = s.sessionStore.state.value.inventory
            compose.runOnIdle { s.exploration.dismissPrompt(prompt.id) }; compose.waitForIdle()
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
            assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room.id }
            dismissNarration(s)
            val inspection = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == raw["name"] }
            assertTrue("${room.id} discovery becomes gray", inspection.completedInspection)
            compose.runOnIdle { s.exploration.onActionSelected(inspection) }; compose.waitForIdle()
            dismissNarration(s)
            assertFalse(s.exploration.uiState.value.prompt is ItemGrantedPrompt)
            assertEquals(inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun repairedInspectionsHaveRealDetailsAndBecomeGrayWithoutGrantingProgress() = runWith { s ->
        for ((room, name) in listOf("orbital_executive_dock_scale_01" to "luggage recess", "source_orion_nightmare" to "black ocean")) {
            restore(s, room)
            val before = s.sessionStore.state.value
            val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == name }
            compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            assertTrue(s.exploration.uiState.value.narrationPrompt!!.message.length > 50)
            dismissNarration(s)
            compose.waitUntil(5000) { s.exploration.uiState.value.roomState.keys.any { it.startsWith("inspection_read_") } }
            val ui = s.exploration.uiState.value
            val prose = resolveRoomDescription(ui.currentRoom, ui.roomState, ui.completedMilestones, false, ui.completedEvents)
            val segment = requireNotNull(buildInlineActionPlan(prose, ui.actions, ui.actionHints, ui.currentRoom, ui.roomState)).segments.single {
                (it.target as? InlineActionTarget.Room)?.action?.name == name
            }
            assertTrue(segment.inspected)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertEquals(before.questTasksCompleted, s.sessionStore.state.value.questTasksCompleted)
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
        }
    }

    @Test fun removingTheShipWorkbenchLeavesMenuTinkeringAvailable() = runWith { s ->
        restore(s, "astra_cargo_bay")
        assertTrue(s.exploration.uiState.value.actions.none { it is TinkeringAction })
        assertFalse(s.exploration.uiState.value.currentRoom!!.description.contains("[action:Astra workbench"))
        compose.runOnIdle { s.exploration.openTinkeringShortcut() }; compose.waitForIdle()
        compose.waitUntil(5000) { s.exploration.uiState.value.isMenuOverlayVisible }
        assertEquals(MenuTab.FIELD_KIT, s.exploration.uiState.value.menuTab)
    }

    @Test @OptIn(ExperimentalTestApi::class)
    fun schematicCardAndKnockedOutPortraitsStayReadable() = runWith { s ->
        val output = File("build/reports/desktop/screenshots").apply { mkdirs() }
        val prompt = ItemGrantedPrompt("Orbital Defense 2000 repair schematic", 1, category = "arcade_schematic",
            description = "Zenith Telemetry Array recovered. Repair from Menu → Tinker. Playable in Astra Recreation after repair.")
        val showingCard = androidx.compose.runtime.mutableStateOf(true)
        val members = listOf("nova", "zeke", "orion", "gh0st").map { id ->
            CombatantState(Combatant(id, id, CombatSide.PLAYER, StatBlock(100,10,10,10,10,10,10)), 0,100)
        }
        compose.setContent { DesktopStarbornTheme(s) {
            if (showingCard.value) Box(Modifier.size(390.dp,844.dp)) {
                DesktopItemGrantedBanner(s, prompt, {})
            } else Box(Modifier.size(1000.dp,500.dp)) {
                DesktopBattleFormation(members,s,Modifier.size(1000.dp,500.dp),false,
                    sprite = { "images/characters/emotes/${it}_down.png" }, selectedId = null,
                    validTargets = emptySet(), targeting = false, meters = emptyMap(), intents = emptyMap(),
                    feedback = emptyMap(), cues = emptyList(), lungeStyle = AttackLungeStyle.MELEE,
                    lungeActor = null,lungeToken = 0,missActor = null,missToken = 0,onSelect = {})
            }
        } }
        compose.onNodeWithText("ARCADE SCHEMATIC ACQUIRED").assertIsDisplayed()
        compose.onNodeWithText(prompt.itemName).assertIsDisplayed()
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(),"png",File(output,"arcade-schematic-acquired.png"))
        compose.runOnIdle { showingCard.value = false }
        for (id in listOf("nova","zeke","orion","gh0st")) compose.onNodeWithText("$id · KO").assertExists()
        compose.waitForIdle()
        Thread.sleep(400) // Let asynchronous asset decoding settle for the visual evidence.
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(),"png",File(output,"party-ko-portraits.png"))
    }
}
