package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.TravelAction
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorUi
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorStatus
import com.example.starborn.data.local.UserSettings
import kotlin.math.roundToInt

private val PanelInk = Color(0xE607111A)
private val PanelBorder = Color(0xFF33434E)
private val PanelCyan = Color(0xFF63E6FF)
private val PanelWarm = Color(0xFFFF9F2E)

internal fun hasThreePanelSpace(layout: PortraitBackdropLayout, density: Float): Boolean =
    minOf(layout.left.width, layout.right.width) / density >= 276f

/** Pure presentation filter: every interaction remains available without duplicating the prose. */
internal fun unmatchedRoomTargets(ui: ExplorationUiState, plan: InlineActionPlan?, isDark: Boolean): List<InlineActionTarget> {
    val represented = plan?.segments.orEmpty().map { it.target }
    val actionKeys = represented.filterIsInstance<InlineActionTarget.Room>().map { it.action.actionKey() }.toSet()
    val npcs = represented.filterIsInstance<InlineActionTarget.Npc>().map { it.name }.toSet()
    val enemies = represented.filterIsInstance<InlineActionTarget.Enemy>().map { it.id }.toSet()
    return buildList {
        ui.actions.distinctBy { it.actionKey() }.filterNot { it.actionKey() in actionKeys || (it is RestStopAction && it.name.equals("bunk", true)) }.forEach { add(InlineActionTarget.Room(it)) }
        if (!isDark) {
            ui.npcs.distinct().filterNot { it in npcs }.forEach { add(InlineActionTarget.Npc(it)) }
            ui.enemies.distinct().filterNot { it in enemies }.forEach { add(InlineActionTarget.Enemy(it, it.replace('_', ' '))) }
        }
    }
}

@Composable
internal fun DesktopExplorationPanels(
    services: DesktopAppServices, ui: ExplorationUiState, layout: PortraitBackdropLayout,
    description: String?, plan: InlineActionPlan?, isDark: Boolean, blocked: Boolean,
    onMenu: (DesktopMenuTab) -> Unit
) {
    val density = LocalDensity.current
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val wide = hasThreePanelSpace(layout, density.density)
    val drawer = LocalExplorationDrawer.current ?: remember { ExplorationDrawerState() }
    var compactPanel by drawer::panel
    LaunchedEffect(wide, ui.currentRoom?.id) { compactPanel = null }
    val accent = ui.theme?.accent?.takeIf { it.size >= 3 }?.let { Color(it[0], it[1], it[2], it.getOrElse(3) { 1f }) } ?: PanelWarm
    CompositionLocalProvider(LocalExplorationAccent provides accent) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (wide) {
            val leftWidth = with(density) { layout.left.width.toDp() }
            val rightX = with(density) { layout.right.left.toDp() }
            val rightWidth = with(density) { layout.right.width.toDp() }
            Box(Modifier.width(leftWidth).fillMaxHeight().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)) {
                DesktopNarrativeArea(services, ui, description, plan, isDark, blocked,
                    Modifier.align(Alignment.TopEnd).widthIn(max = 400.dp).fillMaxSize())
            }
            Box(Modifier.offset(x = rightX).width(rightWidth).fillMaxHeight().padding(start = 16.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)) {
                DesktopIndependentHud(services, ui, isDark, blocked, onMenu,
                    Modifier.align(Alignment.TopStart).widthIn(max = 300.dp).fillMaxSize())
            }
        } else {
            Row(Modifier.align(Alignment.TopCenter).padding(12.dp).background(if (settings.highContrastMode) Color.Black else PanelInk, RoundedCornerShape(12.dp)).border(1.dp, if (settings.highContrastMode) Color.White else PanelCyan.copy(alpha = .25f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { compactPanel = if (compactPanel == "room") null else "room" }, enabled = !blocked) { Text("Room") }
                OutlinedButton(onClick = { compactPanel = if (compactPanel == "status") null else "status" }, enabled = !blocked) { Text("Status") }
                OutlinedButton(onClick = { onMenu(DesktopMenuTab.STATS) }, enabled = !blocked) { Text("Menu") }
            }
            if (compactPanel != null) {
                Column(Modifier.align(if (compactPanel == "room") Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = 68.dp, start = 12.dp, end = 12.dp, bottom = 16.dp)
                    .widthIn(max = if (compactPanel == "room") 400.dp else 300.dp).fillMaxWidth()
                    .height((maxHeight - 84.dp).coerceAtLeast(100.dp)).zIndex(2f)
                    .background(if (settings.highContrastMode) Color.Black else PanelInk, RoundedCornerShape(12.dp))) {
                    Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { compactPanel = null }) { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Close") }
                    }
                    val overlay = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
                    if (compactPanel == "room") DesktopNarrativeArea(services, ui, description, plan, isDark, blocked, overlay, compact = true)
                    else DesktopIndependentHud(services, ui, isDark, blocked, onMenu, overlay)
                }
            }
        }
        DesktopRoomNavigation(ui, layout, blocked, compact = !wide, animate = !settings.disableFlashes && !settings.disableScreenshake, onTravel = services.exploration::travel)
    }
    }
}

