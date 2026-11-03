package com.example.starborn.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.exploration.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopPolishGateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cookingResultStaysVisibleAfterScrollingInCompactLayout() = cookResult(false)
    @Test fun cookingResultStaysVisibleInWideLayout() = cookResult(true)
    private fun cookResult(wide: Boolean) {
        val directory = Files.createTempDirectory("desktop-cooking-result").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            services.startNewGame()
            val recipes = services.craftingService.cookingRecipes.filter { it.category == "meal" }.sortedBy { it.name }
            recipes.flatMap { it.ingredients.keys }.distinct().forEach { services.inventoryService.addItem(it, 100) }
            services.sessionStore.restore(services.sessionStore.state.value.copy(roomStates = services.sessionStore.state.value.roomStates +
                recipes.mapNotNull { it.discoveryRoom }.associateWith { mapOf("cooking_discovered" to true) }))
            val recipe = recipes.last()
            val before = services.inventoryService.snapshot()[recipe.result] ?: 0
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(if (wide) 1000.dp else 560.dp, 650.dp)) {
                    DesktopCookingScreen(services.craftingService, services.inventoryService, "Campfire", {})
                }
            } }
            val target = if (wide) "cooking-select-${recipe.id}" else "cook-${recipe.id}"
            compose.onNodeWithTag("cooking-recipe-browser").performScrollToNode(hasTestTag(target))
            if (wide) compose.onNodeWithTag(target).performClick()
            compose.onNodeWithTag("cook-${recipe.id}").performScrollTo().performClick()
            compose.onNodeWithText("At the cookfire").assertIsDisplayed()
            compose.onAllNodesWithText("Eat from inventory to activate Well-Fed.", substring = true).assertCountEquals(1)
            capture(if (wide) "cooking-wide" else "cooking-compact")
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithText("At the cookfire").assertDoesNotExist()
            assertTrue((services.inventoryService.snapshot()[recipe.result] ?: 0) > before)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun revealFinishesBeforeNarrationAndCancelsCleanlyOnRoomChange() {
        var room by mutableStateOf<String?>(TEMPLE_GATE_ROOM_ID)
        var open by mutableStateOf(false)
        var observed = TempleGateRevealState(false, 1f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val state = rememberTempleGateReveal(room, open, false)
            SideEffect { observed = state }
            Text("Reveal ${state.active}")
        }
        compose.runOnIdle { open = true }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle {
            assertTrue(observed.active)
            assertEquals(ExplorationNotice.NONE, explorationNotice(ExplorationUiState(isLoading = false,
                narrationPrompt = NarrationPrompt("The passage is open.", true)), observed.active))
        }
        compose.mainClock.advanceTimeBy(2500)
        compose.runOnIdle { assertFalse(observed.active); assertEquals(1f, observed.progress, .001f); room = "other" }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { open = false; room = TEMPLE_GATE_ROOM_ID }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { open = true }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertTrue(observed.active); room = "other" }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertFalse(observed.active) }
    }

    @Test fun savedOpenGateAndReducedMotionDoNotReplay() {
        var open by mutableStateOf(true)
        var reduced by mutableStateOf(false)
        var observed = TempleGateRevealState(false, 1f)
        compose.setContent {
            val state = rememberTempleGateReveal(TEMPLE_GATE_ROOM_ID, open, reduced)
            SideEffect { observed = state }
            Text("Reveal ${state.active}")
        }
        compose.runOnIdle { assertFalse(observed.active); open = false; reduced = true }
        compose.runOnIdle { open = true }
        compose.runOnIdle { assertFalse(observed.active) }
    }

    @Test fun gateArtworkAndEffectStayAlignedInPhoneCropAndDesktopFit() {
        val phone = templeGateArtworkFrame(Size(360f,800f), Size(1088f,1920f), true)
        val desktop = templeGateArtworkFrame(Size(900f,600f), Size(1088f,1920f), false)
        assertEquals(180f, phone.left + phone.width * .5f, .01f)
        assertEquals(344f, phone.top + phone.height * .43f, .01f)
        assertEquals(450f, desktop.left + desktop.width * .5f, .01f)
        assertEquals(258f, desktop.top + desktop.height * .43f, .01f)
        val provider = DesktopAssetProvider()
        val gate = AssetJsonReader(provider, MoshiProvider.instance).readList<Room>("rooms.json").single { it.id == TEMPLE_GATE_ROOM_ID }
        val openPath = "images/rooms/world_2/temple_gate_open.png"
        assertEquals(TEMPLE_GATE_CLOSED_ART, resolveRoomBackground(gate, emptyMap(), emptySet()))
        assertEquals(openPath, resolveRoomBackground(gate, mapOf("temple_open" to true), emptySet()))
        assertEquals(openPath, resolveRoomBackground(gate, emptyMap(), setOf("ms_w2_mq02_complete")))
        var crop by mutableStateOf(false)
        var progress by mutableStateOf(0f)
        var subdued by mutableStateOf(false)
        var loaded = false
        compose.setContent {
            val closed = rememberDesktopAssetPainter(TEMPLE_GATE_CLOSED_ART, provider)
            val opened = rememberDesktopAssetPainter(openPath, provider)
            SideEffect { loaded = closed is DesktopBitmapPainter && opened is DesktopBitmapPainter }
            Box(Modifier.size(if (crop) 360.dp else 900.dp, if (crop) 640.dp else 600.dp).background(Color.Black).testTag("gate-preview")) {
                Image(opened, null, Modifier.fillMaxSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit)
                TempleGateReveal(progress, subdued, closedPainter = closed, cropArtwork = crop)
            }
        }
        compose.waitUntil(10000) { loaded }
        for (phoneMode in listOf(false,true)) {
            compose.runOnIdle { crop = phoneMode }
            for ((label,p) in listOf("closed" to 0f,"charge" to .24f,"opening" to .52f,"open" to 1f)) {
                compose.runOnIdle { progress = p }
                capture("gate-${if (phoneMode) "phone" else "desktop"}-$label", "gate-preview")
            }
        }
        compose.runOnIdle { subdued = true; progress = .52f }
        capture("gate-phone-subdued", "gate-preview")
    }

    @Test fun titleShowsGeneratedBuildIdentity() {
        val directory = Files.createTempDirectory("desktop-version-label").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            compose.setContent { DesktopStarbornTheme(services) { DesktopMainMenuScreen(services, {}, onQuit = {}) } }
            compose.onNodeWithTag("desktop-build-label").assertIsDisplayed().assertTextEquals(DesktopBuildInfo.DISPLAY_LABEL)
            assertTrue(DesktopBuildInfo.DISPLAY_LABEL.contains(DesktopBuildInfo.VERSION_NAME))
            assertTrue(DesktopBuildInfo.DISPLAY_LABEL.contains(DesktopBuildInfo.VERSION_CODE.toString()))
        } finally { services.close(); directory.deleteRecursively() }
    }

    private fun capture(name: String, tag: String? = null) {
        val out = File("build/reports/desktop-polish-gate").apply { mkdirs() }
        val node = if (tag == null) compose.onAllNodes(isRoot()).onLast() else compose.onNodeWithTag(tag)
        ImageIO.write(node.captureToImage().asAwtImage(), "png", File(out,"$name.png"))
    }
}
