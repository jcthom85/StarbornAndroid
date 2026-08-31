package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import java.util.Locale

private val TitleWarmColor = Color(0xFFFF9F2E)
private val TitleCyanColor = Color(0xFF63E6FF)
private val HealthGreen = Color(0xFF00E676)
private val ShieldBlue = Color(0xFF2979FF)
private val NeonPink = Color(0xFFFF007F)

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
    onOpenFishing: () -> Unit,
    onOpenArcade: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    val rooms = remember { services.worldDataSource.loadRooms() }
    val roomsById = remember(rooms) { rooms.associateBy { it.id } }
    var currentRoomId by remember { mutableStateOf(rooms.firstOrNull()?.id ?: "default_hub") }

    val currentRoom = roomsById[currentRoomId] ?: rooms.firstOrNull() ?: Room(
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

    val currentTheme = remember(currentRoom.env) { services.themeRepository.getTheme(currentRoom.env) }
    val themeAccent = remember(currentTheme) {
        val rgb = currentTheme?.accent
        if (rgb != null && rgb.size >= 3) Color(rgb[0], rgb[1], rgb[2]) else TitleCyanColor
    }

    var activeDialogue by remember { mutableStateOf<ActiveDialogueSession?>(null) }
    var inspectedKeyword by remember { mutableStateOf<String?>(null) }
    var isTapeDeckOpen by remember { mutableStateOf(false) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isRestOpen by remember { mutableStateOf(false) }
    var isFieldMenuOpen by remember { mutableStateOf(false) }
    val sessionState by services.sessionStore.state.collectAsState()

    // Room contextual station detection (matches Android in-world rules)
    val hasArcadeStation = remember(currentRoom.id, currentRoom.actions) {
        currentRoom.id.contains("arcade", ignoreCase = true) ||
            currentRoom.actions.any { (it["type"] as? String)?.contains("arcade", ignoreCase = true) == true }
    }
    val hasFishingSpot = remember(currentRoom.id, currentRoom.actions) {
        currentRoom.id.contains("shore", ignoreCase = true) ||
            currentRoom.id.contains("water", ignoreCase = true) ||
            currentRoom.id.contains("dock", ignoreCase = true) ||
            currentRoom.actions.any { (it["type"] as? String)?.contains("fish", ignoreCase = true) == true }
    }
    val hasTapeDeckStation = remember(currentRoom.id, currentRoom.actions) {
        currentRoom.id.contains("common_room", ignoreCase = true) ||
            currentRoom.id.contains("quarters", ignoreCase = true) ||
            currentRoom.actions.any { (it["type"] as? String)?.contains("tape", ignoreCase = true) == true }
    }
    val hasShopStation = remember(currentRoom.id, currentRoom.actions) {
        currentRoom.id.contains("shop", ignoreCase = true) ||
            currentRoom.id.contains("market", ignoreCase = true) ||
            currentRoom.id.contains("armory", ignoreCase = true) ||
            currentRoom.actions.any { (it["type"] as? String)?.contains("shop", ignoreCase = true) == true }
    }
    val hasRestStation = remember(currentRoom.id, currentRoom.actions) {
        currentRoom.id.contains("bed", ignoreCase = true) ||
            currentRoom.id.contains("rest", ignoreCase = true) ||
            currentRoom.id.contains("bunk", ignoreCase = true) ||
            currentRoom.actions.any { (it["type"] as? String)?.contains("rest", ignoreCase = true) == true }
    }

    // Traversal helper
    fun travelDirection(dir: String) {
        val targetRoomId = currentRoom.connections[dir.lowercase(Locale.getDefault())]
        if (targetRoomId != null && roomsById.containsKey(targetRoomId)) {
            currentRoomId = targetRoomId
            services.sessionStore.setRoom(targetRoomId)
        }
    }

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
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            if (isFieldMenuOpen) {
                                isFieldMenuOpen = false
                            } else if (isShopOpen) {
                                isShopOpen = false
                            } else if (isRestOpen) {
                                isRestOpen = false
                            } else if (isTapeDeckOpen) {
                                isTapeDeckOpen = false
                            } else if (activeDialogue != null) {
                                activeDialogue = null
                            } else if (inspectedKeyword != null) {
                                inspectedKeyword = null
                            } else {
                                isFieldMenuOpen = true
                            }
                            true
                        }
                        Key.M -> {
                            isFieldMenuOpen = !isFieldMenuOpen
                            true
                        }
                        Key.I -> {
                            onOpenFieldKit()
                            true
                        }
                        Key.F5 -> {
                            services.saveManager.saveGame(1, sessionState, currentRoom.title)
                            true
                        }
                        Key.W, Key.DirectionUp -> {
                            travelDirection("north")
                            true
                        }
                        Key.S, Key.DirectionDown -> {
                            travelDirection("south")
                            true
                        }
                        Key.A, Key.DirectionLeft -> {
                            if (currentRoom.connections.containsKey("west")) {
                                travelDirection("west")
                            } else if (hasArcadeStation) {
                                onOpenArcade()
                            }
                            true
                        }
                        Key.D, Key.DirectionRight -> {
                            travelDirection("east")
                            true
                        }
                        Key.F -> {
                            if (hasFishingSpot) onOpenFishing()
                            true
                        }
                        Key.T -> {
                            if (hasTapeDeckStation) isTapeDeckOpen = !isTapeDeckOpen
                            true
                        }
                        Key.V -> {
                            if (hasShopStation) isShopOpen = !isShopOpen
                            true
                        }
                        Key.R -> {
                            if (hasRestStation) isRestOpen = !isRestOpen
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
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Fullscreen Panoramic Room Artwork (matching Android backgroundPainter)
        val roomBgPainter = rememberDesktopAssetPainter(currentRoom.backgroundImage, services.assetProvider)
        Image(
            painter = roomBgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Dynamic Atmospheric Weather Particles (matching Android WeatherOverlay)
        DesktopWeatherOverlay(currentRoom.weather)

        // 3. Vignette & CRT Scanline Overlays
        DesktopVignetteOverlay(intensity = 0.65f)
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.05f)

        // 4. Dynamic Theme Glow Bands (matching Android ThemeBandOverlay)
        DesktopThemeBandOverlay(theme = currentTheme)

        // 5. Directional Compass Chevrons around screen edges (matching Android DirectionIndicatorsOverlay)
        DesktopDirectionIndicatorsOverlay(
            connections = currentRoom.connections,
            accentColor = themeAccent,
            onTravel = { dir -> travelDirection(dir) }
        )

        // 6. Authentic Android Top HUD Stack (TopCenter alignment)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.92f)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Room Header Panel
            DesktopRoomHeaderPanel(
                roomTitle = currentRoom.title,
                env = currentRoom.env,
                titleColor = themeAccent,
                warmTitleColor = TitleWarmColor,
                currentRoom = currentRoom,
                onOpenMap = { isFieldMenuOpen = true }
            )

            // Room Description Panel
            DesktopRoomDescriptionPanel(
                description = currentRoom.description,
                accentColor = themeAccent,
                inspectedKeyword = inspectedKeyword,
                onInspectKeyword = { lore -> inspectedKeyword = lore }
            )

            // Room Entity Presence Rail (NPCs, Ground Loot, In-world Stations)
            DesktopRoomEntitySection(
                currentRoom = currentRoom,
                hasArcadeStation = hasArcadeStation,
                hasFishingSpot = hasFishingSpot,
                hasTapeDeckStation = hasTapeDeckStation,
                hasShopStation = hasShopStation,
                hasRestStation = hasRestStation,
                accentColor = themeAccent,
                onNpcClick = {
                    activeDialogue = ActiveDialogueSession(
                        npcName = "Dr. Aris",
                        npcRole = "Chief Xenologist",
                        portraitId = "dr_aris",
                        text = "The anomalous energy signatures are rising exponentially across the lower sectors. We need to stabilize the primary array before resonance collapse.",
                        choices = listOf("I'll investigate Sector 4 immediately.", "What kind of hostiles should we expect?", "Step back for now.")
                    )
                },
                onEnemiesClick = { onEnterCombat(currentRoom.enemies) },
                onArcadeClick = onOpenArcade,
                onFishingClick = onOpenFishing,
                onTapeDeckClick = { isTapeDeckOpen = true },
                onShopClick = { isShopOpen = true },
                onRestClick = { isRestOpen = true },
                onOpenMap = { isFieldMenuOpen = true }
            )
        }

        // 7. Authentic Android Bottom Navigation / Party Status Strip
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .padding(bottom = 16.dp)
        ) {
            DesktopPartyStatusBar(
                services = services,
                sessionState = sessionState,
                onOpenFieldMenu = { isFieldMenuOpen = true },
                onOpenFieldKit = onOpenFieldKit
            )
        }

        // 8. Overlays & Modals
        if (isTapeDeckOpen) {
            DesktopTapeDeckDialog(services = services, onDismiss = { isTapeDeckOpen = false })
        }

        if (isShopOpen) {
            DesktopShopDialog(services = services, onDismiss = { isShopOpen = false })
        }

        if (isRestOpen) {
            DesktopRestStopDialog(services = services, onDismiss = { isRestOpen = false })
        }

        if (isFieldMenuOpen) {
            DesktopFieldMenuDialog(
                services = services,
                currentRoomTitle = currentRoom.title,
                onOpenFieldKit = onOpenFieldKit,
                onReturnToTitle = onReturnToMenu,
                onDismiss = { isFieldMenuOpen = false }
            )
        }

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
private fun DesktopRoomHeaderPanel(
    roomTitle: String,
    env: String,
    titleColor: Color,
    warmTitleColor: Color,
    currentRoom: Room,
    onOpenMap: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val panelColor = Color(0xFF061018).copy(alpha = 0.65f)
    val borderColor = titleColor.copy(alpha = 0.38f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = panelColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            warmTitleColor.copy(alpha = 0.08f),
                            titleColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = roomTitle,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = warmTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 2f
                        )
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    warmTitleColor.copy(alpha = 0.78f),
                                    titleColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Text(
                    text = "ENVIRONMENT: ${env.uppercase()} // ACTIVE PERIMETER SECTOR",
                    color = FieldMenuDesign.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Interactive Minimap Radar (matching Android MinimapWidget)
            DesktopMinimapRadarWidget(
                currentRoom = currentRoom,
                onOpenMap = onOpenMap,
                modifier = Modifier.size(78.dp)
            )
        }
    }
}

