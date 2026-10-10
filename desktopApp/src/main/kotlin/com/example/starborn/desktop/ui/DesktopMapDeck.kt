package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import kotlinx.coroutines.launch

enum class DesktopMapPoiFilter(val label: String) {
    ALL("All Nodes"),
    SERVICES("Services"),
    THREATS("Threats"),
    EXITS("Exits")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopMapDeck(
    services: DesktopAppServices,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val ui by services.exploration.uiState.collectAsState()
    val rooms = remember(services) { services.worldDataSource.loadRooms().associateBy { it.id } }
    val cells = ui.fullMap?.cells.orEmpty().filter { it.discovered || it.visited || it.isCurrent }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var activeFilter by rememberSaveable { mutableStateOf(DesktopMapPoiFilter.ALL) }

    val selected = cells.firstOrNull { it.roomId == selectedId } ?: cells.firstOrNull { it.isCurrent }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val scope = rememberCoroutineScope()
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val minX = cells.minOfOrNull { it.gridX } ?: 0
    val maxY = cells.maxOfOrNull { it.gridY } ?: 0
    val width = ((cells.maxOfOrNull { it.gridX } ?: minX) - minX + 1) * 110 + 48
    val height = (maxY - (cells.minOfOrNull { it.gridY } ?: maxY) + 1) * 90 + 48
    val fitZoom = if (viewportWidth > 0 && viewportHeight > 0) {
        minOf(viewportWidth / density.density / width, viewportHeight / density.density / height).coerceIn(.45f, 1.25f)
    } else 1f

    LaunchedEffect(ui.currentHub?.id, viewportWidth, viewportHeight, cells.map { Triple(it.roomId, it.gridX, it.gridY) }) {
        if (cells.isNotEmpty() && viewportWidth > 0 && viewportHeight > 0) {
            zoom = fitZoom
            horizontal.scrollTo(0)
            vertical.scrollTo(0)
        }
    }

    fun changeZoom(next: Float) {
        val old = zoom
        zoom = next.coerceIn(.45f, 2.5f)
        val ratio = zoom / old
        scope.launch {
            withFrameNanos { }
            horizontal.scrollTo(((horizontal.value + viewportWidth / 2f) * ratio - viewportWidth / 2f).toInt())
            vertical.scrollTo(((vertical.value + viewportHeight / 2f) * ratio - viewportHeight / 2f).toInt())
        }
    }

    fun scrollToCell(cell: MinimapCellUi) {
        selectedId = cell.roomId
        scope.launch {
            val paddingX = ((viewportWidth - 12 * density.density - width * zoom * density.density) / 2).coerceAtLeast(0f)
            val paddingY = ((viewportHeight - 12 * density.density - height * zoom * density.density) / 2).coerceAtLeast(0f)
            horizontal.scrollTo((paddingX + ((cell.gridX - minX) * 110 + 66) * zoom * density.density - viewportWidth / 2f).toInt())
            vertical.scrollTo((paddingY + ((maxY - cell.gridY) * 90 + 32) * zoom * density.density - viewportHeight / 2f).toInt())
        }
    }

    fun recenter() {
        val current = cells.firstOrNull { it.isCurrent } ?: return
        scrollToCell(current)
    }

    fun cycleTargetNode(step: Int) {
        if (cells.isEmpty()) return
        val filteredList = when (activeFilter) {
            DesktopMapPoiFilter.ALL -> cells
            DesktopMapPoiFilter.SERVICES -> cells.filter { it.services.isNotEmpty() }
            DesktopMapPoiFilter.THREATS -> cells.filter { it.hasEnemies }
            DesktopMapPoiFilter.EXITS -> cells.filter { it.nodeExits.isNotEmpty() }
        }.ifEmpty { cells }

        val currentIndex = filteredList.indexOfFirst { it.roomId == (selected?.roomId ?: selectedId) }
        val nextIndex = if (currentIndex < 0) 0 else Math.floorMod(currentIndex + step, filteredList.size)
        val nextCell = filteredList[nextIndex]
        scrollToCell(nextCell)
    }

    val mapFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { mapFocusRequester.requestFocus() }
    }

