package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.hub.viewmodel.*
import kotlinx.coroutines.delay

/** Shared controller owns selection, discovery, travel, and quest presentation. */
@Composable
fun DesktopHubScreen(services: DesktopAppServices, onEnterRoom: (String) -> Unit,
    onBackToExploration: () -> Unit, onReturnToTitle: () -> Unit) {
    DesktopExplorationTheme(services) {
        val scope = rememberCoroutineScope()
        val travel=LocalHubTravel.current
        var compactCardHeight by remember { mutableFloatStateOf(140f) }
        val runtime = remember(services) { HubController(services.worldDataSource, services.questRepository, services.sessionStore, parentScope = scope) }
        DisposableEffect(runtime) { onDispose { runtime.close() } }
        val state by runtime.uiState.collectAsState()
        val session by services.sessionStore.state.collectAsState()
        val exploration by services.exploration.uiState.collectAsState()
        var saveRequested by remember { mutableStateOf(false) }
        var menu by remember { mutableStateOf(false) }
        var regionBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        var destinationBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        var region by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }
        LaunchedEffect(saveRequested, exploration.statusMessage) {
            if (saveRequested && exploration.statusMessage != null) { delay(6000); saveRequested = false }
        }
        val focus = remember { FocusRequester() }
        val selected = state.nodes.firstOrNull { it.id == state.selectedNodeId }
        val returnAvailable = session.roomId?.let { it in services.roomDefinitions } == true
        fun select(id: String) {
            if(travel?.busy==true) return
            services.audioDriver.executeAll(services.audioRouter.commandsForUi("sfx_hub_node_select"))
            runtime.selectNode(id)
        }
        fun enter(node: HubNodeUi) {
            if(travel?.busy==true) return
            val action = {
                if(runtime.uiState.value.nodes.firstOrNull { it.id==node.id }?.canEnter==true)
                    services.audioDriver.executeAll(services.audioRouter.commandsForUi("sfx_room_transition"))
                runtime.enterNode(node.id) { services.sessionStore.state.value.roomId?.let(onEnterRoom) }
            }
            if(!node.canEnter || travel==null) action() else travel.request(scope, onFailure={ services.exploration.showStatusMessage("Unable to travel. Please try again.") }, action=action)
        }
        fun goBack() {
            val action={ services.audioDriver.executeAll(services.audioRouter.commandsForUi("sfx_room_transition")); onBackToExploration() }
            if(travel==null) action() else travel.request(scope, onFailure={ services.exploration.showStatusMessage("Unable to travel. Please try again.") }, action=action)
        }
        LaunchedEffect(state.newlyUnlockedNodeId) {
            if(state.newlyUnlockedNodeId!=null) services.audioDriver.executeAll(services.audioRouter.commandsForUi("sfx_hub_node_select"))
        }
        LaunchedEffect(state.hub?.id) {
            state.hub?.id?.let { services.audioDriver.executeAll(services.audioRouter.commandsForRoom(hubId = it, roomId = null)) }
            region = false
        }
        LaunchedEffect(menu, settingsOpen, region, state.lockedPrompt) {
            if (!menu && !settingsOpen && !region && state.lockedPrompt == null) focus.requestFocus()
        }
        Box(Modifier.fillMaxSize().background(Color(0xFF04060A)).focusRequester(focus).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when(event.key) {
                Key.Escape -> { when {
                    state.lockedPrompt != null -> runtime.dismissLockedPrompt()
                    settingsOpen -> settingsOpen = false
                    menu -> menu = false
                    region -> region = false
                    returnAvailable -> goBack()
                }; true }
                Key.I -> { if (!settingsOpen && state.lockedPrompt == null) menu = !menu; true }
                Key.F5 -> { saveRequested = true; services.exploration.clearStatusMessage(); services.exploration.quickSave(); true }
                else -> false
            }
        }.focusable()) {
            if (state.isLoading || state.hub == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(); Text("Loading region", color = Color.White)
                }
            } else {
                val hub = requireNotNull(state.hub)
                val painter = rememberDesktopAssetPainter(state.backgroundImage, services.assetProvider)
                val theme = services.themeRepository.getTheme(services.roomDefinitions[state.nodes.firstOrNull { it.special == null }?.entryRoom]?.env)
                val rgb = theme?.accent
                val accent = if (LocalExplorationSettings.current.highContrastMode) Color.White else
                    if (rgb != null && rgb.size >= 3) Color(rgb[0], rgb[1], rgb[2], rgb.getOrElse(3) { 1f }) else Color(0xFF7BE4FF)
                val regionContent: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HubCard(accent) {
                            Text(hub.title, style = MaterialTheme.typography.headlineMedium, color = accent)
                            Text(hub.description, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                        state.trackedQuest?.let { HubQuestCard(it, accent) }
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                val outerDensity = LocalDensity.current
                val outerLayout = portraitBackdropLayout(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()), painter.intrinsicSize)
                val outerWide = hasThreePanelSpace(outerLayout, outerDensity.density)
                val compactReserve = if (outerWide) 0.dp else hubCompactReserve(compactCardHeight,maxHeight.value).dp
                if (!outerWide) DesktopPortraitBackdrop(painter, null, showCenter = false)
                DesktopPortraitBackdrop(painter, hub.title, Modifier.padding(top = if (outerWide) 0.dp else if(LocalExplorationSettings.current.largeTouchTargets) 80.dp else 64.dp, bottom = compactReserve), showExtensions = outerWide) { layout ->
                    val density = LocalDensity.current
                    val wide = outerWide
                    LaunchedEffect(wide) { if (wide) region = false }
                    val centerLeft = with(density) { layout.center.left.toDp() }
                    val centerTop = with(density) { layout.center.top.toDp() }
                    val centerWidth = with(density) { layout.center.width.toDp() }
                    val centerHeight = with(density) { layout.center.height.toDp() }
                    DesktopHubMapScene(services, state, accent, ::select, ::enter,
                        imageBounds = layout.center,
                        reservedBounds = if (wide) listOf(regionBounds, destinationBounds).filter { it.width > 0f && it.height > 0f } else emptyList(), compact = !wide)
                    Box(Modifier.offset(centerLeft, centerTop).width(centerWidth).height(centerHeight)) {
                        state.statusMessage?.let { message ->
                            Surface(Modifier.align(Alignment.TopCenter).padding(top = if (wide) 12.dp else 64.dp, start = 12.dp, end = 12.dp),
                                color = Color(0xF0061018), border = BorderStroke(1.dp, accent), shape = RoundedCornerShape(8.dp)) {
                                Text(message, Modifier.padding(12.dp), color = Color.White)
                            }
                        }
                    }
                    if (wide) {
                        Box(Modifier.width(with(density) { layout.left.width.toDp() }).fillMaxHeight().padding(20.dp)) {
                            DesktopExplorationScrollPane(rememberScrollState(), Modifier.align(Alignment.TopEnd).widthIn(max = 360.dp).fillMaxWidth().onGloballyPositioned { regionBounds = it.boundsInRoot() }) { regionContent() }
                        }
                        Box(Modifier.offset(x = with(density) { layout.right.left.toDp() }).width(with(density) { layout.right.width.toDp() }).fillMaxHeight().padding(20.dp)) {
                            Column(Modifier.align(Alignment.TopStart).widthIn(max = 340.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DesktopExplorationScrollPane(rememberScrollState(), Modifier.weight(1f).fillMaxWidth()) {
                                    selected?.let { node -> Box(Modifier.onGloballyPositioned { destinationBounds = it.boundsInRoot() }) { HubDestinationCard(node, accent) { enter(node) } } }
                                }
                                HubMenuButton(accent) { menu = true }
                            }
                        }
                    }
                }
                if (!outerWide) {
                        Row(Modifier.align(Alignment.TopCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesktopHubControl("Region",Icons.Rounded.Info,{ region = !region },accent=accent)
                            HubMenuButton(accent) { menu = true }
                        }
                        selected?.let { node ->
                            DesktopExplorationScrollPane(rememberScrollState(), Modifier.align(Alignment.BottomCenter)
                                .widthIn(max = 540.dp).fillMaxWidth().heightIn(max = compactReserve).padding(8.dp)) {
                                HubDestinationCard(node, accent, compact = true,
                                    modifier=Modifier.onSizeChanged { compactCardHeight=it.height/outerDensity.density }) { enter(node) }
                            }
                        }
                        if (region) Surface(Modifier.align(Alignment.TopStart).padding(top = if(LocalExplorationSettings.current.largeTouchTargets) 80.dp else 64.dp, start = 12.dp, bottom = 12.dp)
                            .widthIn(max = 360.dp).fillMaxWidth(.85f).fillMaxHeight(.72f), color = Color(0xFA040A10), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                TextButton(onClick = { region = false }, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                                DesktopExplorationScrollPane(rememberScrollState(), Modifier.weight(1f)) { regionContent() }
                            }
                        }
                    }
                }
            }
            if (saveRequested && !menu) exploration.statusMessage?.let { message ->
                Surface(Modifier.align(Alignment.TopCenter).padding(top = 64.dp), color = Color(0xF0061018), shape = RoundedCornerShape(8.dp)) {
                    Text(message, Modifier.padding(12.dp), color = Color.White)
                }
            }
            if (menu) DesktopExplorationModal("", { menu = false }, "Menu") {
                state.trackedQuest?.let { HubQuestCard(it, Color(0xFF7BE4FF)) }
                if (saveRequested) exploration.statusMessage?.let { Text(it, color = Color.White) }
                OutlinedButton(onClick = { saveRequested = true; services.exploration.clearStatusMessage(); services.exploration.quickSave() }, modifier = Modifier.fillMaxWidth()) { Text("Quick save") }
                OutlinedButton(onClick = { menu = false; settingsOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
                OutlinedButton(onClick = { saveRequested = true; services.exploration.clearStatusMessage(); services.exploration.quickSaveAndReturnToTitle(onReturnToTitle) }, modifier = Modifier.fillMaxWidth()) { Text("Return to title") }
                TextButton(onClick = { menu = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
            if (settingsOpen) Dialog(onDismissRequest = { settingsOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                DesktopFieldMenuTheme(services) {
                    Surface(Modifier.fillMaxWidth(.9f).fillMaxHeight(.9f), color = Color(0xFF040A10), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Settings", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                                TextButton(onClick = { settingsOpen = false }) { Text("Close") }
                            }
                            DesktopSettingsContent(services, LocalExplorationSettings.current, state.hub?.title, { services.exploration.quickSaveAndReturnToTitle(onReturnToTitle) })
                        }
                    }
                }
            }
            state.lockedPrompt?.let { prompt -> DesktopExplorationModal(prompt.message, runtime::dismissLockedPrompt, prompt.title) {
                Button(onClick = runtime::dismissLockedPrompt) { Text("Close") }
            } }
        }
    }
}

@Composable
private fun HubMenuButton(accent: Color, onClick: () -> Unit) {
    DesktopHubControl("Menu",Icons.Rounded.Menu,onClick,accent=accent)
}

@Composable
internal fun HubDestinationCard(node: HubNodeUi, accent: Color, compact: Boolean = false, modifier: Modifier=Modifier, onEnter: () -> Unit) {
    val status = when { !node.canEnter -> "Locked"; node.completed -> "Completed"; node.visited -> "Visited"; else -> "Available" }
    val description = if (!node.canEnter) node.lockReason ?: node.lockedPreview ?: node.description else node.description
    HubCard(accent, modifier.testTag("hub-destination")) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(node.title, color = accent, style = MaterialTheme.typography.titleMedium)
                    Text(status, color = if (!node.canEnter) Color(0xFFFFB394) else Color(0xFF9BDEC0), style = MaterialTheme.typography.labelSmall)
                }
                DesktopHubControl("Enter",Icons.Rounded.ArrowForward,onEnter,Modifier.width(112.dp),node.canEnter,primary=true,accent=accent)
            }
        } else {
            Text(node.title, color = accent, style = MaterialTheme.typography.headlineSmall)
            Text(status, color = if (!node.canEnter) Color(0xFFFFB394) else Color(0xFF9BDEC0), style = MaterialTheme.typography.labelMedium)
        }
        description?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge) }
        if (!compact) DesktopHubControl("Enter",Icons.Rounded.ArrowForward,onEnter,Modifier.fillMaxWidth(),node.canEnter,primary=true,accent=accent)
    }
}

@Composable
private fun HubQuestCard(quest: HubQuestUi, accent: Color) {
    HubCard(accent) {
        Text("Tracked quest", color = accent, style = MaterialTheme.typography.labelMedium)
        Text(quest.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        (quest.objective ?: quest.stageTitle)?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun HubCard(accent: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = if (LocalExplorationSettings.current.highContrastMode) Color.Black else Color(0xF0061018),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
