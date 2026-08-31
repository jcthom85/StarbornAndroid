package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.launch
import java.util.Locale

enum class DesktopMenuTab(val label: String, val shortcut: String) {
    INVENTORY("INVENTORY", "[1]"),
    JOURNAL("JOURNAL", "[2]"),
    MAP("MAP", "[3]"),
    FIELD_KIT("FIELD KIT", "[4]"),
    STATS("STATS", "[5]"),
    SETTINGS("SETTINGS", "[6]")
}

private enum class InventoryCategory { SUPPLIES, GEAR, KEY_ITEMS }
private enum class JournalCategory { ACTIVE, COMPLETED }

@Composable
fun DesktopFieldMenuDialog(
    services: DesktopAppServices,
    initialTab: DesktopMenuTab = DesktopMenuTab.INVENTORY,
    currentRoomTitle: String? = null,
    onOpenFieldKit: () -> Unit,
    onReturnToTitle: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    val sessionState by services.sessionStore.state.collectAsState()
    val allQuests = remember { services.questRepository.allQuests().toList() }
    val allItems = remember { services.itemRepository.allItems().associateBy { it.id } }
    val allRooms = remember { services.worldDataSource.loadRooms() }
    val userSettings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(1060.dp)
                .height(680.dp),
            shape = RoundedCornerShape(FieldMenuDesign.shellRadius),
            color = FieldMenuDesign.shell,
            border = BorderStroke(1.5.dp, FieldMenuDesign.cyan.copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FIELD TACTICAL TERMINAL // [M] MENU",
                            color = FieldMenuDesign.cyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Sector: ${currentRoomTitle ?: "Active Zone"}  •  Level ${sessionState.playerLevel}  •  ${sessionState.playerCredits} CR",
                            color = FieldMenuDesign.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onReturnToTitle,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281018)),
                            border = BorderStroke(1.dp, Color(0xFFFF007F).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(FieldMenuDesign.controlRadius),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "TITLE MENU", color = Color(0xFFFF007F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = FieldMenuDesign.text.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Authentic Menu Tab Row (matching Android MenuTabRow)
                DesktopMenuTabRow(
                    selectedTab = activeTab,
                    onSelectTab = { activeTab = it },
                    accentColor = FieldMenuDesign.cyan,
                    borderColor = FieldMenuDesign.border
                )

                // Menu Content Area in Authentic MenuSectionCard
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        DesktopMenuTab.INVENTORY -> {
                            DesktopMenuSectionCard(title = "Inventory Overview", accentColor = FieldMenuDesign.cyan, borderColor = FieldMenuDesign.border) {
                                DesktopInventoryTabContent(sessionState = sessionState, allItems = allItems, services = services)
                            }
                        }
                        DesktopMenuTab.JOURNAL -> {
                            DesktopMenuSectionCard(title = "Quest Journal", accentColor = FieldMenuDesign.cyan, borderColor = FieldMenuDesign.border) {
                                DesktopJournalTabContent(allQuests = allQuests, sessionState = sessionState)
                            }
                        }
                        DesktopMenuTab.MAP -> {
                            DesktopMenuSectionCard(title = "Sector Star Map", accentColor = FieldMenuDesign.cyan, borderColor = FieldMenuDesign.border) {
                                DesktopMapTabContent(
                                    allRooms = allRooms,
                                    currentRoomId = sessionState.roomId ?: "default_hub",
                                    currentRoomTitle = currentRoomTitle
                                )
                            }
                        }
                        DesktopMenuTab.FIELD_KIT -> {
                            DesktopMenuSectionCard(title = "Tinkering & Schematics Fabricator", accentColor = FieldMenuDesign.gold, borderColor = FieldMenuDesign.border) {
                                DesktopTinkeringFabricatorTabContent(
                                    services = services,
                                    sessionState = sessionState,
                                    allItems = allItems
                                )
                            }
                        }
                        DesktopMenuTab.STATS -> {
                            DesktopMenuSectionCard(title = "Crew Status Dossier", accentColor = FieldMenuDesign.cyan, borderColor = FieldMenuDesign.border) {
                                DesktopStatsTabBody(
                                    services = services,
                                    sessionState = sessionState
                                )
                            }
                        }
                        DesktopMenuTab.SETTINGS -> {
                            DesktopMenuSectionCard(title = "System Settings & Stasis Archive", accentColor = FieldMenuDesign.cyan, borderColor = FieldMenuDesign.border) {
                                DesktopSettingsAndStasisTabContent(
                                    services = services,
                                    userSettings = userSettings,
                                    currentRoomTitle = currentRoomTitle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopMenuSectionCard(
    title: String,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071018).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.42f)),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                color = accentColor.copy(alpha = 0.92f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                Color(0xFFFFC857).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    }
}

@Composable
private fun DesktopMenuTabRow(
    selectedTab: DesktopMenuTab,
    onSelectTab: (DesktopMenuTab) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF061018).copy(alpha = 0.38f))
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(3.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DesktopMenuTab.entries.forEach { tab ->
            DesktopMenuTabChip(
                tab = tab,
                isSelected = tab == selectedTab,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { onSelectTab(tab) }
            )
        }
    }
}

