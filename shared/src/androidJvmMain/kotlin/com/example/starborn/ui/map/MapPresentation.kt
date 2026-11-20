package com.example.starborn.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.exploration.viewmodel.*
import kotlin.math.abs

/** The same room states, symbols and legend are used by Android and desktop. */
object MapPalette {
    val current = Color(0xFFFFC857)
    val visited = Color(0xFF7797AB)
    val discovered = Color(0xFF405462)
    val preview = Color(0xFFADB5BB)
    val gateway = Color(0xFF00E5FF)
    val blocked = Color(0xFFC5B3DE)
    val threat = Color(0xFFFF746D)
    val background = Color(0xFF061018)
    fun room(cell: MinimapCellUi) = when {
        cell.isCurrent -> current
        cell.isPreview -> preview
        cell.visited -> visited
        else -> discovered
    }
}

fun mapServiceLabel(service: MinimapService): String = when (service) {
    MinimapService.SHOP -> "Shop"
    MinimapService.REST -> "Rest"
    MinimapService.COOKING -> "Cooking"
    MinimapService.TINKERING -> "Tinkering"
    MinimapService.EXIT -> "Hub exit"
}

fun mapServiceIcon(service: MinimapService) = when (service) {
    MinimapService.SHOP -> Icons.Default.ShoppingCart
    MinimapService.REST -> Icons.Default.Hotel
    MinimapService.COOKING -> Icons.Default.Restaurant
    MinimapService.TINKERING -> Icons.Default.Build
    MinimapService.EXIT -> Icons.AutoMirrored.Filled.ExitToApp
}

