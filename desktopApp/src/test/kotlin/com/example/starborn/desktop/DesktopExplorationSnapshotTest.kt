package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopExplorationSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktop3PanelExploration() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_deck_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            composeTestRule.setContent {
                DesktopExplorationScreen(
                    services = services,
                    onEnterCombat = {},
                    onOpenHub = {},
                    onOpenFieldKit = {},
                    onOpenFishing = {},
                    onOpenArcade = {},
                    onReturnToMenu = {}
                )
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_3panel_exploration.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
