package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.SkillTreeOverlayUi

@Composable
internal fun DesktopMenuSkillsContent(services: DesktopAppServices, tree: SkillTreeOverlayUi) {
    var branchId by rememberSaveable(tree.characterId) { mutableStateOf(tree.branches.firstOrNull()?.id) }
    val branch = tree.branches.firstOrNull { it.id == branchId } ?: tree.branches.firstOrNull()
    var selectedId by rememberSaveable(tree.characterId, branchId) { mutableStateOf<String?>(null) }
    val selected = branch?.nodes?.firstOrNull { it.id == selectedId } ?: branch?.nodes?.firstOrNull()
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    LaunchedEffect(branchId) { vertical.scrollTo(0); horizontal.scrollTo(0) }
    LaunchedEffect(selectedId) {
        if (selectedId == null) return@LaunchedEffect
        val node = selected ?: return@LaunchedEffect
        val minRow = branch?.nodes?.minOfOrNull { it.row } ?: 0
        val minCol = branch?.nodes?.minOfOrNull { it.column } ?: 0
        val top = ((node.row - minRow) * 130 + 20) * density
        val left = ((node.column - minCol) * 168 + 20) * density
        if (top < vertical.value) vertical.animateScrollTo(top.toInt())
        else if (top + 98 * density > vertical.value + vertical.viewportSize) vertical.animateScrollTo((top + 98 * density - vertical.viewportSize).toInt())
        if (left < horizontal.value) horizontal.animateScrollTo(left.toInt())
        else if (left + 144 * density > horizontal.value + horizontal.viewportSize) horizontal.animateScrollTo((left + 144 * density - horizontal.viewportSize).toInt())
    }
    val names = tree.branches.flatMap { it.nodes }.associate { it.id to it.name }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            tree.branches.forEach { entry ->
                FilterChip(entry.id == branch?.id, onClick = { branchId = entry.id }, modifier = Modifier.desktopPointerHover(), label = { Text(entry.title) })
            }
            Text("${tree.apInvested} AP invested", color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.labelMedium)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val compact = maxWidth < 600.dp
            val inspectorWidth = if (maxWidth < 850.dp) 230.dp else 280.dp
            val graph: @Composable (Modifier) -> Unit = { modifier ->
                DesktopMenuCard(modifier.onPreviewKeyEvent { event ->
                    val direction = when (event.key) { Key.DirectionLeft -> -1 to 0; Key.DirectionRight -> 1 to 0; Key.DirectionUp -> 0 to -1; Key.DirectionDown -> 0 to 1; else -> null }
                    if (event.type != KeyEventType.KeyDown || direction == null || selected == null) false else {
                        branch?.nodes.orEmpty().filter { node ->
                            if (direction.first != 0) (node.column - selected.column) * direction.first > 0 else (node.row - selected.row) * direction.second > 0
                        }.minByOrNull { node ->
                            if (direction.first != 0) kotlin.math.abs(node.column - selected.column) * 100 + kotlin.math.abs(node.row - selected.row)
                            else kotlin.math.abs(node.row - selected.row) * 100 + kotlin.math.abs(node.column - selected.column)
                        }?.let { selectedId = it.id }
                        true
                    }
                }) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(end = 12.dp, bottom = 12.dp).horizontalScroll(horizontal).verticalScroll(vertical)) {
                            val nodes = branch?.nodes.orEmpty()
                            val minRow = nodes.minOfOrNull { it.row } ?: 0
                            val minCol = nodes.minOfOrNull { it.column } ?: 0
                            val rows = (nodes.maxOfOrNull { it.row } ?: 0) - minRow + 1
                            val cols = (nodes.maxOfOrNull { it.column } ?: 0) - minCol + 1
                            Box(Modifier.size((cols * 168 + 40).dp, (rows * 130 + 40).dp)) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val byId = nodes.associateBy { it.id }
                                    fun center(row: Int, col: Int) = Offset(((col - minCol) * 168 + 92).dp.toPx(), ((row - minRow) * 130 + 69).dp.toPx())
                                    nodes.forEach { node -> node.requirements.forEach { requirement -> byId[requirement.id]?.let { parent ->
                                        drawLine(if (parent.status.unlocked) FieldMenuDesign.gold.copy(alpha = .65f) else FieldMenuDesign.cyan.copy(alpha = .18f),
                                            center(parent.row, parent.column), center(node.row, node.column), 2.dp.toPx())
                                    } } }
                                }
                                nodes.forEach { node ->
                                    val color = if (node.status.unlocked) FieldMenuDesign.gold else if (node.status.canPurchase) FieldMenuDesign.cyan else FieldMenuDesign.textMuted
                                    Surface(onClick = { selectedId = node.id }, modifier = Modifier.desktopPointerHover().offset(((node.column - minCol) * 168 + 20).dp, ((node.row - minRow) * 130 + 20).dp).size(144.dp, 98.dp),
                                        color = if (selected?.id == node.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.shell,
                                        shape = RoundedCornerShape(12.dp), border = BorderStroke(if (selected?.id == node.id) 2.dp else 1.dp, color.copy(alpha = if (selected?.id == node.id) 1f else .4f))) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Icon(if (node.status.unlocked) Icons.Default.CheckCircle else if (node.status.canPurchase) Icons.Default.Stars else Icons.Default.Lock, null, Modifier.size(18.dp), tint = color)
                                                Text(if (node.status.unlocked) "Unlocked" else "${node.costAp} AP", color = color, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Text(node.name, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                if (nodes.isEmpty()) Text("No skills available for this branch yet.", Modifier.padding(20.dp))
                            }
                        }
                        VerticalScrollbar(rememberScrollbarAdapter(vertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                        HorizontalScrollbar(rememberScrollbarAdapter(horizontal), Modifier.align(Alignment.BottomStart).fillMaxWidth())
                    }
                }
            }
            val details: @Composable (Modifier) -> Unit = { modifier ->
                DesktopMenuCard(modifier) {
                    DesktopMenuScrollPane(Modifier.fillMaxSize().padding(20.dp)) {
                        selected?.let { node ->
                            DesktopMenuSection(node.name)
                            Text(if (node.status.unlocked) "Unlocked" else "${node.costAp} AP", color = FieldMenuDesign.cyan)
                            node.description?.let { Text(it) }
                            if (node.requirements.isNotEmpty()) {
                                HorizontalDivider()
                                Text("Prerequisites", style = MaterialTheme.typography.labelLarge)
                                node.requirements.forEach { requirement ->
                                    Text(requirement.label, color = if (requirement.id in node.status.unmetRequirements) FieldMenuDesign.textMuted else FieldMenuDesign.gold)
                                }
                            }
                            node.status.unmetRequirements.filter { id -> node.requirements.none { it.id == id } }.forEach { Text(names[it] ?: it, color = FieldMenuDesign.textMuted) }
                            if (!node.status.unlocked && !node.status.meetsTierRequirement) Text("Invest ${node.status.requiredApForTier} AP in this tree to unlock this tier.", color = FieldMenuDesign.textMuted)
                            if (!node.status.unlocked && !node.status.hasEnoughAp) Text("Not enough AP", color = FieldMenuDesign.textMuted)
                            if (!node.status.unlocked) Button(onClick = { services.exploration.unlockSkillNode(node.id) }, enabled = node.status.canPurchase, modifier = Modifier.fillMaxWidth().desktopPointerHover(node.status.canPurchase)) { Text("Unlock skill") }
                        }
                    }
                }
            }
            if (!compact) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                graph(Modifier.weight(1f).fillMaxHeight()); details(Modifier.width(inspectorWidth).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                graph(Modifier.weight(1f).fillMaxWidth()); details(Modifier.height(190.dp).fillMaxWidth())
            }
        }
    }
}
