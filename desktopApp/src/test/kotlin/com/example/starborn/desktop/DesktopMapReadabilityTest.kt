package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.map.AreaMapCanvas
import com.example.starborn.ui.map.mapRoomDescription
import com.example.starborn.ui.map.mapRoomHasExit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopMapReadabilityTest {
    @get:Rule val compose = createComposeRule()

    private fun denseNeighborhood(): List<MinimapCellUi> = (-2..2).flatMap { y ->
        (-2..2).map { x ->
            MinimapCellUi(
                roomId = "$x,$y", title = "Room $x,$y", gridX = x, gridY = y,
                offsetX = x, offsetY = y, visited = x <= 0, discovered = true,
                isCurrent = x == 0 && y == 0, hasEnemies = x == 1 || (x == -1 && y == 1),
                blockedDirections = if (y == 0 && x == 0) setOf("east") else emptySet(),
                connections = buildMap {
                    if (x < 2) put("east", "${x+1},$y")
                    if (x > -2) put("west", "${x-1},$y")
                    if (y < 2) put("north", "$x,${y+1}")
                    if (y > -2) put("south", "$x,${y-1}")
                },
                services = MinimapService.entries.toSet(),
                isDark = x == -2 && y == -1, isPreview = x == 2 && y == 1,
                hubExit = if (x == -1 && y == 0) MapHubExitUi("Homestead", blocked = true) else null,
                nodeExits = if (x == 0 && y == 2) listOf(
                    MapNodeExitUi("north", "next", "Next area", false),
                    MapNodeExitUi("east", "upper", "Upper area", true)
                ) else emptyList()
            )
        }
    }

    @Test fun minimapHidesServicesWhileFullMapRetainsThemAndRoomDetails() {
        val original = denseNeighborhood()
        var cells by mutableStateOf(original)
        compose.setContent {
            DesktopStarbornTheme {
                Row(Modifier.size(840.dp, 430.dp).testTag("preview"), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Android minimap · 78 dp")
                        AreaMapCanvas(cells, Modifier.size(78.dp).testTag("small"), minimap = true)
                        Text("Desktop minimap · 164 dp")
                        AreaMapCanvas(cells, Modifier.size(164.dp).testTag("large"), minimap = true)
                    }
                    Column {
                        Text("Full map · all services")
                        AreaMapCanvas(cells, Modifier.size(560.dp, 390.dp).testTag("full"))
                    }
                }
            }
        }
        val small = compose.onNodeWithTag("small").captureToImage().asAwtImage()
        val large = compose.onNodeWithTag("large").captureToImage().asAwtImage()
        val full = compose.onNodeWithTag("full").captureToImage().asAwtImage()
        val output = File("build/reports/map-readability").apply { mkdirs() }
        ImageIO.write(compose.onNodeWithTag("preview").captureToImage().asAwtImage(), "png", File(output, "map-overview.png"))
        ImageIO.write(small, "png", File(output, "minimap-78dp.png"))
        ImageIO.write(large, "png", File(output, "minimap-164dp.png"))
        compose.runOnIdle { cells = original.map { it.copy(services = emptySet()) } }
        compose.waitForIdle()
        fun pixels(node: String) = compose.onNodeWithTag(node).captureToImage().asAwtImage().let {
            it.getRGB(0, 0, it.width, it.height, null, 0, it.width)
        }
        assertArrayEquals("Services must not add visual clutter to the small minimap",
            small.getRGB(0, 0, small.width, small.height, null, 0, small.width), pixels("small"))
        assertArrayEquals("The desktop minimap uses the same service policy",
            large.getRGB(0, 0, large.width, large.height, null, 0, large.width), pixels("large"))
        assertFalse("Full map must still display services",
            full.getRGB(0, 0, full.width, full.height, null, 0, full.width).contentEquals(pixels("full")))
        val exit = original.first { it.hubExit != null }
        assertTrue(mapRoomHasExit(exit))
        assertFalse(mapRoomHasExit(exit.copy(isDark = true)))
        assertFalse(mapRoomHasExit(exit.copy(isPreview = true)))
        val description = mapRoomDescription(exit)
        listOf("Shop", "Rest", "Cooking", "Tinkering", "Exit to Homestead (blocked)").forEach {
            assertTrue("Room details retain $it", description.contains(it))
        }
    }
}