@Composable
private fun DesktopRoomDescriptionPanel(
    description: String,
    accentColor: Color,
    inspectedKeyword: String?,
    onInspectKeyword: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061018).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = description.ifBlank { "Sensors report stable atmospheric pressures and clear stellar vectors." },
                color = FieldMenuDesign.text,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            if (inspectedKeyword != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3300F5D4))
                        .border(BorderStroke(1.dp, TitleCyanColor), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✦ $inspectedKeyword", color = TitleCyanColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "✕",
                            color = FieldMenuDesign.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onInspectKeyword(null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopRoomEntitySection(
    currentRoom: Room,
    hasArcadeStation: Boolean,
    hasFishingSpot: Boolean,
    hasTapeDeckStation: Boolean,
    hasShopStation: Boolean,
    hasRestStation: Boolean,
    accentColor: Color,
    onNpcClick: () -> Unit,
    onEnemiesClick: () -> Unit,
    onArcadeClick: () -> Unit,
    onFishingClick: () -> Unit,
    onTapeDeckClick: () -> Unit,
    onShopClick: () -> Unit,
    onRestClick: () -> Unit,
    onOpenMap: () -> Unit
) {
    val hasEntities = currentRoom.npcs.isNotEmpty() ||
        currentRoom.enemies.isNotEmpty() ||
        hasArcadeStation ||
        hasFishingSpot ||
        hasTapeDeckStation ||
        hasShopStation ||
        hasRestStation

    if (!hasEntities) return

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF050A10).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentRoom.id.contains("landing", ignoreCase = true) || currentRoom.id.contains("entry", ignoreCase = true) || currentRoom.id.contains("bunk", ignoreCase = true)) {
                item {
                    DesktopOverworldGatewayCard(
                        sectorTitle = currentRoom.env.replace('_', ' ').uppercase(),
                        accentColor = accentColor,
                        onClick = onOpenMap
                    )
                }
            }

            if (currentRoom.npcs.isNotEmpty()) {
                item {
                    DesktopNpcPresenceChip(
                        npcName = "Dr. Aris",
                        role = "Chief Xenologist",
                        accentColor = TitleCyanColor,
                        onClick = onNpcClick
                    )
                }
            }

            if (currentRoom.enemies.isNotEmpty()) {
                item {
                    DesktopServicePresenceChip(
                        label = "Hostile Encounter [2]",
                        detail = "Active Threat",
                        accentColor = NeonPink,
                        onClick = onEnemiesClick
                    )
                }
            }

            if (hasShopStation) {
                item {
                    DesktopServicePresenceChip(
                        label = "Outpost Merchant [V]",
                        detail = "Buy & Sell",
                        accentColor = FieldMenuDesign.gold,
                        onClick = onShopClick
                    )
                }
            }

            if (hasRestStation) {
                item {
                    DesktopServicePresenceChip(
                        label = "Stasis Rest Pod [R]",
                        detail = "Recover Vitals",
                        accentColor = Color(0xFF00E676),
                        onClick = onRestClick
                    )
                }
            }

            if (hasArcadeStation) {
                item {
                    DesktopServicePresenceChip(
                        label = "Arcade Cabinet [A]",
                        detail = "Playable Cabinet",
                        accentColor = TitleCyanColor,
                        onClick = onArcadeClick
                    )
                }
            }

            if (hasFishingSpot) {
                item {
                    DesktopServicePresenceChip(
                        label = "Angling Dock [F]",
                        detail = "Cast Rod",
                        accentColor = TitleCyanColor,
                        onClick = onFishingClick
                    )
                }
            }

            if (hasTapeDeckStation) {
                item {
                    DesktopServicePresenceChip(
                        label = "Cassette Deck [T]",
                        detail = "Hi-Fi Audio",
                        accentColor = TitleWarmColor,
                        onClick = onTapeDeckClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopOverworldGatewayCard(
    sectorTitle: String?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val mapCyan = Color(0xFF00E5FF)
    val warmGold = Color(0xFFFFC857)

    Surface(
        shape = shape,
        color = Color(0xFF08121C).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, mapCyan.copy(alpha = 0.65f)),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            mapCyan.copy(alpha = 0.18f),
                            warmGold.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = mapCyan.copy(alpha = 0.20f),
                border = BorderStroke(1.2.dp, mapCyan.copy(alpha = 0.8f)),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val strokeWidth = 1.8.dp.toPx()
                        drawCircle(
                            color = mapCyan,
                            radius = size.minDimension * 0.44f,
                            style = Stroke(width = strokeWidth)
                        )
                        drawLine(
                            color = mapCyan,
                            start = Offset(size.width * 0.5f, size.height * 0.10f),
                            end = Offset(size.width * 0.5f, size.height * 0.90f),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = mapCyan,
                            start = Offset(size.width * 0.10f, size.height * 0.5f),
                            end = Offset(size.width * 0.90f, size.height * 0.5f),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Overworld: ${sectorTitle ?: "Colony Map"}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Surface Exit",
                    color = warmGold,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = mapCyan.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, mapCyan.copy(alpha = 0.80f))
            ) {
                Text(
                    text = "DEPART ➜",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun DesktopNpcPresenceChip(
    npcName: String,
    role: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = Color(0xFF071018).copy(alpha = 0.86f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.58f)),
        modifier = Modifier
            .height(46.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.62f)),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "NPC", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(text = "$npcName [1]", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = role, color = Color.White.copy(alpha = 0.66f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DesktopServicePresenceChip(
    label: String,
    detail: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = Color(0xFF071018).copy(alpha = 0.86f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.58f)),
        modifier = Modifier
            .height(46.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.62f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "◆", color = accentColor, fontSize = 12.sp)
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = detail, color = Color.White.copy(alpha = 0.66f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DesktopPartyStatusBar(
    services: DesktopAppServices,
    sessionState: com.example.starborn.domain.session.GameSessionState,
    onOpenFieldMenu: () -> Unit,
    onOpenFieldKit: () -> Unit
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FieldMenuDesign.shell.copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Nova status
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = portrait,
                    contentDescription = "Nova",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.5.dp, TitleCyanColor), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "NOVA", color = FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "LV. ${sessionState.playerLevel}", color = TitleWarmColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.width(70.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = HealthGreen,
                            trackColor = Color(0xFF102018)
                        )
                        LinearProgressIndicator(
                            progress = { 0.85f },
                            modifier = Modifier.width(50.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = ShieldBlue,
                            trackColor = Color(0xFF0F1828)
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))

                Text(
                    text = "${sessionState.playerCredits} CR",
                    color = TitleWarmColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Right: Field Menu & Cargo Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopMinimalPillButton("[I] GEAR", onClick = onOpenFieldKit)
                DesktopMinimalPillButton("[M] FIELD MENU", onClick = onOpenFieldMenu)
            }
        }
    }
}

@Composable
private fun DesktopMinimapRadarWidget(
    currentRoom: Room,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenMap),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061018).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, TitleCyanColor.copy(alpha = 0.6f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Radar concentric rings
            drawCircle(color = TitleCyanColor.copy(alpha = 0.2f), radius = size.width * 0.45f, style = Stroke(width = 1f))
            drawCircle(color = TitleCyanColor.copy(alpha = 0.12f), radius = size.width * 0.25f, style = Stroke(width = 1f))

            // Center current node
            drawCircle(color = TitleWarmColor, radius = 4f, center = center)

            // Connected exits
            currentRoom.connections.forEach { (dir, _) ->
                val offset = when (dir.lowercase()) {
                    "north" -> Offset(center.x, center.y - 18f)
                    "south" -> Offset(center.x, center.y + 18f)
                    "west" -> Offset(center.x - 20f, center.y)
                    "east" -> Offset(center.x + 20f, center.y)
                    else -> center
                }
                drawLine(color = TitleCyanColor.copy(alpha = 0.5f), start = center, end = offset, strokeWidth = 1.2f)
                drawCircle(color = TitleCyanColor, radius = 2.5f, center = offset)
            }
        }
    }
}

@Composable
private fun DesktopDirectionIndicatorsOverlay(
    connections: Map<String, String>,
    accentColor: Color,
    onTravel: (String) -> Unit
) {
    val loop = rememberInfiniteTransition(label = "compassPulse")
    val pulse by loop.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compassPulseVal"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (connections.containsKey("north")) {
            DesktopCompassChevron(
                label = "▲ [W] NORTH",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 180.dp),
                pulse = pulse,
                accentColor = accentColor,
                onClick = { onTravel("north") }
            )
        }
        if (connections.containsKey("south")) {
            DesktopCompassChevron(
                label = "▼ [S] SOUTH",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
                pulse = pulse,
                accentColor = accentColor,
                onClick = { onTravel("south") }
            )
        }
        if (connections.containsKey("west")) {
            DesktopCompassChevron(
                label = "◄ [A] WEST",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
                pulse = pulse,
                accentColor = accentColor,
                onClick = { onTravel("west") }
            )
        }
        if (connections.containsKey("east")) {
            DesktopCompassChevron(
                label = "[D] EAST ►",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
                pulse = pulse,
                accentColor = accentColor,
                onClick = { onTravel("east") }
            )
        }
    }
}

