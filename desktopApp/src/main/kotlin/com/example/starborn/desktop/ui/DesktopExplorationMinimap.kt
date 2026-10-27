package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.map.*
import kotlin.math.abs

/** Desktop frame and connections, Android's centered pips, crosshair and exit glyphs. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopExplorationMinimap(state: MinimapUiState?, enabled: Boolean, obscured: Boolean = false, roomName: (String) -> String = { it }, onOpen: () -> Unit) {
    val cells = state?.cells.orEmpty().filter { abs(it.offsetX) <= 2 && abs(it.offsetY) <= 2 && (it.discovered || it.visited || it.isPreview || it.isCurrent) }
    if (cells.isEmpty() && !obscured) return
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var hoverLabel by remember(state, obscured) { mutableStateOf(if (obscured) "Minimap obscured by darkness" else "Open full map") }
    TooltipArea(tooltip = { Surface(color = Color(0xFF07111A), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = .4f))) { Text(hoverLabel, Modifier.padding(10.dp).widthIn(max = 260.dp), color = Color.White, style = MaterialTheme.typography.bodySmall) } }) {
    Column(Modifier.fillMaxWidth().explorationFeedback(enabled && !obscured).then(if (obscured) Modifier else Modifier.clickable(enabled = enabled, onClickLabel = "Open full map", onClick = onOpen))
        .clearAndSetSemantics { contentDescription = if (obscured) "Minimap obscured by darkness" else "Area minimap. " + cells.flatMap { it.nodeExits }.joinToString(". ") { "${it.direction} to ${it.destinationTitle}${if (it.blocked) ", blocked" else ""}" }.ifBlank { "Open full map" } }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AreaMapCanvas(cells, Modifier.fillMaxWidth().height(164.dp).onSizeChanged { canvasSize = it }
            .onPointerEvent(PointerEventType.Move) { event ->
                val p = event.changes.firstOrNull()?.position
                val step = minOf(canvasSize.width,canvasSize.height)/6.4f
                hoverLabel = if (obscured) "Minimap obscured by darkness" else p?.let { pointer ->
                    cells.minByOrNull { cell -> (pointer-Offset(canvasSize.width/2f+cell.offsetX*step,canvasSize.height/2f-cell.offsetY*step)).getDistance() }
                        ?.takeIf { cell -> (pointer-Offset(canvasSize.width/2f+cell.offsetX*step,canvasSize.height/2f-cell.offsetY*step)).getDistance() <= step*.45f }
                        ?.let(::mapRoomDescription)
                } ?: "Open full map"
            }.clip(RoundedCornerShape(10.dp)), minimap = true, obscured = obscured)
    }
    }
}
