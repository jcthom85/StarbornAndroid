package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
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
                    zoneId = "glow_moss_cavern",
                    onClose = {}
                )
            }
            composeTestRule.mainClock.advanceTimeBy(300)

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_fishing_preview.png"))
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
