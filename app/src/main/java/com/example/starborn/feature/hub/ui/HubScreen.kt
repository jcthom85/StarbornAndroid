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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
                .padding(horizontal = 24.dp, vertical = 32.dp)
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
        }

        if (!uiState.isLoading) {
            HubNodeLayer(
                nodes = uiState.nodes,
                selectedId = uiState.selectedNodeId,
                onNodeSelected = onNodeSelected,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp)
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
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        nodes.forEach { node ->
            val sizePx = node.sizeHint.coerceIn(240f, 460f)
            val centerX = widthPx * node.centerX
            val centerY = heightPx * (1f - node.centerY)
            val extraHeight = 80.dp
            val extraHeightPx = with(density) { extraHeight.toPx() }
            val extraWidth = 24.dp
            val extraWidthPx = with(density) { extraWidth.toPx() }
            val totalWidth = sizePx + extraWidthPx
            val offsetX = (centerX - totalWidth / 2f).roundToInt()
            val offsetY = (centerY - (sizePx + extraHeightPx) / 2f).roundToInt()
            val sizeDp = with(density) { sizePx.toDp() }
            HubNodeMarker(
                node = node,
                selected = selectedId == node.id,
                modifier = Modifier
                    .width(sizeDp + extraWidth)
                    .height(sizeDp + extraHeight)
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
    val baseShape = RoundedCornerShape(28.dp)
    val borderColor = Color(0xFF9AE6FF).copy(alpha = highlight * 0.8f + 0.2f)
    Column(
        modifier = modifier
            .clip(baseShape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(vertical = 4.dp),
            shape = baseShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconPainter != null) {
                    Image(
                        painter = iconPainter,
                        contentDescription = node.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1F2F3C),
                                        Color(0xFF0B141A)
                                    )
                                )
                            )
                    )
                }

                if (!node.discovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                    Text(
                        text = "Unknown",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

            Text(
                text = node.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .fillMaxWidth()
            )
    }
}

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
    val default = painterResource(R.drawable.main_menu_background)
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
    return painter ?: default
}
