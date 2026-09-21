package com.example.starborn

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.feature.exploration.ui.FullMapCanvas
import com.example.starborn.feature.exploration.ui.hud.MinimapWidget
import com.example.starborn.feature.exploration.viewmodel.FullMapUiState
import com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import org.junit.Rule
import org.junit.Test
import java.io.File

class MapExitRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exitsAreLabeledAndAccessibleOnBothMaps() {
        val cells = listOf(
            cell("center", 0, 0, mapOf("north" to "north", "east" to "east", "west" to "west", "south" to "south")),
            cell("north", 0, 1, mapOf("south" to "center"), MapNodeExitUi("north", "mine", "Deep Mine")),
            cell("east", 1, 0, mapOf("west" to "center"), MapNodeExitUi("east", "gate", "Dominion Checkpoint", true)),
            cell("west", -1, 0, mapOf("east" to "center"), MapNodeExitUi("west", "hidden", "Unexplored area")),
            cell("south", 0, -1, mapOf("north" to "center"), MapNodeExitUi("down", "lower", "Lower Deck"))
        )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(Modifier.fillMaxSize().background(Color(0xFF061018)).padding(16.dp)) {
                    Text("Area exits", color = Color.White)
                    FullMapCanvas(FullMapUiState(cells), modifier = Modifier.fillMaxWidth().height(300.dp).testTag("full"))
                    Text("Minimap", color = Color.White)
                    MinimapWidget(MinimapUiState(cells), {}, Modifier.size(140.dp).testTag("mini"))
                }
            }
        }
        compose.onNodeWithTag("full").assert(hasContentDescription("east to Dominion Checkpoint, blocked", substring = true))
        compose.onNodeWithTag("mini").assert(hasContentDescription("west to Unexplored area", substring = true))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "map-exit-preview.png")
        output.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun cell(id: String, x: Int, y: Int, connections: Map<String, String>, vararg exits: MapNodeExitUi) =
        MinimapCellUi(id, x, y, x, y, visited = true, discovered = true, isCurrent = id == "center",
            hasEnemies = false, blockedDirections = emptySet(), connections = connections, nodeExits = exits.toList())
}
