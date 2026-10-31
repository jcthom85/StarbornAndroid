package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.exploration.GladePuffball
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File
import javax.imageio.ImageIO

class GladePuffballPresentationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun ballOffersGentlePlayAndLeavingWithoutAPortraitOrAutomaticReward() {
        var plays = 0
        var trust by mutableStateOf(0)
        rule.setContent { MaterialTheme {
            Box(Modifier.size(360.dp, 430.dp).padding(12.dp)) {
                GladePuffball(trust, null, true, true, { plays++; trust++ }, Modifier.fillMaxWidth())
            }
        } }
        rule.onNodeWithTag("glade-puffball").performClick()
        rule.onNodeWithText("Leave it be").performClick()
        rule.runOnIdle { assertEquals(0, plays) }
        rule.onNodeWithTag("glade-puffball").performClick()
        rule.onNodeWithText("Roll it back").performClick()
        rule.runOnIdle { assertEquals(1, plays) }
        rule.onNodeWithTag("glade-puffball").performClick()
        rule.onNodeWithText("Give it a gentle toss").assertIsDisplayed()
        val output = File("build/reports/charlie/puffball.png").apply { parentFile.mkdirs() }
        ImageIO.write(rule.onRoot().captureToImage().asAwtImage(), "png", output)
    }
    @Test fun animatedReturnFinishesBeforeTheInteractionIsCommitted() {
        var plays = 0
        rule.setContent { MaterialTheme {
            GladePuffball(0, null, true, false, { plays++ }, Modifier.width(340.dp))
        } }
        rule.mainClock.advanceTimeBy(1400)
        rule.waitForIdle()
        rule.onNodeWithTag("glade-puffball").performClick()
        rule.onNodeWithText("Roll it back").assertIsDisplayed()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Roll it back").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertEquals(0, plays) }
        rule.mainClock.advanceTimeBy(3000)
        rule.runOnIdle { assertEquals(1, plays) }
    }
}
