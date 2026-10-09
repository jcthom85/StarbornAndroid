package com.example.starborn.desktop.ui

import com.example.starborn.domain.model.actionKey

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import java.util.Locale

enum class DesktopMenuTab(val label: String) {
    STATS("Stats"),
    INVENTORY("Inventory"),
    FIELD_KIT("Tinker"),
    JOURNAL("Journal"),
    MAP("Map"),
    SETTINGS("Settings")
}

private enum class InventoryCategory { SUPPLIES, KEY_ITEMS, GEAR }

/** An in-game overlay: the room remains visible around the menu. */
@Composable
fun DesktopFieldMenuDialog(
    services: DesktopAppServices,
    initialTab: DesktopMenuTab = DesktopMenuTab.STATS,
    currentRoomTitle: String? = null,
    onOpenFieldKit: () -> Unit,
    onReturnToTitle: () -> Unit,
    onDismiss: () -> Unit,
    onOpenControls: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f))
        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center) {
        DesktopFieldMenuTheme(services) {
            DesktopFieldMenuContent(services, initialTab, currentRoomTitle, onOpenFieldKit,
                onReturnToTitle, onDismiss, onOpenControls)
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DesktopFieldMenuContent(
    services: DesktopAppServices,
    initialTab: DesktopMenuTab = DesktopMenuTab.STATS,
    currentRoomTitle: String? = null,
    onOpenFieldKit: () -> Unit,
    onReturnToTitle: () -> Unit,
    onDismiss: () -> Unit,
    onOpenControls: (() -> Unit)? = null
) {
    var activeTab by rememberSaveable { mutableStateOf(initialTab) }
    var selectedMember by rememberSaveable { mutableStateOf<String?>(null) }
    val runtime = services.exploration
    val keyboardFocus = remember { FocusRequester() }
    val ui by runtime.uiState.collectAsState()
    val session by services.sessionStore.state.collectAsState()
    val allItems = remember(services) { services.itemRepository.allItems().associateBy { it.id } }
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val pageState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val accent = when (activeTab) {
        DesktopMenuTab.STATS, DesktopMenuTab.JOURNAL -> FieldMenuDesign.gold
        DesktopMenuTab.FIELD_KIT -> Color(0xFF7EDAB4)
        else -> FieldMenuDesign.cyan
    }
    val entrance = remember { Animatable(1f) }
    LaunchedEffect(activeTab) {
        if (!settings.highContrastMode && !settings.disableFlashes && !settings.disableScreenshake) { entrance.snapTo(0f); entrance.animateTo(1f, tween(180)) }
    }
    val memberId = selectedMember?.takeIf { id -> ui.partyStatus.members.any { it.id == id } }
        ?: ui.partyStatus.members.firstOrNull()?.id
    fun back() {
        when {
            ui.skillTreeOverlay != null -> runtime.closeSkillTreeOverlay()
            ui.partyMemberDetails != null -> runtime.closePartyMemberDetails()
            else -> onDismiss()
        }
    }
    fun select(tab: DesktopMenuTab) {
        runtime.closeQuestDetails()
        runtime.closeSkillTreeOverlay()
        runtime.closePartyMemberDetails()
        if (activeTab != tab) runtime.onMenuActionInvoked()
        activeTab = tab
    }
    LaunchedEffect(activeTab, ui.skillTreeOverlay?.characterId, ui.partyMemberDetails?.id) { withFrameNanos { }; keyboardFocus.requestFocus() }
    LaunchedEffect(activeTab) {
        runtime.selectMenuTab(com.example.starborn.feature.exploration.viewmodel.MenuTab.valueOf(activeTab.name))
    }
    Surface(Modifier.focusRequester(keyboardFocus).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false else {
            val index = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six).indexOf(event.key)
            val directTab = when (event.key) {
                Key.One -> DesktopMenuTab.INVENTORY
                Key.Two -> DesktopMenuTab.FIELD_KIT
                Key.Three -> DesktopMenuTab.JOURNAL
                Key.Four -> DesktopMenuTab.MAP
                Key.Five -> DesktopMenuTab.SETTINGS
                Key.Zero -> DesktopMenuTab.STATS
                else -> null
            }
            when {
                event.key == Key.Escape || event.isAltPressed && event.key == Key.DirectionLeft -> { back(); true }
                event.key == Key.F5 -> { runtime.quickSave(); true }
                event.isAltPressed && index >= 0 -> { select(DesktopMenuTab.entries[index]); true }
                directTab != null && !event.isAltPressed && !event.isCtrlPressed -> { select(directTab); true }
                event.isCtrlPressed && event.key == Key.Tab -> {
                    val step = if (event.isShiftPressed) -1 else 1
                    select(DesktopMenuTab.entries[(activeTab.ordinal + step + DesktopMenuTab.entries.size) % DesktopMenuTab.entries.size]); true
                }
                else -> false
            }
        }
    }.focusable().widthIn(max = 1440.dp).fillMaxWidth(.94f).fillMaxHeight(.92f),
        shape = RoundedCornerShape(18.dp), color = FieldMenuDesign.shell,
        border = BorderStroke(1.dp, if (settings.highContrastMode) Color.White else FieldMenuDesign.border)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            DesktopMenuAtmosphere(Modifier.fillMaxSize(), accent, !settings.highContrastMode && !settings.disableFlashes)
            val compact = maxWidth < 900.dp
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .12f), Color.Transparent)))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("STARBORN", color = accent, style = MaterialTheme.typography.labelSmall, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                        Text(if (ui.skillTreeOverlay != null) "${ui.skillTreeOverlay!!.characterName} - Skills"
                            else if (ui.partyMemberDetails != null) "${ui.partyMemberDetails!!.name} - Attributes"
                            else activeTab.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = FieldMenuDesign.text)
                        currentRoomTitle?.let { Text(it, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text(if (ui.skillTreeOverlay != null) "${ui.skillTreeOverlay!!.availableAp} AP" else ui.progressionSummary.creditsLabel,
                        color = FieldMenuDesign.gold, style = MaterialTheme.typography.titleMedium)
                    DesktopTooltip("Quick save game [F5]") {
                        OutlinedButton(onClick = runtime::quickSave, modifier = Modifier.desktopPointerHover()) { Text(if (compact) "Save" else "Quick save") }
                    }
                    DesktopTooltip("Settings [Alt+6]") {
                        IconButton(onClick = { select(DesktopMenuTab.SETTINGS) }, modifier = Modifier.desktopPointerHover()) { Icon(Icons.Default.Settings, "Settings", tint = FieldMenuDesign.textMuted) }
                    }
                    DesktopTooltip("Close menu [Esc]") {
                        IconButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) { Icon(Icons.Default.Close, "Close menu", tint = FieldMenuDesign.text) }
                    }
                }
                HorizontalDivider(color = accent.copy(alpha = .25f))
                Row(Modifier.weight(1f)) {
                    Column(Modifier.width(if (compact) 104.dp else 180.dp).fillMaxHeight()
                        .background(FieldMenuDesign.panel.copy(alpha = .8f)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { DesktopMenuTab.entries.filter { it != DesktopMenuTab.SETTINGS }.forEach { tab ->
                            val beacon = (tab == DesktopMenuTab.STATS && ui.isGearTutorialActive) ||
                                (tab == DesktopMenuTab.FIELD_KIT && ui.isTinkeringTutorialActive)
                            val icon = when (tab) {
                                DesktopMenuTab.STATS -> Icons.Default.People
                                DesktopMenuTab.INVENTORY -> Icons.Default.Backpack
                                DesktopMenuTab.FIELD_KIT -> Icons.Default.Build
                                DesktopMenuTab.JOURNAL -> Icons.Default.Book
                                else -> Icons.Default.Map
                            }
                            val tabIndex = DesktopMenuTab.entries.indexOf(tab)
                            DesktopTooltip("${tab.label} [Alt+${tabIndex + 1}]") {
                                Surface(onClick = { select(tab) }, modifier = Modifier.fillMaxWidth().desktopPointerHover().semantics { selected = activeTab == tab },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (activeTab == tab) accent.copy(alpha = .5f) else Color.Transparent),
                                    color = if (activeTab == tab) accent.copy(alpha = .10f) else Color.Transparent) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(icon, null, Modifier.size(20.dp), tint = if (activeTab == tab) FieldMenuDesign.gold else FieldMenuDesign.textMuted)
                                            if (!compact) Text(if (tab == DesktopMenuTab.JOURNAL && services.questPresentations.journalBadge > 0) "${tab.label} (${services.questPresentations.journalBadge})" else tab.label, color = if (activeTab == tab) FieldMenuDesign.gold else FieldMenuDesign.text)
                                        }
                                        if (compact) Text(if (tab == DesktopMenuTab.JOURNAL && services.questPresentations.journalBadge > 0) "${tab.label} (${services.questPresentations.journalBadge})" else tab.label, style = MaterialTheme.typography.labelMedium)
                                        if (beacon) Text("Guide", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        }
                        DesktopTooltip("Settings [Alt+6]") {
                            TextButton(onClick = { select(DesktopMenuTab.SETTINGS) }, modifier = Modifier.desktopPointerHover()) { Text("Settings", color = if (activeTab == DesktopMenuTab.SETTINGS) FieldMenuDesign.gold else FieldMenuDesign.textMuted) }
                        }
                        onOpenControls?.let {
                            DesktopTooltip("Tactical controls & keybindings guide [H]") {
                                TextButton(onClick = it, modifier = Modifier.desktopPointerHover()) { Text("Controls") }
                            }
                        }
                        ui.actions.filterIsInstance<com.example.starborn.domain.model.RestStopAction>()
                            .firstOrNull { it.name.equals("bunk", true) }?.let { action ->
                                TextButton(onClick = { onDismiss(); runtime.onActionSelected(action) },
                                    modifier = Modifier.desktopPointerHover(ui.actionHints[action.actionKey()]?.locked != true),
                                    enabled = ui.actionHints[action.actionKey()]?.locked != true) { Text("Rest here") }
                            }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().padding(if (compact) 16.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AnimatedVisibility(ui.menuFeedback != null, enter = fadeIn(tween(if (settings.disableFlashes) 0 else 160)), exit = fadeOut()) {
                            Surface(color = FieldMenuDesign.cyan.copy(alpha = .08f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .3f))) {
                                Text(ui.menuFeedback.orEmpty(), Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), color = FieldMenuDesign.cyan, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (ui.skillTreeOverlay != null) {
                            TextButton(onClick = runtime::closeSkillTreeOverlay) { Text("Back to Party") }
                            DesktopMenuSkillsContent(services, ui.skillTreeOverlay!!)
                        } else if (ui.partyMemberDetails != null) {
                            TextButton(onClick = runtime::closePartyMemberDetails) { Text("Back to Party") }
                            DesktopMenuAttributesContent(services, ui.partyMemberDetails!!)
                        } else {
                            if ((activeTab == DesktopMenuTab.STATS || activeTab == DesktopMenuTab.INVENTORY) && ui.partyStatus.members.size > 1) {
                                DesktopMenuPartyStrip(services, ui.partyStatus.members, memberId, settings.largeTouchTargets) { selectedMember = it; if (activeTab == DesktopMenuTab.INVENTORY) select(DesktopMenuTab.STATS) }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                                alpha = entrance.value; translationY = (1f - entrance.value) * 8.dp.toPx()
                            }) {
                                pageState.SaveableStateProvider(activeTab.name) { when (activeTab) {
                                    DesktopMenuTab.STATS -> DesktopMenuPartyContent(services, memberId)
                                    DesktopMenuTab.INVENTORY -> DesktopInventoryTabContent(session, allItems, services)
                                    DesktopMenuTab.FIELD_KIT -> DesktopTinkeringContent(services)
                                    DesktopMenuTab.JOURNAL -> DesktopRuntimeJournalContent(services)
                                    DesktopMenuTab.MAP -> DesktopRuntimeMapContent(services)
                                    DesktopMenuTab.SETTINGS -> DesktopSettingsContent(services, settings, currentRoomTitle, onReturnToTitle, onOpenControls)
                                } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopInventoryTabContent(
    sessionState: com.example.starborn.domain.session.GameSessionState,
    allItems: Map<String, Item>,
    services: DesktopAppServices
) {
    var category by rememberSaveable { mutableStateOf(InventoryCategory.SUPPLIES) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var useItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Name") }
    val visibleEntries = sessionState.inventory.entries.filter { entry ->
        entry.value > 0 && (allItems[entry.key]?.name ?: entry.key).contains(search, ignoreCase = true)
    }.sortedWith(when (sort) {
        "Quantity" -> compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { allItems[it.key]?.name ?: it.key }
        "Type" -> compareBy<Map.Entry<String, Int>> { allItems[it.key]?.type.orEmpty() }.thenBy { allItems[it.key]?.name ?: it.key }
        else -> compareBy { allItems[it.key]?.name ?: it.key }
    })

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Authentic Android Carousel Toggle Bar (50.dp radius pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DesktopSubTogglePill("Supplies", category == InventoryCategory.SUPPLIES, modifier = Modifier.weight(1f)) {
                category = InventoryCategory.SUPPLIES
                selectedItemId = null
            }
            DesktopSubTogglePill("Key Items", category == InventoryCategory.KEY_ITEMS, modifier = Modifier.weight(1f)) {
                category = InventoryCategory.KEY_ITEMS
                selectedItemId = null
            }
            DesktopSubTogglePill("Gear", category == InventoryCategory.GEAR, modifier = Modifier.weight(1f)) {
                category = InventoryCategory.GEAR
                selectedItemId = null
            }
        }

        if (category != InventoryCategory.GEAR) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search cargo") }, placeholder = { Text("Search cargo") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort:", color = FieldMenuDesign.textMuted)
                listOf("Name", "Quantity", "Type").forEach { option ->
                    FilterChip(sort == option, onClick = { sort = option }, label = { Text(option) })
                }
            }
        }
        when (category) {
            InventoryCategory.GEAR -> {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DesktopRuntimeGearContent(services)
                }
            }
            InventoryCategory.SUPPLIES -> {
                val supplies = visibleEntries
                    .filterNot { allItems[it.key]?.type == "key" || allItems[it.key]?.categoryOverride == "key" }
                    .filterNot { allItems[it.key]?.equipment != null }
                    .toList()
                DesktopInventorySplitLayout(
                    services = services,
                    entries = supplies,
                    selectedItemId = selectedItemId,
                    allItems = allItems,
                    emptyMessage = "No items collected yet.",
                    sessionState = sessionState,
                    resolveName = services::contentName,
                    onSelectItem = { selectedItemId = it },
                    onUseItem = { id -> useItemId = id }
                )
            }
            InventoryCategory.KEY_ITEMS -> {
                val keyItems = visibleEntries
                    .filter { allItems[it.key]?.type == "key" || allItems[it.key]?.categoryOverride == "key" }
                    .toList()
                DesktopInventorySplitLayout(
                    services = services,
                    entries = keyItems,
                    selectedItemId = selectedItemId,
                    allItems = allItems,
                    emptyMessage = "— No key mission items in cargo. Key passcodes and stasis tokens will appear here. —",
                    sessionState = sessionState,
                    resolveName = services::contentName,
                    allowUse = false,
                    onSelectItem = { selectedItemId = it }
                )
            }
        }
    }
    useItemId?.let { id ->
        val meal = services.exploration.isPreparedMeal(id)
        AlertDialog(onDismissRequest = { useItemId = null }, title = { Text(allItems[id]?.name ?: id) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopMenuItemArt(services, allItems[id], Modifier.size(72.dp))
                if (meal) {
                    Text(if (sessionState.activeMealBuff != null) "Eating this meal replaces the current party meal." else "Eat this meal to prepare the party for combat.")
                    Button(onClick = { services.exploration.useInventoryItem(id, replaceMeal = true); useItemId = null }) { Text("Eat meal") }
                } else sessionState.partyMembers.forEach { target ->
                    Surface(onClick = { services.exploration.useInventoryItem(id, target); useItemId = null }, modifier = Modifier.fillMaxWidth(),
                        color = FieldMenuDesign.elevatedPanel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .3f))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val member = services.exploration.uiState.value.partyStatus.members.firstOrNull { it.id == target }
                            Image(rememberDesktopAssetPainter(member?.portraitPath, services.assetProvider), null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                            Column { Text("Use on ${services.characterDefinitions[target]?.name ?: target}"); member?.hpLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted) } }
                        }
                    }
                }
            } }, confirmButton = { TextButton(onClick = { useItemId = null }) { Text("Cancel") } })
    }
}