@Composable
private fun DesktopMenuTabChip(
    tab: DesktopMenuTab,
    isSelected: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val background = when {
        isSelected -> {
            Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.28f),
                    Color(0xFFFFC857).copy(alpha = 0.12f)
                )
            )
        }
        else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val chipBorderColor = when {
        isSelected -> borderColor.copy(alpha = 0.72f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> Color.White
        else -> accentColor.copy(alpha = 0.78f)
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, chipBorderColor),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${tab.label} ${tab.shortcut}",
                color = contentColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
private fun DesktopInventoryTabContent(
    sessionState: com.example.starborn.domain.session.GameSessionState,
    allItems: Map<String, Item>,
    services: DesktopAppServices
) {
    var category by remember { mutableStateOf(InventoryCategory.SUPPLIES) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Toggle Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DesktopSubTogglePill("SUPPLIES & CONSUMABLES", category == InventoryCategory.SUPPLIES) { category = InventoryCategory.SUPPLIES }
            DesktopSubTogglePill("GEAR & LOADOUT", category == InventoryCategory.GEAR) { category = InventoryCategory.GEAR }
            DesktopSubTogglePill("KEY ITEMS", category == InventoryCategory.KEY_ITEMS) { category = InventoryCategory.KEY_ITEMS }
        }

        when (category) {
            InventoryCategory.SUPPLIES -> {
                DesktopInventoryTabBody(sessionState = sessionState, allItems = allItems)
            }
            InventoryCategory.GEAR -> {
                DesktopGearEquipSubContent(sessionState = sessionState, allItems = allItems, services = services)
            }
            InventoryCategory.KEY_ITEMS -> {
                val keyItemEntries = sessionState.inventory.entries.filter { allItems[it.key]?.type == "key" || allItems[it.key]?.categoryOverride == "key" }
                if (keyItemEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "— No key mission items in cargo. Key passcodes and stasis tokens will appear here. —", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(keyItemEntries) { entry ->
                            val item = allItems[entry.key]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FieldMenuDesign.elevatedPanel)
                                    .border(BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = item?.name ?: entry.key, color = FieldMenuDesign.gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = item?.description.orEmpty(), color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopGearEquipSubContent(
    sessionState: com.example.starborn.domain.session.GameSessionState,
    allItems: Map<String, Item>,
    services: DesktopAppServices
) {
    val activeWeaponId = sessionState.equippedWeapons["nova"] ?: sessionState.equippedItems["weapon"]
    val activeArmorId = sessionState.equippedArmors["nova"] ?: sessionState.equippedItems["armor"]
    val activeAccessoryId = sessionState.equippedItems["accessory"]
    val activeSnackId = sessionState.equippedItems["snack"]

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left 4 Paper Doll Slots
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DesktopSlotBadge("PRIMARY WEAPON", activeWeaponId?.let { allItems[it]?.name } ?: "Kinetic Blaster", FieldMenuDesign.gold)
            DesktopSlotBadge("BODY ARMOR", activeArmorId?.let { allItems[it]?.name } ?: "Nano-Weave Field Suit", Color(0xFF2979FF))
            DesktopSlotBadge("TACTICAL ACCESSORY", activeAccessoryId?.let { allItems[it]?.name } ?: "Sensor Targeting Matrix", FieldMenuDesign.cyan)
            DesktopSlotBadge("FIELD SNACK / STIM", activeSnackId?.let { allItems[it]?.name } ?: "Nutrient Ration Block", Color(0xFF00E676))
        }

        // Right Available Gear to Equip
        Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "AVAILABLE GEAR IN CARGO", color = FieldMenuDesign.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val equippable = sessionState.inventory.keys.mapNotNull { allItems[it] }.filter { it.equipment != null || it.type == "weapon" || it.type == "armor" }

            if (equippable.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "— No equippable weapons or armor in cargo. —", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(equippable) { item ->
                        val isEquipped = activeWeaponId == item.id || activeArmorId == item.id || activeAccessoryId == item.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isEquipped) FieldMenuDesign.cyan.copy(alpha = 0.15f) else FieldMenuDesign.elevatedPanel)
                                .border(BorderStroke(1.dp, if (isEquipped) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                                .clickable {
                                    val slot = item.equipment?.slot ?: if (item.type == "weapon") "weapon" else "armor"
                                    val updated = sessionState.equippedItems.toMutableMap()
                                    if (isEquipped) updated.remove(slot) else updated[slot] = item.id
                                    services.sessionStore.restore(sessionState.copy(equippedItems = updated))
                                }
                                .padding(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = item.name, color = FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isEquipped) "EQUIPPED" else "EQUIP", color = if (isEquipped) Color(0xFF00E676) else FieldMenuDesign.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSlotBadge(slot: String, name: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FieldMenuDesign.elevatedPanel)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = slot, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = name, color = FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DesktopSubTogglePill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) FieldMenuDesign.cyan.copy(alpha = 0.22f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.cyan else Color.Transparent), RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
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
private fun DesktopJournalTabContent(
    allQuests: List<Quest>,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    var journalPage by remember { mutableStateOf(JournalCategory.ACTIVE) }
    val activeQuests = allQuests.filter { sessionState.activeQuests.contains(it.id) || it.id in listOf("q_intro_01", "q_station_01") }
    val completedQuests = allQuests.filter { sessionState.completedQuests.contains(it.id) }
    val displayQuests = if (journalPage == JournalCategory.ACTIVE) (if (activeQuests.isNotEmpty()) activeQuests else allQuests.take(10)) else completedQuests
    var selectedQuest by remember(displayQuests) { mutableStateOf(displayQuests.firstOrNull()) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DesktopSubTogglePill("ACTIVE DIRECTIVES (${displayQuests.size})", journalPage == JournalCategory.ACTIVE) { journalPage = JournalCategory.ACTIVE }
            DesktopSubTogglePill("COMPLETED LOGS (${completedQuests.size})", journalPage == JournalCategory.COMPLETED) { journalPage = JournalCategory.COMPLETED }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(displayQuests) { quest ->
                        val isSelected = quest.id == selectedQuest?.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) FieldMenuDesign.cyan.copy(alpha = 0.15f) else FieldMenuDesign.elevatedPanel)
                                .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                                .clickable { selectedQuest = quest }
                                .padding(10.dp)
                        ) {
                            Text(text = quest.title, color = if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FieldMenuDesign.elevatedPanel)
                    .padding(16.dp)
            ) {
                selectedQuest?.let { quest ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = quest.title, color = FieldMenuDesign.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = quest.description.ifBlank { quest.summary }, color = FieldMenuDesign.textMuted, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopInventoryTabBody(
    sessionState: com.example.starborn.domain.session.GameSessionState,
    allItems: Map<String, Item>
) {
    val inventoryEntries = sessionState.inventory.entries.toList()

    if (inventoryEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "— Cargo hold is currently empty. Explore sectors to acquire materials and tech modules. —",
                color = FieldMenuDesign.textMuted,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(inventoryEntries) { entry ->
                val item = allItems[entry.key]
                val itemName = item?.name ?: entry.key.replace("_", " ").uppercase()
                val itemDesc = item?.description.orEmpty().ifBlank { "Standard Field Resource / Technology Component." }
                val qty = entry.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FieldMenuDesign.elevatedPanel)
                        .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = itemName, color = FieldMenuDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "x$qty", color = FieldMenuDesign.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = itemDesc, color = FieldMenuDesign.textMuted, fontSize = 11.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopMapTabContent(
    allRooms: List<Room>,
    currentRoomId: String,
    currentRoomTitle: String?
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF040810))
                .border(BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val roomsWithPos = allRooms.filter { it.pos.size >= 2 }
                if (roomsWithPos.isEmpty()) return@Canvas

                val minX = roomsWithPos.minOf { it.pos[0] }
                val maxX = roomsWithPos.maxOf { it.pos[0] }
                val minY = roomsWithPos.minOf { it.pos[1] }
                val maxY = roomsWithPos.maxOf { it.pos[1] }

                val spanX = (maxX - minX).coerceAtLeast(1)
                val spanY = (maxY - minY).coerceAtLeast(1)

                val paddingX = 40f
                val paddingY = 40f
                val availableW = size.width - paddingX * 2
                val availableH = size.height - paddingY * 2

                fun nodePos(room: Room): Offset {
                    val normX = (room.pos[0] - minX).toFloat() / spanX
                    val normY = (room.pos[1] - minY).toFloat() / spanY
                    return Offset(paddingX + normX * availableW, paddingY + (1f - normY) * availableH)
                }

                // Vectors
                val roomsById = roomsWithPos.associateBy { it.id }
                roomsWithPos.forEach { room ->
                    val start = nodePos(room)
                    room.connections.forEach { (_, targetId) ->
                        val target = roomsById[targetId]
                        if (target != null) {
                            val end = nodePos(target)
                            drawLine(
                                color = FieldMenuDesign.cyan.copy(alpha = 0.35f),
                                start = start,
                                end = end,
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // Nodes
                roomsWithPos.forEach { room ->
                    val pos = nodePos(room)
                    val isCurrent = room.id == currentRoomId || room.title == currentRoomTitle
                    val nodeColor = if (isCurrent) FieldMenuDesign.gold else FieldMenuDesign.cyan
                    val radius = if (isCurrent) 10f else 6f

                    if (isCurrent) {
                        drawCircle(color = FieldMenuDesign.gold.copy(alpha = 0.25f), radius = 18f, center = pos)
                    }

                    drawCircle(color = nodeColor, radius = radius, center = pos)
                    drawCircle(color = Color.White, radius = radius, center = pos, style = Stroke(width = 1.2f))
                }
            }
        }
    }
}

@Composable
fun DesktopTinkeringFabricatorTabContent(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState,
    allItems: Map<String, Item>
) {
    val recipes = remember { services.craftingService.tinkeringRecipes }
    var selectedRecipe by remember { mutableStateOf(recipes.firstOrNull()) }
    var craftOutcomeMessage by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Recipe Catalog
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "AVAILABLE SCHEMATICS (${recipes.size})", color = FieldMenuDesign.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(recipes) { recipe ->
                    val isSelected = selectedRecipe?.id == recipe.id
                    val canCraft = services.craftingService.canCraft(recipe)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) FieldMenuDesign.gold.copy(alpha = 0.18f) else FieldMenuDesign.elevatedPanel)
                            .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.gold else FieldMenuDesign.border.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
                            .clickable {
                                selectedRecipe = recipe
                                craftOutcomeMessage = null
                            }
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = recipe.name, color = if (isSelected) FieldMenuDesign.gold else FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = if (canCraft) "READY" else "NEED MATS", color = if (canCraft) Color(0xFF00E676) else FieldMenuDesign.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Selected Recipe Details & Fabrication Deck
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(FieldMenuDesign.elevatedPanel)
                .padding(16.dp)
        ) {
            selectedRecipe?.let { recipe ->
                val resultItem = allItems[recipe.result]
                val canCraft = services.craftingService.canCraft(recipe)

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = recipe.name.uppercase(), color = FieldMenuDesign.gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = recipe.description?.ifBlank { "Construct advanced module using salvage components." } ?: "Construct advanced module using salvage components.", color = FieldMenuDesign.textMuted, fontSize = 12.sp)

                        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.25f))

                        Text(text = "REQUIRED INGREDIENTS:", color = FieldMenuDesign.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        recipe.ingredients.forEach { (matId, neededQty) ->
                            val currentQty = sessionState.inventory[matId] ?: 0
                            val matName = allItems[matId]?.name ?: matId.replace("_", " ").uppercase()
                            val hasEnough = currentQty >= neededQty

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "• $matName", color = if (hasEnough) FieldMenuDesign.text else Color(0xFFFF5252), fontSize = 12.sp)
                                Text(text = "$currentQty / $neededQty", color = if (hasEnough) Color(0xFF00E676) else Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (craftOutcomeMessage != null) {
                            Text(text = craftOutcomeMessage!!, color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (canCraft) {
                                    val currentInv = sessionState.inventory.toMutableMap()
                                    recipe.ingredients.forEach { (matId, qty) ->
                                        val existing = currentInv[matId] ?: 0
                                        if (existing <= qty) currentInv.remove(matId) else currentInv[matId] = existing - qty
                                    }
                                    currentInv[recipe.result] = (currentInv[recipe.result] ?: 0) + recipe.resultQuantity.coerceAtLeast(1)
                                    services.sessionStore.setInventory(currentInv)
                                    craftOutcomeMessage = "✓ FABRICATION SUCCESS: Created ${resultItem?.name ?: recipe.result}!"
                                } else {
                                    craftOutcomeMessage = "✗ Missing required components."
                                }
                            },
                            enabled = canCraft,
                            colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "FABRICATE ITEM", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopStatsTabBody(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = portrait,
            contentDescription = "Nova",
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, FieldMenuDesign.cyan), CircleShape),
            contentScale = ContentScale.Crop
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "NOVA // EXPEDITION SPECIALIST", color = FieldMenuDesign.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(text = "Level: ${sessionState.playerLevel}  •  XP: ${sessionState.playerXp}  •  Credits: ${sessionState.playerCredits} CR", color = FieldMenuDesign.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "Active Directives: ${sessionState.activeQuests.size}  •  Completed: ${sessionState.completedQuests.size}", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DesktopSettingsAndStasisTabContent(
    services: DesktopAppServices,
    userSettings: UserSettings,
    currentRoomTitle: String?
) {
    val coroutineScope = rememberCoroutineScope()
    var musicVol by remember { mutableStateOf(userSettings.musicVolume) }
    var sfxVol by remember { mutableStateOf(userSettings.sfxVolume) }
    var selectedSlot by remember { mutableStateOf(1) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Audio Settings
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "AUDIO FIDELITY", color = FieldMenuDesign.cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Soundtrack & Ambience", color = FieldMenuDesign.text, fontSize = 12.sp)
                    Text(text = "${(musicVol * 100).toInt()}%", color = FieldMenuDesign.gold, fontSize = 12.sp)
                }
                Slider(
                    value = musicVol,
                    onValueChange = {
                        musicVol = it
                        coroutineScope.launch {
                            services.userSettingsStore.setMusicVolume(it)
                            services.audioDriver.setUserGain(AudioCueType.MUSIC, it)
                        }
                    }
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "SFX & Cues", color = FieldMenuDesign.text, fontSize = 12.sp)
                    Text(text = "${(sfxVol * 100).toInt()}%", color = FieldMenuDesign.gold, fontSize = 12.sp)
                }
                Slider(
                    value = sfxVol,
                    onValueChange = {
                        sfxVol = it
                        coroutineScope.launch {
                            services.userSettingsStore.setSfxVolume(it)
                            services.audioDriver.setUserGain(AudioCueType.UI, it)
                        }
                    }
                )
            }
        }

        // Stasis Archive
        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "STASIS DISK ARCHIVE", color = FieldMenuDesign.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                (1..3).forEach { slot ->
                    val meta = services.saveManager.getSlotMetadata(slot)
                    val isSelected = selectedSlot == slot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) FieldMenuDesign.gold.copy(alpha = 0.15f) else FieldMenuDesign.elevatedPanel)
                            .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.gold else FieldMenuDesign.border.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                            .clickable {
                                selectedSlot = slot
                                statusMessage = null
                            }
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "SLOT $slot: ${meta?.roomTitle ?: "— Empty Slot —"}", color = if (isSelected) FieldMenuDesign.gold else FieldMenuDesign.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (meta != null) {
                                    Text(text = "${meta.formattedDate} • Level ${meta.playerLevel} • ${meta.credits} CR", color = FieldMenuDesign.textMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = statusMessage ?: "Slot $selectedSlot ready", color = if (statusMessage != null) Color(0xFF00E676) else FieldMenuDesign.textMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val state = services.sessionStore.state.value
                            val ok = services.saveManager.saveGame(selectedSlot, state, currentRoomTitle)
                            statusMessage = if (ok) "✓ ARCHIVED TO SLOT $selectedSlot" else "✗ FAILED"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.cyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val loaded = services.saveManager.loadGame(selectedSlot)
                            if (loaded != null) {
                                services.sessionStore.restore(loaded)
                                statusMessage = "✓ RESTORED SLOT $selectedSlot"
                            } else {
                                statusMessage = "✗ SLOT EMPTY"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "LOAD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
