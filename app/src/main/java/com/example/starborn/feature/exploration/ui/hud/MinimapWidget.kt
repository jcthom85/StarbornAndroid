package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.animation.core.Animatable as CoreAnimatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.starborn.feature.exploration.ui.drawDarkRoomOverlay
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun MinimapWidget(
    minimap: MinimapUiState?,
    onLegend: () -> Unit,
    modifier: Modifier = Modifier,
    obscured: Boolean = false
) {
    val clrBackground = Color(0xFF061018).copy(alpha = 0.88f)
    val clrBorder = Color(0xFF7FE6FF).copy(alpha = 0.92f)
    val clrGrid = Color(0xFF7FE6FF).copy(alpha = 0.16f)
    val clrTile = Color(0xFF8FD9FF).copy(alpha = 0.72f)
    val clrTileGlow = Color(0xFFE8FCFF)
    val clrPreview = Color.White.copy(alpha = 0.38f)
    val clrPlayer = Color(0xFFFFC857)

    val playerPulse = remember { CoreAnimatable(1f) }
    LaunchedEffect(Unit) {
        playerPulse.animateTo(
            targetValue = 0.1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onLegend),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val base = min(w, h)
                val cx = w / 2
                val cy = h / 2

                val g = base / 11.5f
                val pad = g / 2.7f
                val step = g + pad
                val radius = base * 0.12f

                drawRoundRect(
                    color = clrBackground,
                    size = size,
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )

                val gridStartX = cx - 2.5f * step
                val gridStartY = cy - 2.5f * step
                for (i in 0..5) {
                    drawLine(
                        color = clrGrid,
                        start = Offset(gridStartX + i * step, gridStartY),
                        end = Offset(gridStartX + i * step, gridStartY + 5 * step),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = clrGrid,
                        start = Offset(gridStartX, gridStartY + i * step),
                        end = Offset(gridStartX + 5 * step, gridStartY + i * step),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawRoundRect(
                    color = clrBorder,
                    size = Size(w - 2, h - 2),
                    topLeft = Offset(1f, 1f),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 1.dp.toPx())
                )

                minimap?.let { state ->
                    val cellsInViewport = state.cells.filter {
                        abs(it.offsetX) <= 2 &&
                            abs(it.offsetY) <= 2 &&
                            (it.visited || it.discovered || it.isPreview || it.isCurrent)
                    }
                    val idToCell = state.cells.associateBy { it.roomId }

                    cellsInViewport.forEach { cell ->
                        for (direction in setOf("east", "north")) {
                            val connectedRoomId = cell.connections[direction]
                            if (connectedRoomId != null) {
                                val neighbor = idToCell[connectedRoomId]
                                if (neighbor != null &&
                                    abs(neighbor.offsetX) <= 2 &&
                                    abs(neighbor.offsetY) <= 2 &&
                                    (neighbor.visited || neighbor.discovered || neighbor.isPreview || neighbor.isCurrent)
                                ) {
                                    val x1 = cx + cell.offsetX * step
                                    val y1 = cy - cell.offsetY * step
                                    val x2 = cx + neighbor.offsetX * step
                                    val y2 = cy - neighbor.offsetY * step
                                    val lineColor = if (cell.isPreview || neighbor.isPreview) {
                                        clrPreview.copy(alpha = 0.5f)
                                    } else {
                                        clrBorder.copy(alpha = 0.42f)
                                    }
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = 1.5f.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    cellsInViewport.forEach { cell ->
                        val px = cx + cell.offsetX * step
                        val py = cy - cell.offsetY * step
                        val center = Offset(px, py)

                        val isCurrent = cell.isCurrent
                        val baseColor = when {
                            isCurrent -> clrTileGlow
                            cell.isPreview -> clrPreview
                            else -> clrTile
                        }
                        val pipColor = baseColor.copy(alpha = if (cell.isDark) baseColor.alpha * 0.6f else baseColor.alpha)
                        val pipSize = g * if (isCurrent) 0.95f else 0.58f

                        drawCircle(
                            color = pipColor,
                            radius = pipSize / 2,
                            center = center
                        )

                        if (cell.isDark) {
                            val overlaySize = Size(pipSize * 1.05f, pipSize * 1.05f)
                            val topLeft = Offset(
                                x = center.x - overlaySize.width / 2f,
                                y = center.y - overlaySize.height / 2f
                            )
                            drawDarkRoomOverlay(
                                topLeft = topLeft,
                                size = overlaySize,
                                cornerRadius = CornerRadius(overlaySize.width * 0.35f, overlaySize.height * 0.35f),
                                overlayColor = Color.Black.copy(alpha = 0.75f),
                                hatchColor = Color.White.copy(alpha = 0.08f),
                                hatchSpacing = overlaySize.width / 4f
                            )
                        }
                    }
                }

                val playerSize = g * 0.6f
                val glowSize = playerSize * (1 + playerPulse.value * 0.5f)
                drawCircle(
                    color = clrPlayer.copy(alpha = 0.3f * (1 - playerPulse.value)),
                    radius = glowSize / 2,
                    center = Offset(cx, cy)
                )

                val hairLength = playerSize / 2
                val lineWidth = 2.dp.toPx()
                drawLine(clrPlayer, Offset(cx - hairLength, cy), Offset(cx + hairLength, cy), strokeWidth = lineWidth)
                drawLine(clrPlayer, Offset(cx, cy - hairLength), Offset(cx, cy + hairLength), strokeWidth = lineWidth)
            }
            if (obscured) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0f, 0f, 0f, 0.96f),
                                Color(0.03f, 0.02f, 0.08f, 0.9f),
                                Color(0f, 0f, 0f, 0.94f)
                            )
                        )
                    )
                    val hatchSpacing = size.minDimension / 9f
                    var offset = -size.height
                    while (offset < size.width + size.height) {
                        drawLine(
                            color = Color(0.07f, 0.05f, 0.12f, 0.35f),
                            start = Offset(offset, 0f),
                            end = Offset(offset + size.height * 1.4f, size.height),
                            strokeWidth = 2f
                        )
                        offset += hatchSpacing
                    }
                }
            }
        }
    }
}
