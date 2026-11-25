package com.example.starborn.desktop

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.cinematic.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import kotlin.math.abs

class DesktopPopupAlignmentTest {
    @get:Rule val compose=createComposeRule()
    @Test @OptIn(ExperimentalTestApi::class) fun informationCardsAcknowledgeOnceByBodyOrKeyboard() {
        var count by mutableIntStateOf(0)
        compose.setContent { MaterialTheme {
            AlertDialog(onDismissRequest={count++}, modifier=Modifier.informationAcknowledgement {count++},
                title={Text("Information")},text={Text("Card body")},confirmButton={TextButton(onClick={count++}) {Text("Continue")}})
        } }
        compose.onNodeWithText("Card body").performTouchInput { click() }
        compose.runOnIdle {assertEquals(1,count)}
        listOf(Key.Enter,Key.Spacebar,Key.Escape).forEachIndexed { i,key ->
            compose.onNodeWithText("Card body").performKeyInput {pressKey(key)}
            compose.runOnIdle {assertEquals(i+2,count)}
        }
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle {assertEquals(5,count)}
    }
    @Test fun choiceBodiesDoNotConfirmOrDismissAndExplicitActionsStillWork() {
        var confirmed=0;var dismissed=0
        compose.setContent { MaterialTheme {
            DesktopExplorationModal("Choice body",{dismissed++},"Choose") {
                Button(onClick={confirmed++}) {Text("Do it")}
                TextButton(onClick={dismissed++}) {Text("Cancel")}
            }
        } }
        compose.onNodeWithText("Choice body").performTouchInput {click()}
        compose.runOnIdle {assertEquals(0,confirmed);assertEquals(0,dismissed)}
        compose.onNodeWithText("Do it").performClick()
        compose.runOnIdle {assertEquals(1,confirmed);assertEquals(0,dismissed)}
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {assertEquals(1,dismissed)}
    }
    @Test fun standardCinematicBodyRevealsBeforeAdvancingAndKeepsSkipExplicit() {
        val directory=Files.createTempDirectory("cinematic-body-").toFile()
        val services=DesktopAppServices(directory,RecordingAudioDriver())
        var advances=0;var skips=0
        val text="Authored cinematic text. ".repeat(40)
        val state=CinematicUiState("body-test",null,CinematicBackdrop.BLACK,skippable=true,stepIndex=0,stepCount=1,
            step=CinematicStepUi(CinematicStepType.NARRATION,null,text))
        try {
            compose.mainClock.autoAdvance=false
            compose.setContent {DesktopStarbornTheme(services) {DesktopCinematicOverlay(state,services,{advances++},{skips++})}}
            compose.mainClock.advanceTimeByFrame()
            compose.onRoot().performTouchInput {click(Offset(100f,100f))}
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {assertEquals(0,advances);assertEquals(0,skips)}
            javax.imageio.ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png",java.io.File("build/reports/desktop/popup-cinematic.png"))
            compose.onNodeWithText(text).assertExists()
            compose.onNodeWithText("Continue").performClick()
            compose.runOnIdle {assertEquals(1,advances);assertEquals(0,skips)}
            compose.onNodeWithText("Skip").performClick()
            compose.runOnIdle {assertEquals(1,skips)}
        } finally {services.close();directory.deleteRecursively()}
    }
    @Test fun illustratedNarrationCentersAndRapidTapsAdvanceOnlyOnce() {
        val directory = Files.createTempDirectory("cinematic-narration-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        val text = "A long enough caption to still be typing after the shot begins. ".repeat(4)
        val state = CinematicUiState(
            "illustrated-test", null, CinematicBackdrop.BLACK,
            presentation = CinematicPresentation.ILLUSTRATED, stepIndex = 0, stepCount = 2,
            step = CinematicStepUi(CinematicStepType.NARRATION, null, text, durationSeconds = 0.6)
        )
        var advances = 0
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent {
                DesktopStarbornTheme(services) {
                    DesktopCinematicOverlay(state, services, { advances++ }, {})
                }
            }
            compose.mainClock.advanceTimeByFrame()
            val viewport = compose.onRoot().getUnclippedBoundsInRoot()
            val caption = compose.onAllNodesWithText(text, useUnmergedTree = true).onLast()
                .getUnclippedBoundsInRoot()
            assertTrue(abs((viewport.left + viewport.right - caption.left - caption.right).value) < 4f)
            assertTrue(abs((viewport.top + viewport.bottom - caption.top - caption.bottom).value) < 4f)

            // Fixed-duration illustrated shots ignore stray clicks and advance once when duration elapses.
            compose.onRoot().performClick()
            compose.mainClock.advanceTimeBy(100)
            compose.runOnIdle { assertEquals(0, advances) }
            compose.onRoot().performClick()
            compose.runOnIdle { assertEquals(0, advances) }
            compose.mainClock.advanceTimeBy(600)
            compose.runOnIdle { assertEquals(1, advances) }
            compose.onRoot().performClick()
            compose.runOnIdle { assertEquals(1, advances) }
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }

}
