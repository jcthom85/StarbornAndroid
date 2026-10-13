package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopVisualSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun capturePodRowAndJedBunkSnapshots() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        val services = DesktopAppServices(saveDirectory = tempSaveDir)
        try {
            kotlinx.coroutines.runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
            }
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.setRoom("pit_L2_corridor")

            // Capture the authored room layout, independently of time-based entry presentations.
            // The real opening-to-Pod-Row interaction is covered by DesktopOpeningAcceptanceTest.
            val room = services.roomDefinitions.getValue("pit_L2_corridor")
            val fixture = ExplorationUiState(currentRoom = room)
            composeTestRule.setContent { DesktopStarbornTheme(services) { Box(Modifier.size(1280.dp, 800.dp)) {
                DesktopPortraitBackdrop(rememberDesktopAssetPainter(room.backgroundImage, services.assetProvider), room.title) { layout ->
                    DesktopExplorationPanels(services, fixture, layout, room.description, null, false, false) {}
                }
            } } }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(room.title).assertIsDisplayed()

            val artifactsDir = File("build/reports/desktop/screenshots")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_pod_row.png"))

        } finally {
            services.close()
            tempSaveDir.deleteRecursively()
        }
    }
}
