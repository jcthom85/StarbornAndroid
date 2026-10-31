package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.fishing.viewmodel.*
import com.example.starborn.ui.fishing.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File
import javax.imageio.ImageIO

class FishingPortraitPresentationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun portraitSceneSupportsSwipeAndShowsReadableSurgeFeedback() {
        var hooks = 0
        var state by mutableStateOf(FishingUiState(fishingState = FishingState.HOOKSET,
            hookState = FishingHookState(0, false, true, true)))
        rule.setContent { FishingTheme {
            Box(Modifier.size(360.dp, 500.dp).padding(16.dp)) { FishingWaterScene(state, { hooks++ }) }
        } }
        rule.onNodeWithTag("fishing-portrait-scene").performTouchInput { swipeUp() }
        rule.runOnIdle { assertEquals(1, hooks); state = FishingUiState(fishingState = FishingState.REELING,
            reelState = FishingReelState(.4f, .7f, false, "Glowfish", null, com.example.starborn.domain.fishing.FishingFightPhase.SURGE)) }
        rule.onNodeWithText("Give it slack").assertIsDisplayed()
        val target = File("build/reports/fishing-portrait/portrait-scene.png").apply { parentFile.mkdirs() }
        ImageIO.write(rule.onRoot().captureToImage().asAwtImage(), "png", target)
    }
    @Test fun prismCaptureStaysVisibleOnACompactPanel() {
        rule.setContent { FishingTheme {
            Box(Modifier.size(360.dp, 480.dp).padding(12.dp)) {
                com.example.starborn.shared.puzzle.PrismOpticsPanel(listOf(
                    com.example.starborn.shared.puzzle.SignalDial("angle", 30f, 0f, 90f, 30f, 3f),
                    com.example.starborn.shared.puzzle.SignalDial("freq", 50f, 0f, 100f, 50f, 3f),
                    com.example.starborn.shared.puzzle.SignalDial("lux", 50f, 0f, 100f, 50f, 3f)), { _, _ -> }, {})
            }
        } }
        rule.onNodeWithText("Capture light").assertIsDisplayed()
        val target = File("build/reports/fishing-portrait/prism-panel.png").apply { parentFile.mkdirs() }
        ImageIO.write(rule.onRoot().captureToImage().asAwtImage(), "png", target)
    }

}
