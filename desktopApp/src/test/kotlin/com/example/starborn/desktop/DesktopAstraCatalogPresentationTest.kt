package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.exploration.presentation.AstraCatalog
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopAstraCatalogPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun simulationsDisplayOriginalTitlesDescriptionsAndFinalProgram() {
        val directory = Files.createTempDirectory("starborn-simulation-catalog-").toFile()
        val services = DesktopAppServices(directory)
        try {
            compose.setContent { DesktopStarbornTheme {
                DesktopRuntimeOverlays(services, services.exploration, ExplorationUiState(isSimulationDeckVisible = true))
            } }
            val first = AstraCatalog.simulations.first()
            compose.onNodeWithText(first.title).assertExists()
            compose.onNodeWithText(first.description).assertExists()
            capture("simulation-original-catalog.png")
            val last = AstraCatalog.simulations.last()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(last.title))
            compose.onNodeWithText(last.title).assertExists()
            compose.onNodeWithText(last.description).assertExists()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun filmsDisplayOriginalTitlesLocationsAndItemDescriptions() {
        val directory = Files.createTempDirectory("starborn-film-catalog-").toFile()
        val services = DesktopAppServices(directory)
        try {
            compose.setContent { DesktopStarbornTheme {
                DesktopRuntimeOverlays(services, services.exploration, ExplorationUiState(isTapeDeckVisible = true))
            } }
            val first = AstraCatalog.films.first()
            compose.onNodeWithText(first.third.first).assertExists().assertIsNotEnabled()
            compose.onNodeWithText(first.third.second).assertExists()
            compose.onNodeWithText(requireNotNull(services.itemRepository.findItem(first.first)?.description)).assertExists()
            capture("films-original-catalog.png")
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(AstraCatalog.films.last().third.first))
            compose.onNodeWithText(AstraCatalog.films.last().third.first).assertExists()
        } finally { services.close(); directory.deleteRecursively() }
    }

    private fun capture(name: String) {
        val output = File("build/reports/desktop/screenshots").apply { mkdirs() }
        ImageIO.write(compose.onAllNodes(isRoot()).onLast().captureToImage().asAwtImage(), "png", File(output, name))
    }
}
