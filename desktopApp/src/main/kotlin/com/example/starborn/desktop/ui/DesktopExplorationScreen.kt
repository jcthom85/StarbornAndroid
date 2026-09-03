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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
    onOpenHub: () -> Unit,
    onOpenFieldKit: () -> Unit,
    onOpenFishing: () -> Unit,
    onOpenArcade: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    val rooms = remember { services.worldDataSource.loadRooms() }
    val roomsById = remember(rooms) { rooms.associateBy { it.id } }
    val npcs = remember { services.worldDataSource.loadNpcs() }
    val npcsById = remember(npcs) { npcs.mapNotNull { npc -> npc.id?.let { it to npc } }.toMap() }

    val sessionState by services.sessionStore.state.collectAsState()
    val currentRoomId = sessionState.roomId
    val currentRoom = roomsById[currentRoomId] ?: rooms.firstOrNull() ?: Room(
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

    // Compute dynamic description based on variants and state/milestones
    val displayDescription = remember(currentRoom, sessionState) {
        val matchingVariant = currentRoom.descriptionVariants.firstOrNull { variant ->
            val stateMatches = variant.requiresState.all { (k, v) ->
                (sessionState.roomStates[currentRoom.id]?.get(k) as? Boolean) == v ||
                (currentRoom.state[k] as? Boolean) == v
            }
            val milestoneMatches = variant.requiresMilestones.all { it in sessionState.completedMilestones }
            val forbiddenMilestoneMatches = variant.forbiddenMilestones.none { it in sessionState.completedMilestones }
            stateMatches && milestoneMatches && forbiddenMilestoneMatches
        }
        matchingVariant?.description ?: currentRoom.description
    }

    // Dynamic NPCs present in current room
    val activeRoomNpcs = remember(currentRoom, sessionState) {
        val staticNpcs = currentRoom.npcs
        val ruleNpcs = currentRoom.npcPresence.filter { rule ->
            val req = rule.requiresMilestones.all { it in sessionState.completedMilestones }
            val forb = rule.forbiddenMilestones.none { it in sessionState.completedMilestones }
            req && forb
        }.map { it.npc }
        (staticNpcs + ruleNpcs).distinct()
    }

    val currentTheme = remember(currentRoom.env) { services.themeRepository.getTheme(currentRoom.env) }
    val themeAccent = remember(currentTheme) {
        val rgb = currentTheme?.accent
        if (rgb != null && rgb.size >= 3) Color(rgb[0], rgb[1], rgb[2]) else TitleCyanColor
    }

    var activeDialogueSession by remember { mutableStateOf<com.example.starborn.domain.dialogue.DialogueSession?>(null) }
    var currentDialogueSpeakerId by remember { mutableStateOf<String?>(null) }
    var inspectedKeyword by remember { mutableStateOf<String?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var isTapeDeckOpen by remember { mutableStateOf(false) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isRestOpen by remember { mutableStateOf(false) }
    var isFieldMenuOpen by remember { mutableStateOf(false) }
    var isControlsOpen by remember { mutableStateOf(false) }
    val allTuningPuzzles = remember { services.worldDataSource.loadTuningPuzzles().associateBy { it.id } }
    var activeTuningPuzzle by remember { mutableStateOf<com.example.starborn.domain.model.TuningPuzzle?>(null) }

    // Start NPC dialogue using DialogueService
    fun talkToNpc(npcId: String) {
        val session = services.dialogueService.startDialogue(npcId)
        if (session != null) {
            activeDialogueSession = session
            currentDialogueSpeakerId = npcId
        } else {
            // Fallback to default talk if no structured dialogue tree found
            val npcDef = npcsById[npcId]
            actionNotification = "${npcDef?.name ?: npcId}: \"...\""
        }
    }

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

    // Traversal helper with locked/blocked direction checks
    fun travelDirection(dir: String) {
        val blocked = currentRoom.blockedDirections?.get(dir.lowercase(Locale.getDefault()))
        if (blocked != null) {
            val reqsMet = blocked.requires?.all { req ->
                val roomState = sessionState.roomStates[req.roomId]
                val currentVal = roomState?.get(req.stateKey) ?: currentRoom.state[req.stateKey]
                (currentVal as? Boolean) == req.value
            } ?: false

            if (!reqsMet) {
                actionNotification = blocked.messageLocked ?: "The passage is blocked."
                return
            }
        }

        val targetRoomId = currentRoom.connections[dir.lowercase(Locale.getDefault())]
        if (targetRoomId != null && roomsById.containsKey(targetRoomId)) {
            services.sessionStore.setRoom(targetRoomId)
        }
    }

    // Room action execution
    fun executeRoomAction(action: Map<String, Any?>) {
        val type = action["type"] as? String ?: "generic"
        val actionEvent = action["action_event"] as? String
        val stateKey = action["state_key"] as? String
        val name = action["name"] as? String ?: "Object"

        if (type == "toggle" && stateKey != null) {
            val currentVal = (sessionState.roomStates[currentRoom.id]?.get(stateKey) as? Boolean)
                ?: (currentRoom.state[stateKey] as? Boolean) ?: false
            val newVal = !currentVal
            val currentRoomState = ((sessionState.roomStates[currentRoom.id] ?: currentRoom.state.mapNotNull { (k, v) ->
                (v as? Boolean)?.let { k to it }
            }.toMap())).toMutableMap()
            currentRoomState[stateKey] = newVal
            val updatedMap = sessionState.roomStates.toMutableMap()
            updatedMap[currentRoom.id] = currentRoomState
            services.sessionStore.restore(sessionState.copy(roomStates = updatedMap))

            val toggleEvent = if (newVal) action["action_event_on"] as? String else action["action_event_off"] as? String
            val label = if (newVal) action["label_on"] as? String ?: "Activated" else action["label_off"] as? String ?: "Deactivated"
            actionNotification = "$name: $label"
            return
        }

        val puzzleId = action["puzzle_id"] as? String ?: action["tuning_puzzle_id"] as? String
        if (type == "puzzle" || type == "tuning_puzzle" || puzzleId != null) {
            val puzzle = puzzleId?.let { allTuningPuzzles[it] }
                ?: allTuningPuzzles.values.firstOrNull { it.id.contains(currentRoom.id, ignoreCase = true) }
                ?: allTuningPuzzles.values.firstOrNull()
            if (puzzle != null) {
                activeTuningPuzzle = puzzle
                return
            }
        }

        if (actionEvent != null) {
            actionNotification = "Interacted with $name."
        } else {
            val message = action["condition_unmet_message"] as? String ?: action["description"] as? String ?: "Inspected $name."
            actionNotification = message
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
                            if (isControlsOpen) {
                                isControlsOpen = false
                            } else if (isFieldMenuOpen) {
                                isFieldMenuOpen = false
                            } else if (isShopOpen) {
                                isShopOpen = false
                            } else if (isRestOpen) {
                                isRestOpen = false
                            } else if (isTapeDeckOpen) {
                                isTapeDeckOpen = false
                            } else if (activeDialogueSession != null) {
                                activeDialogueSession = null
                                currentDialogueSpeakerId = null
                            } else if (inspectedKeyword != null) {
                                inspectedKeyword = null
                            } else if (actionNotification != null) {
                                actionNotification = null
                            } else {
                                isFieldMenuOpen = true
                            }
                            true
                        }
                        Key.H, Key.Slash -> {
                            isControlsOpen = !isControlsOpen
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
                            actionNotification = "Game saved to Slot 1."
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
                            if (activeDialogueSession != null) {
                                val choices = activeDialogueSession!!.choices()
                                if (choices.isNotEmpty()) {
                                    activeDialogueSession!!.choose(choices[0].id)
                                    if (activeDialogueSession!!.isFinished()) {
                                        activeDialogueSession = null
                                        currentDialogueSpeakerId = null
                                    }
                                } else {
                                    activeDialogueSession!!.advance()
                                    if (activeDialogueSession!!.isFinished()) {
                                        activeDialogueSession = null
                                        currentDialogueSpeakerId = null
                                    }
                                }
                            } else if (activeRoomNpcs.isNotEmpty()) {
                                talkToNpc(activeRoomNpcs.first())
                            }
                            true
                        }
                        Key.Two -> {
                            if (activeDialogueSession != null) {
                                val choices = activeDialogueSession!!.choices()
                                if (choices.size > 1) {
                                    activeDialogueSession!!.choose(choices[1].id)
                                    if (activeDialogueSession!!.isFinished()) {
                                        activeDialogueSession = null
                                        currentDialogueSpeakerId = null
                                    }
                                }
                            } else if (currentRoom.enemies.isNotEmpty()) {
                                onEnterCombat(currentRoom.enemies)
                            }
                            true
                        }
                        Key.Three -> {
                            if (activeDialogueSession != null) {
                                val choices = activeDialogueSession!!.choices()
                                if (choices.size > 2) {
                                    activeDialogueSession!!.choose(choices[2].id)
                                    if (activeDialogueSession!!.isFinished()) {
                                        activeDialogueSession = null
                                        currentDialogueSpeakerId = null
                                    }
                                }
                            }
                            true
                        }
                        Key.E, Key.Spacebar, Key.Enter, Key.ButtonA -> {
                            if (activeDialogueSession != null) {
                                activeDialogueSession!!.advance()
                                if (activeDialogueSession!!.isFinished()) {
                                    activeDialogueSession = null
                                    currentDialogueSpeakerId = null
                                }
                            } else if (currentRoom.actions.isNotEmpty()) {
                                executeRoomAction(currentRoom.actions.first())
                            }
                            true
                        }
                        Key.ButtonB -> {
                            if (activeDialogueSession != null) {
                                activeDialogueSession = null
                                currentDialogueSpeakerId = null
                                true
                            } else if (isTapeDeckOpen || isShopOpen || isRestOpen) {
                                isTapeDeckOpen = false
                                isShopOpen = false
                                isRestOpen = false
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Ambient Widescreen Extension: Full-Bleed Artwork with Gentle Vignette & Telemetry Grids
        val roomBgPainter = rememberDesktopAssetPainter(currentRoom.backgroundImage, services.assetProvider)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02060B))) {
            // Fullscreen Panoramic Artwork
            Image(
                painter = roomBgPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Side HUD Telemetry Grid & Reticle Graphics
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridAlpha = 0.05f
                val step = 48.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = themeAccent.copy(alpha = gridAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = themeAccent.copy(alpha = gridAlpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }

                // Left & Right subtle telemetry rings
                drawCircle(
                    color = themeAccent.copy(alpha = 0.04f),
                    radius = 160f,
                    center = Offset(140f, size.height * 0.45f),
                    style = Stroke(width = 1.2f)
                )
                drawCircle(
                    color = TitleWarmColor.copy(alpha = 0.04f),
                    radius = 180f,
                    center = Offset(size.width - 140f, size.height * 0.55f),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 2. Dynamic Atmospheric Weather Particles
        DesktopWeatherOverlay(currentRoom.weather)

        // 3. Vignette & CRT Scanline Overlays
        DesktopVignetteOverlay(intensity = 0.55f)
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.03f)

        // 4. Dynamic Theme Glow Bands
        DesktopThemeBandOverlay(theme = currentTheme)

        // 5. Directional Compass Chevrons around screen edges
        DesktopDirectionIndicatorsOverlay(
            connections = currentRoom.connections,
            accentColor = themeAccent,
            onTravel = { dir -> travelDirection(dir) }
        )

        // 6. Widescreen 3-Panel Tactical Command Deck Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // LEFT PANEL: MAP & STATS
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Map Panel (5x5 Local Sector Grid)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF040A12).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAP",
                                color = themeAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.clickable(onClick = onOpenHub),
                                    shape = RoundedCornerShape(4.dp),
                                    color = themeAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Map,
                                            contentDescription = "Open Star Map",
                                            tint = themeAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "STAR MAP",
                                            color = themeAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = "${currentRoom.connections.size} EXITS",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF03080E).copy(alpha = 0.85f))) {
                            DesktopMiniMapCanvas(
                                allRooms = rooms,
                                currentRoom = currentRoom,
                                accentColor = themeAccent
                            )
                        }
                    }
                }

                // Stats Panel
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF040A12).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "STATS",
                            color = TitleWarmColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        DesktopCrewVitalsRow("Nova", "Lv. ${sessionState.playerLevel}", 1f, 0.85f, rememberDesktopAssetPainter("nova_portrait", services.assetProvider))
                        DesktopCrewVitalsRow("Zeke", "Lv. 1", 1f, 0.60f, rememberDesktopAssetPainter("zeke_portrait", services.assetProvider))
                    }
                }
            }

            // CENTER STAGE: Room Narrative, Points of Interest & Environmental Actions
            Column(
                modifier = Modifier
                    .weight(1.85f)
                    .fillMaxHeight(),
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
                    description = displayDescription,
                    actions = currentRoom.actions,
                    accentColor = themeAccent,
                    warmAccentColor = TitleWarmColor,
                    onActionClick = { act -> executeRoomAction(act) }
                )

                // Room Entity Presence Rail (NPCs, Stations, POIs)
                DesktopRoomEntitySection(
                    currentRoom = currentRoom,
                    activeNpcs = activeRoomNpcs,
                    npcsById = npcsById,
                    hasArcadeStation = hasArcadeStation,
                    hasFishingSpot = hasFishingSpot,
                    hasTapeDeckStation = hasTapeDeckStation,
                    hasShopStation = hasShopStation,
                    hasRestStation = hasRestStation,
                    accentColor = themeAccent,
                    onNpcClick = { npcId -> talkToNpc(npcId) },
                    onActionClick = { act -> executeRoomAction(act) },
                    onEnemiesClick = { onEnterCombat(currentRoom.enemies) },
                    onArcadeClick = onOpenArcade,
                    onFishingClick = onOpenFishing,
                    onTapeDeckClick = { isTapeDeckOpen = true },
                    onShopClick = { isShopOpen = true },
                    onRestClick = { isRestOpen = true },
                    onOpenMap = { isFieldMenuOpen = true }
                )

                // Notification Banner for Actions & Feedback
                if (actionNotification != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xEE0A1624),
                        border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth().clickable { actionNotification = null }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = actionNotification!!,
                                color = Color(0xFFF7FBFF),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }

                // Spacer to push Menu button to bottom-right of Center Stage
                Spacer(modifier = Modifier.weight(1f))

                // Bottom-Right Center Stage Menu & Controls Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { isControlsOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF63E6FF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                tint = Color(0xFF63E6FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CONTROLS",
                                color = Color(0xFF63E6FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "[H]",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Button(
                        onClick = { isFieldMenuOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF040A12).copy(alpha = 0.88f),
                            contentColor = themeAccent
                        ),
                        border = BorderStroke(1.2.dp, themeAccent.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MENU",
                                color = themeAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "[ESC]",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // RIGHT PANEL: JOURNAL & INVENTORY
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Journal Panel
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF040A12).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JOURNAL",
                                color = TitleWarmColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${sessionState.activeQuests.size} ACTIVE",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (sessionState.activeQuests.isEmpty()) {
                            Text(
                                text = "No active directives. Explore surroundings or consult local inhabitants.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val activeList = sessionState.activeQuests.toList()
                                items(activeList) { questId ->
                                    val quest = services.questRepository.allQuests().firstOrNull { it.id == questId }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.04f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = quest?.title ?: questId.replace("_", " ").uppercase(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = quest?.description ?: "Proceed with mission objectives in current sector.",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 10.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Inventory Panel
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF040A12).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().weight(0.8f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INVENTORY",
                                color = themeAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${sessionState.playerCredits} CR",
                                color = TitleWarmColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val nonZeroItems = sessionState.inventory.filter { it.value > 0 }.toList().take(4)
                        val allItemsMap = remember { services.itemRepository.allItems().associateBy { it.id } }
                        if (nonZeroItems.isEmpty()) {
                            Text(
                                text = "Field pack empty.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                nonZeroItems.forEach { (itemId, qty) ->
                                    val item = allItemsMap[itemId]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${item?.name ?: itemId}",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "x$qty",
                                            color = themeAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Overlays & Modals
        if (isControlsOpen) {
            DesktopControlsDialog(onDismiss = { isControlsOpen = false }, accentColor = themeAccent)
        }

        if (isTapeDeckOpen) {
            DesktopTapeDeckDialog(services = services, onDismiss = { isTapeDeckOpen = false })
        }

        if (isShopOpen) {
            DesktopShopDialog(services = services, onDismiss = { isShopOpen = false })
        }

        if (isRestOpen) {
            DesktopRestStopDialog(services = services, onDismiss = { isRestOpen = false })
        }

        if (activeTuningPuzzle != null) {
            val puzzle = activeTuningPuzzle!!
            DesktopTuningPuzzleDialog(
                services = services,
                puzzle = puzzle,
                onSuccess = {
                    val msg = puzzle.successMessage ?: "Resonance alignment achieved. Mechanism activated."
                    actionNotification = msg
                    activeTuningPuzzle = null
                },
                onDismiss = { activeTuningPuzzle = null }
            )
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

        if (activeDialogueSession != null) {
            val currentLine = activeDialogueSession!!.current()
            if (currentLine != null) {
                val speakerNpc = npcsById[currentDialogueSpeakerId ?: ""]
                val speakerName = currentLine.speaker.ifBlank { speakerNpc?.name ?: "Speaker" }
                val speakerRole = speakerNpc?.role ?: "Inhabitant"
                val portrait = speakerNpc?.portrait ?: speakerNpc?.id ?: currentDialogueSpeakerId ?: "default_avatar"
                val choices = activeDialogueSession!!.choices()

                DesktopDialogueOverlay(
                    speakerName = speakerName,
                    speakerRole = speakerRole,
                    portraitId = portrait,
                    text = currentLine.text,
                    choices = choices.map { it.text },
                    services = services,
                    onSelectChoice = { index ->
                        if (choices.isNotEmpty() && index in choices.indices) {
                            activeDialogueSession!!.choose(choices[index].id)
                        } else {
                            activeDialogueSession!!.advance()
                        }
                        if (activeDialogueSession!!.isFinished()) {
                            activeDialogueSession = null
                            currentDialogueSpeakerId = null
                        }
                    },
                    onAdvance = {
                        activeDialogueSession!!.advance()
                        if (activeDialogueSession!!.isFinished()) {
                            activeDialogueSession = null
                            currentDialogueSpeakerId = null
                        }
                    },
                    onClose = {
                        activeDialogueSession = null
                        currentDialogueSpeakerId = null
                    }
                )
            }
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
                    text = "ENVIRONMENT: ${env.uppercase()}",
                    color = FieldMenuDesign.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DesktopRoomDescriptionPanel(
    description: String,
    actions: List<Map<String, Any?>>,
    accentColor: Color,
    warmAccentColor: Color,
    onActionClick: (Map<String, Any?>) -> Unit
) {
    val rawText = description.ifBlank { "Sensors report stable atmospheric pressures and clear stellar vectors." }
    
    // Scan for action keywords in text
    val actionMap = remember(actions, rawText) {
        val matches = mutableListOf<Triple<Int, Int, Map<String, Any?>>>()
        actions.forEach { act ->
            val name = act["name"] as? String
            if (!name.isNullOrBlank()) {
                var startIndex = 0
                while (startIndex < rawText.length) {
                    val idx = rawText.indexOf(name, startIndex, ignoreCase = true)
                    if (idx == -1) break
                    matches.add(Triple(idx, idx + name.length, act))
                    startIndex = idx + name.length
                }
            }
        }
        matches.sortedBy { it.first }
    }

    val annotatedString = remember(rawText, actionMap, warmAccentColor) {
        androidx.compose.ui.text.buildAnnotatedString {
            if (actionMap.isEmpty()) {
                append(rawText)
            } else {
                var lastIndex = 0
                actionMap.forEach { (start, end, act) ->
                    if (start >= lastIndex) {
                        append(rawText.substring(lastIndex, start))
                        pushStringAnnotation(tag = "action", annotation = act["name"] as? String ?: "")
                        pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = warmAccentColor,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        )
                        append(rawText.substring(start, end))
                        pop()
                        pop()
                        lastIndex = end
                    }
                }
                if (lastIndex < rawText.length) {
                    append(rawText.substring(lastIndex))
                }
            }
        }
    }

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
            androidx.compose.foundation.text.ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = FieldMenuDesign.text,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "action", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val clickedAction = actions.find { (it["name"] as? String).equals(annotation.item, ignoreCase = true) }
                            if (clickedAction != null) {
                                onActionClick(clickedAction)
                            }
                        }
                }
            )
        }
    }
}

@Composable
private fun DesktopRoomEntitySection(
    currentRoom: Room,
    activeNpcs: List<String>,
    npcsById: Map<String, com.example.starborn.domain.model.Npc>,
    hasArcadeStation: Boolean,
    hasFishingSpot: Boolean,
    hasTapeDeckStation: Boolean,
    hasShopStation: Boolean,
    hasRestStation: Boolean,
    accentColor: Color,
    onNpcClick: (String) -> Unit,
    onActionClick: (Map<String, Any?>) -> Unit,
    onEnemiesClick: () -> Unit,
    onArcadeClick: () -> Unit,
    onFishingClick: () -> Unit,
    onTapeDeckClick: () -> Unit,
    onShopClick: () -> Unit,
    onRestClick: () -> Unit,
    onOpenMap: () -> Unit
) {
    val hasEntities = activeNpcs.isNotEmpty() ||
        currentRoom.enemies.isNotEmpty() ||
        currentRoom.actions.isNotEmpty() ||
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
                        accentColor = accentColor,
                        onClick = onOpenMap
                    )
                }
            }

            // Real In-World Room Actions (Switches, Panels, Terminals, Cabinets)
            currentRoom.actions.forEachIndexed { idx, action ->
                val actionName = (action["name"] as? String)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Inspect"
                val actionType = action["type"] as? String ?: "generic"
                item {
                    DesktopServicePresenceChip(
                        label = if (idx == 0) "$actionName [E]" else actionName,
                        detail = if (actionType == "toggle") "Switch" else "Interact",
                        accentColor = TitleWarmColor,
                        onClick = { onActionClick(action) }
                    )
                }
            }

            // Real Active NPCs
            activeNpcs.forEachIndexed { index, npcId ->
                val npcDef = npcsById[npcId]
                val name = npcDef?.name ?: npcId.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val role = npcDef?.role ?: "Inhabitant"
                item {
                    DesktopNpcPresenceChip(
                        npcName = name,
                        role = role,
                        shortcutKey = if (index == 0) "1" else null,
                        accentColor = TitleCyanColor,
                        onClick = { onNpcClick(npcId) }
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
                        label = "Film Archive [T]",
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
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val mapCyan = Color(0xFF00E5FF)

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
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = mapCyan.copy(alpha = 0.20f),
                border = BorderStroke(1.2.dp, mapCyan.copy(alpha = 0.8f)),
                modifier = Modifier.size(26.dp)
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
            Text(
                text = "OVERWORLD MAP",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = mapCyan.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, mapCyan.copy(alpha = 0.80f))
            ) {
                Text(
                    text = "OPEN >",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DesktopNpcPresenceChip(
    npcName: String,
    role: String,
    shortcutKey: String? = null,
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
                Text(
                    text = if (shortcutKey != null) "$npcName [$shortcutKey]" else npcName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    Text(text = "◆", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF040A12).copy(alpha = 0.80f))
            .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Status Feed
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HealthGreen)
                )
                Text(
                    text = "SECTOR ONLINE",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Right: Field Menu Action Button
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[ESC] or [M]",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                DesktopMinimalPillButton("Menu", onClick = onOpenFieldMenu)
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
    speakerName: String,
    speakerRole: String,
    portraitId: String,
    text: String,
    choices: List<String>,
    services: DesktopAppServices,
    onSelectChoice: (Int) -> Unit,
    onAdvance: () -> Unit,
    onClose: () -> Unit
) {
    val portraitPainter = rememberDesktopAssetPainter(portraitId, services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = {
                if (choices.isEmpty()) onAdvance() else onClose()
            }),
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
                    contentDescription = speakerName,
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
                            text = "${speakerName.uppercase()}  //  ${speakerRole.uppercase()}",
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
                        text = text,
                        color = FieldMenuDesign.text,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (choices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            choices.forEachIndexed { index, choiceText ->
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
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            DesktopMinimalPillButton(
                                text = "Continue ➜",
                                onClick = onAdvance
                            )
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

@Composable
private fun DesktopCrewVitalsRow(
    name: String,
    levelText: String,
    hpProgress: Float,
    shieldProgress: Float,
    portrait: androidx.compose.ui.graphics.painter.Painter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = portrait,
            contentDescription = name,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, TitleCyanColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = levelText, color = TitleWarmColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            LinearProgressIndicator(
                progress = { hpProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = HealthGreen,
                trackColor = Color(0xFF102018)
            )
            LinearProgressIndicator(
                progress = { shieldProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = ShieldBlue,
                trackColor = Color(0xFF0F1828)
            )
        }
    }
}

@Composable
private fun DesktopMiniMapCanvas(
    allRooms: List<Room>,
    currentRoom: Room,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val step = (minOf(w, h) / 5.5f).coerceAtLeast(24f)
        val roomsById = allRooms.associateBy { it.id }

        // 1. Draw 5x5 Grid Lines
        val gridAlpha = 0.12f
        for (i in -2..2) {
            drawLine(
                color = accentColor.copy(alpha = gridAlpha),
                start = Offset(cx + i * step, cy - 2.5f * step),
                end = Offset(cx + i * step, cy + 2.5f * step),
                strokeWidth = 1f
            )
            drawLine(
                color = accentColor.copy(alpha = gridAlpha),
                start = Offset(cx - 2.5f * step, cy + i * step),
                end = Offset(cx + 2.5f * step, cy + i * step),
                strokeWidth = 1f
            )
        }

        // 2. Compute local cells based on direct exits from current room
        val basePosX = if (currentRoom.pos.size >= 2) currentRoom.pos[0] else 0
        val basePosY = if (currentRoom.pos.size >= 2) currentRoom.pos[1] else 0

        // In minimap viewport: only current room + connected target rooms in same local area
        val connectedRoomIds = currentRoom.connections.values.toSet()
        val visibleRooms = allRooms.filter { room ->
            room.id == currentRoom.id || connectedRoomIds.contains(room.id)
        }

        // Draw Route Lines from Current Room to Connected Exits
        val currentCenter = Offset(cx, cy)
        currentRoom.connections.forEach { (dir, targetId) ->
            val target = roomsById[targetId]
            val targetOffset = when {
                target != null && target.pos.size >= 2 -> {
                    val dx = target.pos[0] - basePosX
                    val dy = target.pos[1] - basePosY
                    Offset(cx + dx * step, cy - dy * step)
                }
                dir.equals("north", ignoreCase = true) -> Offset(cx, cy - step)
                dir.equals("south", ignoreCase = true) -> Offset(cx, cy + step)
                dir.equals("east", ignoreCase = true) -> Offset(cx + step, cy)
                dir.equals("west", ignoreCase = true) -> Offset(cx - step, cy)
                else -> null
            }

            if (targetOffset != null) {
                drawLine(
                    color = accentColor.copy(alpha = 0.65f),
                    start = currentCenter,
                    end = targetOffset,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                // Target Preview Node
                drawCircle(
                    color = accentColor.copy(alpha = 0.70f),
                    radius = 5.5f,
                    center = targetOffset
                )
            }
        }

        // Draw Player Location (Current Room) Center Node
        drawCircle(
            color = TitleWarmColor.copy(alpha = 0.30f),
            radius = 16f,
            center = currentCenter
        )
        drawCircle(
            color = TitleWarmColor,
            radius = 7f,
            center = currentCenter
        )

        // Subtle Crosshair on Player
        val crosshair = 10f
        drawLine(TitleWarmColor, Offset(cx - crosshair, cy), Offset(cx + crosshair, cy), strokeWidth = 1.5f)
        drawLine(TitleWarmColor, Offset(cx, cy - crosshair), Offset(cx, cy + crosshair), strokeWidth = 1.5f)
    }
}
