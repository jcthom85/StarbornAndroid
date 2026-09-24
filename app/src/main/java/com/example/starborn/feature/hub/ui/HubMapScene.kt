package com.example.starborn.feature.hub.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.starborn.feature.hub.viewmodel.HubNodeUi
import com.example.starborn.feature.hub.viewmodel.HubQuestUi
import kotlin.math.roundToInt
import kotlin.math.sin

/** The picture and all ground anchors share this single transform. Labels never move the map. */
@Composable
internal fun HubMapScene(
    hubId: String?, background: Painter, nodes: List<HubNodeUi>, selectedId: String?, trackedQuest: HubQuestUi?,
    onSelect: (HubNodeUi) -> Unit, onEnter: (HubNodeUi) -> Unit, modifier: Modifier = Modifier
) {
    val layout = HubMapLayouts.all[hubId] ?: return
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val intrinsic = background.intrinsicSize
        val aspect = if (intrinsic.width > 0 && intrinsic.height > 0) intrinsic.width / intrinsic.height else 9f / 16f
        val transform = HubMapTransform.cover(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), aspect)
        val mapWidth = with(density) { transform.width.toDp() }
        val viewportWidth = maxWidth
        val viewportWidthPx = constraints.maxWidth.toFloat()

        val infiniteTransition = rememberInfiniteTransition(label = "hub_map_motion")
        val idlePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "idle_phase"
        )
        val selectPulse by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "select_pulse"
        )

        Box(Modifier.fillMaxSize().clipToBounds()) {
            Image(background, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

            // --- Layer 1: Ground Contact Shadows & Selection Sonar Rings ---
            Canvas(Modifier.fillMaxSize()) {
                nodes.forEach { node ->
                    val site = (if (node.id == "astra_access") layout.astraDock else layout.sites[node.id]) ?: return@forEach
                    val selected = node.id == selectedId
                    val anchorX = transform.x(site.x)
                    val anchorY = transform.y(site.y)

                    if (site.artworkWidth > 0f) {
                        val imageWidthPx = (minOf(mapWidth * site.artworkWidth, viewportWidth * .36f)).toPx()
                        val isFloating = node.id == "astra_access" || node.id == "the_sky"
                        val shadowWidth = imageWidthPx * (if (node.id == "astra_access") 0.76f else 0.68f)
                        val shadowHeight = shadowWidth * 0.28f

                        val floatProgress = if (isFloating) sin(idlePhase + (node.id.hashCode() % 7)) else 0f
                        val shadowScale = if (isFloating) 1f - (floatProgress * 0.08f) else 1f
                        val shadowAlpha = (if (node.canEnter) 0.52f else 0.28f) * (if (isFloating) 0.9f - (floatProgress * 0.1f) else 1f)

                        // Ground contact shadow
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF02070D).copy(alpha = shadowAlpha),
                                    Color(0xFF02070D).copy(alpha = shadowAlpha * 0.55f),
                                    Color.Transparent
                                ),
                                center = Offset(anchorX, anchorY),
                                radius = (shadowWidth * shadowScale) / 2f
                            ),
                            topLeft = Offset(anchorX - (shadowWidth * shadowScale) / 2f, anchorY - (shadowHeight * shadowScale) / 2f),
                            size = Size(shadowWidth * shadowScale, shadowHeight * shadowScale)
                        )

                        // Selection Sonar Pulse on Ground
                        if (selected) {
                            val pulseRadius = (shadowWidth * 0.5f) + (shadowWidth * 0.35f * selectPulse)
                            val pulseHeight = pulseRadius * 0.28f
                            val pulseAlpha = (1f - selectPulse).coerceIn(0f, 1f) * 0.6f
                            drawOval(
                                color = Color(0xFFFFD477).copy(alpha = pulseAlpha),
                                topLeft = Offset(anchorX - pulseRadius, anchorY - pulseHeight),
                                size = Size(pulseRadius * 2f, pulseHeight * 2f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawOval(
                                color = Color(0xFFFFD477).copy(alpha = 0.35f),
                                topLeft = Offset(anchorX - shadowWidth * 0.52f, anchorY - shadowHeight * 0.55f),
                                size = Size(shadowWidth * 1.04f, shadowHeight * 1.1f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    } else {
                        // Beacon ground ring for pin nodes
                        if (selected) {
                            val pinRadius = 14.dp.toPx() + (10.dp.toPx() * selectPulse)
                            val pulseAlpha = (1f - selectPulse).coerceIn(0f, 1f) * 0.65f
                            drawCircle(
                                color = Color(0xFFFFD477).copy(alpha = pulseAlpha),
                                center = Offset(anchorX, anchorY),
                                radius = pinRadius,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            // --- Layer 2: Illustrated Node Sprites, Tactical Pins, & Callouts ---
            nodes.forEach { node ->
                val site = if (node.id == "astra_access") layout.astraDock else layout.sites[node.id]
                if (site == null) return@forEach
                val selected = node.id == selectedId
                val objective = trackedQuest?.let { nodeMatchesQuest(node, it) } == true
                val tint = when {
                    !node.canEnter -> Color(0xFFFFB394)
                    selected || objective -> Color(0xFFFFD477)
                    node.completed -> Color(0xFF9BDEC0)
                    else -> Color(0xFFBFEFFF)
                }
                val imageSize = minOf(mapWidth * site.artworkWidth, viewportWidth * .36f)
                val painter = if (site.artworkWidth > 0) rememberHubNodePainter(
                    if (node.id == "astra_access") "images/nodes/astra_ship_map_v2.webp" else node.iconPath
                ) else null
                val anchorX = transform.x(site.x)
                val anchorY = transform.y(site.y)

                if (site.labelDx != 0f || site.labelDy != 0f) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(tint.copy(alpha = .6f), Offset(anchorX, anchorY),
                            Offset(transform.x(site.x + site.labelDx), transform.y(site.y + site.labelDy) + 14.dp.toPx()),
                            strokeWidth = 1.dp.toPx())
                    }
                }

                if (painter != null) {
                    val isFloating = node.id == "astra_access" || node.id == "the_sky"
                    val floatOffsetPx = if (isFloating) {
                        sin(idlePhase.toDouble() + (node.id.hashCode() % 7)).toFloat() * with(density) { 3.dp.toPx() }
                    } else 0f

                    val targetScale = when {
                        selected -> 1.08f
                        selectedId != null -> 0.96f
                        else -> 1.0f
                    }
                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "node_scale_${node.id}"
                    )

                    val targetAlpha = when {
                        !node.canEnter -> 0.48f
                        selected -> 1f
                        selectedId != null -> 0.86f
                        else -> 1f
                    }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 220),
                        label = "node_alpha_${node.id}"
                    )

                    val anchorRatio = if (node.id == "astra_access") .79f else .94f

                    // Artwork bottoms meet the painted ground; labels are independent siblings.
                    Image(
                        painter = painter,
                        contentDescription = "Artwork: ${node.title}",
                        contentScale = ContentScale.Fit,
                        alpha = animatedAlpha,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (anchorX - with(density) { imageSize.toPx() } / 2).roundToInt(),
                                    (anchorY - with(density) { imageSize.toPx() } * anchorRatio).roundToInt()
                                )
                            }
                            .size(imageSize)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = floatOffsetPx
                                transformOrigin = TransformOrigin(0.5f, anchorRatio)
                            }
                            .zIndex(if (selected) 2f else 1f)
                            .pointerInput(node.id, node.canEnter) {
                                detectTapGestures(
                                    onTap = { onSelect(node) },
                                    onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) }
                                )
                            }
                    )
                } else {
                    val pinScale by animateFloatAsState(
                        targetValue = if (selected) 1.2f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "pin_scale_${node.id}"
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(0xE607141D),
                        border = BorderStroke(if (selected) 2.dp else 1.dp, tint),
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (anchorX - with(density) { 11.dp.toPx() }).roundToInt(),
                                    (anchorY - with(density) { 11.dp.toPx() }).roundToInt()
                                )
                            }
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = pinScale
                                scaleY = pinScale
                            }
                            .zIndex(if (selected) 2f else 1f)
                            .pointerInput(node.id, node.canEnter) {
                                detectTapGestures(
                                    onTap = { onSelect(node) },
                                    onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!node.canEnter || node.completed || objective) Icon(
                                if (!node.canEnter) Icons.Default.Lock else if (objective) Icons.Default.Star else Icons.Default.CheckCircle, null,
                                tint = tint, modifier = Modifier.size(13.dp))
                            else Box(Modifier.size(if (selected) 8.dp else 5.dp).background(tint, CircleShape))
                        }
                    }
                }

                // --- Layer 3: Independent Grounded Labels (Bounds strictly preserved) ---
                val labelWidth = minOf(104.dp, mapWidth * .32f)
                val labelX = (transform.x(site.x + site.labelDx) - with(density) { labelWidth.toPx() } / 2)
                    .coerceIn(0f, (viewportWidthPx - with(density) { labelWidth.toPx() }).coerceAtLeast(0f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .zIndex(3f)
                        .offset {
                            IntOffset(
                                labelX.roundToInt(),
                                (transform.y(site.y + site.labelDy) - with(density) { 12.dp.toPx() }).roundToInt()
                            )
                        }
                        .width(labelWidth)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Enter ${node.title}"
                            role = Role.Button
                            onClick(label = "Select ${node.title}") { onSelect(node); true }
                        }
                        .pointerInput(node.id, node.canEnter) {
                            detectTapGestures(
                                onTap = { onSelect(node) },
                                onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) }
                            )
                        }
                ) {
                    Spacer(Modifier.height(if (painter != null) 12.dp else 26.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xEF08121B),
                        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) .85f else .3f))
                    ) {
                        Text(
                            node.title,
                            color = tint,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

