package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
private val NeonAmber = Color(0xFFFFB703)
private val GlassDark = Color(0xDD090E18)
private val GlassBorder = Color(0x4400F5D4)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)
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

    // Room audio routing
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
            .background(Color(0xFF04060C))
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
        // 1. Full-Bleed 100% Panoramic Room Artwork
        val roomBgPainter = rememberDesktopAssetPainter(currentRoom.backgroundImage, services.assetProvider)
        Image(
            painter = roomBgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Dynamic Atmospheric Weather Particles
        DesktopWeatherParticleCanvas(currentRoom.weather)

        // 3. Subtle Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x66000000), Color(0xD004060C)),
                        radius = 1100f
                    )
                )
        )

        // 4. Minimal Floating Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Top Left: Sleek Crew Chip
            DesktopFloatingCrewChip(services, sessionState)

            // Top Right: Minimal Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopMinimalPillButton("[I] CARGO", onClick = onOpenFieldKit)
                DesktopMinimalPillButton("[ESC] MENU", onClick = onReturnToMenu)
            }
        }

        // 5. Center-Bottom Atmospheric Narrative & Action Deck
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.85f)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Keyword Lore Popup if active
            AnimatedVisibility(
                visible = inspectedKeyword != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                inspectedKeyword?.let { lore ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xE6081424))
                            .border(BorderStroke(1.dp, NeonCyan), RoundedCornerShape(10.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✦ $lore", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "✕",
                                color = TextMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { inspectedKeyword = null }.padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Main Narrative Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassDark)
                    .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp, vertical = 22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentRoom.title.uppercase(),
                            color = NeonAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = currentRoom.env.uppercase(),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentRoom.description.ifBlank { "Sensors report stable atmospheric pressures and clear stellar vectors." },
                        color = TextWhite,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )

                    // Clickable Keyword Chips
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Inspect:", color = TextMuted, fontSize = 12.sp)
                        DesktopInlineKeyword("[Distress Beacon]") {
                            inspectedKeyword = "An automated perimeter frequency repeating a distress loop."
                        }
                        DesktopInlineKeyword("[Console Interface]") {
                            inspectedKeyword = "Active console displaying navigational vector routes to Sector 4."
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Minimal Action Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentRoom.npcs.isNotEmpty()) {
                            DesktopActionPill(
                                label = "[1] Talk to Dr. Aris",
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

                        if (currentRoom.enemies.isNotEmpty()) {
                            DesktopActionPill(
                                label = "[2] Hostile Patrol (Combat)",
                                color = Color(0xFFFF3366),
                                onClick = { onEnterCombat(currentRoom.enemies) }
                            )
                        }

                        DesktopActionPill(
                            label = "[3] Sector Gateway →",
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
private fun DesktopFloatingCrewChip(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xDD090E18))
            .border(BorderStroke(1.dp, Color(0x3300F5D4)), RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = portrait,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.dp, NeonCyan), CircleShape),
                contentScale = ContentScale.Crop
            )

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Nova", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Lv. ${sessionState.playerLevel}", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = HealthGreen,
                        trackColor = Color(0xFF14242A)
                    )
                    LinearProgressIndicator(
                        progress = { 0.85f },
                        modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0xFF101B2E)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0x33FFFFFF))
            )

            Text(
                text = "${sessionState.playerCredits} CR",
                color = NeonAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DesktopMinimalPillButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC090E18))
            .border(BorderStroke(1.dp, Color(0x3300F5D4)), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DesktopInlineKeyword(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = NeonCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x2200F5D4))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DesktopActionPill(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.8f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
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
            .background(Color(0xCC030408))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF5090E18))
                .border(BorderStroke(1.dp, NeonCyan), RoundedCornerShape(16.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portrait,
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, NeonCyan), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.npcName.uppercase()}  •  ${session.npcRole.uppercase()}",
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    session.choices.forEachIndexed { index, choice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2200F5D4))
                                .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                                .clickable { onSelectChoice(index) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "[${index + 1}] $choice",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopWeatherParticleCanvas(weather: String?) {
    val transition = rememberInfiniteTransition(label = "weather_drift")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather_p"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (i in 0 until 35) {
            val startX = (i * 51f) % width
            val curY = ((i * 79f) + progress * height * 0.7f) % height
            drawCircle(
                color = if (weather == "rain") Color(0x5563E6FF) else Color(0x28FFB703),
                radius = if (weather == "rain") 1.8f else 1.2f,
                center = Offset(startX, curY)
            )
        }
    }
}
