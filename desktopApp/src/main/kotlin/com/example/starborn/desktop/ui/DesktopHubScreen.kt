package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.node.NodeProgressionEvaluator
import com.example.starborn.domain.node.NodeVisibility
import kotlin.math.roundToInt

private val HubCyan = Color(0xFF63E6FF)
private val HubAmber = Color(0xFFFF9F2E)
private val HubGreen = Color(0xFF00E676)
private val HubRed = Color(0xFFFF5252)

data class DesktopHubNodeUi(
    val id: String,
    val hubId: String,
    val title: String,
    val description: String?,
    val lockedPreview: String?,
    val iconPath: String,
    val rooms: List<String>,
    val centerX: Float,
    val centerY: Float,
    val canEnter: Boolean,
    val completed: Boolean,
    val discovered: Boolean
)

@Composable
fun DesktopHubScreen(
    services: DesktopAppServices,
    onEnterRoom: (String) -> Unit,
    onBackToExploration: () -> Unit
) {
    val sessionState by services.sessionStore.state.collectAsState()
    val allHubs = remember { services.worldDataSource.loadHubs() }
    val allNodes = remember { services.worldDataSource.loadHubNodes() }
    val nodeDescriptions = remember { services.worldDataSource.loadHubNodeDescriptions() }
    val nodeLockedPreviews = remember { services.worldDataSource.loadHubNodeLockedPreviews() }
    val evaluator = remember { NodeProgressionEvaluator() }

    // Active Hub Selection
    val currentHubId = sessionState.hubId ?: allHubs.firstOrNull()?.id ?: "hub_1_homestead"
    var selectedHubId by remember(currentHubId) { mutableStateOf(currentHubId) }
    val activeHub = remember(selectedHubId, allHubs) {
        allHubs.firstOrNull { it.id == selectedHubId } ?: allHubs.firstOrNull() ?: Hub(
            id = "hub_1_homestead",
            worldId = "world_1",
            title = "Homestead Sector",
            description = "Residential quarters and mining barracks in the upper crust.",
            backgroundImage = "images/hubs/hub_mining_colony.png",
            discovered = true
        )
    }

    // Build UI Nodes for active hub
    val hubNodes = remember(activeHub.id, sessionState, allNodes) {
        val rawNodes = allNodes.filter { it.hubId == activeHub.id }
        rawNodes.map { node ->
            val availability = evaluator.evaluate(node, sessionState)
            val isDiscovered = availability.visibility == NodeVisibility.REVEALED || sessionState.visitedNodes.contains(node.id)
            val canEnter = availability.canEnterFromHub
            val isCompleted = availability.completed

            DesktopHubNodeUi(
                id = node.id,
                hubId = node.hubId,
                title = node.title,
                description = nodeDescriptions[node.id],
                lockedPreview = nodeLockedPreviews[node.id] ?: availability.unmetRequirement,
                iconPath = node.iconImage ?: "images/nodes/node_generic.png",
                rooms = node.rooms,
                centerX = node.position.centerX,
                centerY = node.position.centerY,
                canEnter = canEnter,
                completed = isCompleted,
                discovered = isDiscovered
            )
        }
    }

    var selectedNodeId by remember(hubNodes) {
        mutableStateOf(hubNodes.firstOrNull { it.discovered }?.id ?: hubNodes.firstOrNull()?.id)
    }
    val selectedNode = remember(selectedNodeId, hubNodes) {
        hubNodes.firstOrNull { it.id == selectedNodeId } ?: hubNodes.firstOrNull()
    }

    // Parallax & Atmosphere Animation
    val infiniteTransition = rememberInfiniteTransition(label = "hubAtmosphere")
    val parallaxScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hubParallax"
    )

    // Audio routing for current hub
    LaunchedEffect(activeHub.id) {
        val cmds = services.audioRouter.commandsForRoom(hubId = activeHub.id, roomId = null)
        services.audioDriver.executeAll(cmds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060A))
    ) {
        // 1. Hub Background Artwork
        val bgPainter = rememberDesktopAssetPainter(
            path = activeHub.backgroundImage ?: "images/hubs/hub_mining_colony.png",
            assetProvider = services.assetProvider
        )
        Image(
            painter = bgPainter,
            contentDescription = activeHub.title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = parallaxScale
                    scaleY = parallaxScale
                },
            contentScale = ContentScale.Crop
        )

        // 2. Atmospheric Sci-Fi Vignette & Scanline Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        // 3. Widescreen 3-Panel Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT PANEL: Sector / World Hub Selector & Active Quest Telemetry
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF080D18).copy(alpha = 0.90f),
                border = BorderStroke(1.2.dp, HubCyan.copy(alpha = 0.35f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBackToExploration,
                            modifier = Modifier
                                .size(36.dp)
                                .background(HubCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Return to Exploration",
                                tint = HubCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "OVERWORLD MAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.5.sp
                                ),
                                color = HubCyan
                            )
                            Text(
                                text = activeHub.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    // Sector / Hub Directory
                    Text(
                        text = "AVAILABLE HUBS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = 0.60f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allHubs.forEach { hub ->
                            val isSelected = hub.id == selectedHubId
                            val isCurrentLocation = hub.id == sessionState.hubId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedHubId = hub.id
                                        services.sessionStore.setHub(hub.id)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) HubCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) HubCyan else Color.White.copy(alpha = 0.10f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hub.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) HubCyan else Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = hub.worldId.replace("_", " ").uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                    }
                                    if (isCurrentLocation) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = HubAmber.copy(alpha = 0.20f),
                                            border = BorderStroke(1.dp, HubAmber)
                                        ) {
                                            Text(
                                                text = "HERE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                ),
                                                color = HubAmber,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active Quest Target Footer
                    sessionState.activeQuests.firstOrNull()?.let { qId ->
                        val quest = services.questRepository.questById(qId)
                        if (quest != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF141926),
                                border = BorderStroke(1.dp, HubAmber.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "MISSION OBJECTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = HubAmber
                                    )
                                    Text(
                                        text = quest.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (quest.description.isNotBlank()) quest.description else quest.summary.ifBlank { "Investigate local sector nodes." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.70f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CENTER STAGE: Interactive Node Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                // Nodes on interactive map layer
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val density = LocalDensity.current

                    hubNodes.forEach { node ->
                        val isSelected = node.id == selectedNodeId
                        val markerSize = 64.dp
                        val markerSizePx = with(density) { markerSize.toPx() }

                        val posX = (widthPx * node.centerX - markerSizePx / 2f)
                            .coerceIn(0f, (widthPx - markerSizePx).coerceAtLeast(0f))
                            .roundToInt()
                        val posY = (heightPx * node.centerY - markerSizePx / 2f)
                            .coerceIn(0f, (heightPx - markerSizePx).coerceAtLeast(0f))
                            .roundToInt()

                        val pulseTransition = rememberInfiniteTransition(label = "nodePulse_${node.id}")
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                        val nodeBorderColor = when {
                            !node.canEnter -> HubRed
                            node.completed -> HubGreen
                            isSelected -> HubCyan
                            else -> Color.White.copy(alpha = 0.60f)
                        }

                        Surface(
                            modifier = Modifier
                                .size(markerSize)
                                .offset { IntOffset(posX, posY) }
                                .scale(if (isSelected) pulseScale else 1f)
                                .clickable {
                                    selectedNodeId = node.id
                                },
                            shape = CircleShape,
                            color = Color(0xFF090E17).copy(alpha = 0.88f),
                            border = BorderStroke(if (isSelected) 2.dp else 1.2.dp, nodeBorderColor),
                            shadowElevation = if (isSelected) 14.dp else 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val iconPainter = rememberDesktopAssetPainter(node.iconPath, services.assetProvider)
                                Image(
                                    painter = iconPainter,
                                    contentDescription = node.title,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                if (!node.canEnter) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.50f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Locked",
                                            tint = HubRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else if (node.completed) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Cleared",
                                        tint = HubGreen,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT PANEL: Selected Destination Details & Travel Action
            Surface(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF080D18).copy(alpha = 0.90f),
                border = BorderStroke(1.2.dp, HubCyan.copy(alpha = 0.35f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "DESTINATION DOSSIER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            ),
                            color = HubCyan
                        )

                        if (selectedNode != null) {
                            Text(
                                text = selectedNode.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedNode.canEnter) HubGreen.copy(alpha = 0.12f) else HubRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (selectedNode.canEnter) HubGreen else HubRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedNode.canEnter) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = if (selectedNode.canEnter) HubGreen else HubRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (selectedNode.canEnter) "CLEARANCE GRANTED" else "ACCESS RESTRICTED",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (selectedNode.canEnter) HubGreen else HubRed
                                    )
                                }
                            }

                            Text(
                                text = if (selectedNode.canEnter) {
                                    selectedNode.description ?: activeHub.description ?: "Local exploration sector."
                                } else {
                                    selectedNode.lockedPreview ?: "Requires prerequisite story milestones or security clearance."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        } else {
                            Text(
                                text = "Select a node on the map to view destination telemetry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }

                    // Travel Execution Button
                    Button(
                        onClick = {
                            if (selectedNode != null && selectedNode.canEnter) {
                                val targetRoom = selectedNode.rooms.firstOrNull() ?: selectedNode.id
                                services.sessionStore.visitNode(selectedNode.id)
                                services.sessionStore.setRoom(targetRoom)
                                onEnterRoom(targetRoom)
                            }
                        },
                        enabled = selectedNode?.canEnter == true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HubCyan,
                            disabledContainerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = Color.Black,
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Text(
                                text = if (selectedNode?.canEnter == true) "ENTER DESTINATION" else "LOCKED",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
