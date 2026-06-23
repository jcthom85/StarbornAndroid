package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorUi
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorStatus
import java.util.Locale

@Composable
fun DirectionIndicatorsOverlay(
    indicators: Map<String, DirectionIndicatorUi>,
    onTravel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (indicators.isEmpty()) return
    Box(modifier = modifier) {
        indicators.values.forEach { indicator ->
            val direction = indicator.direction.lowercase(Locale.getDefault())
            val (alignment, padding) = when (direction) {
                "north" -> Alignment.TopCenter to PaddingValues(top = 112.dp)
                "south" -> Alignment.BottomCenter to PaddingValues(bottom = 4.dp)
                "east" -> Alignment.CenterEnd to PaddingValues(end = 4.dp)
                "west" -> Alignment.CenterStart to PaddingValues(start = 4.dp)
                "northeast" -> Alignment.TopEnd to PaddingValues(top = 112.dp, end = 4.dp)
                "southeast" -> Alignment.BottomEnd to PaddingValues(bottom = 4.dp, end = 4.dp)
                "southwest" -> Alignment.BottomStart to PaddingValues(bottom = 4.dp, start = 4.dp)
                "northwest" -> Alignment.TopStart to PaddingValues(top = 112.dp, start = 4.dp)
                else -> Alignment.Center to PaddingValues(0.dp)
            }
            val loop = rememberInfiniteTransition(label = "dirPulse-$direction")
            val pulse by loop.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (indicator.status == DirectionIndicatorStatus.ENEMY) 900 else 1200,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse-$direction"
            )
            val baseAlphaRange = if (indicator.status == DirectionIndicatorStatus.ENEMY) 0.55f..0.98f else 0.65f..0.95f
            val alpha = lerp(baseAlphaRange.start, baseAlphaRange.endInclusive, pulse)
            val scale = 1f + 0.08f * pulse
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(padding)
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    }
                    .semantics {
                        contentDescription = when (indicator.status) {
                            DirectionIndicatorStatus.UNEXPLORED -> "Travel $direction"
                            DirectionIndicatorStatus.EXPLORED -> "Travel $direction"
                            DirectionIndicatorStatus.LOCKED -> "$direction exit locked"
                            DirectionIndicatorStatus.ENEMY -> "$direction exit blocked by enemy"
                            DirectionIndicatorStatus.NEARBY_THREAT -> "Threat nearby in $direction direction"
                        }
                    }
                    .clickable(
                        enabled = indicator.status == DirectionIndicatorStatus.UNEXPLORED || indicator.status == DirectionIndicatorStatus.EXPLORED,
                        onClick = { onTravel(indicator.direction) }
                    )
            ) {
                DirectionIndicatorIcon(
                    status = indicator.status,
                    direction = direction
                )
            }
        }
    }
}

@Composable
private fun DirectionIndicatorIcon(
    status: DirectionIndicatorStatus,
    direction: String
) {
    val baseSize = 36.dp
    val color = when (status) {
        DirectionIndicatorStatus.UNEXPLORED -> Color(0.56f, 0.78f, 1.0f)
        DirectionIndicatorStatus.EXPLORED -> Color(0.48f, 0.62f, 0.78f)
        DirectionIndicatorStatus.LOCKED -> Color(0.70f, 0.72f, 0.80f)
        DirectionIndicatorStatus.ENEMY -> Color(1.0f, 0.32f, 0.32f)
        DirectionIndicatorStatus.NEARBY_THREAT -> Color(1.0f, 0.5f, 0.2f)
    }
    when (status) {
        DirectionIndicatorStatus.UNEXPLORED -> DirectionArrowIndicator(direction, baseSize, color, isExplored = false)
        DirectionIndicatorStatus.EXPLORED -> DirectionArrowIndicator(direction, baseSize, color, isExplored = true)
        DirectionIndicatorStatus.LOCKED -> DirectionLockIndicator(direction, baseSize, color)
        DirectionIndicatorStatus.ENEMY -> DirectionEnemyIndicator(direction, baseSize, color)
        DirectionIndicatorStatus.NEARBY_THREAT -> DirectionEnemyIndicator(direction, baseSize, color)
    }
}

