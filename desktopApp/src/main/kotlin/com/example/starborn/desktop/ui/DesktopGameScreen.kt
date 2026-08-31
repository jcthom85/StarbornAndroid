package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Room

private val NeonCyan = Color(0xFF00F5D4)
private val NeonPink = Color(0xFFFF007F)
private val NeonAmber = Color(0xFFFFB703)
private val DeepSpaceDark = Color(0xFF070A12)
private val PanelDark = Color(0xFF0E1322)
private val PanelBorder = Color(0xFF1B2438)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFF8899B0)
private val HealthGreen = Color(0xFF00E676)
private val ShieldBlue = Color(0xFF2979FF)

enum class DesktopActiveTab {
    EXPLORATION, INVENTORY, JOURNAL, MAP
}

@Composable
fun DesktopGameScreen(
    services: DesktopAppServices,
    onReturnToMenu: () -> Unit
) {
    val rooms = remember { services.worldDataSource.loadRooms() }
    var currentRoomIndex by remember { mutableStateOf(0) }
    val currentRoom = rooms.getOrNull(currentRoomIndex) ?: Room(
        id = "default_hub",
        env = "station",
        title = "Orbital Station Alpha",
        backgroundImage = "bg_station",
        description = "A humming perimeter docking bay looking out over the ringed planet below.",
        npcs = emptyList(),
        items = emptyList(),
        enemies = emptyList(),
        connections = emptyMap(),
        pos = listOf(0, 0),
        state = emptyMap(),
        actions = emptyList()
    )

    var activeTab by remember { mutableStateOf(DesktopActiveTab.EXPLORATION) }
    var selectedNodeIndex by remember { mutableStateOf(0) }

    val sessionState by services.sessionStore.state.collectAsState()
    val questEntries = remember { services.questRepository.allQuests().toList() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            if (activeTab != DesktopActiveTab.EXPLORATION) {
                                activeTab = DesktopActiveTab.EXPLORATION
                            } else {
                                onReturnToMenu()
                            }
                            true
                        }
                        Key.I -> {
                            activeTab = if (activeTab == DesktopActiveTab.INVENTORY) DesktopActiveTab.EXPLORATION else DesktopActiveTab.INVENTORY
                            true
                        }
                        Key.M -> {
                            activeTab = if (activeTab == DesktopActiveTab.MAP) DesktopActiveTab.EXPLORATION else DesktopActiveTab.MAP
                            true
                        }
                        Key.J -> {
                            activeTab = if (activeTab == DesktopActiveTab.JOURNAL) DesktopActiveTab.EXPLORATION else DesktopActiveTab.JOURNAL
                            true
                        }
                        Key.One -> {
                            if (rooms.isNotEmpty()) {
                                currentRoomIndex = (currentRoomIndex + 1) % rooms.size
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation / Sector Header Bar
            DesktopHeaderBar(
                currentRoom = currentRoom,
                activeTab = activeTab,
                onTabSelect = { activeTab = it },
                onMenuClick = onReturnToMenu
            )

            // Main 3-Column Landscape Layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Room & Node Exploration Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelDark)
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    when (activeTab) {
                        DesktopActiveTab.EXPLORATION -> DesktopExplorationPanel(
                            room = currentRoom,
                            onNextRoom = {
                                if (rooms.isNotEmpty()) {
                                    currentRoomIndex = (currentRoomIndex + 1) % rooms.size
                                }
                            }
                        )
                        DesktopActiveTab.INVENTORY -> DesktopInventoryPanel(services)
                        DesktopActiveTab.JOURNAL -> DesktopJournalPanel(questEntries)
                        DesktopActiveTab.MAP -> DesktopMapPanel(rooms, currentRoomIndex) { currentRoomIndex = it }
                    }
                }

                // Center Column: Interactive Stage / Combat & Narrative Terminal
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelDark)
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    DesktopStageTerminal(currentRoom)
                }

                // Right Column: Party Status & Active Mission Telemetry
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelDark)
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    DesktopTelemetryPanel(services, sessionState)
                }
            }

            // Bottom Hotkey Action Bar
            DesktopBottomBar(activeTab) { activeTab = it }
        }
    }
}