    // Infinite radar sweep animation for currently active node
    val infiniteTransition = rememberInfiniteTransition(label = "map_radar_pulse")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(mapFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Equals, Key.Plus, Key.NumPadAdd -> { changeZoom(zoom + .15f); true }
                        Key.Minus, Key.NumPadSubtract -> { changeZoom(zoom - .15f); true }
                        Key.R, Key.F -> {
                            zoom = fitZoom
                            scope.launch { horizontal.scrollTo(0); vertical.scrollTo(0) }
                            true
                        }
                        Key.H -> { recenter(); true }
                        Key.Tab -> {
                            val step = if (event.isShiftPressed) -1 else 1
                            cycleTargetNode(step)
                            true
                        }
                        Key.T -> {
                            if (ui.canReturnToHub) {
                                services.exploration.requestReturnToHub()
                                onClose()
                                true
                            } else false
                        }
                        Key.DirectionLeft -> {
                            scope.launch { horizontal.scrollTo((horizontal.value - 40 * density.density).toInt()) }
                            true
                        }
                        Key.DirectionRight -> {
                            scope.launch { horizontal.scrollTo((horizontal.value + 40 * density.density).toInt()) }
                            true
                        }
                        Key.DirectionUp -> {
                            scope.launch { vertical.scrollTo((vertical.value - 40 * density.density).toInt()) }
                            true
                        }
                        Key.DirectionDown -> {
                            scope.launch { vertical.scrollTo((vertical.value + 40 * density.density).toInt()) }
                            true
                        }
                        else -> false
                    }
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP SECTOR TELEMETRY & CONTROLS HEADER
        DesktopMapHeader(
            hubTitle = ui.currentHub?.title ?: "Regional Sector Deck",
            cells = cells,
            selectedCell = selected,
            zoom = zoom,
            activeFilter = activeFilter,
            onSelectFilter = { activeFilter = it },
            onZoomIn = { changeZoom(zoom + .15f) },
            onZoomOut = { changeZoom(zoom - .15f) },
            onFit = {
                zoom = fitZoom
                scope.launch { horizontal.scrollTo(0); vertical.scrollTo(0) }
            },
            onCenterCurrent = ::recenter
        )

        // MAIN CONTENT AREA: MAP CANVAS (LEFT/CENTER) + WAYPOINT INSPECTOR (RIGHT)
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wideLayout = maxWidth >= 840.dp
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DesktopMapCanvas(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cells = cells,
                        selected = selected,
                        rooms = rooms,
                        services = services,
                        zoom = zoom,
                        minX = minX,
                        maxY = maxY,
                        width = width,
                        height = height,
                        activeFilter = activeFilter,
                        beaconPulse = beaconPulse,
                        horizontal = horizontal,
                        vertical = vertical,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        onViewportSizeChanged = { w, h -> viewportWidth = w; viewportHeight = h },
                        onSelectCell = { selectedId = it.roomId },
                        onChangeZoom = ::changeZoom,
                        density = density,
                        scope = scope
                    )

                    DesktopMapInspectorPane(
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                        selected = selected,
                        rooms = rooms,
                        cells = cells,
                        services = services,
                        canReturnToHub = ui.canReturnToHub,
                        hubTitle = ui.currentHub?.title,
                        onFastTravel = {
                            services.exploration.requestReturnToHub()
                            onClose()
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DesktopMapCanvas(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        cells = cells,
                        selected = selected,
                        rooms = rooms,
                        services = services,
                        zoom = zoom,
                        minX = minX,
                        maxY = maxY,
                        width = width,
                        height = height,
                        activeFilter = activeFilter,
                        beaconPulse = beaconPulse,
                        horizontal = horizontal,
                        vertical = vertical,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        onViewportSizeChanged = { w, h -> viewportWidth = w; viewportHeight = h },
                        onSelectCell = { selectedId = it.roomId },
                        onChangeZoom = ::changeZoom,
                        density = density,
                        scope = scope
                    )

                    DesktopMapInspectorPane(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        selected = selected,
                        rooms = rooms,
                        cells = cells,
                        services = services,
                        canReturnToHub = ui.canReturnToHub,
                        hubTitle = ui.currentHub?.title,
                        onFastTravel = {
                            services.exploration.requestReturnToHub()
                            onClose()
                        }
                    )
                }
            }
        }

        // BOTTOM DOCKED KEY LEGEND BAR
        DesktopMapBottomLegend(
            canFastTravel = ui.canReturnToHub,
            onFastTravel = {
                services.exploration.requestReturnToHub()
                onClose()
            },
            onCenterCurrent = ::recenter,
            onFit = {
                zoom = fitZoom
                scope.launch { horizontal.scrollTo(0); vertical.scrollTo(0) }
            }
        )
    }
}

