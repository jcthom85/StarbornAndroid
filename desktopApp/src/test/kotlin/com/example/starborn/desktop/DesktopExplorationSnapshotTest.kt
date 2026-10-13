package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopStarbornTheme
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

        val services = DesktopAppServices(saveDirectory = tempSaveDir)
        try {
            kotlinx.coroutines.runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
            }
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.setRoom("pit_nova_bunk")

            composeTestRule.setContent { DesktopStarbornTheme(services) { Box(Modifier.size(1280.dp, 800.dp)) {
                DesktopExplorationScreen(
                    services = services,
                    onEnterCombat = {},
                    onOpenHub = {},
                    onOpenFieldKit = {},
                    onOpenFishing = {},
                    onOpenArcade = {},
                    onReturnToMenu = {}
                )
            } } }
            composeTestRule.waitForIdle()

            val artifactsDir = File("build/reports/desktop/screenshots")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_3panel_exploration.png"))

        } finally {
            services.close()
            tempSaveDir.deleteRecursively()
        }
    }
}