@Composable
private fun DesktopNarrativeArea(services: DesktopAppServices, ui: ExplorationUiState, description: String?, plan: InlineActionPlan?,
    isDark: Boolean, blocked: Boolean, modifier: Modifier, compact: Boolean = false) {
    val runtime = services.exploration
    val accent = LocalExplorationAccent.current
    val settings = LocalExplorationSettings.current
    val bodyScroll = remember(ui.currentRoom?.id) { ScrollState(0) }
    val contextScroll = remember(ui.currentRoom?.id) { ScrollState(0) }
    BoxWithConstraints(modifier) {
        val narrativeHeightLimit = maxHeight * .6f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(Modifier.fillMaxWidth().heightIn(max = narrativeHeightLimit).testTag("exploration-narrative"),
                shape = RoundedCornerShape(12.dp), color = if (settings.highContrastMode) Color.Black else Color(0xEB07111A), border = BorderStroke(1.dp, if (settings.highContrastMode) Color.White else accent.copy(alpha = .24f))) {
                Column(Modifier.drawBehind { drawRect(accent.copy(alpha = .85f), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
                    .padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DesktopRoomHeading(if (isDark && ui.currentRoom?.revealTitleWhenDark != true) "Dark room" else ui.currentRoom?.title ?: "Loading?",
                        if (settings.highContrastMode) Color.White else accent, compact)
                    DesktopExplorationScrollPane(bodyScroll, Modifier.weight(1f, fill = false).fillMaxWidth()) {
                    RoomDescription(plan, description, isDark, Color(0xFFF0F3F5),
                        onAction = { if (!blocked) runtime.onActionSelected(it) },
                        onNpcClick = { if (!blocked && !isDark) runtime.onNpcInteraction(it) },
                        onEnemyClick = { if (!blocked && !isDark) runtime.engageEnemy(it) }, accentColor = PanelCyan,
                        modifier = Modifier.fillMaxWidth().testTag("narrative-body"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp))
                    }
                }
            }
            DesktopExplorationScrollPane(contextScroll, Modifier.weight(1f, fill = false).fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DesktopContextualControls(services, ui, plan, isDark, blocked)
                listOfNotNull(ui.enemyMovementNotice, ui.nearbyThreatDirection?.let { "Threat nearby: $it" }).forEach {
                    Text(it, color = Color(0xFFFF998A), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.background(PanelInk, RoundedCornerShape(8.dp)).padding(10.dp))
                }
            }
            }
        }
    }
}

