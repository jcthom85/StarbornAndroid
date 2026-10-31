package com.example.starborn.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.exploration.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import javax.imageio.ImageIO

class GladePuffballPresentationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun scenePropHasNoCustomTextAndUsesTheWholeLane() {
        var inspections = 0
        var plays = 0
        rule.setContent { MaterialTheme {
            Box(Modifier.size(360.dp, 320.dp).background(Color(0xFF182C26))) {
                GladePuffball(0, 0, true, true, { inspections++ }, { plays++ }, {}, Modifier.fillMaxWidth())
            }
        } }
        rule.onNodeWithTag("glade-puffball").performClick()
        rule.runOnIdle { assertEquals(1, inspections); assertEquals(0, plays) }
        rule.onNodeWithText("Roll it back").assertDoesNotExist()
        rule.onNodeWithText("A puffball rolls out of the ferns.").assertDoesNotExist()
        rule.onNodeWithTag("glade-puffball-lane").assertWidthIsEqualTo(360.dp)
        val output = File("build/reports/charlie/puffball-revised.png").apply { parentFile.mkdirs() }
        ImageIO.write(rule.onRoot().captureToImage().asAwtImage(), "png", output)
    }
    @Test fun externalConfirmationAnimatesThenCommitsOnce() {
        var plays = 0
        var token by mutableStateOf(0)
        rule.setContent { MaterialTheme {
            GladePuffball(0, token, true, false, {}, { plays++ }, {}, Modifier.width(360.dp))
        } }
        rule.mainClock.advanceTimeBy(1400)
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { token++ }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertEquals(0, plays) }
        rule.mainClock.advanceTimeBy(3100)
        rule.runOnIdle { assertEquals(1, plays) }
    }
    @Test fun gateRevealReachesBothViewportEdges() {
        rule.setContent { MaterialTheme {
            Box(Modifier.size(360.dp, 640.dp).background(Color.Black)) { TempleGateReveal(.48f, false) }
        } }
        rule.onNodeWithTag("temple-gate-reveal").assertWidthIsEqualTo(360.dp).assertHeightIsEqualTo(640.dp)
        val output = File("build/reports/charlie/temple-reveal.png").apply { parentFile.mkdirs() }
        ImageIO.write(rule.onRoot().captureToImage().asAwtImage(), "png", output)
    }
}
