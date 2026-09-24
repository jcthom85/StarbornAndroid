package com.example.starborn.feature.hub.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
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
        val transform = HubMapTransform.fit(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(),
            aspect, layout.cropTop, layout.cropBottom)
        val mapWidth = with(density) { transform.width.toDp() }
        val fullHeight = with(density) { transform.imageHeight.toDp() }
        Box(Modifier.size(mapWidth, fullHeight * (layout.cropBottom - layout.cropTop)).clipToBounds()) {
            Image(background, null, contentScale = ContentScale.FillBounds,
                modifier = Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(mapWidth, fullHeight)
                    .offset(y = -fullHeight * layout.cropTop))
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
                val imageSize = mapWidth * site.artworkWidth
                val painter = if (site.artworkWidth > 0) rememberHubNodePainter(
                    if (node.id == "astra_access") "images/nodes/astra_ship_map.webp" else node.iconPath
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
                    // Artwork bottoms meet the painted ground; labels are independent siblings.
                    Image(painter, null, contentScale = ContentScale.Fit,
                        alpha = if (node.canEnter) 1f else .48f,
                        modifier = Modifier.offset { IntOffset(
                            (anchorX - with(density) { imageSize.toPx() } / 2).roundToInt(),
                            (anchorY - with(density) { imageSize.toPx() } * if (node.id == "astra_access") .79f else .94f).roundToInt()) }
                            .size(imageSize).pointerInput(node.id, node.canEnter) {
                                detectTapGestures(onTap = { onSelect(node) }, onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) })
                            })
                } else {
                    Surface(shape = CircleShape, color = Color(0xE607141D), border = BorderStroke(if (selected) 2.dp else 1.dp, tint),
                        modifier = Modifier.offset { IntOffset((anchorX - with(density) { 11.dp.toPx() }).roundToInt(),
                            (anchorY - with(density) { 11.dp.toPx() }).roundToInt()) }.size(22.dp)
                            .pointerInput(node.id, node.canEnter) {
                                detectTapGestures(onTap = { onSelect(node) }, onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) })
                            }) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!node.canEnter || node.completed || objective) Icon(
                                if (!node.canEnter) Icons.Default.Lock else if (objective) Icons.Default.Star else Icons.Default.CheckCircle, null,
                                tint = tint, modifier = Modifier.size(13.dp))
                            else Box(Modifier.size(if (selected) 8.dp else 5.dp).background(tint, CircleShape))
                        }
                    }
                }
                val labelWidth = minOf(104.dp, mapWidth * .32f)
                val labelX = (transform.x(site.x + site.labelDx) - with(density) { labelWidth.toPx() } / 2)
                    .coerceIn(0f, (transform.width - with(density) { labelWidth.toPx() }).coerceAtLeast(0f))
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset { IntOffset(labelX.roundToInt(),
                        (transform.y(site.y + site.labelDy) - with(density) { 12.dp.toPx() }).roundToInt()) }
                        .width(labelWidth).heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Enter ${node.title}"
                            role = Role.Button
                            onClick(label = "Select ${node.title}") { onSelect(node); true }
                        }
                        .pointerInput(node.id, node.canEnter) {
                            detectTapGestures(onTap = { onSelect(node) }, onDoubleTap = { onSelect(node); if (node.canEnter) onEnter(node) })
                        }) {
                    Spacer(Modifier.height(26.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xEF08121B),
                        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) .85f else .3f))) {
                        Text(node.title, color = tint, fontSize = 10.sp, lineHeight = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                    }
                }
            }
        }
    }
}