@Composable
private fun DirectionArrowIndicator(direction: String, size: Dp, color: Color, isExplored: Boolean = false) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "northeast" -> 45f
        "east" -> 90f
        "southeast" -> 135f
        "south" -> 180f
        "southwest" -> 225f
        "west" -> 270f
        "northwest" -> 315f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val inset = w * 0.28f
        val tipY = inset
        val baseY = w - inset
        val midX = w / 2f
        val wing = w * 0.26f
        val path = Path().apply {
            moveTo(midX, tipY)
            lineTo(midX + wing, baseY)
            lineTo(midX, baseY - wing * 0.8f)
            lineTo(midX - wing, baseY)
            close()
        }
        if (isExplored) {
            drawPath(path, color = color.copy(alpha = 0.3f))
            drawPath(path, color = color.copy(alpha = 0.8f), style = Stroke(width = w * 0.05f))
        } else {
            // Glow
            drawCircle(color = color.copy(alpha = 0.25f), radius = w * 0.55f, center = Offset(midX, midX))
            drawCircle(color = color.copy(alpha = 0.35f), radius = w * 0.42f, center = Offset(midX, midX))
            drawPath(path, color = color)
            drawPath(path, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = w * 0.05f))
        }
    }
}

@Composable
private fun DirectionLockIndicator(direction: String, size: Dp, color: Color) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "northeast" -> 45f
        "east" -> 90f
        "southeast" -> 135f
        "south" -> 180f
        "southwest" -> 225f
        "west" -> 270f
        "northwest" -> 315f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val bodyWidth = w * 0.62f
        val bodyHeight = w * 0.44f
        val bodyTop = w * 0.48f
        val bodyLeft = (w - bodyWidth) / 2f
        val corner = CornerRadius(w * 0.12f, w * 0.12f)

        // Glow
        drawCircle(color = color.copy(alpha = 0.2f), radius = w * 0.52f, center = Offset(w / 2f, w / 2f))
        drawCircle(color = color.copy(alpha = 0.3f), radius = w * 0.38f, center = Offset(w / 2f, w / 2f))

        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner,
            style = Stroke(width = w * 0.05f)
        )

        // Shackle
        val shackleThickness = w * 0.12f
        val shackleWidth = w * 0.6f
        val shackleHeight = w * 0.46f
        val shackleLeft = (w - shackleWidth) / 2f
        val shackleTop = w * 0.06f
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(shackleLeft, shackleTop),
            size = Size(shackleWidth, shackleHeight),
            style = Stroke(width = shackleThickness, cap = StrokeCap.Round)
        )

        // Keyhole
        val keyholeRadius = w * 0.075f
        val keyholeCenter = Offset(w / 2f, bodyTop + bodyHeight * 0.32f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.7f),
            radius = keyholeRadius,
            center = keyholeCenter
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(keyholeCenter.x - keyholeRadius * 0.45f, keyholeCenter.y - keyholeRadius * 0.05f),
            size = Size(keyholeRadius * 0.9f, keyholeRadius * 1.6f),
            cornerRadius = CornerRadius(keyholeRadius * 0.4f, keyholeRadius * 0.4f)
        )
    }
}

@Composable
private fun DirectionEnemyIndicator(direction: String, size: Dp, color: Color) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "northeast" -> 45f
        "east" -> 90f
        "southeast" -> 135f
        "south" -> 180f
        "southwest" -> 225f
        "west" -> 270f
        "northwest" -> 315f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val center = Offset(w / 2f, w / 2f)
        val radius = w * 0.36f
        val diamond = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }

        // Glow halo
        drawCircle(color = color.copy(alpha = 0.22f), radius = w * 0.55f, center = center)
        drawCircle(color = color.copy(alpha = 0.32f), radius = w * 0.42f, center = center)

        // Core diamond
        drawPath(diamond, color = color.copy(alpha = 0.92f))
        drawPath(diamond, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = w * 0.05f))

        // Inner bevel
        drawPath(diamond, color = Color.White.copy(alpha = 0.08f), style = Stroke(width = w * 0.14f))

        // Exclamation mark
        val barHeight = radius * 0.95f
        val barWidth = w * 0.09f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(center.x - barWidth / 2f, center.y - barHeight * 0.55f),
            size = Size(barWidth, barHeight * 0.7f),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
        drawCircle(
            color = Color.White,
            radius = barWidth * 0.65f,
            center = Offset(center.x, center.y + barHeight * 0.35f)
        )
    }
}
