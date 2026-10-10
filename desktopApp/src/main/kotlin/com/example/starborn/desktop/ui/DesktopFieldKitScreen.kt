package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import java.util.Locale

enum class FieldKitTab { LOADOUT, CARGO, CRAFTING }

enum class CargoCategory(val label: String) {
    ALL("All Cargo"),
    CONSUMABLES("Consumables"),
    MATERIALS("Materials"),
    EQUIPMENT("Equipment"),
    KEY_ITEMS("Key / Tech")
}

@Composable
fun DesktopFieldKitScreen(
    services: DesktopAppServices,
    onClose: () -> Unit
) {
    var tab by rememberSaveable {
        mutableStateOf(if (services.openTinkeringOnNextFieldKit) FieldKitTab.CRAFTING else FieldKitTab.LOADOUT)
    }
    val focusRequester = remember { FocusRequester() }
    val uiState by services.exploration.uiState.collectAsState()
    val session by services.sessionStore.state.collectAsState()
    val fonts = LocalStarbornFonts.current

    LaunchedEffect(Unit) {
        services.openTinkeringOnNextFieldKit = false
        focusRequester.requestFocus()
    }

    val playCue: (String) -> Unit = { cueName ->
        services.audioDriver.executeAll(services.audioRouter.commandsForUi(cueName))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040A12))
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            playCue("action_retreat")
                            onClose()
                            true
                        }
                        Key.One -> {
                            tab = FieldKitTab.LOADOUT
                            playCue("sfx_button_toggle")
                            true
                        }
                        Key.Two -> {
                            tab = FieldKitTab.CARGO
                            playCue("sfx_button_toggle")
                            true
                        }
                        Key.Three -> {
                            tab = FieldKitTab.CRAFTING
                            playCue("sfx_button_toggle")
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Deck Telemetry Bar
            FieldKitDeckHeader(
                sectorId = session.roomId ?: "Field Transit",
                playerCredits = session.playerCredits,
                activeMealBuff = session.activeMealBuff,
                partyMembers = session.partyMembers,
                partyMemberHp = session.partyMemberHp,
                onClose = {
                    playCue("action_retreat")
                    onClose()
                }
            )

            // Primary Navigation Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldKitNavTabButton(
                        label = "LOADOUT & GEAR",
                        shortcut = "1",
                        isSelected = tab == FieldKitTab.LOADOUT,
                        tag = "tab-loadout",
                        onClick = {
                            tab = FieldKitTab.LOADOUT
                            playCue("sfx_button_toggle")
                        }
                    )
                    FieldKitNavTabButton(
                        label = "CARGO MANIFEST",
                        shortcut = "2",
                        isSelected = tab == FieldKitTab.CARGO,
                        tag = "tab-cargo",
                        onClick = {
                            tab = FieldKitTab.CARGO
                            playCue("sfx_button_toggle")
                        }
                    )
                    FieldKitNavTabButton(
                        label = "TINKERING BENCH",
                        shortcut = "3",
                        isSelected = tab == FieldKitTab.CRAFTING,
                        tag = "tab-crafting",
                        onClick = {
                            tab = FieldKitTab.CRAFTING
                            playCue("sfx_button_toggle")
                        }
                    )
                }

                // Operational Status Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0C2030),
                    border = BorderStroke(1.dp, Color(0xFF163E5C))
                ) {
                    Text(
                        text = "TACTICAL RIG ONLINE",
                        color = Color(0xFF00E5FF),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Interactive Feedback Alert (if present)
            uiState.menuFeedback?.let { feedback ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x33FFD54F),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "▶ $feedback",
                        color = Color(0xFFFFD54F),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Central Work Deck Pane
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xDD081524),
                border = BorderStroke(1.5.dp, Color(0xFF1B3850)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    when (tab) {
                        FieldKitTab.LOADOUT -> DesktopRuntimeGearContent(services)
                        FieldKitTab.CARGO -> DesktopFieldCargoContent(
                            services = services,
                            onJumpToLoadout = { tab = FieldKitTab.LOADOUT },
                            onJumpToCrafting = { tab = FieldKitTab.CRAFTING }
                        )
                        FieldKitTab.CRAFTING -> DesktopTinkeringContent(services)
                    }
                }
            }

            // Bottom Key Legend HUD Bar
            FieldKitBottomKeyLegend(currentTab = tab, onClose = onClose)
        }
    }
}