fun mapRoomDescription(cell: MinimapCellUi): String = buildList {
    add(cell.title)
    add(when { cell.isCurrent -> "You are here"; cell.isPreview -> "Unexplored"; cell.visited -> "Visited"; else -> "Discovered" })
    if (cell.hasEnemies) add("Enemies present")
    if (cell.blockedDirections.isNotEmpty()) add("Blocked paths: ${cell.blockedDirections.joinToString()}")
    addAll(cell.services.filterNot { it == MinimapService.EXIT }.map(::mapServiceLabel))
    cell.hubExit?.let { add("Exit to ${it.destinationTitle}${if (it.blocked) " (blocked)" else ""}") }
    addAll(cell.nodeExits.map { "${it.direction} to ${it.destinationTitle}${if (it.blocked) ", blocked" else ""}" })
}.joinToString(". ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "ROOM MARKERS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MapPalette.gateway
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapLegendSwatch("You are here", MapPalette.current, shape = "current")
            MapLegendSwatch("Visited", MapPalette.visited)
            MapLegendSwatch("Unexplored", MapPalette.discovered)
            MapLegendSwatch("Exit room", MapPalette.gateway, shape = "diamond")
            MapLegendSwatch("Enemies", MapPalette.threat, shape = "dot")
            MapLegendSwatch("Blocked route", MapPalette.blocked, shape = "lock")
            MapLegendSwatch("Dark room", MapPalette.discovered, shape = "dark")
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Text(
            "ROOM SERVICES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MapPalette.gateway
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(MinimapService.SHOP, MinimapService.REST, MinimapService.COOKING, MinimapService.TINKERING).forEach { service ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MapPalette.visited.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(mapServiceIcon(service), null, Modifier.size(16.dp), tint = MapPalette.gateway)
                        Text(mapServiceLabel(service), style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLegendSwatch(label: String, color: Color, shape: String = "tile") {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Canvas(Modifier.size(14.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.42f
                when (shape) {
                    "current" -> {
                        drawCircle(color.copy(alpha = 0.25f), r * 1.25f, c)
                        drawCircle(color, r * 0.85f, c)
                        drawCircle(Color.White, r * 0.32f, c)
                    }
                    "diamond" -> {
                        drawPath(Path().apply {
                            moveTo(c.x, c.y - r); lineTo(c.x + r, c.y)
                            lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close()
                        }, color, style = Stroke(1.6.dp.toPx()))
                    }
                    "dot" -> drawCircle(color, r * 0.65f, c)
                    "lock" -> drawMapLock(c)
                    "dark" -> {
                        drawRoundRect(color.copy(alpha = 0.55f), c - Offset(r, r), Size(r * 2, r * 2), CornerRadius(3.dp.toPx()))
                        drawMapDarkHatch(c, r)
                    }
                    else -> drawRoundRect(color, c - Offset(r, r), Size(r * 2, r * 2), CornerRadius(3.dp.toPx()))
                }
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapRoomDetails(
    cell: MinimapCellUi,
    modifier: Modifier = Modifier,
    cells: List<MinimapCellUi> = emptyList(),
    onSelectRoom: ((String) -> Unit)? = null
) {
    val statusLabel = when {
        cell.isCurrent -> "YOU ARE HERE"
        cell.isDark -> "DARK"
        cell.isPreview -> "UNEXPLORED"
        cell.visited -> "VISITED"
        else -> "DISCOVERED"
    }
    val statusColor = MapPalette.room(cell)
    val services = cell.services.filterNot { it == MinimapService.EXIT }
    val nodeExitByDir = cell.nodeExits.associateBy { it.direction.lowercase() }

    Column(
        modifier.semantics { contentDescription = mapRoomDescription(cell) },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Room name + status & threat badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cell.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cell.hasEnemies) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MapPalette.threat.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, MapPalette.threat.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MapPalette.threat))
                            Text(
                                "HOSTILES",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MapPalette.threat
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Hub exit highlight chip
        cell.hubExit?.let { exit ->
            val exitColor = if (exit.blocked) MapPalette.blocked else MapPalette.gateway
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = exitColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, exitColor.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (exit.blocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = exitColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Exit to ${exit.destinationTitle}${if (exit.blocked) " (Locked)" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = exitColor
                    )
                }
            }
        }

        // Service chips
        if (services.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                services.forEach { service ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MapPalette.gateway.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MapPalette.gateway.copy(alpha = 0.32f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(mapServiceIcon(service), null, Modifier.size(14.dp), tint = MapPalette.gateway)
                            Text(
                                mapServiceLabel(service),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Deduplicated directional exit & connection chips (tappable to jump selection)
        val exitChips = buildList {
            if (cells.isNotEmpty()) {
                cell.connections.forEach { (direction, targetId) ->
                    val dirLower = direction.lowercase()
                    val nodeExit = nodeExitByDir[dirLower]
                    val targetCell = cells.firstOrNull { it.roomId == targetId }
                    val label = nodeExit?.destinationTitle ?: targetCell?.title ?: "Unexplored"
                    val blocked = (nodeExit?.blocked == true) || (direction in cell.blockedDirections)
                    val isAreaExit = nodeExit != null
                    add(Triple(direction, label, Triple(blocked, isAreaExit, targetCell?.roomId)))
                }
            }
            // Include any nodeExits not already covered by connections
            cell.nodeExits.forEach { exit ->
                if (cell.connections.keys.none { it.equals(exit.direction, ignoreCase = true) }) {
                    add(Triple(exit.direction, exit.destinationTitle, Triple(exit.blocked, true, null)))
                }
            }
        }
        if (exitChips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                exitChips.forEach { (direction, destination, meta) ->
                    val (blocked, isAreaExit, targetRoomId) = meta
                    val dirBadge = when (direction.lowercase()) {
                        "north" -> "N"
                        "south" -> "S"
                        "east" -> "E"
                        "west" -> "W"
                        "up" -> "UP"
                        "down" -> "DN"
                        else -> direction.take(2).uppercase()
                    }
                    val chipAccent = when {
                        blocked -> MapPalette.blocked
                        isAreaExit -> MapPalette.gateway
                        else -> MapPalette.visited
                    }
                    val canJump = onSelectRoom != null && targetRoomId != null
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = chipAccent.copy(alpha = if (isAreaExit || blocked) 0.13f else 0.08f),
                        border = BorderStroke(1.dp, chipAccent.copy(alpha = if (canJump) 0.42f else 0.25f)),
                        modifier = if (canJump) {
                            Modifier.clip(RoundedCornerShape(9.dp)).clickable { onSelectRoom!!(targetRoomId!!) }
                        } else Modifier
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = dirBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                color = chipAccent
                            )
                            Text(
                                text = destination,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (blocked) MapPalette.blocked else Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (blocked) {
                                Icon(Icons.Default.Lock, null, Modifier.size(12.dp), tint = MapPalette.blocked)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Exit rooms use one diamond, regardless of how many area or hub exits they offer. */
fun mapRoomHasExit(cell: MinimapCellUi): Boolean =
    !cell.isDark && !cell.isPreview && (cell.hubExit != null || cell.nodeExits.isNotEmpty())

fun DrawScope.drawMapRoom(
    cell: MinimapCellUi,
    center: Offset,
    radius: Float,
    selected: Boolean = false,
    tacticalTile: Boolean = false,
    dimmed: Boolean = false
) {
    val dimAlpha = if (dimmed) 0.24f else 1f
    val baseColor = MapPalette.room(cell).copy(alpha = (if (cell.isPreview) .38f else 1f) * dimAlpha)
    if (mapRoomHasExit(cell)) {
        val diamond = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }
        if (tacticalTile && !dimmed) {
            val glowColor = if (cell.isCurrent) MapPalette.current else MapPalette.gateway
            drawCircle(glowColor.copy(alpha = 0.16f), radius * 1.45f, center)
        }
        drawPath(diamond, (if (cell.isCurrent) MapPalette.current else Color(0xFF0A1B2A)).copy(alpha = dimAlpha))
        drawPath(diamond, (if (cell.isCurrent) MapPalette.current else MapPalette.gateway).copy(alpha = dimAlpha),
            style = Stroke((radius * .28f).coerceAtMost(2.dp.toPx())))
    } else if (tacticalTile) {
        val half = radius * 0.96f
        val topLeft = center - Offset(half, half)
        val rectSize = Size(half * 2f, half * 2f)
        val corner = CornerRadius(half * 0.42f)
        if (cell.isCurrent && !dimmed) {
            drawCircle(MapPalette.current.copy(alpha = 0.22f), radius * 1.65f, center)
            drawCircle(MapPalette.current.copy(alpha = 0.45f), radius * 1.25f, center, style = Stroke(1.2.dp.toPx()))
        }
        drawRoundRect(
            color = if (cell.isCurrent) MapPalette.current.copy(alpha = dimAlpha)
            else baseColor.copy(alpha = (if (cell.visited) 0.28f else 0.16f) * dimAlpha),
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = corner
        )
        drawRoundRect(
            color = baseColor,
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = corner,
            style = Stroke(if (cell.isCurrent || selected) 2.dp.toPx() else 1.4.dp.toPx())
        )
    } else {
        drawCircle(baseColor, radius, center)
    }
    if (cell.isDark) drawMapDarkHatch(center, radius)
    if (selected) {
        drawCircle(MapPalette.gateway.copy(alpha = 0.28f), radius + 6.dp.toPx(), center)
        drawCircle(Color.White, radius + 3.5.dp.toPx(), center, style = Stroke(1.6.dp.toPx()))
    }
    if (cell.isCurrent) drawCircle(Color.White.copy(alpha = dimAlpha), radius * .32f, center)
}

fun DrawScope.drawMapThreat(cell: MinimapCellUi, center: Offset, radius: Float, dimmed: Boolean = false) {
    if (!cell.hasEnemies || cell.isDark || cell.isPreview) return
    val p = center + Offset(radius * .85f, -radius * .85f)
    val dot = (radius * .42f).coerceAtMost(3.5.dp.toPx())
    val alpha = if (dimmed) 0.25f else 1f
    drawCircle(MapPalette.background, dot * 1.55f, p)
    drawCircle(MapPalette.threat.copy(alpha = alpha), dot, p)
}

/** Service details belong to the full map; keep all services instead of silently choosing one. */
fun DrawScope.drawMapFeatures(
    cell: MinimapCellUi,
    center: Offset,
    radius: Float,
    servicePainters: Map<MinimapService, Painter>,
    availableWidth: Float = radius * 4f,
    dimmed: Boolean = false
) {
    if (cell.isDark || cell.isPreview) return
    val services = MinimapService.entries.filter { it != MinimapService.EXIT && it in cell.services }
    if (services.isEmpty()) return
    val iconSize = minOf(11.dp.toPx(), availableWidth / (services.size * 1.25f - .2f))
    val spacing = iconSize * 1.25f
    val totalW = (services.size - 1) * spacing + iconSize + 6.dp.toPx()
    val pillH = iconSize + 4.dp.toPx()
    val pillCenter = center + Offset(0f, radius + iconSize / 2 + 4.dp.toPx())
    val alpha = if (dimmed) 0.25f else 1f
    drawRoundRect(
        color = Color(0xFF061018).copy(alpha = 0.88f * alpha),
        topLeft = pillCenter - Offset(totalW / 2f, pillH / 2f),
        size = Size(totalW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    services.forEachIndexed { index, service ->
        val p = center + Offset((index - (services.size - 1) / 2f) * spacing, radius + iconSize / 2 + 4.dp.toPx())
        translate(p.x - iconSize / 2, p.y - iconSize / 2) {
            with(servicePainters.getValue(service)) {
                draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(MapPalette.gateway.copy(alpha = 0.9f * alpha)))
            }
        }
    }
}

fun DrawScope.drawMapDarkHatch(center: Offset, radius: Float) {
    clipPath(Path().apply { addOval(Rect(center - Offset(radius, radius), Size(radius * 2, radius * 2))) }) {
        drawCircle(Color.Black.copy(alpha = .55f), radius, center)
        var shift = -radius * 2
        while (shift <= radius * 2) {
            drawLine(MapPalette.discovered, center + Offset(shift-radius, radius), center + Offset(shift+radius, -radius), 1.dp.toPx())
            shift += 4.dp.toPx()
        }
    }
}

/** Shared drawing for both HUD minimaps and Android's selectable full map. */
@Composable
fun AreaMapCanvas(
    cells: List<MinimapCellUi>,
    modifier: Modifier = Modifier,
    minimap: Boolean = false,
    obscured: Boolean = false,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    selectedId: String? = null,
    onSelect: ((MinimapCellUi) -> Unit)? = null,
    highlightedRoomIds: Set<String>? = null
) {
    val visible = if (minimap) cells.filter { abs(it.offsetX) <= 2 && abs(it.offsetY) <= 2 } else cells
    var centers by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var hitRadius by remember { mutableFloatStateOf(24f) }
    val textMeasurer = rememberTextMeasurer()
    val servicePainters = MinimapService.entries.associateWith { rememberVectorPainter(mapServiceIcon(it)) }
    Canvas(modifier.background(MapPalette.background).testTag(if (minimap) "area-minimap" else "area-map")
        .semantics {
            contentDescription = if (obscured) "Minimap obscured by darkness" else visible.joinToString("; ", transform = ::mapRoomDescription)
            if (onSelect != null && !obscured) customActions = visible.map { cell ->
                CustomAccessibilityAction("Inspect ${cell.title}") { onSelect(cell); true }
            }
        }
        .pointerInput(visible, onSelect, centers, obscured) { if (onSelect != null && !obscured) detectTapGestures { tap ->
            visible.minByOrNull { (centers[it.roomId]?.minus(tap) ?: Offset(100000f,100000f)).getDistance() }
                ?.takeIf { (centers[it.roomId]!! - tap).getDistance() <= hitRadius }?.let(onSelect)
        } }) {
        if (obscured) {
            var y = -size.height
            while (y < size.width) { drawLine(MapPalette.discovered.copy(alpha=.18f), Offset(y,0f), Offset(y+size.height,size.height), 1.dp.toPx()); y += 12.dp.toPx() }
            return@Canvas
        }
        val minX = visible.minOfOrNull { it.gridX } ?: 0
        val maxX = visible.maxOfOrNull { it.gridX } ?: 0
        val minY = visible.minOfOrNull { it.gridY } ?: 0
        val maxY = visible.maxOfOrNull { it.gridY } ?: 0
        val step = if (minimap) size.minDimension / 6.4f else minOf(size.width / (maxX-minX+3), size.height / (maxY-minY+3)).coerceAtMost(58.dp.toPx()) * scale
        val mid = Offset(size.width/2,size.height/2) + offset

        // Tactical blueprint grid & subtle center vignette for full map only
        if (!minimap) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0E2233), MapPalette.background),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = maxOf(size.width, size.height) * 0.72f
                )
            )
            val gridStep = step.coerceIn(22.dp.toPx(), 56.dp.toPx())
            val gridColor = MapPalette.gateway.copy(alpha = 0.055f)
            val startX = ((mid.x % gridStep) + gridStep) % gridStep
            val startY = ((mid.y % gridStep) + gridStep) % gridStep
            var gx = startX
            while (gx < size.width) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1.dp.toPx())
                gx += gridStep
            }
            var gy = startY
            while (gy < size.height) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
                gy += gridStep
            }
        }

        fun point(cell: MinimapCellUi) = if (minimap) mid + Offset(cell.offsetX*step,-cell.offsetY*step)
            else mid + Offset((cell.gridX-(minX+maxX)/2f)*step, ((minY+maxY)/2f-cell.gridY)*step)
        val drawnCenters = visible.associate { it.roomId to point(it) }
        if (centers != drawnCenters) centers = drawnCenters
        hitRadius = maxOf(26.dp.toPx(), step*.45f)
        val known = visible.associateBy { it.roomId }
        val edges = mutableSetOf<Set<String>>()
        visible.forEach { cell -> cell.connections.forEach { (direction,id) -> known[id]?.let { other ->
            if (edges.add(setOf(cell.roomId,id))) {
                val blocked = direction in cell.blockedDirections || other.connections.any { (d,target) -> target==cell.roomId && d in other.blockedDirections }
                val a=point(cell); val b=point(other)
                if (minimap && blocked) {
                    val midpoint = (a + b) / 2f
                    val vector = (b - a) / (b - a).getDistance().coerceAtLeast(1f)
                    val normal = Offset(-vector.y, vector.x)
                    val gap = step * .12f
                    val lineWidth = (step * .07f).coerceAtMost(1.5.dp.toPx())
                    drawLine(MapPalette.visited.copy(alpha = .55f), a, midpoint - vector * gap, lineWidth)
                    drawLine(MapPalette.visited.copy(alpha = .55f), midpoint + vector * gap, b, lineWidth)
                    drawLine(MapPalette.blocked, midpoint - normal * step * .13f,
                        midpoint + normal * step * .13f, lineWidth, StrokeCap.Round)
                } else {
                    val edgeDim = if (!minimap && highlightedRoomIds != null &&
                        cell.roomId !in highlightedRoomIds && other.roomId !in highlightedRoomIds) 0.22f else 1f
                    val routeWidth = if (minimap) (step * .07f).coerceAtMost(1.5.dp.toPx()) else 2.2.dp.toPx()
                    if (!minimap && !blocked) {
                        drawLine(MapPalette.gateway.copy(alpha = 0.14f * edgeDim), a, b, routeWidth * 2.4f, StrokeCap.Round)
                    }
                    drawLine(
                        (if (blocked) MapPalette.blocked else MapPalette.visited.copy(alpha = if (minimap) .55f else .75f)).copy(alpha = (if (blocked) 0.9f else if (minimap) 0.55f else 0.75f) * edgeDim),
                        a, b, routeWidth, StrokeCap.Round,
                        pathEffect = if (blocked) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx())) else null
                    )
                    if (blocked) drawMapLock((a+b)/2f)
                }
            }
        } } }
        // Keep long paths visible even when their destination dot is outside this window.
        // Only existing local connections supply geometry; no remote room features are exposed.
        if (minimap) clipRect {
            visible.forEach { cell ->
                cell.continuationOffsets.forEach { (direction, target) ->
                    val a = point(cell)
                    val targetPoint = mid + Offset(target.first * step, -target.second * step)
                    val delta = targetPoint - a
                    val b = a + delta * (maxOf(size.width, size.height) * 2f / delta.getDistance().coerceAtLeast(1f))
                    val blocked = direction in cell.blockedDirections
                    drawLine(if (blocked) MapPalette.blocked else MapPalette.visited.copy(alpha = .55f),
                        a, b, (step * .07f).coerceAtMost(1.5.dp.toPx()), StrokeCap.Round,
                        pathEffect = if (blocked) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null)
                }
            }
        }
        visible.forEach { cell ->
            val p = point(cell)
            val dimmed = !minimap && highlightedRoomIds != null && cell.roomId !in highlightedRoomIds && !cell.isCurrent && cell.roomId != selectedId
            val radius = if (minimap) {
                step * (if (cell.isCurrent) .25f else if (mapRoomHasExit(cell)) .21f else .16f)
            } else {
                step * (if (cell.isCurrent) .28f else if (mapRoomHasExit(cell)) .24f else .21f)
            }
            drawMapRoom(cell, p, radius, selected = !minimap && cell.roomId == selectedId, tacticalTile = !minimap, dimmed = dimmed)
            drawMapThreat(cell, p, radius, dimmed = dimmed)
            if (!minimap) drawMapFeatures(cell, p, radius, servicePainters, availableWidth = step * .85f, dimmed = dimmed)
            else cell.nodeExits.forEach { exit ->
                drawNodeExitMarker(p, exit, radius, step * .48f)
            }
        }
        if (!minimap) drawFullMapNodeExits(visible, ::point, step*.36f, textMeasurer)
    }
}

fun DrawScope.drawMapLock(p: Offset) {
    val r=3.dp.toPx()
    drawRoundRect(MapPalette.blocked,p-Offset(r,0f),Size(r*2,r*1.4f),CornerRadius(1.dp.toPx()))
    drawArc(MapPalette.blocked,180f,180f,false,p-Offset(r,r),Size(r*2,r*2),style=Stroke(1.dp.toPx()))
}
