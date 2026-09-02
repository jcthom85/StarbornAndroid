package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopFieldMenuContent
import com.example.starborn.desktop.ui.DesktopMenuTab
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopMenuSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureFieldMenuAndTabsSnapshots() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_menu_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()
            val populatedState = services.sessionStore.state.value.copy(
                inventory = mapOf(
                    "mining_pistol" to 1,
                    "flux_liner" to 1,
                    "cryo_inductor" to 1,
                    "scrap_metal" to 4,
                    "ration_pack" to 2,
                    "repair_patch" to 3
                )
            )
            services.sessionStore.restore(populatedState)

            composeTestRule.setContent {
                DesktopFieldMenuContent(
                    services = services,
                    initialTab = DesktopMenuTab.INVENTORY,
                    currentRoomTitle = "Pod Row",
                    onOpenFieldKit = {},
                    onReturnToTitle = {},
                    onDismiss = {}
                )
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_field_menu.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
