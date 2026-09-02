package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopFieldMenuContent
import com.example.starborn.desktop.ui.DesktopMenuTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopTinkerSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTinkeringFabricatorAndCaptureSnapshot() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_tinker_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            // Seed inventory with materials required for Deep Mine Cabinet repair
            services.inventoryService.addItem("hyperion_logic_board", 1)
            services.inventoryService.addItem("scrap_metal", 5)
            services.inventoryService.addItem("wiring_bundle", 2)
            services.sessionStore.setInventory(services.inventoryService.snapshot())

            val recipes = services.craftingService.tinkeringRecipes
            assertTrue("Tinkering recipes must be loaded from recipes_tinkering.json", recipes.isNotEmpty())
            val deepMineRecipe = recipes.find { it.id == "repair_deep_mine_cabinet" }
            assertTrue("Deep Mine repair recipe should be present", deepMineRecipe != null)

            // Validate crafting check
            val canCraft = services.craftingService.canCraft(deepMineRecipe!!)
            assertTrue("Player should have enough materials to craft Deep Mine Cabinet", canCraft)

            // Render Tinker tab UI
            composeTestRule.setContent {
                DesktopFieldMenuContent(
                    services = services,
                    initialTab = DesktopMenuTab.FIELD_KIT,
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

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_tinker_menu.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
