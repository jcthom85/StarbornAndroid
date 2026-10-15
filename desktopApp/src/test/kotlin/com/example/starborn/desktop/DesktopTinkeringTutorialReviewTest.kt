package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.crafting.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopTinkeringTutorialReviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun tutorialInstructionsFollowWorkbenchControls() {
        val directory = Files.createTempDirectory("starborn-tinker-tutorial-").toFile()
        val services = DesktopAppServices(directory)
        try {
            services.startNewGame()
            services.inventoryService.addItem("cryo_inductor", 1)
            services.inventoryService.addItem("scrap_metal", 3)
            services.sessionStore.restore(services.sessionStore.state.value.copy(completedMilestones = services.sessionStore.state.value.completedMilestones + "ms_w1_mq01_workshop_briefed"))
            compose.setContent { DesktopStarbornTheme(services) { DesktopFieldMenuContent(services, DesktopMenuTab.FIELD_KIT, "Workshop", {}, {}, {}) } }
            fun capture(step: TinkeringTutorialStep) {
                val copy = tinkeringTutorialInstruction(step)
                compose.waitUntil(10000) { compose.onAllNodesWithText(copy).fetchSemanticsNodes().isNotEmpty() }
                val file = File("build/reports/desktop/screenshots/menu-review/tutorial-${step.name.lowercase()}.png").apply { parentFile.mkdirs() }
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
            }
            capture(TinkeringTutorialStep.SLOT_BASE)
            compose.onAllNodesWithText(services.itemRepository.findItem("cryo_inductor")!!.name).onFirst().performClick()
            capture(TinkeringTutorialStep.SLOT_COMPONENT)
            compose.onAllNodesWithText(services.itemRepository.findItem("scrap_metal")!!.name).onFirst().performClick()
            capture(TinkeringTutorialStep.SYNTHESIZE)
            compose.onNodeWithTag("tinker-tutorial-result").assertIsDisplayed()
            compose.onNodeWithText("Synthesize").assertIsDisplayed().assertIsEnabled()
        } finally { services.close(); directory.deleteRecursively() }
    }
}