@Composable
private fun DesktopContextualControls(services: DesktopAppServices, ui: ExplorationUiState, plan: InlineActionPlan?, isDark: Boolean, blocked: Boolean) {
    val runtime = services.exploration
    val remaining = unmatchedRoomTargets(ui, plan, isDark).filterNot { it is InlineActionTarget.Npc || it is InlineActionTarget.Enemy && ui.visualEnemyParties.isNotEmpty() }
    val specialExits = ui.availableConnections.filter { (direction, target) -> target != null && direction.lowercase() !in cardinalDirections &&
        ui.actions.filterIsInstance<TravelAction>().none { it.direction.equals(direction, true) } }
    if (remaining.isEmpty() && (isDark || ui.groundItems.isEmpty()) && specialExits.isEmpty() && (isDark || ui.npcs.isEmpty()) && !ui.canReturnToHub) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (ui.canReturnToHub) DesktopOverworldGateway(ui, blocked, runtime::requestReturnToHub)
        if (!isDark && ui.npcs.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("npc-presence"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.npcs.distinct().forEach { npc -> DesktopNpcPresenceChip(services, ui, npc, blocked) }
            }
        }
        if (remaining.isNotEmpty() || (!isDark && ui.groundItems.isNotEmpty()) || specialExits.isNotEmpty()) {
            ExplorationHudCard(Modifier.testTag("exploration-context")) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    remaining.forEach { target ->
                        when (target) {
                            is InlineActionTarget.Room -> DesktopExplorationActionRow(target.action.name,
                                when { (target.action as? RestStopAction)?.cookSource != null -> "cook"; target.action is RestStopAction -> "rest"; target.action is TravelAction -> "travel"; else -> "inspect" },
                                !blocked && ui.actionHints[target.action.actionKey()]?.locked != true) { runtime.onActionSelected(target.action) }
                            is InlineActionTarget.Npc -> Unit
                            is InlineActionTarget.Enemy -> DesktopExplorationActionRow("Engage ${services.enemyDefinitions[target.id]?.name ?: target.label}", "enemy", !blocked) { runtime.engageEnemy(target.id) }
                        }
                    }
                    if (!isDark) ui.groundItems.forEach { (item, quantity) ->
                        DesktopExplorationActionRow("Collect ${runtime.itemDisplayName(item)} \u00D7$quantity", "item", !blocked) { runtime.collectGroundItem(item) }
                    }
                    specialExits.forEach { (direction, _) ->
                        DesktopExplorationActionRow(direction, "travel", !blocked && ui.blockedDirections.none { it.equals(direction, true) }) { runtime.travel(direction) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopIndependentHud(services: DesktopAppServices, ui: ExplorationUiState, isDark: Boolean, blocked: Boolean,
    onMenu: (DesktopMenuTab) -> Unit, modifier: Modifier) {
    val detailsScroll = rememberScrollState()
    Column(modifier.testTag("exploration-status"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isDark || ui.minimap?.cells?.any { it.discovered || it.visited || it.isCurrent } == true) {
            val map = ui.minimap
            ExplorationHudCard(Modifier.testTag("hud-minimap")) {
                Box(Modifier.padding(10.dp)) { DesktopExplorationMinimap(map, !blocked, obscured = isDark, roomName = services::roomTitle) { onMenu(DesktopMenuTab.MAP) } }
            }
        }
        DesktopExplorationScrollPane(detailsScroll, Modifier.weight(1f).fillMaxWidth().testTag("hud-details")) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DesktopObjectiveCard(services, ui, blocked, onMenu)
            DesktopPartyCard(services, ui, blocked, onMenu)
        }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { onMenu(DesktopMenuTab.STATS) }, enabled = !blocked,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (LocalExplorationSettings.current.highContrastMode) Color.Black else PanelInk, contentColor = Color.White),
                border = BorderStroke(1.dp, if (LocalExplorationSettings.current.highContrastMode) Color.White else PanelBorder), modifier = Modifier.heightIn(min = if (LocalExplorationSettings.current.largeTouchTargets) 56.dp else 44.dp).explorationFeedback(!blocked), shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                Icon(Icons.Rounded.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Menu", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DesktopObjectiveCard(services: DesktopAppServices, ui: ExplorationUiState, blocked: Boolean, onMenu: (DesktopMenuTab) -> Unit) {
    DesktopTrackedQuestCard(services, ui, blocked) { onMenu(DesktopMenuTab.JOURNAL) }
}

@Composable
private fun DesktopPartyCard(services: DesktopAppServices, ui: ExplorationUiState, blocked: Boolean, onMenu: (DesktopMenuTab) -> Unit) {
    if (ui.partyStatus.members.isEmpty()) return
    val short = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height / LocalDensity.current.density < 800f
    val settings = LocalExplorationSettings.current
    ExplorationHudCard(Modifier.testTag("hud-party")) {
        Column(Modifier.fillMaxWidth().explorationFeedback(!blocked).clickable(enabled = !blocked) { onMenu(DesktopMenuTab.STATS) }
            .semantics { contentDescription = "Open party menu" }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PARTY", color = Color(0xFF91A8B3), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Text("Manage", color = PanelCyan.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
            }
            ui.partyStatus.members.forEach { member ->
                Row(Modifier.heightIn(min = if (settings.largeTouchTargets) 56.dp else if (short) 44.dp else 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    member.portraitPath?.let { Image(rememberDesktopAssetPainter(it, services.assetProvider), null,
                        Modifier.size(if (short) 32.dp else 36.dp).clip(RoundedCornerShape(10.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(member.name, color = Color(0xFFE0EBEF), style = MaterialTheme.typography.labelLarge)
                            Text("Lv. ${member.level}", color = Color(0xFF91A8B3), style = MaterialTheme.typography.labelSmall)
                        }
                        member.hpLabel?.let { Text(it, color = Color(0xFF91A8B3), style = MaterialTheme.typography.labelSmall) }
                        member.hpProgress?.let { hp -> LinearProgressIndicator(progress = { hp.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = Color(0xFF65BFA5), trackColor = Color(0xFF203B39)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorationHudCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val contrast = LocalExplorationSettings.current.highContrastMode
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (contrast) Color.Black else Color(0xE007111A),
        border = BorderStroke(1.dp, if (contrast) Color.White else PanelBorder.copy(alpha = .55f)), content = content)
}

private val cardinalDirections = setOf("north", "south", "east", "west")

@Composable
private fun BoxScope.DesktopRoomNavigation(ui: ExplorationUiState, layout: PortraitBackdropLayout, blocked: Boolean,
    compact: Boolean, animate: Boolean, onTravel: (String) -> Unit) {
    val density = LocalDensity.current
    val indicators = if (ui.directionIndicators.isNotEmpty()) ui.directionIndicators.values.toList() else
        ui.availableConnections.filter { (direction, target) -> target != null && direction.lowercase() in cardinalDirections }.map { (direction, _) ->
            DirectionIndicatorUi(direction, if (ui.blockedDirections.any { it.equals(direction, true) }) DirectionIndicatorStatus.LOCKED else DirectionIndicatorStatus.UNEXPLORED)
        }
    indicators.forEach { indicator ->
        val direction = indicator.direction.lowercase()
        val center = layout.center
        val largeTargets = LocalExplorationSettings.current.largeTouchTargets
        val inset = with(density) { (if (largeTargets) 32.dp else 28.dp).toPx() }
        val northY = center.top + if (compact) with(density) { 102.dp.toPx() } else inset
        val position = when (direction) {
            "north" -> Offset(center.center.x, northY)
            "south" -> Offset(center.center.x, center.bottom - inset)
            "west" -> Offset(center.left + inset, center.center.y)
            "east" -> Offset(center.right - inset, center.center.y)
            "northeast" -> Offset(center.right - inset, northY)
            "northwest" -> Offset(center.left + inset, northY)
            "southeast" -> Offset(center.right - inset, center.bottom - inset)
            "southwest" -> Offset(center.left + inset, center.bottom - inset)
            else -> return@forEach
        }
        DesktopDirectionMarker(direction, indicator.status, blocked || ui.blockedDirections.any { it.equals(direction, true) }, animate,
            Modifier.offset { IntOffset((position.x - inset).roundToInt(), (position.y - inset).roundToInt()) }, onTravel)
    }
}

@Composable
private fun DesktopNpcPresenceChip(services: DesktopAppServices, ui: ExplorationUiState, npc: String, blocked: Boolean) {
    val key = npc.trim().lowercase(java.util.Locale.ROOT)
    val label = ui.npcPresenceNames[key] ?: npc
    val portrait = ui.npcPortraitPaths[key]
    Surface(onClick = { services.exploration.onNpcInteraction(npc) }, enabled = !blocked,
        modifier = Modifier.explorationFeedback(!blocked).widthIn(min = 152.dp, max = 208.dp).height(if (LocalExplorationSettings.current.largeTouchTargets) 64.dp else 54.dp)
            .testTag("npc-$key").semantics { contentDescription = "Talk to $label" },
        color = if (LocalExplorationSettings.current.highContrastMode) Color.Black else Color(0xE6071018), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (LocalExplorationSettings.current.highContrastMode) Color.White else PanelCyan.copy(alpha = .54f))) {
        Row(Modifier.background(Brush.horizontalGradient(listOf(PanelCyan.copy(alpha = .18f), Color.Transparent)))
            .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).border(1.dp, PanelCyan.copy(alpha = .72f), CircleShape).padding(3.dp)
                .clip(CircleShape).background(PanelCyan.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                if (!portrait.isNullOrBlank()) Image(rememberDesktopAssetPainter(portrait, services.assetProvider), null,
                    Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                else Text(label.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White)
            }
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DesktopOverworldGateway(ui: ExplorationUiState, blocked: Boolean, onOpen: () -> Unit) {
    val disembarking = ui.currentRoom?.id == com.example.starborn.domain.session.AstraTravel.ENTRY_ROOM_ID
    Surface(onClick = onOpen, enabled = !blocked, modifier = Modifier.fillMaxWidth().explorationFeedback(!blocked).testTag("overworld-gateway")
        .semantics { contentDescription = if (disembarking) "Disembark from the Astra" else "Exit to Overworld" },
        color = if (LocalExplorationSettings.current.highContrastMode) Color.Black else Color(0xE6060F17), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, if (LocalExplorationSettings.current.highContrastMode) Color.White else PanelCyan.copy(alpha = .35f))) {
        Row(Modifier.background(Brush.horizontalGradient(listOf(PanelCyan.copy(alpha = .10f), Color.Transparent)))
            .padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Map, null, Modifier.size(26.dp), tint = PanelCyan)
            Text(if (disembarking) "DISEMBARK" else "OVERWORLD MAP", Modifier.weight(1f), color = Color.White,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("OPEN >", color = PanelCyan, style = MaterialTheme.typography.labelSmall)
        }
    }
}