@Composable
private fun DesktopHeaderBar(
    currentRoom: Room,
    activeTab: DesktopActiveTab,
    onTabSelect: (DesktopActiveTab) -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050810))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "STARBORN",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted)
            )
            Text(
                text = currentRoom.title.ifBlank { "Uncharted Sector" },
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tab Navigation Buttons with Hotkey hints
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesktopTabButton("EXPLORE", activeTab == DesktopActiveTab.EXPLORATION) { onTabSelect(DesktopActiveTab.EXPLORATION) }
            DesktopTabButton("INVENTORY [I]", activeTab == DesktopActiveTab.INVENTORY) { onTabSelect(DesktopActiveTab.INVENTORY) }
            DesktopTabButton("JOURNAL [J]", activeTab == DesktopActiveTab.JOURNAL) { onTabSelect(DesktopActiveTab.JOURNAL) }
            DesktopTabButton("MAP [M]", activeTab == DesktopActiveTab.MAP) { onTabSelect(DesktopActiveTab.MAP) }
            DesktopTabButton("MENU [ESC]", false, onClick = onMenuClick)
        }
    }
}

@Composable
private fun DesktopTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                BorderStroke(1.dp, if (isSelected) NeonCyan else PanelBorder),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DesktopExplorationPanel(
    room: Room,
    onNextRoom: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "ROOM SECTOR",
            color = NeonPink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Text(
            text = room.title.ifBlank { "Sector Chamber" },
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Text(
            text = room.description.ifBlank { "Deep-space sensor readings detect no atmospheric disturbances." },
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "WAYPOINTS & ACTIONS",
            color = NeonAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onNextRoom,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF142036)),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "[1] Traverse to Next Sector",
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DesktopInventoryPanel(services: DesktopAppServices) {
    val items = remember { services.itemRepository.allItems() }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CARGO & INVENTORY [I]",
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items.take(15)) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF090D18))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = item.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = item.description.orEmpty(), color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopJournalPanel(quests: List<com.example.starborn.domain.model.Quest>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "MISSION LOG [J]",
            color = NeonAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(quests) { _, quest ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF090D18))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = quest.title, color = NeonAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = quest.description.orEmpty(), color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMapPanel(
    rooms: List<Room>,
    currentIndex: Int,
    onSelectRoom: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STELLAR CARTOGRAPHY [M]",
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(rooms) { index, room ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (index == currentIndex) NeonCyan.copy(alpha = 0.15f) else Color(0xFF090D18))
                        .border(
                            BorderStroke(1.dp, if (index == currentIndex) NeonCyan else PanelBorder),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectRoom(index) }
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${room.title.ifBlank { "Uncharted Sector" }}",
                        color = if (index == currentIndex) NeonCyan else TextWhite,
                        fontSize = 12.sp,
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopStageTerminal(room: Room) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "STAGE VIEWPORT • TACTICAL SIMULATION",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF04060C))
                    .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "[ SCANNER ACTIVE ]",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sector: ${room.id}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050810))
                .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "> System status nominal. Ready for commands.",
                color = NeonGreen,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private val NeonGreen = Color(0xFF00E676)

@Composable
private fun DesktopTelemetryPanel(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CREW STATUS",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Commander Vitals Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090D18))
                .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Commander (Vanguard)", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Lv. ${sessionState.playerLevel}", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HP Bar
                Text(text = "HULL INTEGRITY (HP)", color = TextMuted, fontSize = 10.sp)
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HealthGreen,
                    trackColor = Color(0xFF1B2438)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Energy / Shield Bar
                Text(text = "SHIELD MATRIX", color = TextMuted, fontSize = 10.sp)
                LinearProgressIndicator(
                    progress = { 0.85f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ShieldBlue,
                    trackColor = Color(0xFF1B2438)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "RESOURCES & CURRENCY",
            color = NeonAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Credits: ${sessionState.playerCredits} CR",
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DesktopBottomBar(
    activeTab: DesktopActiveTab,
    onTabChange: (DesktopActiveTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF04060C))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HOTKEYS: [1] Sector Action  •  [I] Inventory  •  [M] Map  •  [J] Journal  •  [ESC] Menu",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "FPS: 60 • Windows x64",
            color = TextMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
