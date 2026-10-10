package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.desktop.ui.DesktopFishingScreen
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopFishingSnapshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktopFishingScreen() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_fishing_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                DesktopFishingScreen(
                    services = services,
                    zoneId = "sector9_stream",
                    onClose = {}
                )
            }
            composeTestRule.mainClock.advanceTimeBy(300)

            val artifactsDir = File("C:/Users/jcthomas/.gemini/antigravity-ide/brain/4679957f-9fc8-4b49-a04b-cc85f289b043")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_fishing_preview.png"))
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }

    @Test
    fun fishingScreenSetupCastAndCancelFlow() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_fishing_flow_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            org.junit.Assert.assertTrue(services.startNewGame())

            // Seed player inventory with rods and lures
            services.inventoryService.addItem("wooden_rod", 1)
            services.inventoryService.addItem("fiberglass_rod", 1)
            services.inventoryService.addItem("basic_lure", 3)

            var closed = false

            composeTestRule.setContent {
                DesktopFishingScreen(
                    services = services,
                    zoneId = "sector9_stream",
                    onClose = { closed = true }
                )
            }

            // Verify Header & Telemetry
            composeTestRule.onNodeWithText("SONAR").assertExists()
            composeTestRule.onNodeWithText("SECTOR 9 TIDE POOLS").assertExists()
            composeTestRule.onNodeWithText("TACKLE & RIGGING PREPARATION").assertExists()

            // Verify equipped or available gear cards
            composeTestRule.onNodeWithTag("gear-card-field_rod").assertExists()
            composeTestRule.onNodeWithTag("gear-card-resin_rod").assertExists()
            composeTestRule.onNodeWithTag("gear-card-plain_chime_lure").assertExists()

            // Open Journal Modal
            composeTestRule.onNodeWithTag("journal-toggle-button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("ABYSSAL FISHING JOURNAL", substring = true).assertExists()

            // Close Journal
            composeTestRule.onNodeWithText("Close").performClick()
            composeTestRule.waitForIdle()

            // Cast line into deep water
            composeTestRule.onNodeWithTag("cast-line-button").assertIsEnabled()
            composeTestRule.onNodeWithTag("cast-line-button").performClick()

            // Assert WAITING state
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("LURE SUBMERGED · SCANNING FOR THERMAL BITE...", substring = true).assertExists()

            // Cancel cast
            composeTestRule.onNodeWithTag("cancel-cast-button").performClick()
            composeTestRule.waitForIdle()

            // Back in SETUP state
            composeTestRule.onNodeWithText("TACKLE & RIGGING PREPARATION").assertExists()

            // Exit zone
            composeTestRule.onNodeWithText("Leave Zone").performClick()
            org.junit.Assert.assertTrue("Fishing screen should invoke onClose", closed)
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
