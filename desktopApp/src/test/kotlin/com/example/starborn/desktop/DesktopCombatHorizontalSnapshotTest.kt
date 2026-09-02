package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopCombatScreen
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopCombatHorizontalSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktopHorizontalCombat() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_combat_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                DesktopCombatScreen(
                    services = services,
                    enemyIds = listOf("faulted_loader", "resonance_buoy", "echo_borer", "resonance_buoy"),
                    onVictory = {},
                    onDefeat = {},
                    onFlee = {}
                )
            }
            composeTestRule.mainClock.advanceTimeBy(100)

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_horizontal_combat.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
