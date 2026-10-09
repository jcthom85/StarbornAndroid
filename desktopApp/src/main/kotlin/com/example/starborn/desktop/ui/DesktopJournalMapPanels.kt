package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import com.example.starborn.desktop.DesktopAppServices

@Composable
internal fun DesktopRuntimeJournalContent(services: DesktopAppServices) {
    val runtime = services.exploration
    val ui by runtime.uiState.collectAsState()
    var category by rememberSaveable { mutableStateOf(if (ui.questDetail?.completed == true) "Completed" else "Active") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(ui.questDetail?.id) }
    val quests = (if (category == "Completed") ui.questLogCompleted else ui.questLogActive)
        .filter { it.title.contains(query, true) || it.summary.contains(query, true) }
        .sortedWith(compareByDescending<com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi> { it.id == ui.trackedQuestId }.thenBy { it.title })
    val selected = quests.firstOrNull { it.id == selectedId } ?: quests.firstOrNull()
    LaunchedEffect(Unit) { services.questPresentations.readJournal() }
    LaunchedEffect(selected?.id, category) {
        if (category == "Active" || category == "Completed") selected?.let { runtime.openQuestDetails(it.id) } ?: runtime.closeQuestDetails()
        else runtime.closeQuestDetails()
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Active", "Completed", "History", "Milestones", "Fishing").forEach { tab ->
                FilterChip(category == tab, onClick = { category = tab; selectedId = null }, label = { Text(tab) })
            }
        }
        if (category == "Active" || category == "Completed") {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search quests") }, singleLine = true)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (quests.isEmpty()) item { Text("No quests match this view.") }
                    items(quests, key = { it.id }) { quest ->
                        Surface(Modifier.fillMaxWidth().clickable { selectedId = quest.id }, shape = RoundedCornerShape(12.dp),
                            color = if (selected?.id == quest.id) Color(0xFF173443) else Color(0xFF101D28),
                            border = BorderStroke(1.dp, if (selected?.id == quest.id) Color(0xFF63E6FF) else Color(0xFF354454))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(quest.title, style = MaterialTheme.typography.titleSmall)
                                Text(if (quest.completed) "Completed" else if (quest.id == ui.trackedQuestId) "TRACKED" else "Stage ${quest.stageIndex + 1} of ${quest.totalStages}",
                                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                selected?.let { quest ->
                    DesktopMenuCard(Modifier.weight(1.4f).fillMaxHeight()) {
                        DesktopMenuScrollPane(Modifier.fillMaxSize().padding(18.dp)) {
                            val authored = services.questRepository.questById(quest.id)
                            Text(if (quest.completed) "COMPLETED" else if (quest.id == ui.trackedQuestId) "TRACKED QUEST" else "ACTIVE QUEST", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelSmall)
                            Text(authored?.title ?: quest.title, style = MaterialTheme.typography.headlineSmall)
                            val summary = authored?.summary?.takeIf { it.isNotBlank() } ?: quest.summary
                            if (summary.isNotBlank()) Text(summary)
                            authored?.description?.takeIf { it.isNotBlank() && it != summary }?.let { Text(it) }
                            authored?.flavor?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                            val detail = ui.questDetail?.takeIf { it.id == quest.id }
                            if (detail == null) Text("Loading quest details...") else {
                                val stages = detail.stages
                                if (stages.isNotEmpty()) stages.forEachIndexed { index, stage ->
                                    HorizontalDivider()
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(shape = CircleShape, color = if (stage.completed) FieldMenuDesign.gold.copy(alpha = .18f) else if (stage.current) FieldMenuDesign.cyan.copy(alpha = .18f) else FieldMenuDesign.panel,
                                            border = BorderStroke(1.dp, if (stage.completed) FieldMenuDesign.gold else if (stage.current) FieldMenuDesign.cyan else FieldMenuDesign.border)) {
                                            Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (stage.completed) FieldMenuDesign.gold else FieldMenuDesign.text)
                                        }
                                        Text(stage.title, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(if (stage.completed) "Completed" else if (stage.current) "Current stage" else "Upcoming", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    stage.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                                    stage.objectives.forEach { objective ->
                                        Row(Modifier.semantics { contentDescription = "${if (objective.completed) "Completed" else "Incomplete"} objective: ${objective.text}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(if (objective.completed) androidx.compose.material.icons.Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Default.RadioButtonUnchecked, null, Modifier.size(18.dp), tint = if (objective.completed) FieldMenuDesign.gold else FieldMenuDesign.cyan)
                                            Text(objective.text, Modifier.weight(1f), color = if (objective.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                } else {
                                    detail.stageTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                                    detail.stageDescription?.let { Text(it) }
                                    detail.objectives.forEach { objective ->
                                        Text("${if (objective.completed) "Completed" else "Incomplete"}: ${objective.text}")
                                    }
                                }
                                if (detail.rewards.isNotEmpty()) {
                                    HorizontalDivider(); Text("Rewards", style = MaterialTheme.typography.titleMedium)
                                    detail.rewards.forEach { Text(it) }
                                }
                            }
                            if (!quest.completed) Button(onClick = { runtime.toggleQuestTracking(quest.id) }) {
                                Text(if (quest.id == ui.trackedQuestId) "Stop tracking" else "Track this quest")
                            }
                        }
                    }
                }
            }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (category == "History") {
                if (ui.questLogEntries.isEmpty()) item { Text("Your quest history will appear here.") }
                items(ui.questLogEntries.sortedByDescending { it.timestamp }) { entry ->
                    DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.questTitle?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        Text(entry.message)
                    } }
                }
            } else if (category == "Fishing") {
                if (ui.fishingJournal.isEmpty()) item { Text("No fishing records yet.") }
                items(ui.fishingJournal, key = { it.zoneId }) { zone ->
                    DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(zone.name, style = MaterialTheme.typography.titleMedium)
                        zone.species.forEach { species ->
                            Text("${if (species.caught) species.name else "Undiscovered species"}: ${if (species.clean) "Clean catch" else if (species.caught) "Caught" else "Not caught"}")
                        }
                    } }
                }
            } else {
                if (ui.milestoneHistory.isEmpty()) item { Text("Your journey's milestones will appear here.") }
                items(ui.milestoneHistory) { milestone -> DesktopMenuCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(milestone.title, color = MaterialTheme.colorScheme.primary); Text(milestone.message)
                    }
                } }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopRuntimeMapContent(services: DesktopAppServices) {
    val ui by services.exploration.uiState.collectAsState()
    val rooms = remember(services) { services.worldDataSource.loadRooms().associateBy { it.id } }
    val cells = ui.fullMap?.cells.orEmpty().filter { it.discovered || it.visited || it.isCurrent }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
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
    val fitZoom = if (viewportWidth > 0 && viewportHeight > 0) minOf(viewportWidth / density.density / width, viewportHeight / density.density / height).coerceIn(.45f, 1.25f) else 1f
    LaunchedEffect(ui.currentHub?.id, viewportWidth, viewportHeight, cells.map { Triple(it.roomId, it.gridX, it.gridY) }) {
        zoom = fitZoom
        horizontal.scrollTo(0); vertical.scrollTo(0)
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
    fun recenter() {
        val current = cells.firstOrNull { it.isCurrent } ?: return
        selectedId = current.roomId
        scope.launch {
            val paddingX = ((viewportWidth - 12 * density.density - width * zoom * density.density) / 2).coerceAtLeast(0f)
            val paddingY = ((viewportHeight - 12 * density.density - height * zoom * density.density) / 2).coerceAtLeast(0f)
            horizontal.scrollTo((paddingX + ((current.gridX - minX) * 110 + 66) * zoom * density.density - viewportWidth / 2f).toInt())
            vertical.scrollTo((paddingY + ((maxY - current.gridY) * 90 + 32) * zoom * density.density - viewportHeight / 2f).toInt())
        }
    }
    val details: @Composable (Modifier) -> Unit = { modifier ->
        Surface(modifier, color = Color(0xFF0A1720), shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF7FE6FF).copy(alpha = .16f))) {
            DesktopMenuScrollPane(Modifier.fillMaxSize().padding(16.dp)) {
                selected?.let { cell ->
                    if (!cell.isDark) rooms[cell.roomId]?.backgroundImage?.let { path ->
                        Image(rememberDesktopAssetPainter(path, services.assetProvider), null,
                            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
                    }
                    Text(rooms[cell.roomId]?.title ?: services.contentName(cell.roomId), style = MaterialTheme.typography.titleMedium)
                    if (cell.isCurrent) Text("You are here", color = Color(0xFFFFC857))
                    if (cell.services.isNotEmpty()) Text("Services: ${cell.services.joinToString { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }}")
                    if (cell.isDark) Text("Dark room")
                    if (cell.isPreview) Text("Preview - not yet explored")
                    if (cell.hasEnemies) Text("Threats present")
                    cell.pathHints.forEach { Text("Path: $it", style = MaterialTheme.typography.bodySmall) }
                    cell.nodeExits.forEach { exit -> Text("${exit.direction} -> ${exit.destinationTitle}${if (exit.blocked) " (locked)" else ""}", style = MaterialTheme.typography.bodySmall) }
                    cell.connections.forEach { (direction, destination) ->
                        Text("$direction -> ${if (cells.any { it.roomId == destination }) rooms[destination]?.title ?: services.contentName(destination) else "Unexplored"}${if (direction in cell.blockedDirections) " (locked)" else ""}", style = MaterialTheme.typography.bodySmall)
                    }
                } ?: Text("Select a room to inspect its exits.")
            }
        }
    }
    val map: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier.onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
            if (delta != 0f) { changeZoom(zoom - delta * .1f); event.changes.forEach { it.consume() } }
        }.onSizeChanged { viewportWidth = it.width; viewportHeight = it.height }.clip(RoundedCornerShape(12.dp)).background(Color(0xFF07111A)).border(1.dp, Color(0xFF7FE6FF).copy(alpha = .16f), RoundedCornerShape(12.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 32.dp.toPx()
                var x = size.width / 2 % step
                while (x < size.width) { drawLine(Color(0xFF7FE6FF).copy(alpha = .035f), Offset(x, 0f), Offset(x, size.height)); x += step }
                var y = size.height / 2 % step
                while (y < size.height) { drawLine(Color(0xFF7FE6FF).copy(alpha = .035f), Offset(0f, y), Offset(size.width, y)); y += step }
            }
            Box(Modifier.fillMaxSize().padding(end = 12.dp, bottom = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        scope.launch { horizontal.scrollTo((horizontal.value - drag.x).toInt()); vertical.scrollTo((vertical.value - drag.y).toInt()) }
                    }
                }.horizontalScroll(horizontal).verticalScroll(vertical)) {
                Box(Modifier.size(maxOf(width * zoom, viewportWidth / density.density - 12f).coerceAtLeast(0f).dp,
                    maxOf(height * zoom, viewportHeight / density.density - 12f).coerceAtLeast(0f).dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size((width * zoom).dp, (height * zoom).dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val known = cells.associateBy { it.roomId }
                        cells.forEach { cell -> cell.connections.forEach { (direction, destination) -> known[destination]?.let { target ->
                            drawLine(if (direction in cell.blockedDirections) Color(0xFF986D46) else Color(0xFF466475),
                                Offset(((cell.gridX - minX) * 110 + 66).times(zoom).dp.toPx(), ((maxY - cell.gridY) * 90 + 32).times(zoom).dp.toPx()),
                                Offset(((target.gridX - minX) * 110 + 66).times(zoom).dp.toPx(), ((maxY - target.gridY) * 90 + 32).times(zoom).dp.toPx()), 2.dp.toPx())
                        } } }
                    }
                    cells.forEach { cell ->
                        val nodeColor = if (cell.isCurrent) Color(0xFFFFC857) else if (cell.visited) Color(0xFF7FE6FF) else Color(0xFF687985)
                        Column(Modifier.offset(((cell.gridX - minX) * 110 + 16).times(zoom).dp, ((maxY - cell.gridY) * 90 + 16).times(zoom).dp)
                            .width((100 * zoom).dp).clickable { selectedId = cell.roomId }, horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(Modifier.size((32 * zoom).dp), shape = CircleShape,
                                color = if (cell.roomId == selected?.roomId) nodeColor.copy(alpha = .28f) else Color(0xFF10202C),
                                border = BorderStroke(if (cell.isCurrent || cell.roomId == selected?.roomId) 2.dp else 1.dp, nodeColor)) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = when {
                                        cell.hasEnemies -> Icons.Default.Warning
                                        cell.services.any { it.name == "SHOP" } -> Icons.Default.ShoppingCart
                                        cell.services.any { it.name == "TINKERING" } -> Icons.Default.Build
                                        cell.services.any { it.name == "COOKING" } -> Icons.Default.Restaurant
                                        cell.services.any { it.name == "EXIT" } -> Icons.Default.ExitToApp
                                        cell.isCurrent -> Icons.Default.PersonPinCircle
                                        else -> Icons.Default.Room
                                    }
                                    Icon(icon, null, Modifier.size((18 * zoom).dp), tint = nodeColor)
                                }
                            }
                            Text(rooms[cell.roomId]?.title ?: services.contentName(cell.roomId), Modifier.fillMaxWidth().background(Color(0xD907111A), RoundedCornerShape(4.dp)).padding(3.dp),
                                color = if (cell.roomId == selected?.roomId) Color.White else Color(0xFFC3D0D8),
                                fontSize = (11 * zoom).coerceAtLeast(10f).sp, lineHeight = (14 * zoom).coerceAtLeast(12f).sp,
                                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            }
            VerticalScrollbar(rememberScrollbarAdapter(vertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            HorizontalScrollbar(rememberScrollbarAdapter(horizontal), Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp))
            if (cells.isEmpty()) Text("Explore this area to reveal its map.", Modifier.align(Alignment.Center))
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui.currentHub?.title.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = ::recenter) { Text("You are here") }
            Text("N \u2191", style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = { changeZoom(zoom - .15f) }, enabled = zoom > .45f) { Text("-") }
            TextButton(onClick = { zoom = fitZoom; scope.launch { horizontal.scrollTo(0); vertical.scrollTo(0) } }) { Text("Fit") }
            OutlinedButton(onClick = { changeZoom(zoom + .15f) }, enabled = zoom < 2.5f) { Text("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Current", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
            Text("Visited", color = Color(0xFF7FE6FF), style = MaterialTheme.typography.labelMedium)
            Text("Discovered", color = Color(0xFF8A99A4), style = MaterialTheme.typography.labelMedium)
            Text("Drag to pan / wheel to zoom", color = Color(0xFF8A99A4), style = MaterialTheme.typography.labelMedium)
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            if (maxWidth >= 700.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                map(Modifier.weight(1f).fillMaxHeight()); details(Modifier.width(250.dp).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                map(Modifier.weight(1f).fillMaxWidth()); details(Modifier.heightIn(max = 140.dp).fillMaxWidth())
            }
        }
    }
}
