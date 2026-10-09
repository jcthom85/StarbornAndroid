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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.exploration.viewmodel.*
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
        .semantics { contentDescription = if (obscured) "Minimap obscured by darkness" else "Area minimap. " + cells.flatMap { it.nodeExits }.joinToString(". ") { "${it.direction} to ${it.destinationTitle}${if (it.blocked) ", blocked" else ""}" } }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(164.dp).onSizeChanged { canvasSize = it }.onPointerEvent(PointerEventType.Move) { event ->
            if (!obscured) {
                val pointer = event.changes.firstOrNull()?.position
                val step = minOf(canvasSize.width, canvasSize.height) / 4.8f
                val nearest = pointer?.let { position -> cells.minByOrNull { cell ->
                    (position - Offset(canvasSize.width / 2f + cell.offsetX * step, canvasSize.height / 2f - cell.offsetY * step)).getDistance()
                }?.takeIf { cell -> (position - Offset(canvasSize.width / 2f + cell.offsetX * step, canvasSize.height / 2f - cell.offsetY * step)).getDistance() <= step * .45f } }
                hoverLabel = nearest?.let { cell ->
                    if (cell.isDark) "Dark room" else buildList {
                        add(if (cell.visited || cell.isCurrent) roomName(cell.roomId) else "Unexplored room")
                        if (cell.isCurrent) add("You are here")
                        if (cell.hasEnemies) add("Enemies present")
                        addAll(cell.services.map { when(it) { MinimapService.SHOP -> "Shop"; MinimapService.COOKING -> "Cooking"; MinimapService.TINKERING -> "Tinkering"; MinimapService.EXIT -> "Exit" } })
                        addAll(cell.nodeExits.map { "${it.direction}: ${it.destinationTitle}${if (it.blocked) " (blocked)" else ""}" })
                        if (cell.blockedDirections.isNotEmpty()) add("Blocked: ${cell.blockedDirections.joinToString()}")
                    }.joinToString("\n")
                } ?: "Open full map"
            }
        }.clip(RoundedCornerShape(10.dp)).background(Color(0xFF061018))) {
            val cyan = Color(0xFF7FE6FF)
            val gold = Color(0xFFFFC857)
            val center = Offset(size.width / 2, size.height / 2)
            val step = size.minDimension / 4.8f
            fun point(cell: MinimapCellUi) = center + Offset(cell.offsetX * step, -cell.offsetY * step)
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .04f), Color.Transparent, Color.Black.copy(alpha = .16f))))
            var x = center.x - step / 2
            while (x >= 0) { drawLine(cyan.copy(alpha = .055f), Offset(x, 0f), Offset(x, size.height)); x -= step }
            x = center.x + step / 2
            while (x <= size.width) { drawLine(cyan.copy(alpha = .055f), Offset(x, 0f), Offset(x, size.height)); x += step }
            var y = center.y - step / 2
            while (y >= 0) { drawLine(cyan.copy(alpha = .055f), Offset(0f, y), Offset(size.width, y)); y -= step }
            y = center.y + step / 2
            while (y <= size.height) { drawLine(cyan.copy(alpha = .055f), Offset(0f, y), Offset(size.width, y)); y += step }
            if (obscured) {
                // No room topology is drawn: interference preserves the HUD without revealing it.
                drawRect(Brush.radialGradient(listOf(cyan.copy(alpha = .07f), Color(0xFF03080D).copy(alpha = .88f)), center, size.maxDimension * .65f))
                val random = kotlin.random.Random(37)
                repeat(380) {
                    val p = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                    drawCircle(cyan.copy(alpha = random.nextFloat() * .09f), .6.dp.toPx(), p)
                }
                var scan = 0f
                while (scan < size.height) {
                    drawLine(Color.Black.copy(alpha = .22f), Offset(0f, scan), Offset(size.width, scan), 1.dp.toPx())
                    scan += 4.dp.toPx()
                }
                val r = 13.dp.toPx()
                drawCircle(cyan.copy(alpha = .18f), r, center, style = Stroke(1.dp.toPx()))
                drawLine(cyan.copy(alpha = .3f), center + Offset(-r * .7f, r * .7f), center + Offset(r * .7f, -r * .7f), 1.5.dp.toPx())
                return@Canvas
            }
            val known = cells.associateBy { it.roomId }
            val edges = mutableSetOf<Set<String>>()
            cells.forEach { cell -> cell.connections.forEach { (direction, id) -> known[id]?.let { other ->
                if (edges.add(setOf(cell.roomId, id))) {
                    val locked = direction in cell.blockedDirections || other.connections.any { (reverse, target) -> target == cell.roomId && reverse in other.blockedDirections }
                    val from = point(cell); val to = point(other)
                    drawLine(if (locked) gold.copy(alpha = .6f) else cyan.copy(alpha = if (cell.isPreview || other.isPreview) .22f else .38f), from, to, 1.5.dp.toPx(), StrokeCap.Round)
                    if (locked) {
                        val middle = (from + to) / 2f
                        drawRoundRect(gold, middle - Offset(2.dp.toPx(), 1.dp.toPx()), Size(4.dp.toPx(), 4.dp.toPx()), CornerRadius(1.dp.toPx()))
                        drawArc(gold, 180f, 180f, false, middle - Offset(2.dp.toPx(), 3.dp.toPx()), Size(4.dp.toPx(), 4.dp.toPx()), style = Stroke(1.dp.toPx()))
                    }
                }
            } } }
            cells.forEach { cell ->
                val p = point(cell)
                val radius = step * if (cell.isCurrent) .27f else .17f
                val color = when { cell.isCurrent -> Color(0xFFE8FCFF); cell.isPreview -> Color.White.copy(alpha = .38f); cell.visited -> Color(0xFF8FD9FF).copy(alpha = .72f); else -> Color(0xFF8FD9FF).copy(alpha = .42f) }
                if (cell.isCurrent) drawCircle(gold.copy(alpha = .10f), radius * 1.8f, p)
                drawCircle(color.copy(alpha = if (cell.isDark) color.alpha * .45f else color.alpha), radius, p)
                if (cell.isDark) drawLine(Color.Black.copy(alpha = .75f), p - Offset(radius * .6f, -radius * .6f), p + Offset(radius * .6f, -radius * .6f), 2.dp.toPx())
                else {
                    if (cell.hasEnemies) drawCircle(Color(0xFFFF746D), radius + 2.dp.toPx(), p, style = Stroke(1.3.dp.toPx()))
                    cell.services.filterNot { it == MinimapService.EXIT && cell.nodeExits.isNotEmpty() }.forEachIndexed { index, service ->
                        drawServiceGlyph(service, p.x + radius + 3.dp.toPx(), p.y - radius + index * 9.dp.toPx(), 3.5.dp.toPx())
                    }
                }
                cell.nodeExits.forEach { exit -> drawNodeExitMarker(p, exit, radius, step * .4f) }
            }
            val cross = step * .14f
            drawLine(gold, center - Offset(cross, 0f), center + Offset(cross, 0f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(gold, center - Offset(0f, cross), center + Offset(0f, cross), 2.dp.toPx(), StrokeCap.Round)
        }
    }
    }
}