@Composable
private fun DesktopInventorySplitLayout(
    services: DesktopAppServices,
    entries: List<Map.Entry<String, Int>>,
    selectedItemId: String?,
    allItems: Map<String, Item>,
    emptyMessage: String,
    sessionState: com.example.starborn.domain.session.GameSessionState,
    onSelectItem: (String) -> Unit,
    onUseItem: (String) -> Unit = {},
    resolveName: (String) -> String = { it },
    allowUse: Boolean = true
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val activeSelection = selectedItemId?.takeIf { id -> entries.any { it.key == id } } ?: entries.firstOrNull()?.key
    val selectedItem = activeSelection?.let { allItems[it] }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left: Item list in authentic Android card styling
        Column(modifier = Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = emptyMessage, color = FieldMenuDesign.textMuted, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), state = gridState, modifier = Modifier.weight(1f).fillMaxWidth().onPreviewKeyEvent { event ->
                    val current = entries.indexOfFirst { it.key == activeSelection }.coerceAtLeast(0)
                    val firstRow = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.row
                    val columns = gridState.layoutInfo.visibleItemsInfo.count { it.row == firstRow }.coerceAtLeast(1)
                    val step = when (event.key) { Key.DirectionLeft -> -1; Key.DirectionRight -> 1; Key.DirectionUp -> -columns; Key.DirectionDown -> columns; else -> 0 }
                    if (event.type != KeyEventType.KeyDown || step == 0 || entries.isEmpty()) false else {
                        val index = (current + step).coerceIn(0, entries.lastIndex)
                        onSelectItem(entries[index].key)
                        if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index }) scope.launch { gridState.animateScrollToItem(index) }
                        true
                    }
                },
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    gridItems(entries, key = { it.key }) { entry ->
                        val item = allItems[entry.key]
                        val isSelected = entry.key == activeSelection
                        Surface(onClick = { onSelectItem(entry.key) }, shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = .3f))) {
                            Column(Modifier.fillMaxWidth().height(150.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.fillMaxWidth().weight(1f)) {
                                    DesktopMenuItemArt(services, item, Modifier.align(Alignment.Center).size(64.dp))
                                    Text("x${entry.value}", Modifier.align(Alignment.TopEnd), color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelLarge)
                                }
                                Text(item?.name ?: services.contentName(entry.key), style = MaterialTheme.typography.labelLarge, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.text)
                                Text(item?.type.orEmpty().replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = FieldMenuDesign.textMuted)
                            }
                        }
                    }
                }
            }

        }

        // Right: Rich Item Inspector (matching Android Item Detail Sheet)
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF061018).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.45f))
        ) {
            if (selectedItem != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    DesktopMenuScrollPane(Modifier.weight(1f).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FieldMenuDesign.cyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    DesktopMenuItemArt(services, selectedItem, Modifier.size(72.dp))
                                }
                            }
                            Column {
                                Text(
                                    text = selectedItem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = selectedItem.type.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FieldMenuDesign.cyan
                                )
                            }
                        }

                        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.35f))

                        Text(
                            text = selectedItem.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        com.example.starborn.feature.exploration.presentation.ItemDetails.lines(selectedItem, resolveName).forEach { detail ->
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                        }

                    }

                    // Key items have no field action.
                    if (allowUse && selectedItem.effect != null) Button(
                        onClick = { onUseItem(selectedItem.id) },
                        enabled = allowUse && selectedItem.effect != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FieldMenuDesign.cyan,
                            contentColor = Color(0xFF040810),
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(text = if (selectedItem.effect != null) "USE ITEM" else "CANNOT USE IN FIELD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Select an item to inspect details", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DesktopSubTogglePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) FieldMenuDesign.cyan.copy(alpha = 0.22f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.cyan else Color.Transparent), RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else FieldMenuDesign.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}


@Composable
private fun DesktopItemGlyph(type: String, modifier: Modifier = Modifier) {
    val icon = when (type.lowercase()) {
        "consumable" -> Icons.Default.LocalHospital
        "component", "material", "mod" -> Icons.Default.Memory
        "key", "quest" -> Icons.Default.VpnKey
        "accessory" -> Icons.Default.Stars
        "weapon" -> Icons.Default.GpsFixed
        "armor" -> Icons.Default.Shield
        else -> Icons.Default.Inventory2
    }
    Icon(icon, null, modifier, tint = FieldMenuDesign.cyan)
}
