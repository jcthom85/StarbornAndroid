package com.example.starborn.feature.hub.ui

import com.example.starborn.feature.hub.presentation.HubMapLayouts
import com.example.starborn.feature.hub.presentation.nodeMatchesQuest

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.feature.hub.viewmodel.HubNodeUi
import com.example.starborn.feature.hub.viewmodel.HubQuestUi
import kotlin.math.roundToInt
import kotlin.math.sin

/** The picture and all ground anchors share this single transform. Labels never move the map. */
@Composable
internal fun HubMapScene(
    hubId: String?,
    background: Painter,
    nodes: List<HubNodeUi>,
    selectedId: String?,
    trackedQuest: HubQuestUi?,
    onSelect: (HubNodeUi) -> Unit,
    onEnter: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier,
    newlyUnlockedNodeId: String? = null,
    visualTheme: HubVisualTheme = hubVisualTheme(hubId, null)
) {
    val layout = HubMapLayouts.all[hubId] ?: return
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val intrinsic = background.intrinsicSize
        val aspect = if (intrinsic.width > 0 && intrinsic.height > 0) intrinsic.width / intrinsic.height else 9f / 16f
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        val transform = HubMapTransform.fit(viewportWidthPx, viewportHeightPx, aspect)
        val coverTransform = HubMapTransform.cover(viewportWidthPx, viewportHeightPx, aspect)
        val mapWidth = with(density) { transform.width.toDp() }
        val coverWidth = with(density) { coverTransform.width.toDp() }
        val viewportWidth = maxWidth

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
            // Soft ambient cover echo fills any margin left by aspect-fitting the portrait map.
            Image(
                painter = background,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alpha = 0.22f,
                modifier = Modifier
                    .offset { IntOffset(coverTransform.offsetX.roundToInt(), coverTransform.offsetY.roundToInt()) }
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(coverWidth, with(density) { coverTransform.imageHeight.toDp() })
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(visualTheme.backdropColor.copy(alpha = 0.56f))
            )

            // Primary authored map artwork using the same transform as every destination anchor.
            Image(
                painter = background,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset { IntOffset(transform.offsetX.roundToInt(), transform.offsetY.roundToInt()) }
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(mapWidth, with(density) { transform.imageHeight.toDp() })
            )

            // Edge-feather vignette blends the fitted map perimeter into the viewport cleanly.
            Canvas(Modifier.fillMaxSize()) {
                val feather = 22.dp.toPx()
                val fadeBase = visualTheme.backdropColor
                val fitLeft = transform.offsetX
                val fitRight = transform.offsetX + transform.width
                if (fitLeft > 1f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(fadeBase.copy(alpha = 0.62f), Color.Transparent),
                            startX = fitLeft,
                            endX = fitLeft + feather
                        ),
                        topLeft = Offset(fitLeft, 0f),
                        size = Size(feather.coerceAtMost(size.width - fitLeft), size.height)
                    )
                }
                if (fitRight < size.width - 1f) {
                    val startX = (fitRight - feather).coerceAtLeast(0f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, fadeBase.copy(alpha = 0.62f)),
                            startX = startX,
                            endX = fitRight
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size((fitRight - startX).coerceAtLeast(0f), size.height)
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(fadeBase.copy(alpha = 0.58f), Color.Transparent),
                        startY = 0f,
                        endY = feather * 1.4f
                    ),
                    size = Size(size.width, feather * 1.4f)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, fadeBase.copy(alpha = 0.65f)),
                        startY = (size.height - feather * 1.4f).coerceAtLeast(0f),
                        endY = size.height
                    ),
                    topLeft = Offset(0f, (size.height - feather * 1.4f).coerceAtLeast(0f)),
                    size = Size(size.width, feather * 1.4f)
                )
            }

            // --- Layer 1: Ground Contact Shadows, Selection Aura & Objective Beacons ---
            Canvas(Modifier.fillMaxSize()) {
                nodes.forEach { node ->
                    val site = (if (node.id == "astra_access") layout.astraDock else layout.sites[node.id]) ?: return@forEach
                    val selected = node.id == selectedId
                    val objective = trackedQuest?.let { nodeMatchesQuest(node, it) } == true
                    val isNewlyUnlocked = node.id == newlyUnlockedNodeId
                    val usesSyntheticPosition = node.id == "astra_access" || node.id == "astra_disembark"
                    val nodeX = if (usesSyntheticPosition) site.x else node.centerX
                    val nodeY = if (usesSyntheticPosition) site.y else node.centerY
                    val anchorX = transform.x(nodeX)
                    val anchorY = transform.y(nodeY)
                    val highlightColor = when {
                        !node.canEnter -> Color(0xFFFFB394)
                        selected || objective -> Color(0xFFFFD477)
                        else -> visualTheme.accent
                    }

                    if (site.artworkWidth > 0f) {
                        val imageWidthPx = (minOf(mapWidth * site.artworkWidth, viewportWidth * .36f)).toPx()
                        val isFloating = node.id == "astra_access" || node.id == "the_sky"
                        val shadowWidth = imageWidthPx * (if (node.id == "astra_access") 0.76f else 0.68f)
                        val shadowHeight = shadowWidth * 0.28f

                        val floatProgress = if (isFloating) sin(idlePhase + (node.id.hashCode() % 7)) else 0f
                        val shadowScale = if (isFloating) 1f - (floatProgress * 0.08f) else 1f
                        val shadowAlpha = (if (node.canEnter) 0.56f else 0.28f) * (if (isFloating) 0.9f - (floatProgress * 0.1f) else 1f)

                        if (selected) {
                            val haloRadius = imageWidthPx * 0.62f
                            val haloCenterY = anchorY - imageWidthPx * 0.34f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        highlightColor.copy(alpha = 0.28f + 0.08f * (1f - selectPulse)),
                                        visualTheme.accent.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    center = Offset(anchorX, haloCenterY),
                                    radius = haloRadius
                                ),
                                center = Offset(anchorX, haloCenterY),
                                radius = haloRadius
                            )
                        }

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

                        // Selection / Objective / Unlock Ground Rings
                        if (selected || objective || isNewlyUnlocked) {
                            val ringColor = if (isNewlyUnlocked) visualTheme.accent else Color(0xFFFFD477)
                            val pulseRadius = (shadowWidth * 0.50f) + (shadowWidth * 0.36f * selectPulse)
                            val pulseHeight = pulseRadius * 0.28f
                            val baseAlpha = when {
                                selected || isNewlyUnlocked -> 0.68f
                                else -> 0.42f
                            }
                            val pulseAlpha = (1f - selectPulse).coerceIn(0f, 1f) * baseAlpha
                            drawOval(
                                color = ringColor.copy(alpha = pulseAlpha),
                                topLeft = Offset(anchorX - pulseRadius, anchorY - pulseHeight),
                                size = Size(pulseRadius * 2f, pulseHeight * 2f),
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                            if (selected) {
                                drawOval(
                                    color = highlightColor.copy(alpha = 0.48f),
                                    topLeft = Offset(anchorX - shadowWidth * 0.52f, anchorY - shadowHeight * 0.55f),
                                    size = Size(shadowWidth * 1.04f, shadowHeight * 1.1f),
                                    style = Stroke(width = 1.3.dp.toPx())
                                )
                            }
                        }
                    } else if (selected || objective || isNewlyUnlocked) {
                        val pinRadius = 14.dp.toPx() + (10.dp.toPx() * selectPulse)
                        val pulseAlpha = (1f - selectPulse).coerceIn(0f, 1f) * if (selected) 0.70f else 0.45f
                        drawCircle(
                            color = highlightColor.copy(alpha = pulseAlpha),
                            center = Offset(anchorX, anchorY),
                            radius = pinRadius,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }
            }

            // --- Layer 2: Illustrated Node Sprites, Tactical Pins, & Callouts ---
            nodes.forEach { node ->
                val site = if (node.id == "astra_access") layout.astraDock else layout.sites[node.id]
                if (site == null) return@forEach
                val usesSyntheticPosition = node.id == "astra_access" || node.id == "astra_disembark"
                val nodeX = if (usesSyntheticPosition) site.x else node.centerX
                val nodeY = if (usesSyntheticPosition) site.y else node.centerY
                val selected = node.id == selectedId
                val objective = trackedQuest?.let { nodeMatchesQuest(node, it) } == true
                val tint = when {
                    !node.canEnter -> Color(0xFFFFB394)
                    selected || objective -> Color(0xFFFFD477)
                    node.completed -> Color(0xFF9BDEC0)
                    else -> visualTheme.accent
                }
                val imageSize = minOf(mapWidth * site.artworkWidth, viewportWidth * .36f)
                val anchorRatio = if (node.id == "astra_access") .79f else .94f
                val painter = if (site.artworkWidth > 0) rememberHubNodePainter(
                    if (node.id == "astra_access") "images/nodes/astra_ship_map_v2.webp" else node.iconPath
                ) else null
                val anchorX = transform.x(nodeX)
                val anchorY = transform.y(nodeY)

                val reveal = remember(node.id) {
                    Animatable(if (node.id == newlyUnlockedNodeId) 0f else 1f)
                }
                androidx.compose.runtime.LaunchedEffect(node.id, newlyUnlockedNodeId) {
                    if (node.id == newlyUnlockedNodeId) {
                        reveal.snapTo(0f)
                        kotlinx.coroutines.delay(250)
                        reveal.animateTo(
                            1f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    } else {
                        reveal.snapTo(1f)
                    }
                }
                if (site.labelDx != 0f || site.labelDy != 0f) {
                    Canvas(Modifier.fillMaxSize()) {
                        val lineAlpha = (if (selected) 0.85f else 0.45f) * reveal.value.coerceIn(0f, 1f)
                        drawLine(
                            tint.copy(alpha = lineAlpha),
                            Offset(anchorX, anchorY),
                            Offset(transform.x(nodeX + site.labelDx), transform.y(nodeY + site.labelDy) + 14.dp.toPx()),
                            strokeWidth = if (selected) 1.4.dp.toPx() else 1.dp.toPx()
                        )
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
                        selectedId != null -> 0.88f
                        else -> 1f
                    }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 220),
                        label = "node_alpha_${node.id}"
                    )

                    // Artwork bottoms meet the painted ground; labels are independent siblings.
                    Image(
                        painter = painter,
                        contentDescription = "Artwork: ${node.title}",
                        contentScale = ContentScale.Fit,
                        alpha = animatedAlpha * reveal.value.coerceIn(0f, 1f),
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (anchorX - with(density) { imageSize.toPx() } / 2).roundToInt(),
                                    (anchorY - with(density) { imageSize.toPx() } * anchorRatio).roundToInt()
                                )
                            }
                            .size(imageSize)
                            .graphicsLayer {
                                scaleX = scale * (0.55f + 0.45f * reveal.value)
                                scaleY = scale * (0.55f + 0.45f * reveal.value)
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
                        color = Color(0xEB07141D),
                        border = BorderStroke(if (selected) 2.dp else 1.2.dp, tint),
                        shadowElevation = if (selected) 8.dp else 2.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (anchorX - with(density) { 12.dp.toPx() }).roundToInt(),
                                    (anchorY - with(density) { 12.dp.toPx() }).roundToInt()
                                )
                            }
                            .size(24.dp)
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
                            if (!node.canEnter || node.completed || objective) {
                                Icon(
                                    imageVector = when {
                                        !node.canEnter -> Icons.Default.Lock
                                        objective -> Icons.Default.Star
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Box(
                                    Modifier
                                        .size(if (selected) 8.dp else 6.dp)
                                        .background(tint, CircleShape)
                                )
                            }
                        }
                    }
                }

                // --- Layer 3: Independent Grounded Labels (Dimensions invariant on selection) ---
                val labelWidth = minOf(116.dp, mapWidth * .35f)
                val labelX = (transform.x(nodeX + site.labelDx) - with(density) { labelWidth.toPx() } / 2)
                    .coerceIn(0f, (viewportWidthPx - with(density) { labelWidth.toPx() }).coerceAtLeast(0f))

                val panelTopPx = viewportHeightPx
                val flipAbove = site.labelDy < 0f || (site.labelDy == 0f && (
                    anchorY + with(density) { 52.dp.toPx() } > panelTopPx
                ))

                val defaultPillHeightPx = with(density) { 26.dp.roundToPx() }
                var pillHeightPx by remember(node.id) { mutableIntStateOf(defaultPillHeightPx) }

                val imageSizePx = with(density) { imageSize.toPx() }
                val pillY = if (flipAbove) {
                    if (painter != null) {
                        val artworkTopY = anchorY - (imageSizePx * anchorRatio)
                        artworkTopY - with(density) { 10.dp.toPx() } - pillHeightPx
                    } else {
                        anchorY - with(density) { 16.dp.toPx() } - pillHeightPx
                    }
                } else {
                    if (painter != null) {
                        transform.y(nodeY + site.labelDy)
                    } else {
                        transform.y(nodeY + site.labelDy) + with(density) { 14.dp.toPx() }
                    }
                }.coerceIn(0f, (viewportHeightPx - pillHeightPx).coerceAtLeast(0f))

                val statusIcon = when {
                    !node.canEnter -> Icons.Default.Lock
                    objective -> Icons.Default.Star
                    node.completed -> Icons.Default.CheckCircle
                    else -> null
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .zIndex(if (selected) 4f else 3f)
                        .offset {
                            IntOffset(
                                labelX.roundToInt(),
                                pillY.roundToInt()
                            )
                        }
                        .width(labelWidth)
                        .heightIn(min = 36.dp)
                        .graphicsLayer {
                            alpha = reveal.value.coerceIn(0f, 1f)
                            translationY = (1f - reveal.value.coerceIn(0f, 1f)) * 10.dp.toPx()
                        }
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) Color(0xFA0C1924) else Color(0xE607111A),
                        border = BorderStroke(
                            width = if (selected) 1.4.dp else 1.dp,
                            color = tint.copy(alpha = if (selected) 0.92f else if (objective) 0.65f else 0.34f)
                        ),
                        shadowElevation = if (selected) 8.dp else 2.dp,
                        modifier = Modifier.onSizeChanged { size ->
                            if (size.height > 0) {
                                pillHeightPx = size.height
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                        ) {
                            if (statusIcon != null) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = node.title,
                                color = if (selected) Color.White else tint,
                                fontSize = 10.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