@Composable
private fun FieldKitDeckHeader(
    sectorId: String,
    playerCredits: Int,
    activeMealBuff: com.example.starborn.domain.session.ActiveMealBuff?,
    partyMembers: List<String>,
    partyMemberHp: Map<String, Int>,
    onClose: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xDD081524),
        border = BorderStroke(1.5.dp, Color(0xFF1B3850)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Sector Telemetry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E283D),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "FIELD KIT",
                        color = Color(0xFF00E5FF),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SECTOR: ${sectorId.replace('_', ' ').uppercase()}",
                        color = Color.White,
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "TACTICAL FIELD DECK & RESOURCE ALLOCATION",
                        color = Color(0xFF8FB0C4),
                        fontSize = 11.sp
                    )
                }
            }

            // Middle & Right Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Meal Buff Chip
                if (activeMealBuff != null && activeMealBuff.recipeName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x22FFD54F),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🍱", fontSize = 12.sp)
                            Text(
                                text = "${activeMealBuff.recipeName.uppercase()} (+${activeMealBuff.hpBonus} HP · ${activeMealBuff.remainingEncounters} left)",
                                color = Color(0xFFFFD54F),
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Party Roster Indicators
                if (partyMembers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        partyMembers.forEach { member ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0F2233),
                                border = BorderStroke(1.dp, Color(0xFF1E3E58))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FF9D))
                                    )
                                    Text(
                                        text = member.uppercase(),
                                        color = Color(0xFFC7DEEC),
                                        fontFamily = fonts.orbitron,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Credits Balance
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E2436),
                    border = BorderStroke(1.dp, Color(0xFF1D476B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💳", fontSize = 12.sp)
                        Text(
                            text = "$playerCredits CREDITS",
                            color = Color(0xFFFFD54F),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Close Badge
                DesktopKeyBadge(
                    keyGlyph = "Esc",
                    label = "Close",
                    onClick = onClose,
                    modifier = Modifier.testTag("field-kit-close-button")
                )
            }
        }
    }
}

