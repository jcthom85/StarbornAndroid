package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.map.drawNodeExitMarker as sharedDrawNodeExitMarker
import com.example.starborn.ui.map.drawFullMapNodeExits as sharedDrawFullMapNodeExits

internal val NodeExitColor = com.example.starborn.ui.map.NodeExitColor
internal fun nodeExitDescription(cells: List<MinimapCellUi>) = com.example.starborn.ui.map.nodeExitDescription(cells)
internal fun nodeExitVector(direction: String) = com.example.starborn.ui.map.nodeExitVector(direction)
internal fun DrawScope.drawNodeExitMarker(center: Offset, exit: MapNodeExitUi, radius: Float, length: Float) = sharedDrawNodeExitMarker(center, exit, radius, length)
internal fun DrawScope.drawFullMapNodeExits(cells: List<MinimapCellUi>, centerOf: (MinimapCellUi) -> Offset, cellSize: Float, textMeasurer: TextMeasurer) = sharedDrawFullMapNodeExits(cells, centerOf, cellSize, textMeasurer)