@Composable
private fun DesktopMapHeader(
    hubTitle: String,
    cells: List<MinimapCellUi>,
    selectedCell: MinimapCellUi?,
    zoom: Float,
    activeFilter: DesktopMapPoiFilter,
    onSelectFilter: (DesktopMapPoiFilter) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    onCenterCurrent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF09141D),
        border = BorderStroke(1.dp, Color(0xFF1B3549)),
        modifier = Modifier.fillMaxWidth().testTag("map-header-telemetry")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = Color(0xFF63E6FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        hubTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    val visitedCount = cells.count { it.visited }
                    val totalCount = maxOf(1, cells.size)
                    val pct = (visitedCount * 100) / totalCount
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF63E6FF).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "SECTOR EXPLORED: $pct%",
                            color = Color(0xFF63E6FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    selectedCell?.let { cell ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF285E75).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF285E75).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "GRID: [X: ${cell.gridX}, Y: ${cell.gridY}]",
                                color = Color(0xFFC3D0D8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // ZOOM & FIT CONTROLS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DesktopTooltip("Center on Current Room [H]", FieldMenuDesign.cyan) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFC857).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onCenterCurrent() }.desktopPointerHover()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.MyLocation, null, Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                Text("You are here", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    DesktopTooltip("Zoom Out [-]", FieldMenuDesign.cyan) {
                        OutlinedButton(
                            onClick = onZoomOut,
                            enabled = zoom > .45f,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).desktopPointerHover()
                        ) { Text("-", fontSize = 13.sp) }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10202C),
                        border = BorderStroke(1.dp, Color(0xFF1B3549))
                    ) {
                        Text(
                            "${(zoom * 100).toInt()}%",
                            color = Color(0xFFC3D0D8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    DesktopTooltip("Zoom In [+]", FieldMenuDesign.cyan) {
                        OutlinedButton(
                            onClick = onZoomIn,
                            enabled = zoom < 2.5f,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).desktopPointerHover()
                        ) { Text("+", fontSize = 13.sp) }
                    }

                    DesktopTooltip("Fit Sector to Viewport [R / F]", FieldMenuDesign.cyan) {
                        OutlinedButton(
                            onClick = onFit,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).desktopPointerHover()
                        ) { Text("Fit", fontSize = 11.sp) }
                    }
                }
            }

            // LEGEND ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gold: current room", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                Text("Visited", color = Color(0xFF7FE6FF), style = MaterialTheme.typography.labelMedium)
                Text("Discovered", color = Color(0xFF8A99A4), style = MaterialTheme.typography.labelMedium)
                Text("Drag to pan / wheel to zoom", color = Color(0xFF8A99A4), style = MaterialTheme.typography.labelMedium)
            }

            // FILTER CHIPS ROW
            Row(
                modifier = Modifier.fillMaxWidth().testTag("map-filter-row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FILTERS:", color = Color(0xFF91A8B3), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                DesktopMapPoiFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    val count = when (filter) {
                        DesktopMapPoiFilter.ALL -> cells.size
                        DesktopMapPoiFilter.SERVICES -> cells.count { it.services.isNotEmpty() }
                        DesktopMapPoiFilter.THREATS -> cells.count { it.hasEnemies }
                        DesktopMapPoiFilter.EXITS -> cells.count { it.nodeExits.isNotEmpty() }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Color(0xFF63E6FF).copy(alpha = 0.2f) else Color(0xFF0F1E29),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF63E6FF) else Color(0xFF1D3546)),
                        modifier = Modifier.clickable { onSelectFilter(filter) }.desktopPointerHover()
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            color = if (isSelected) Color(0xFF63E6FF) else Color(0xFF8A99A4),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopMapCanvas(
    modifier: Modifier,
    cells: List<MinimapCellUi>,
    selected: MinimapCellUi?,
    rooms: Map<String, Room>,
    services: DesktopAppServices,
    zoom: Float,
    minX: Int,
    maxY: Int,
    width: Int,
    height: Int,
    activeFilter: DesktopMapPoiFilter,
    beaconPulse: Float,
    horizontal: ScrollState,
    vertical: ScrollState,
    viewportWidth: Int,
    viewportHeight: Int,
    onViewportSizeChanged: (Int, Int) -> Unit,
    onSelectCell: (MinimapCellUi) -> Unit,
    onChangeZoom: (Float) -> Unit,
    density: androidx.compose.ui.unit.Density,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Box(
        modifier = modifier
            .testTag("map-canvas-container")
            .onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (delta != 0f) {
                    onChangeZoom(zoom - delta * .1f)
                    event.changes.forEach { it.consume() }
                }
            }
            .onSizeChanged { onViewportSizeChanged(it.width, it.height) }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060F17))
            .border(1.dp, Color(0xFF1B3549), RoundedCornerShape(12.dp))
    ) {
        // High-tech Cyber Grid Canvas Background
        Canvas(Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            var x = size.width / 2 % step
            while (x < size.width) {
                drawLine(Color(0xFF63E6FF).copy(alpha = .04f), Offset(x, 0f), Offset(x, size.height))
                x += step
            }
            var y = size.height / 2 % step
            while (y < size.height) {
                drawLine(Color(0xFF63E6FF).copy(alpha = .04f), Offset(0f, y), Offset(size.width, y))
                y += step
            }
        }

        // Panning Canvas Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp, bottom = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        scope.launch {
                            horizontal.scrollTo((horizontal.value - drag.x).toInt())
                            vertical.scrollTo((vertical.value - drag.y).toInt())
                        }
                    }
                }
                .horizontalScroll(horizontal)
                .verticalScroll(vertical)
        ) {
            Box(
                modifier = Modifier.size(
                    maxOf(width * zoom, viewportWidth / density.density - 12f).coerceAtLeast(0f).dp,
                    maxOf(height * zoom, viewportHeight / density.density - 12f).coerceAtLeast(0f).dp
                ),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size((width * zoom).dp, (height * zoom).dp)) {
                    // Transit Vectors / Connecting Route Lines
                    Canvas(Modifier.fillMaxSize()) {
                        val known = cells.associateBy { it.roomId }
                        cells.forEach { cell ->
                            cell.connections.forEach { (direction, destination) ->
                                known[destination]?.let { target ->
                                    val isBlocked = direction in cell.blockedDirections
                                    val lineColor = if (isBlocked) Color(0xFFFF8844) else Color(0xFF285E75)
                                    val strokeWidth = if (isBlocked) 2.dp.toPx() else 2.5f.dp.toPx()
                                    val pathEffect = if (isBlocked) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null

                                    drawLine(
                                        color = lineColor,
                                        start = Offset(((cell.gridX - minX) * 110 + 66).times(zoom).dp.toPx(), ((maxY - cell.gridY) * 90 + 32).times(zoom).dp.toPx()),
                                        end = Offset(((target.gridX - minX) * 110 + 66).times(zoom).dp.toPx(), ((maxY - target.gridY) * 90 + 32).times(zoom).dp.toPx()),
                                        strokeWidth = strokeWidth,
                                        pathEffect = pathEffect
                                    )
                                }
                            }
                        }
                    }

                    // Room Waypoint Nodes
                    cells.forEach { cell ->
                        val isFilteredOut = when (activeFilter) {
                            DesktopMapPoiFilter.ALL -> false
                            DesktopMapPoiFilter.SERVICES -> cell.services.isEmpty()
                            DesktopMapPoiFilter.THREATS -> !cell.hasEnemies
                            DesktopMapPoiFilter.EXITS -> cell.nodeExits.isEmpty()
                        }
                        val opacity = if (isFilteredOut) 0.3f else 1.0f

                        val nodeColor = when {
                            cell.isCurrent -> Color(0xFFFFC857)
                            cell.hasEnemies -> Color(0xFFFF5252)
                            cell.services.isNotEmpty() -> Color(0xFF63E6FF)
                            cell.visited -> Color(0xFF7FE6FF)
                            else -> Color(0xFF687985)
                        }

                        val isTargetSelected = cell.roomId == selected?.roomId

                        Column(
                            modifier = Modifier
                                .offset(((cell.gridX - minX) * 110 + 16).times(zoom).dp, ((maxY - cell.gridY) * 90 + 16).times(zoom).dp)
                                .width((100 * zoom).dp)
                                .testTag("map-node-${cell.roomId}")
                                .clickable { onSelectCell(cell) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Pulsing Aura for Current Room
                                if (cell.isCurrent) {
                                    Surface(
                                        modifier = Modifier.size((42 * zoom * beaconPulse).dp),
                                        shape = CircleShape,
                                        color = Color(0xFFFFC857).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.4f))
                                    ) {}
                                }

                                // Waypoint Node Center
                                Surface(
                                    modifier = Modifier.size((34 * zoom).dp),
                                    shape = CircleShape,
                                    color = if (isTargetSelected) nodeColor.copy(alpha = .32f * opacity) else Color(0xFF0F1E29).copy(alpha = opacity),
                                    border = BorderStroke(
                                        width = if (cell.isCurrent || isTargetSelected) 2.5.dp else 1.2.dp,
                                        color = if (isTargetSelected) Color.White else nodeColor.copy(alpha = opacity)
                                    ),
                                    shadowElevation = if (isTargetSelected) 6.dp else 0.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val icon = when {
                                            cell.isCurrent -> Icons.Default.PersonPinCircle
                                            cell.hasEnemies -> Icons.Default.Warning
                                            cell.services.any { it.name == "SHOP" } -> Icons.Default.ShoppingCart
                                            cell.services.any { it.name == "TINKERING" } -> Icons.Default.Build
                                            cell.services.any { it.name == "COOKING" } -> Icons.Default.Restaurant
                                            cell.services.any { it.name == "EXIT" } -> Icons.AutoMirrored.Rounded.ExitToApp
                                            else -> Icons.Default.Room
                                        }
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size((18 * zoom).dp),
                                            tint = (if (isTargetSelected) Color.White else nodeColor).copy(alpha = opacity)
                                        )
                                    }
                                }
                            }

                            // Waypoint Title Label
                            val roomTitle = rooms[cell.roomId]?.title ?: services.contentName(cell.roomId)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isTargetSelected) Color(0xFF142B3A) else Color(0xD907111A)).copy(alpha = opacity),
                                border = BorderStroke(
                                    1.dp,
                                    (if (isTargetSelected) Color(0xFF63E6FF) else Color(0xFF1B3549)).copy(alpha = opacity)
                                )
                            ) {
                                Text(
                                    text = roomTitle,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = (if (isTargetSelected) Color.White else Color(0xFFC3D0D8)).copy(alpha = opacity),
                                    fontSize = (11 * zoom).coerceAtLeast(10f).sp,
                                    lineHeight = (13 * zoom).coerceAtLeast(12f).sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isTargetSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Scrollbars
        VerticalScrollbar(rememberScrollbarAdapter(vertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        HorizontalScrollbar(rememberScrollbarAdapter(horizontal), Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp))

        if (cells.isEmpty()) {
            Text(
                "Explore this sector to populate regional telemetry.",
                color = Color(0xFF91A8B3),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun DesktopMapInspectorPane(
    modifier: Modifier,
    selected: MinimapCellUi?,
    rooms: Map<String, Room>,
    cells: List<MinimapCellUi>,
    services: DesktopAppServices,
    canReturnToHub: Boolean,
    hubTitle: String?,
    onFastTravel: () -> Unit
) {
    Surface(
        modifier = modifier.testTag("map-inspector-pane"),
        color = Color(0xFF09141D),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1B3549))
    ) {
        DesktopMenuScrollPane(Modifier.fillMaxSize().padding(14.dp)) {
            selected?.let { cell ->
                val roomDef = rooms[cell.roomId]
                val roomTitle = roomDef?.title ?: services.contentName(cell.roomId)

                // Room Thumbnail Preview (if explored & has backdrop)
                if (!cell.isDark && roomDef?.backgroundImage != null) {
                    Image(
                        painter = rememberDesktopAssetPainter(roomDef.backgroundImage, services.assetProvider),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1B3549), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Sector Title & Coordinates
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = roomTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sector ID: ${cell.roomId}",
                        fontSize = 11.sp,
                        color = Color(0xFF91A8B3)
                    )
                }

                // Status Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cell.isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFC857).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFFC857))
                        ) {
                            Text("YOU ARE HERE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (cell.hasEnemies) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Text("THREATS DETECTED", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (cell.isDark) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFD3A1FF).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFD3A1FF))
                        ) {
                            Text("DARK ROOM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFD3A1FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (cell.isPreview) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF7FE6FF).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF7FE6FF).copy(alpha = 0.5f))
                        ) {
                            Text("UNEXPLORED PREVIEW", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF7FE6FF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1B3549))

                // DISCOVERED SERVICES
                if (cell.services.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SECTOR SERVICES", color = Color(0xFF63E6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            cell.services.forEach { svc ->
                                val (icon, label) = when (svc.name) {
                                    "SHOP" -> Icons.Default.ShoppingCart to "Vendor Kiosk"
                                    "TINKERING" -> Icons.Default.Build to "Field Workshop"
                                    "COOKING" -> Icons.Default.Restaurant to "Galley Station"
                                    "EXIT" -> Icons.AutoMirrored.Rounded.ExitToApp to "Transit Airlock"
                                    else -> Icons.Default.Star to svc.name
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0F2432),
                                    border = BorderStroke(1.dp, Color(0xFF285E75))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(icon, null, Modifier.size(12.dp), tint = Color(0xFF63E6FF))
                                        Text(label, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1B3549))
                }

                // TRANSIT VECTORS & EXITS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TRANSIT VECTORS", color = Color(0xFF63E6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val exits = cell.nodeExits
                    if (exits.isNotEmpty()) {
                        exits.forEach { exit ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0C1924),
                                border = BorderStroke(1.dp, if (exit.blocked) Color(0xFFFF8844).copy(alpha = 0.6f) else Color(0xFF1B3549)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            exit.direction.uppercase(),
                                            color = if (exit.blocked) Color(0xFFFF8844) else Color(0xFF63E6FF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(exit.destinationTitle, color = Color.White, fontSize = 12.sp)
                                    }
                                    if (exit.blocked) {
                                        Text("LOCKED", color = Color(0xFFFF8844), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("OPEN", color = Color(0xFF80E7A0), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else if (cell.connections.isNotEmpty()) {
                        cell.connections.forEach { (direction, destination) ->
                            val isBlocked = direction in cell.blockedDirections
                            val destTitle = if (cells.any { it.roomId == destination }) rooms[destination]?.title ?: services.contentName(destination) else "Unexplored Sector"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0C1924),
                                border = BorderStroke(1.dp, if (isBlocked) Color(0xFFFF8844).copy(alpha = 0.6f) else Color(0xFF1B3549)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(direction.uppercase(), color = if (isBlocked) Color(0xFFFF8844) else Color(0xFF63E6FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(destTitle, color = Color.White, fontSize = 12.sp)
                                    }
                                    if (isBlocked) {
                                        Text("BLOCKED", color = Color(0xFFFF8844), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("No direct transit vectors registered.", color = Color(0xFF91A8B3), fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF1B3549))

                // FAST TRAVEL ACTION CARD
                if (canReturnToHub) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF63E6FF).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.FlightTakeoff, null, tint = Color(0xFF63E6FF), modifier = Modifier.size(16.dp))
                                Text("FAST TRAVEL PROTOCOL", color = Color(0xFF63E6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Beacon link active. You may initiate rapid extraction back to ${hubTitle ?: "Hub"}.",
                                color = Color(0xFFC3D0D8),
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = onFastTravel,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF63E6FF), contentColor = Color(0xFF030A12)),
                                modifier = Modifier.fillMaxWidth().testTag("map-fast-travel-button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.FlightTakeoff, null, modifier = Modifier.size(16.dp))
                                    Text("Deploy to Hub [T]", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0A141D),
                        border = BorderStroke(1.dp, Color(0xFF1B3549)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("FAST TRAVEL OFFLINE", color = Color(0xFF91A8B3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Reach a safe haven beacon or gateway to activate sector fast travel.", color = Color(0xFF7E8F9B), fontSize = 11.sp)
                        }
                    }
                }
            } ?: run {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Room, null, tint = Color(0xFF466475), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Select a sector node to inspect waypoints and transit routes.", color = Color(0xFF91A8B3), textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DesktopMapBottomLegend(
    canFastTravel: Boolean,
    onFastTravel: () -> Unit,
    onCenterCurrent: () -> Unit,
    onFit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xDD060F18),
        border = BorderStroke(1.dp, Color(0xFF1B3549)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("map-bottom-legend")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopKeyBadge("Drag / Arrows", "Pan Sector")
                DesktopKeyBadge("Wheel / +/-", "Zoom View")
                DesktopKeyBadge("Tab", "Cycle Waypoints", highlighted = true)
                DesktopKeyBadge("H", "Center on You", onClick = onCenterCurrent)
                DesktopKeyBadge("R / F", "Fit Extents", onClick = onFit)
                if (canFastTravel) {
                    DesktopKeyBadge("T", "Fast Travel to Hub", highlighted = true, onClick = onFastTravel)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopKeyBadge("Esc", "Return to Field")
            }
        }
    }
}