@Composable
private fun FieldKitNavTabButton(
    label: String,
    shortcut: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val accentColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF7897AC)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF0F2B40) else Color(0xFF0A1826),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF1A354B)),
        modifier = Modifier
            .desktopPointerHover()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF162D3E)
            ) {
                Text(
                    text = shortcut,
                    color = accentColor,
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }

            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF9AB7C7),
                fontFamily = fonts.orbitron,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun DesktopFieldCargoContent(
    services: DesktopAppServices,
    onJumpToLoadout: () -> Unit,
    onJumpToCrafting: () -> Unit
) {
    val inventory by services.inventoryService.state.collectAsState()
    val session by services.sessionStore.state.collectAsState()
    val fonts = LocalStarbornFonts.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(CargoCategory.ALL) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }

    // Categorization logic
    fun itemCategory(item: Item): CargoCategory = when {
        services.exploration.isPreparedMeal(item.id) || item.type.equals("consumable", true) || item.type.equals("meal", true) -> CargoCategory.CONSUMABLES
        item.type.equals("material", true) || item.type.equals("tinker", true) || item.type.equals("scrap", true) -> CargoCategory.MATERIALS
        item.type.equals("weapon", true) || item.type.equals("armor", true) || item.type.equals("mod", true) || item.equipment != null -> CargoCategory.EQUIPMENT
        item.type.equals("key", true) || item.type.equals("quest", true) || item.unsellable -> CargoCategory.KEY_ITEMS
        else -> CargoCategory.MATERIALS
    }

    val filteredItems = remember(inventory, searchQuery, selectedCategory) {
        inventory.filter { entry ->
            val matchesCategory = when (selectedCategory) {
                CargoCategory.ALL -> true
                else -> itemCategory(entry.item) == selectedCategory
            }
            val matchesSearch = searchQuery.isBlank() ||
                entry.item.name.contains(searchQuery, ignoreCase = true) ||
                entry.item.description.orEmpty().contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Auto-select first item if current selection is invalid
    val activeSelection = remember(filteredItems, selectedItemId) {
        filteredItems.firstOrNull { it.item.id == selectedItemId } ?: filteredItems.firstOrNull()
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Cargo Catalog & Filtering
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search & Category Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter cargo manifest...", fontSize = 12.sp, color = Color(0xFF6B8799)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF8FB0C4), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E3A52),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFC7DEEC),
                        cursorColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF081522),
                        unfocusedContainerColor = Color(0xFF081522)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cargo-search-input")
                )
            }

            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CargoCategory.entries.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    val count = when (cat) {
                        CargoCategory.ALL -> inventory.size
                        else -> inventory.count { itemCategory(it.item) == cat }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCatSelected) Color(0xFF13364D) else Color(0xFF0A1C2B),
                        border = BorderStroke(1.dp, if (isCatSelected) Color(0xFF00E5FF) else Color(0xFF163246)),
                        modifier = Modifier
                            .desktopPointerHover()
                            .clickable { selectedCategory = cat }
                            .testTag("cargo-category-${cat.name.lowercase()}")
                    ) {
                        Text(
                            text = "${cat.label} ($count)",
                            color = if (isCatSelected) Color(0xFF00E5FF) else Color(0xFF8BA5B5),
                            fontFamily = fonts.orbitron,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Cargo Items Scrollable List
            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF07121E),
                    border = BorderStroke(1.dp, Color(0xFF183042)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No cargo collected in this category." else "No matching cargo items found.",
                            color = Color(0xFF7897AC),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredItems, key = { it.item.id }) { entry ->
                        val isItemSelected = activeSelection?.item?.id == entry.item.id
                        CargoItemRow(
                            entry = entry,
                            isSelected = isItemSelected,
                            isMeal = services.exploration.isPreparedMeal(entry.item.id),
                            onClick = { selectedItemId = entry.item.id }
                        )
                    }
                }
            }
        }

        // Right Column: Tactical Cargo Inspector & Action Console
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF091B2C),
            border = BorderStroke(1.5.dp, Color(0xFF1F4462)),
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
        ) {
            if (activeSelection != null) {
                TacticalItemInspector(
                    entry = activeSelection,
                    isMeal = services.exploration.isPreparedMeal(activeSelection.item.id),
                    activeMealBuff = session.activeMealBuff,
                    partyMembers = session.partyMembers,
                    onUseOnMember = { memberId ->
                        services.exploration.useInventoryItem(activeSelection.item.id, targetId = memberId)
                    },
                    onConsumeMeal = {
                        services.exploration.useInventoryItem(activeSelection.item.id, replaceMeal = true)
                    },
                    onJumpToLoadout = onJumpToLoadout,
                    onJumpToCrafting = onJumpToCrafting
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📦", fontSize = 32.sp)
                        Text(
                            text = "NO CARGO SELECTED",
                            color = Color(0xFF7897AC),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Select an item from the manifest to inspect telemetry and deploy supplies.",
                            color = Color(0xFF5A788C),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CargoItemRow(
    entry: InventoryEntry,
    isSelected: Boolean,
    isMeal: Boolean,
    onClick: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val item = entry.item

    val itemIcon = when {
        isMeal -> "🍱"
        item.type.equals("consumable", true) -> "💊"
        item.type.equals("weapon", true) -> "⚔"
        item.type.equals("armor", true) -> "🛡"
        item.type.equals("mod", true) -> "🧩"
        item.type.equals("material", true) || item.type.equals("scrap", true) -> "⚙"
        item.type.equals("key", true) -> "🔑"
        else -> "📦"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF13344D) else Color(0xFF091C2C),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF17354B)),
        modifier = Modifier
            .fillMaxWidth()
            .desktopPointerHover()
            .clickable(onClick = onClick)
            .testTag("cargo-item-${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(itemIcon, fontSize = 16.sp)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = if (isSelected) Color.White else Color(0xFFC7DEEC),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isMeal) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0x33FFD54F)
                            ) {
                                Text(
                                    text = "MEAL",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = (item.categoryOverride ?: item.type).replace('_', ' ').uppercase(),
                        color = Color(0xFF6B8C9E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item.value > 0) {
                    Text(
                        text = "${item.value} CR",
                        color = Color(0xFFFFD54F),
                        fontFamily = fonts.orbitron,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF142F45),
                    border = BorderStroke(1.dp, Color(0xFF225175))
                ) {
                    Text(
                        text = "×${entry.quantity}",
                        color = Color(0xFF00FF9D),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalItemInspector(
    entry: InventoryEntry,
    isMeal: Boolean,
    activeMealBuff: com.example.starborn.domain.session.ActiveMealBuff?,
    partyMembers: List<String>,
    onUseOnMember: (String) -> Unit,
    onConsumeMeal: () -> Unit,
    onJumpToLoadout: () -> Unit,
    onJumpToCrafting: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val item = entry.item

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Holographic Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name.uppercase(),
                    color = Color.White,
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = (item.categoryOverride ?: item.type).replace('_', ' ').uppercase(),
                    color = Color(0xFF00E5FF),
                    fontFamily = fonts.orbitron,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF143047),
                border = BorderStroke(1.dp, Color(0xFF23557D))
            ) {
                Text(
                    text = "QTY: ${entry.quantity}",
                    color = Color(0xFF00FF9D),
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1B3D59))

        // Description Box
        if (!item.description.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF071420),
                border = BorderStroke(1.dp, Color(0xFF142B3E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.description.orEmpty(),
                    color = Color(0xFFB5D0E0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Equipment / Effect Stat Telemetry
        item.equipment?.let { equip ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "EQUIPMENT SPECIFICATIONS",
                    color = Color(0xFF8FB0C4),
                    fontFamily = fonts.orbitron,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF071524),
                    border = BorderStroke(1.dp, Color(0xFF173854)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Slot: ${equip.slot.uppercase()}", color = Color.White, fontSize = 11.sp)
                        equip.damageMin?.let { min ->
                            Text("Damage Range: $min - ${equip.damageMax ?: min}", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        }
                        equip.defense?.let { def ->
                            Text("Defense: +$def", color = Color(0xFF80D8FF), fontSize = 11.sp)
                        }
                        equip.hpBonus?.let { hp ->
                            Text("HP Bonus: +$hp", color = Color(0xFF00FF9D), fontSize = 11.sp)
                        }
                        equip.attackElement?.let { elem ->
                            Text("Element: ${elem.uppercase()}", color = Color(0xFFFFD54F), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item.effect?.let { eff ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SUPPLY / EFFECT TELEMETRY",
                    color = Color(0xFF8FB0C4),
                    fontFamily = fonts.orbitron,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF071524),
                    border = BorderStroke(1.dp, Color(0xFF173854)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        eff.restoreHp?.let { hp ->
                            Text("Restores: +$hp HP", color = Color(0xFF00FF9D), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        eff.status?.let { st ->
                            Text("Treats Condition: ${st.uppercase()}", color = Color(0xFF80D8FF), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Action Deck Bay
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isMeal) {
                Button(
                    onClick = onConsumeMeal,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF061420)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .desktopPointerHover()
                        .testTag("eat-meal-button")
                ) {
                    Text(
                        text = if (activeMealBuff == null) "EAT MEAL BUFF [Spacebar]" else "REPLACE CURRENT BUFF WITH THIS MEAL",
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else if (item.type.equals("consumable", true) && item.effect != null) {
                Text(
                    text = "DEPLOY ON PARTY MEMBER:",
                    color = Color(0xFF8FB0C4),
                    fontFamily = fonts.orbitron,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    partyMembers.forEach { member ->
                        Button(
                            onClick = { onUseOnMember(member) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4),
                                contentColor = Color(0xFF04101A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .desktopPointerHover()
                                .testTag("use-item-${item.id}-${member.lowercase()}")
                        ) {
                            Text(
                                text = member.uppercase(),
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (item.equipment != null || item.type in listOf("weapon", "armor", "mod")) {
                OutlinedButton(
                    onClick = onJumpToLoadout,
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .desktopPointerHover()
                ) {
                    Text(
                        text = "VIEW IN LOADOUT & RIGGING [Tab 1]",
                        color = Color(0xFF00E5FF),
                        fontFamily = fonts.orbitron,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (item.type in listOf("material", "scrap", "tinker")) {
                OutlinedButton(
                    onClick = onJumpToCrafting,
                    border = BorderStroke(1.dp, Color(0xFF00FF9D)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .desktopPointerHover()
                ) {
                    Text(
                        text = "USE AT TINKERING BENCH [Tab 3]",
                        color = Color(0xFF00FF9D),
                        fontFamily = fonts.orbitron,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldKitBottomKeyLegend(
    currentTab: FieldKitTab,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC07111B),
        border = BorderStroke(1.dp, Color(0xFF182D3D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DesktopKeyBadge("1", "Loadout Deck", highlighted = currentTab == FieldKitTab.LOADOUT)
                DesktopKeyBadge("2", "Cargo Manifest", highlighted = currentTab == FieldKitTab.CARGO)
                DesktopKeyBadge("3", "Tinkering Bench", highlighted = currentTab == FieldKitTab.CRAFTING)
            }

            DesktopKeyBadge(
                keyGlyph = "Esc",
                label = "Return to Exploration",
                onClick = onClose
            )
        }
    }
}
