package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.Room

private val NeonCyan = Color(0xFF00F5D4)
private val NeonPink = Color(0xFFFF007F)
private val NeonAmber = Color(0xFFFFB703)
private val DeepSpaceDark = Color(0xFF05070D)
private val PanelDark = Color(0xFF090E18)
private val PanelBorder = Color(0xFF1B283E)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFF8FA1B7)
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
    val sessionState by services.sessionStore.state.collectAsState()
    val questEntries = remember { services.questRepository.allQuests().toList() }
    val items = remember { services.itemRepository.allItems() }

    // Play Room background ambience & music when entering room
    LaunchedEffect(currentRoom.id) {
        val cmds = services.audioRouter.commandsForRoom(
            hubId = currentRoom.env,
            roomId = currentRoom.id,
            weatherId = currentRoom.weather
        )
        services.audioDriver.executeAll(cmds)
    }

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
        // Room Background Art with atmospheric backdrop
        val roomBgPainter = rememberDesktopAssetPainter(currentRoom.backgroundImage, services.assetProvider)
        Image(
            painter = roomBgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradient tint to guarantee UI legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC05070D),
                            Color(0x8805070D),
                            Color(0xEE05070D)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
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
                // Left Column: Room Narrative & Sector Traversal Viewport
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark.copy(alpha = 0.92f))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    when (activeTab) {
                        DesktopActiveTab.EXPLORATION -> DesktopExplorationPanel(
                            room = currentRoom,
                            services = services,
                            onNextRoom = {
                                if (rooms.isNotEmpty()) {
                                    currentRoomIndex = (currentRoomIndex + 1) % rooms.size
                                }
                            }
                        )
                        DesktopActiveTab.INVENTORY -> DesktopInventoryPanel(items)
                        DesktopActiveTab.JOURNAL -> DesktopJournalPanel(questEntries)
                        DesktopActiveTab.MAP -> DesktopMapPanel(rooms, currentRoomIndex) { currentRoomIndex = it }
                    }
                }

                // Center Column: Visual Scene Stage & Environmental Encounters
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark.copy(alpha = 0.88f))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    DesktopStageTerminal(currentRoom, services)
                }

                // Right Column: Party Vitals & Mission Telemetry
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark.copy(alpha = 0.92f))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    DesktopTelemetryPanel(services, sessionState)
                }
            }

            // Bottom Command Palette
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
            .background(Color(0xF0050812))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(NeonAmber)
            )
            Text(
                text = currentRoom.title.ifBlank { "Uncharted Sector" },
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tab Navigation Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesktopTabButton("EXPLORE", activeTab == DesktopActiveTab.EXPLORATION) { onTabSelect(DesktopActiveTab.EXPLORATION) }
            DesktopTabButton("CARGO [I]", activeTab == DesktopActiveTab.INVENTORY) { onTabSelect(DesktopActiveTab.INVENTORY) }
            DesktopTabButton("LOG [J]", activeTab == DesktopActiveTab.JOURNAL) { onTabSelect(DesktopActiveTab.JOURNAL) }
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                BorderStroke(1.dp, if (isSelected) NeonCyan else PanelBorder),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DesktopExplorationPanel(
    room: Room,
    services: DesktopAppServices,
    onNextRoom: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CURRENT SECTOR",
            color = NeonPink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Text(
            text = room.title.ifBlank { "Sector Chamber" },
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Text(
            text = room.description.ifBlank { "Sensors report stable atmospheric pressures and clear stellar vectors." },
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SECTOR WAYPOINTS & EXITS",
            color = NeonAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onNextRoom,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14223A)),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "[1] Traverse to Next Sector",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DesktopInventoryPanel(items: List<com.example.starborn.domain.model.Item>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CARGO HOLD & INVENTORY [I]",
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items.take(20)) { _, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1322))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = item.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = item.description.orEmpty(), color = TextMuted, fontSize = 11.sp, maxLines = 2)
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
            text = "MISSION LOG & DIRECTIVES [J]",
            color = NeonAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(quests) { _, quest ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1322))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                        .padding(12.dp)
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
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(rooms) { index, room ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index == currentIndex) NeonCyan.copy(alpha = 0.18f) else Color(0xFF0C1322))
                        .border(
                            BorderStroke(1.dp, if (index == currentIndex) NeonCyan else PanelBorder),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectRoom(index) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${room.title.ifBlank { "Uncharted Sector" }}",
                        color = if (index == currentIndex) NeonCyan else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopStageTerminal(room: Room, services: DesktopAppServices) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "STAGE VIEWPORT • LIVE SENSOR FEED",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Live Room Artwork Frame
            val stageBg = rememberDesktopAssetPainter(room.backgroundImage, services.assetProvider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = stageBg,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC05070D))
                            )
                        )
                )
                Text(
                    text = "SECTOR: ${room.id.uppercase()}",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF050812))
                .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "> Sublight drives standing by. Environment: ${room.env.uppercase()}",
                color = HealthGreen,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DesktopTelemetryPanel(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    val portraitPainter = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CREW STATUS & VITALS",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Commander Card with Authentic Portrait
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0C1322))
                .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = portraitPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Commander", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Lv. ${sessionState.playerLevel}", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "HULL INTEGRITY", color = TextMuted, fontSize = 10.sp)
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = HealthGreen,
                        trackColor = Color(0xFF182236)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "SHIELD MATRIX", color = TextMuted, fontSize = 10.sp)
                    LinearProgressIndicator(
                        progress = { 0.85f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0xFF182236)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "RESOURCES & CURRENCY",
            color = NeonAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Credits: ${sessionState.playerCredits} CR",
            color = TextWhite,
            fontSize = 14.sp,
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
            .background(Color(0xFF04060E))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CONTROLS: [1] Sector Action  •  [I] Cargo  •  [M] Cartography  •  [J] Mission Log  •  [ESC] Menu",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "FPS: 60 • Windows Widescreen",
            color = TextMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
