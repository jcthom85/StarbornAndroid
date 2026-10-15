package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopMenuReviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun fullPartyAndAllPagesRenderAtWideAndCompactSizes() {
        val directory = Files.createTempDirectory("starborn-menu-review-").toFile()
        val services = DesktopAppServices(directory)
        val tab = mutableStateOf(DesktopMenuTab.STATS)
        val compact = mutableStateOf(false)
        try {
            services.startNewGame()
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            services.sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
            services.inventoryService.addItem("mining_pistol", 1)
            services.inventoryService.addItem("medkit", 3)
            services.inventoryService.addItem("scrap_metal", 5)
            compose.setContent { DesktopStarbornTheme(services) {
                key(tab.value, compact.value) { Box(Modifier.size(if (compact.value) 800.dp else 1280.dp, if (compact.value) 600.dp else 900.dp)) {
                    DesktopFieldMenuContent(services, tab.value, "Nova's Bunk", {}, {}, {})
                } }
            } }
            compose.waitUntil(20000) { !services.exploration.uiState.value.isLoading }
            for (small in listOf(false, true)) for (page in DesktopMenuTab.entries) {
                compose.runOnIdle { compact.value = small; tab.value = page }
                compose.waitForIdle()
                val file = File("build/reports/desktop/screenshots/menu-review/${page.name.lowercase()}-${if (small) "compact" else "wide"}.png").apply { parentFile.mkdirs() }
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
                if (page == DesktopMenuTab.STATS) {
                    compose.onAllNodesWithText("Skills").onFirst().performClick()
                    compose.waitUntil(5000) { services.exploration.uiState.value.skillTreeOverlay != null }
                    ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(file.parentFile, "skills-${if (small) "compact" else "wide"}.png"))
                    compose.onAllNodesWithText("Zeke").onFirst().performClick()
                    compose.waitUntil(5000) { services.exploration.uiState.value.skillTreeOverlay?.characterId == "zeke" }
                    compose.runOnIdle { services.exploration.closeSkillTreeOverlay() }
                }
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