private fun DrawScope.drawServiceGlyph(service: MinimapService, centerX: Float, centerY: Float, size: Float) {
    val center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    val color = minimapServiceColor(service)
    when (service) {
        MinimapService.SHOP -> {
            drawCircle(color, radius = size, center = center, style = Stroke(width = size * 0.6f))
            drawLine(
                color = color,
                start = center.copy(y = center.y - size * 0.6f),
                end = center.copy(y = center.y + size * 0.6f),
                strokeWidth = size * 0.3f
            )
        }
        MinimapService.COOKING -> {
            val path = Path().apply {
                moveTo(center.x - size, center.y + size)
                lineTo(center.x + size, center.y + size)
                lineTo(center.x, center.y - size)
                close()
            }
            drawPath(path, color)
        }
        MinimapService.TINKERING -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y)
                lineTo(center.x, center.y + size)
                lineTo(center.x - size, center.y)
                close()
            }
            drawPath(path, color, style = Stroke(width = size * 0.3f))
        }
        MinimapService.EXIT -> {
            val strokeWidth = size * 0.45f
            drawCircle(color, radius = size * 1.15f, center = center, style = Stroke(width = strokeWidth))
            drawLine(
                color = color,
                start = center.copy(y = center.y + size * 0.65f),
                end = center.copy(y = center.y - size * 0.65f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = center.copy(y = center.y - size * 0.65f),
                end = androidx.compose.ui.geometry.Offset(center.x - size * 0.45f, center.y - size * 0.15f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = center.copy(y = center.y - size * 0.65f),
                end = androidx.compose.ui.geometry.Offset(center.x + size * 0.45f, center.y - size * 0.15f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}


private fun minimapServiceColor(service: MinimapService): Color = when (service) {
    MinimapService.SHOP -> Color(0xFFFFC107)
    MinimapService.COOKING -> Color(0xFFFF8A65)
    MinimapService.TINKERING -> Color(0xFFBA68C8)
    MinimapService.EXIT -> Color(0xFF00E5FF)
}

internal val NodeExitColor = Color(0xFFFFC857)

internal fun nodeExitDescription(cells: List<MinimapCellUi>): String = cells.flatMap { it.nodeExits }
    .joinToString(". ") { "${it.direction} to ${it.destinationTitle}${if (it.blocked) ", blocked" else ""}" }

internal fun nodeExitVector(direction: String): Offset = when (direction) {
    "north", "up" -> Offset(0f, -1f)
    "south", "down" -> Offset(0f, 1f)
    "east" -> Offset(1f, 0f)
    "west" -> Offset(-1f, 0f)
    "northeast" -> Offset(0.7071f, -0.7071f)
    "northwest" -> Offset(-0.7071f, -0.7071f)
    "southeast" -> Offset(0.7071f, 0.7071f)
    "southwest" -> Offset(-0.7071f, 0.7071f)
    else -> Offset.Zero
}

/** A directional arrow, with a padlock for a blocked passage and double chevrons for stairs. */
internal fun DrawScope.drawNodeExitMarker(center: Offset, exit: MapNodeExitUi, radius: Float, length: Float): Offset {
    val vector = nodeExitVector(exit.direction)
    val normal = Offset(-vector.y, vector.x)
    val tip = center + vector * (radius + length)
    val start = center + vector * radius
    val color = if (exit.blocked) NodeExitColor.copy(alpha = 0.75f) else NodeExitColor
    val stroke = 1.5.dp.toPx()
    drawLine(color, start, tip, stroke, StrokeCap.Round)
    val wing = length * 0.35f
    fun chevron(point: Offset) {
        drawLine(color, point, point - vector * wing + normal * wing, stroke, StrokeCap.Round)
        drawLine(color, point, point - vector * wing - normal * wing, stroke, StrokeCap.Round)
    }
    chevron(tip)
    if (exit.direction == "up" || exit.direction == "down") chevron(tip - vector * (wing * 0.8f))
    if (exit.blocked) {
        val lockCenter = start + vector * (length * 0.35f) + normal * (wing + 2.dp.toPx())
        val lockSize = 4.dp.toPx()
        drawRect(Color(0xFF061018), lockCenter - Offset(lockSize, lockSize), Size(lockSize * 2, lockSize * 2))
        drawRoundRect(color, lockCenter - Offset(lockSize / 2, 0f), Size(lockSize, lockSize), CornerRadius(1f))
        drawArc(color, 180f, 180f, false, lockCenter - Offset(lockSize / 2, lockSize / 2),
            Size(lockSize, lockSize), style = Stroke(stroke))
    }
    return tip
}

