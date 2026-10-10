package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.desktop.ui.DesktopArcadeScreen
import com.example.starborn.feature.arcade.domain.ArcadeIds
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopArcadeSnapshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktopArcadeSelector() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_arcade_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                DesktopArcadeScreen(
                    services = services,
                    onClose = {}
                )
            }
            composeTestRule.mainClock.advanceTimeBy(300)

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_arcade_preview.png"))
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }

    @Test
    fun arcadeDirectoryAllowsSelectingCabinetsAndInspectsRepairStatus() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_arcade_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            composeTestRule.setContent {
                DesktopArcadeScreen(
                    services = services,
                    onClose = {}
                )
            }

            // Assert arcade header
            composeTestRule.onNodeWithText("ASTRA RECREATION LOUNGE").assertExists()
            composeTestRule.onNodeWithText("CABINET DIRECTORY").assertExists()

            // Verify cabinet cards are present in directory
            composeTestRule.onNodeWithTag("cabinet-card-deep_mine_asteroid_drill").assertExists()
            composeTestRule.onNodeWithTag("cabinet-card-canopy_hopper").assertExists()
            composeTestRule.onNodeWithTag("cabinet-card-spire_infiltrator").assertExists()
            composeTestRule.onNodeWithTag("cabinet-card-slag_catcher").assertExists()
            composeTestRule.onNodeWithTag("cabinet-card-orbital_defense").assertExists()
            composeTestRule.onNodeWithTag("cabinet-card-harmonic_pulse").assertExists()

            // Click Canopy Hopper to view its showcase
            composeTestRule.onNodeWithTag("cabinet-card-canopy_hopper").performClick()

            // Verify Canopy Hopper briefing is displayed
            composeTestRule.onNodeWithText("Bioluminescent Swamp Crosser", substring = true).assertExists()
            composeTestRule.onNodeWithText("Sector-9 Optic Board", substring = true).assertExists()

            // Now repair Canopy Hopper in session and verify status updates to ready
            services.sessionStore.updateArcadeProgress(ArcadeIds.CANOPY_HOPPER) {
                it.copy(discovered = true, repaired = true, highScore = 14200)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("launch-cabinet-button").assertExists()
            composeTestRule.onNodeWithText("INSERT TOKEN & LAUNCH RUN", substring = true).assertExists()
            composeTestRule.onAllNodesWithText("14,200", substring = true).onFirst().assertExists()
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