@Composable
private fun DesktopCompassChevron(
    label: String,
    modifier: Modifier,
    pulse: Float,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC061018))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = pulse * 0.85f)), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = accentColor.copy(alpha = pulse),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DesktopDialogueOverlay(
    session: ActiveDialogueSession,
    services: DesktopAppServices,
    onSelectChoice: (Int) -> Unit,
    onClose: () -> Unit
) {
    val portraitPainter = rememberDesktopAssetPainter(session.portraitId, services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                .background(FieldMenuDesign.shell.copy(alpha = 0.98f))
                .border(BorderStroke(1.5.dp, TitleCyanColor), RoundedCornerShape(FieldMenuDesign.cardRadius))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = portraitPainter,
                    contentDescription = session.npcName,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.5.dp, TitleCyanColor), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${session.npcName.uppercase()}  //  ${session.npcRole.uppercase()}",
                            color = TitleWarmColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "[ESC] DISMISS",
                            color = FieldMenuDesign.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable(onClick = onClose)
                        )
                    }

                    Text(
                        text = session.text,
                        color = FieldMenuDesign.text,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        session.choices.forEachIndexed { index, choiceText ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
                                    .background(FieldMenuDesign.elevatedPanel)
                                    .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(FieldMenuDesign.controlRadius))
                                    .clickable { onSelectChoice(index) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "◆ [${index + 1}] $choiceText",
                                    color = TitleCyanColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
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
fun DesktopMinimalPillButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
            .background(FieldMenuDesign.panel.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.45f)), RoundedCornerShape(FieldMenuDesign.controlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = FieldMenuDesign.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
