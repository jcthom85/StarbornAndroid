package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
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

data class ActiveDialogueSession(
    val npcName: String,
    val npcRole: String,
    val portraitId: String,
    val text: String,
    val choices: List<String>
)

@Composable
fun DesktopExplorationScreen(
    services: DesktopAppServices,
    onEnterCombat: (List<String>) -> Unit,
    onOpenFieldKit: () -> Unit,
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
        npcs = listOf("dr_aris"),
        items = listOf("medkit"),
        enemies = listOf("scrapper_guard"),
        connections = emptyMap(),
        pos = listOf(0, 0),
        state = emptyMap(),
        actions = emptyList()
    )

    var activeDialogue by remember { mutableStateOf<ActiveDialogueSession?>(null) }
    var inspectedKeyword by remember { mutableStateOf<String?>(null) }
    val sessionState by services.sessionStore.state.collectAsState()
    val questEntries = remember { services.questRepository.allQuests().toList() }

    // Audio Room routing
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
                            if (activeDialogue != null) {
                                activeDialogue = null
                            } else if (inspectedKeyword != null) {
                                inspectedKeyword = null
                            } else {
                                onReturnToMenu()
                            }
                            true
                        }
                        Key.I -> {
                            onOpenFieldKit()
                            true
                        }
                        Key.One -> {
                            if (activeDialogue != null) {
                                activeDialogue = null
                            } else if (currentRoom.npcs.isNotEmpty()) {
                                activeDialogue = ActiveDialogueSession(
                                    npcName = "Dr. Aris",
                                    npcRole = "Chief Xenologist",
                                    portraitId = "dr_aris",
                                    text = "The anomalous energy signatures are rising exponentially across the lower sectors. We need to stabilize the primary array before resonance collapse.",
                                    choices = listOf("I'll investigate Sector 4 immediately.", "What kind of hostiles should we expect?", "Step back for now.")
                                )
                            }
                            true
                        }
                        Key.Two -> {
                            if (activeDialogue == null && currentRoom.enemies.isNotEmpty()) {
                                onEnterCombat(currentRoom.enemies)
                            }
                            true
                        }
                        Key.Three, Key.W, Key.DirectionRight -> {
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
        // 1. Panoramic Room Artwork Backdrop
        val roomBgPainter = rememberDesktopAssetPainter(currentRoom.backgroundImage, services.assetProvider)
        Image(
            painter = roomBgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Dynamic Weather Particle Overlay (Dust, Rain, Starfall, Sparks)
        DesktopWeatherParticleCanvas(currentRoom.weather)

        // 3. Vignette gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xBB05070D),
                            Color(0x4405070D),
                            Color(0xF005070D)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            DesktopHeaderBar(
                currentRoom = currentRoom,
                onOpenFieldKit = onOpenFieldKit,
                onMenuClick = onReturnToMenu
            )

            // Main 16:9 Panoramic Split (Left Viewport + Right Telemetry)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Panoramic Sector Narrative & Entity Action Tray (60% width)
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Room Narrative Card with Keyword Highlighting
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelDark.copy(alpha = 0.94f))
                            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SECTOR: ${currentRoom.title.uppercase()}",
                                    color = NeonPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "ENVIRONMENT: ${currentRoom.env.uppercase()}",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentRoom.description.ifBlank { "Deep-space sensor readings detect no atmospheric disturbances." },
                                color = TextWhite,
                                fontSize = 15.sp,
                                lineHeight = 23.sp
                            )

                            // Interactive Keywords Row
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "INVESTIGATE:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DesktopKeywordTag("Anomalous Array") { inspectedKeyword = "Ancient perimeter relay array emitting subharmonic distress beacons." }
                                DesktopKeywordTag("Security Terminal") { inspectedKeyword = "Locked with clearance code Sigma-9. Requires decryption mod." }
                            }
                        }
                    }

                    // Keyword Lore Popup
                    AnimatedVisibility(visible = inspectedKeyword != null) {
                        inspectedKeyword?.let { lore ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0C1628))
                                    .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔍 $lore", color = NeonCyan, fontSize = 13.sp)
                                    Text(text = "[ESC: CLOSE]", color = TextMuted, fontSize = 10.sp, modifier = Modifier.clickable { inspectedKeyword = null })
                                }
                            }
                        }
                    }

                    // Entities & Sector Action Tray
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelDark.copy(alpha = 0.94f))
                            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE ENTITIES & WAYPOINTS",
                                color = NeonAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // NPC Button
                                if (currentRoom.npcs.isNotEmpty()) {
                                    DesktopActionBadge(
                                        title = "[1] Talk to Dr. Aris",
                                        subtitle = "Dialogue Tree",
                                        color = NeonCyan,
                                        onClick = {
                                            activeDialogue = ActiveDialogueSession(
                                                npcName = "Dr. Aris",
                                                npcRole = "Chief Xenologist",
                                                portraitId = "dr_aris",
                                                text = "The anomalous energy signatures are rising exponentially across the lower sectors. We need to stabilize the primary array before resonance collapse.",
                                                choices = listOf("I'll investigate Sector 4 immediately.", "What kind of hostiles should we expect?", "Step back for now.")
                                            )
                                        }
                                    )
                                }

                                // Combat Trigger Button
                                if (currentRoom.enemies.isNotEmpty()) {
                                    DesktopActionBadge(
                                        title = "[2] Engage Hostile Patrol",
                                        subtitle = "Turn-Based Combat",
                                        color = Color(0xFFFF3366),
                                        onClick = { onEnterCombat(currentRoom.enemies) }
                                    )
                                }

                                // Next Room Waypoint
                                DesktopActionBadge(
                                    title = "[3] Next Sector Gateway",
                                    subtitle = "Traverse Waypoint",
                                    color = NeonAmber,
                                    onClick = {
                                        if (rooms.isNotEmpty()) {
                                            currentRoomIndex = (currentRoomIndex + 1) % rooms.size
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Right Column: Persistent Crew Status, Stellar Radar, Directives (40% width)
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Crew Vitals Card
                    DesktopCrewVitalsCard(services, sessionState)

                    // Stellar Cartography Radar
                    DesktopCartographyCard(rooms, currentRoomIndex) { currentRoomIndex = it }

                    // Mission Directives Tracker
                    DesktopQuestDirectivesCard(questEntries)
                }
            }

            // Bottom Hotkey Palette
            DesktopExplorationBottomBar(onOpenFieldKit)
        }

        // Dialogue Modal Overlay
        if (activeDialogue != null) {
            DesktopDialogueOverlay(
                session = activeDialogue!!,
                services = services,
                onSelectChoice = { activeDialogue = null },
                onClose = { activeDialogue = null }
            )
        }
    }
}

