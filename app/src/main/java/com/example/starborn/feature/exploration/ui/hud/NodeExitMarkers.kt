package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi

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

/** Labels avoid room tiles and each other; a leader preserves the association if moved. */
internal fun DrawScope.drawFullMapNodeExits(
    cells: List<MinimapCellUi>,
    centerOf: (MinimapCellUi) -> Offset,
    cellSize: Float,
    textMeasurer: TextMeasurer
) {
    val occupied = cells.map { cell ->
        val center = centerOf(cell)
        Rect(center - Offset(cellSize / 2, cellSize / 2), Size(cellSize, cellSize)).inflate(3.dp.toPx())
    }.toMutableList()
    cells.forEach { cell ->
        cell.nodeExits.forEach { exit ->
            val tip = drawNodeExitMarker(centerOf(cell), exit, cellSize * 0.55f, 12.dp.toPx())
            val direction = when (exit.direction) {
                "up" -> "Up · "
                "down" -> "Down · "
                else -> ""
            }
            val text = textMeasurer.measure(
                AnnotatedString(direction + exit.destinationTitle + if (exit.blocked) " (blocked)" else ""),
                style = TextStyle(color = NodeExitColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                constraints = Constraints(maxWidth = 128.dp.roundToPx())
            )
            val padding = 4.dp.toPx()
            val labelSize = Size(text.size.width + padding * 2, text.size.height + padding * 2)
            val v = nodeExitVector(exit.direction)
            val preferred = tip + v * (padding + labelSize.height / 2) - Offset(
                if (v.x < 0f) labelSize.width else if (v.x > 0f) 0f else labelSize.width / 2,
                labelSize.height / 2
            )
            // Try near the exit first, then the nearest free part of the canvas.
            val candidates = buildList {
                add(preferred)
                for (ring in 1..8) {
                    val distance = ring * 14.dp.toPx()
                    listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(1f, 0f), Offset(-1f, 0f),
                        Offset(1f, -1f), Offset(-1f, -1f), Offset(1f, 1f), Offset(-1f, 1f)).forEach {
                        add(preferred + it * distance)
                    }
                }
            }
            val bounds = candidates.map { point ->
                Rect(Offset(point.x.coerceIn(padding, (size.width - labelSize.width - padding).coerceAtLeast(padding)),
                    point.y.coerceIn(padding, (size.height - labelSize.height - padding).coerceAtLeast(padding))), labelSize)
            }
            val label = bounds.firstOrNull { candidate -> occupied.none { it.overlaps(candidate) } } ?: bounds.first()
            occupied.add(label.inflate(2.dp.toPx()))
            val anchor = Offset(tip.x.coerceIn(label.left, label.right), tip.y.coerceIn(label.top, label.bottom))
            drawLine(NodeExitColor.copy(alpha = 0.55f), tip, anchor, 1.dp.toPx())
            drawRoundRect(Color(0xFF061018).copy(alpha = 0.96f), label.topLeft, label.size, CornerRadius(padding))
            drawRoundRect(NodeExitColor.copy(alpha = 0.4f), label.topLeft, label.size, CornerRadius(padding), style = Stroke(1.dp.toPx()))
            drawText(text, topLeft = label.topLeft + Offset(padding, padding))
        }
    }
}
