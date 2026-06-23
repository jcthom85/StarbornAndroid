package com.example.starborn.feature.hub.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.feature.hub.viewmodel.HubNodeUi
import com.example.starborn.feature.hub.viewmodel.HubUiState
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import kotlin.math.roundToInt

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    onEnterNode: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HubScreenContent(
        uiState = uiState,
        onNodeSelected = { node ->
            viewModel.enterNode(node.id, onEnterNode)
        },
        modifier = modifier
    )
}

@Composable
private fun HubScreenContent(
    uiState: HubUiState,
    onNodeSelected: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundPainter = rememberHubBackgroundPainter(uiState.backgroundImage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiState.hub?.title ?: "Hub",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = uiState.hub?.description ?: "Select a destination to continue your journey.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFCC80),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!uiState.isLoading) {
            HubNodeLayer(
                nodes = uiState.nodes,
                selectedId = uiState.selectedNodeId,
                onNodeSelected = onNodeSelected,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 156.dp, end = 16.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun HubNodeLayer(
    nodes: List<HubNodeUi>,
    selectedId: String?,
    onNodeSelected: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        nodes.forEach { node ->
            val composition = hubNodeComposition(node)
            val markerWidthPx = with(density) { composition.width.toPx() }
            val markerHeightPx = with(density) { composition.height.toPx() }
            val centerX = widthPx * node.centerX
            val centerY = heightPx * node.centerY
            val offsetX = (centerX - markerWidthPx * composition.anchorX)
                .coerceIn(0f, (widthPx - markerWidthPx).coerceAtLeast(0f))
                .roundToInt()
            val offsetY = (centerY - markerHeightPx * composition.anchorY)
                .coerceIn(0f, (heightPx - markerHeightPx).coerceAtLeast(0f))
                .roundToInt()
            HubNodeMarker(
                node = node,
                selected = selectedId == node.id,
                modifier = Modifier
                    .width(composition.width)
                    .height(composition.height)
                    .offset { IntOffset(offsetX, offsetY) },
                onClick = { onNodeSelected(node) }
            )
        }
    }
}

@Composable
private fun HubNodeMarker(
    node: HubNodeUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconPainter = rememberHubNodePainter(node.iconPath)
    val composition = hubNodeComposition(node)
    val highlight by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "hubNodeHighlight"
    )
    val pulseTransition = rememberInfiniteTransition(label = "hubNodePulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hubNodeScale"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) pulse else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "hubNodeScaleEase"
    )
    Column(
        modifier = modifier
            .semantics { contentDescription = "Enter ${node.title}" }
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = if (node.unlocked) 1f else 0.38f,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = composition.imageWidth)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(composition.imageWidth * 0.62f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF24536A), Color(0xFF0B1820))
                            )
                        )
                        .border(1.5.dp, TitleMarkerBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (node.unlocked) node.title.take(1).uppercase() else "?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xDC080C11),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = if (selected) 0.34f else 0.16f)
            ),
            shadowElevation = 4.dp
        ) {
            Text(
                text = when {
                    !node.unlocked -> "${node.title} - Locked"
                    node.completed -> "${node.title} - Complete"
                    else -> node.title
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 132.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private data class HubNodeComposition(
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val imageWidth: androidx.compose.ui.unit.Dp = width,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
)

private fun hubNodeComposition(node: HubNodeUi): HubNodeComposition = when (node.id) {
    "pit" -> HubNodeComposition(width = 132.dp, height = 132.dp, imageWidth = 116.dp, anchorX = 0.48f, anchorY = 0.68f)
    "workshop" -> HubNodeComposition(width = 166.dp, height = 180.dp, anchorX = 0.5f, anchorY = 0.72f)
    "med_bay" -> HubNodeComposition(width = 132.dp, height = 134.dp, imageWidth = 118.dp, anchorX = 0.5f, anchorY = 0.58f)
    "trade_row" -> HubNodeComposition(width = 132.dp, height = 128.dp, imageWidth = 112.dp, anchorX = 0.5f, anchorY = 0.68f)
    "admin_gate" -> HubNodeComposition(width = 150.dp, height = 126.dp, imageWidth = 110.dp, anchorX = 0.56f, anchorY = 0.62f)
    "admin_concourse" -> HubNodeComposition(width = 142.dp, height = 126.dp, imageWidth = 106.dp, anchorX = 0.5f, anchorY = 0.62f)
    "server_room" -> HubNodeComposition(width = 136.dp, height = 132.dp, imageWidth = 108.dp, anchorX = 0.5f, anchorY = 0.68f)
    "deep_mine" -> HubNodeComposition(width = 132.dp, height = 138.dp, imageWidth = 114.dp, anchorX = 0.5f, anchorY = 0.66f)
    "echo_chamber" -> HubNodeComposition(width = 142.dp, height = 130.dp, imageWidth = 110.dp, anchorX = 0.5f, anchorY = 0.62f)
    "launch_bay" -> HubNodeComposition(width = 132.dp, height = 136.dp, imageWidth = 114.dp, anchorX = 0.5f, anchorY = 0.62f)
    else -> {
        // Asset metadata uses source-pixel hints, not density-independent screen sizes.
        val size = (node.sizeHint * 0.36f).coerceIn(72f, 96f).dp
        HubNodeComposition(width = size, height = size + 28.dp, imageWidth = size, anchorY = 0.62f)
    }
}

private val TitleMarkerBorder = Color(0xFF7BE4FF).copy(alpha = 0.72f)

@Composable
private fun rememberHubNodePainter(iconPath: String?): Painter? {
    if (iconPath.isNullOrBlank()) return null
    val context = LocalContext.current
    return remember(iconPath) {
        runCatching {
            context.assets.open(iconPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { BitmapPainter(it) }
            }
        }.getOrNull()
    }
}

private fun nodeColor(node: HubNodeUi, selected: Boolean, highlight: Float): Color {
    val base = if (node.discovered) Color(0xFF1E5C8E) else Color(0xFF455A64)
    val selectedTint = Color(0xFF80CBC4)
    return lerpColor(base, selectedTint, highlight)
}

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped
    )
}

@Composable
private fun rememberHubBackgroundPainter(imagePath: String?): Painter {
    val context = LocalContext.current
    val fallback = ColorPainter(Color.Black)
    val painter = remember(imagePath) {
        if (imagePath.isNullOrBlank()) {
            null
        } else {
            runCatching {
                context.assets.open(imagePath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { BitmapPainter(it) }
                }
            }.getOrNull()
        }
    }
    return painter ?: fallback
}