@Composable
private fun DesktopHeaderBar(
    currentRoom: Room,
    onOpenFieldKit: () -> Unit,
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
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonAmber))
            Text(
                text = currentRoom.title.ifBlank { "Uncharted Sector" },
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenFieldKit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF142036), contentColor = NeonCyan),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
            ) {
                Text(text = "FIELD KIT & CARGO [I]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onMenuClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141A26), contentColor = TextMuted),
                border = BorderStroke(1.dp, PanelBorder)
            ) {
                Text(text = "MENU [ESC]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DesktopKeywordTag(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NeonCyan.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "[$text]",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DesktopActionBadge(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(text = title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextWhite.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DesktopCrewVitalsCard(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDark.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = portrait,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Commander Nova", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Lv. ${sessionState.playerLevel}", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(text = "HULL INTEGRITY", color = TextMuted, fontSize = 9.sp)
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = HealthGreen,
                    trackColor = Color(0xFF14242A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "SHIELD MATRIX", color = TextMuted, fontSize = 9.sp)
                LinearProgressIndicator(
                    progress = { 0.85f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = ShieldBlue,
                    trackColor = Color(0xFF101B2E)
                )
            }
        }
    }
}

@Composable
private fun DesktopCartographyCard(
    rooms: List<Room>,
    currentIndex: Int,
    onSelectRoom: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDark.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = "STELLAR CARTOGRAPHY", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(rooms.take(8)) { idx, r ->
                    Text(
                        text = "${if (idx == currentIndex) "▶ " else "  "}${r.title}",
                        color = if (idx == currentIndex) NeonCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (idx == currentIndex) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { onSelectRoom(idx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopQuestDirectivesCard(quests: List<com.example.starborn.domain.model.Quest>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDark.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = "ACTIVE DIRECTIVES", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            quests.take(2).forEach { q ->
                Text(text = "• ${q.title}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = q.description.orEmpty(), color = TextMuted, fontSize = 11.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DesktopDialogueOverlay(
    session: ActiveDialogueSession,
    services: DesktopAppServices,
    onSelectChoice: (Int) -> Unit,
    onClose: () -> Unit
) {
    val portrait = rememberDesktopAssetPainter(session.portraitId, services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC04060E))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1220))
                .border(BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portrait,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, NeonCyan), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.npcName.uppercase()} // ${session.npcRole.uppercase()}",
                    color = NeonAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${session.text}\"",
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dialogue Choices
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    session.choices.forEachIndexed { index, choice ->
                        Button(
                            onClick = { onSelectChoice(index) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14243A),
                                contentColor = NeonCyan
                            ),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Text(text = "[${index + 1}] $choice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopWeatherParticleCanvas(weather: String?) {
    val transition = rememberInfiniteTransition(label = "weather_motion")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather_progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Subtle drifting particles
        for (i in 0 until 40) {
            val startX = (i * 47f) % width
            val curY = ((i * 73f) + progress * height * 0.8f) % height
            drawCircle(
                color = if (weather == "rain") Color(0x6663E6FF) else Color(0x33FFB703),
                radius = if (weather == "rain") 1.8f else 1.2f,
                center = Offset(startX, curY)
            )
        }
    }
}

@Composable
private fun DesktopExplorationBottomBar(onOpenFieldKit: () -> Unit) {
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
            text = "HOTKEYS: [1] Dialogue  •  [2] Engage Enemy  •  [3] Traversal Waypoint  •  [I] Field Kit  •  [ESC] Menu",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "WIDESCREEN 16:9 • 60 FPS",
            color = TextMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
