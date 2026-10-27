package com.example.starborn.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
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

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Gold: you are here · Blue-gray: visited · Dim: discovered", style = MaterialTheme.typography.bodySmall, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(18.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension * .35f
                drawPath(Path().apply {
                    moveTo(center.x, center.y - r); lineTo(center.x + r, center.y)
                    lineTo(center.x, center.y + r); lineTo(center.x - r, center.y); close()
                }, MapPalette.gateway, style = Stroke(1.5.dp.toPx()))
            }
            Text("Cyan diamond: exit room · Red dot: enemies", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
        Text("Minimap: routes and nearby rooms. Lavender bar: blocked route.\nFull map: service icons, exit destinations, and locks. Hatched rooms are dark; faint rooms are previews.", style = MaterialTheme.typography.bodySmall, color = MapPalette.preview)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(MinimapService.SHOP, MinimapService.REST, MinimapService.COOKING, MinimapService.TINKERING).forEach { service ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(mapServiceIcon(service), null, Modifier.size(18.dp), tint = MapPalette.visited)
                    Text(mapServiceLabel(service), style = MaterialTheme.typography.labelSmall, color = MapPalette.preview)
                }
            }
        }
    }
}

@Composable
fun MapRoomDetails(cell: MinimapCellUi, modifier: Modifier = Modifier, cells: List<MinimapCellUi> = emptyList()) {
    Column(modifier.semantics { contentDescription = mapRoomDescription(cell) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(cell.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(when { cell.isCurrent -> "You are here"; cell.visited -> "Visited"; else -> "Discovered" }, color = MapPalette.room(cell), style = MaterialTheme.typography.bodySmall)
        if (cell.hasEnemies) Text("Enemies present", color = MapPalette.threat)
        cell.hubExit?.let { Text("Exit to ${it.destinationTitle}${if (it.blocked) " — blocked" else ""}", color = MapPalette.gateway) }
        val services = cell.services.filterNot { it == MinimapService.EXIT }
        if (services.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            services.forEach { service -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(mapServiceIcon(service), null, Modifier.size(20.dp), tint = MapPalette.visited)
                Text(mapServiceLabel(service), style = MaterialTheme.typography.labelSmall, color = Color.White)
            } }
        }
        cell.nodeExits.forEach { Text("${it.direction.replaceFirstChar { c -> c.uppercase() }}: ${it.destinationTitle}${if (it.blocked) " — blocked" else ""}", color = if (it.blocked) MapPalette.blocked else MapPalette.gateway, style = MaterialTheme.typography.bodySmall) }
        if (cell.blockedDirections.isNotEmpty()) Text("Blocked paths: ${cell.blockedDirections.joinToString()}", color = MapPalette.blocked, style = MaterialTheme.typography.bodySmall)
        if (cells.isNotEmpty()) cell.connections.forEach { (direction, id) ->
            val destination = cells.firstOrNull { it.roomId == id }?.title ?: "Unexplored room"
            val blocked = direction in cell.blockedDirections
            Text("${direction.replaceFirstChar { it.uppercase() }}: $destination${if (blocked) " (blocked)" else ""}",
                color = if (blocked) MapPalette.blocked else MapPalette.preview, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Exit rooms use one diamond, regardless of how many area or hub exits they offer. */
fun mapRoomHasExit(cell: MinimapCellUi): Boolean =
    !cell.isDark && !cell.isPreview && (cell.hubExit != null || cell.nodeExits.isNotEmpty())

fun DrawScope.drawMapRoom(cell: MinimapCellUi, center: Offset, radius: Float, selected: Boolean = false) {
    val color = MapPalette.room(cell).copy(alpha = if (cell.isPreview) .35f else 1f)
    if (mapRoomHasExit(cell)) {
        val diamond = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }
        drawPath(diamond, if (cell.isCurrent) MapPalette.current else MapPalette.background)
        drawPath(diamond, if (cell.isCurrent) MapPalette.current else MapPalette.gateway,
            style = Stroke((radius * .3f).coerceAtMost(1.5.dp.toPx())))
    } else {
        drawCircle(color, radius, center)
    }
    if (cell.isDark) drawMapDarkHatch(center, radius)
    if (selected) drawCircle(Color.White, radius + 3.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    if (cell.isCurrent) drawCircle(Color.White, radius * .3f, center)
}

fun DrawScope.drawMapThreat(cell: MinimapCellUi, center: Offset, radius: Float) {
    if (!cell.hasEnemies || cell.isDark || cell.isPreview) return
    val p = center + Offset(radius * .85f, -radius * .85f)
    val dot = (radius * .42f).coerceAtMost(3.dp.toPx())
    drawCircle(MapPalette.background, dot * 1.45f, p)
    drawCircle(MapPalette.threat, dot, p)
}

/** Service details belong to the full map; keep all services instead of silently choosing one. */
fun DrawScope.drawMapFeatures(cell: MinimapCellUi, center: Offset, radius: Float, servicePainters: Map<MinimapService, Painter>, availableWidth: Float = radius * 4f) {
    if (cell.isDark || cell.isPreview) return
    val services = MinimapService.entries.filter { it != MinimapService.EXIT && it in cell.services }
    if (services.isEmpty()) return
    val iconSize = minOf(11.dp.toPx(), availableWidth / (services.size * 1.2f - .2f))
    val spacing = iconSize * 1.2f
    services.forEachIndexed { index, service ->
        val p = center + Offset((index - (services.size - 1) / 2f) * spacing, radius + iconSize / 2 + 3.dp.toPx())
        translate(p.x - iconSize / 2, p.y - iconSize / 2) {
            with(servicePainters.getValue(service)) {
                draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(MapPalette.visited))
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
fun AreaMapCanvas(cells: List<MinimapCellUi>, modifier: Modifier = Modifier, minimap: Boolean = false,
    obscured: Boolean = false, scale: Float = 1f, offset: Offset = Offset.Zero,
    selectedId: String? = null, onSelect: ((MinimapCellUi) -> Unit)? = null) {
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
        fun point(cell: MinimapCellUi) = if (minimap) mid + Offset(cell.offsetX*step,-cell.offsetY*step)
            else mid + Offset((cell.gridX-(minX+maxX)/2f)*step, ((minY+maxY)/2f-cell.gridY)*step)
        val drawnCenters = visible.associate { it.roomId to point(it) }
        if (centers != drawnCenters) centers = drawnCenters
        hitRadius = maxOf(24.dp.toPx(), step*.4f)
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
                    drawLine(if (blocked) MapPalette.blocked else MapPalette.visited.copy(alpha = .55f), a, b,
                        if (minimap) (step * .07f).coerceAtMost(1.5.dp.toPx()) else 1.5.dp.toPx(), StrokeCap.Round,
                        pathEffect = if (blocked) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx())) else null)
                    if (blocked) drawMapLock((a+b)/2f)
                }
            }
        } } }
        visible.forEach { cell ->
            val p = point(cell)
            val radius = step * (if (cell.isCurrent) .25f else if (mapRoomHasExit(cell)) .21f else .16f)
            drawMapRoom(cell, p, radius, selected = !minimap && cell.roomId == selectedId)
            drawMapThreat(cell, p, radius)
            if (!minimap) drawMapFeatures(cell, p, radius, servicePainters, availableWidth = step * .8f)
        }
        if (!minimap) drawFullMapNodeExits(visible, ::point, step*.32f, textMeasurer)
    }
}

fun DrawScope.drawMapLock(p: Offset) {
    val r=3.dp.toPx()
    drawRoundRect(MapPalette.blocked,p-Offset(r,0f),Size(r*2,r*1.4f),CornerRadius(1.dp.toPx()))
    drawArc(MapPalette.blocked,180f,180f,false,p-Offset(r,r),Size(r*2,r*2),style=Stroke(1.dp.toPx()))
}
