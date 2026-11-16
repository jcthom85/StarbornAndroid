package com.example.starborn.feature.hub.ui

import com.example.starborn.feature.hub.presentation.HubMapLayouts
import com.example.starborn.feature.hub.presentation.nodeMatchesQuest

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.data.local.UserSettings
import com.example.starborn.feature.combat.ui.components.CommandPaletteMarkers
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.hub.viewmodel.HubLockedPrompt
import com.example.starborn.feature.hub.viewmodel.HubNodeUi
import com.example.starborn.feature.hub.viewmodel.HubQuestUi
import com.example.starborn.feature.hub.viewmodel.HubUiState
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    userSettings: UserSettings,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onEnterNode: (HubNodeUi) -> Unit,
    onReturnToTitle: () -> Unit = {},
    modifier: Modifier = Modifier,
    saveFeedback: String? = null,
    fieldMenuContent: (@Composable (MenuTab?, String?, () -> Unit) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(uiState.newlyUnlockedNodeId) {
        if (uiState.newlyUnlockedNodeId == null) return@LaunchedEffect
        onPlayAudio("sfx_hub_node_select")
    }
    val settings = remember(userSettings) {
        SettingsUiState(
            musicVolume = userSettings.musicVolume,
            sfxVolume = userSettings.sfxVolume,
            voiceVolume = userSettings.voiceVolume,
            vignetteEnabled = userSettings.vignetteEnabled,
            tutorialsEnabled = userSettings.tutorialsEnabled
        )
    }

    HubScreenContent(
        uiState = uiState,
        settings = settings,
        onNodeFocused = { node ->
            onPlayAudio("sfx_hub_node_select")
            viewModel.selectNode(node.id)
        },
        onEnterSelectedNode = { node ->
            onPlayAudio("sfx_room_transition")
            viewModel.enterNode(node.id, onEnterNode)
        },
        onLockedPromptDismiss = viewModel::dismissLockedPrompt,
        onMusicVolumeChange = onMusicVolumeChange,
        onSfxVolumeChange = onSfxVolumeChange,
        onVoiceVolumeChange = onVoiceVolumeChange,
        onToggleTutorials = onToggleTutorials,
        onToggleVignette = onToggleVignette,
        onQuickSave = onQuickSave,
        onPlayAudio = onPlayAudio,
        onReturnToTitle = onReturnToTitle,
        modifier = modifier,
        saveFeedback = saveFeedback,
        fieldMenuContent = fieldMenuContent
    )
}

@Composable
internal fun HubScreenContent(
    uiState: HubUiState,
    settings: SettingsUiState,
    onNodeFocused: (HubNodeUi) -> Unit,
    onEnterSelectedNode: (HubNodeUi) -> Unit,
    onLockedPromptDismiss: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onReturnToTitle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    saveFeedback: String? = null,
    fieldMenuContent: (@Composable (MenuTab?, String?, () -> Unit) -> Unit)? = null
) {
    val visualTheme = remember(uiState.hub?.id, uiState.hub?.worldId) {
        hubVisualTheme(uiState.hub?.id, uiState.hub?.worldId)
    }
    val backgroundPainter = rememberHubBackgroundPainter(uiState.backgroundImage)
    var menuVisible by remember { mutableStateOf(false) }
    var menuTab by remember { mutableStateOf<MenuTab?>(null) }
    var menuQuest by remember { mutableStateOf<String?>(null) }
    fun openMenu(tab: MenuTab? = null, questId: String? = null) {
        menuTab = tab; menuQuest = questId; menuVisible = true
    }
    val selectedNode = remember(uiState.nodes, uiState.selectedNodeId) {
        uiState.nodes.firstOrNull { it.id == uiState.selectedNodeId } ?: uiState.nodes.firstOrNull()
    }

    BackHandler {
        when {
            menuVisible -> menuVisible = false
            uiState.lockedPrompt != null -> onLockedPromptDismiss()
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(visualTheme.backdropColor)
    ) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            visualTheme.backdropColor.copy(alpha = 0.72f),
                            visualTheme.backdropColor.copy(alpha = 0.56f),
                            visualTheme.backdropColor.copy(alpha = 0.78f)
                        )
                    )
                )
        )
        HubAtmosphereCanvas(
            primaryColor = visualTheme.motePrimary,
            secondaryColor = visualTheme.moteSecondary,
            modifier = Modifier.fillMaxSize()
        )

        // Header, map and destination controls occupy separate layout space.
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub-header")
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = visualTheme.worldLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = visualTheme.accent.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = uiState.hub?.title ?: "Region",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HubQuickSaveButton(
                            onClick = onQuickSave,
                            visualTheme = visualTheme
                        )
                        HubMenuButton(
                            onClick = { openMenu() },
                            visualTheme = visualTheme
                        )
                    }
                }
                HubHeader(
                    subtitle = uiState.hub?.description.orEmpty(),
                    trackedQuest = uiState.trackedQuest,
                    statusMessage = saveFeedback ?: uiState.statusMessage,
                    visualTheme = visualTheme,
                    onQuestClick = { uiState.trackedQuest?.let { openMenu(MenuTab.JOURNAL, it.id) } },
                    modifier = Modifier.fillMaxWidth()
                )
                val astraAccess = uiState.nodes.firstOrNull { it.id == "astra_access" }
                if (astraAccess != null && HubMapLayouts.all[uiState.hub?.id]?.astraDock == null) {
                    Surface(
                        onClick = { onEnterSelectedNode(astraAccess) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xE60B1520),
                        border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.48f)),
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = 44.dp)
                            .semantics { contentDescription = "Enter The Astra" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rememberHubNodePainter("images/nodes/astra_ship_map_v2.webp")?.let { painter ->
                                Image(painter, null, modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text = "The Astra",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(visualTheme.backdropColor)
                    .border(
                        1.1.dp,
                        visualTheme.border.copy(alpha = 0.34f),
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("hub-map-viewport")
            ) {
                if (!uiState.isLoading) {
                    HubMapScene(
                        hubId = uiState.hub?.id,
                        background = backgroundPainter,
                        nodes = uiState.nodes,
                        selectedId = uiState.selectedNodeId,
                        trackedQuest = uiState.trackedQuest,
                        newlyUnlockedNodeId = uiState.newlyUnlockedNodeId,
                        onSelect = onNodeFocused,
                        onEnter = onEnterSelectedNode,
                        visualTheme = visualTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HubCornerBrackets(color = visualTheme.accent)
            }

            selectedNode?.let { node ->
                val isTrackedObjective = uiState.trackedQuest?.let { nodeMatchesQuest(node, it) } == true
                HubDestinationPanel(
                    node = node,
                    isTrackedObjective = isTrackedObjective,
                    visualTheme = visualTheme,
                    onEnter = { onEnterSelectedNode(node) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (menuVisible && fieldMenuContent != null) {
            fieldMenuContent(menuTab, menuQuest) { menuVisible = false }
        } else if (menuVisible) {
            HubMenuOverlay(
                settings = settings,
                trackedQuest = uiState.trackedQuest,
                visualTheme = visualTheme,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onVoiceVolumeChange = onVoiceVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette,
                onQuickSave = onQuickSave,
                onReturnToTitle = onReturnToTitle,
                onDismiss = { menuVisible = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        uiState.lockedPrompt?.let { prompt ->
            HubLockedPromptOverlay(
                prompt = prompt,
                visualTheme = visualTheme,
                onDismiss = onLockedPromptDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HubHeader(
    subtitle: String,
    trackedQuest: HubQuestUi?,
    statusMessage: String?,
    visualTheme: HubVisualTheme,
    onQuestClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = modifier.widthIn(max = 460.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (subtitle.isNotBlank() && (trackedQuest == null || fontScale < 1.25f)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8CAD7),
                maxLines = if (trackedQuest == null) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trackedQuest?.let { quest ->
            Surface(
                onClick = onQuestClick,
                shape = RoundedCornerShape(10.dp),
                color = visualTheme.panelColor.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.40f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFC857).copy(alpha = 0.14f),
                                    visualTheme.accent.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Assignment,
                        contentDescription = null,
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = quest.objective ?: quest.stageTitle ?: "Check the journal for the next step.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8D7E2),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = visualTheme.accent.copy(alpha = 0.72f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        statusMessage?.let { message ->
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xEC111924),
                border = BorderStroke(1.dp, visualTheme.secondaryAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = visualTheme.secondaryAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFFFE0A6),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HubQuickSaveButton(
    onClick: () -> Unit,
    visualTheme: HubVisualTheme,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(width = 46.dp, height = 44.dp)
            .semantics { contentDescription = "Quick save" },
        shape = RoundedCornerShape(12.dp),
        color = visualTheme.panelColor.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.42f)),
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
                tint = visualTheme.secondaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HubMenuButton(
    onClick: () -> Unit,
    visualTheme: HubVisualTheme,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .semantics { contentDescription = "Open menu" },
        shape = RoundedCornerShape(12.dp),
        color = visualTheme.panelColor.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.42f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = visualTheme.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Menu",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun HubDestinationPanel(
    node: HubNodeUi,
    isTrackedObjective: Boolean,
    visualTheme: HubVisualTheme,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        !node.canEnter -> Color(0xFFFFB394)
        isTrackedObjective -> Color(0xFFFFD477)
        else -> visualTheme.accent
    }
    val detail = if (!node.canEnter) node.lockReason ?: node.lockedPreview ?: node.description else node.description
    var detailsOpen by remember(node.id) { mutableStateOf(false) }
    val status = when {
        !node.canEnter -> "Locked"
        node.completed -> "Completed"
        node.visited -> "Visited"
        else -> "Available"
    }
    val statusColor = when {
        !node.canEnter -> Color(0xFFFFB394)
        node.completed -> Color(0xFF9BDEC0)
        else -> visualTheme.accent
    }
    val statusIcon = when {
        !node.canEnter -> Icons.Filled.Lock
        node.completed -> Icons.Filled.CheckCircle
        else -> Icons.Filled.Place
    }
    val thumbnailPath = if (node.id == "astra_access") {
        "images/nodes/astra_ship_map_v2.webp"
    } else {
        node.iconPath
    }
    val thumbnailPainter = rememberHubNodePainter(thumbnailPath)

    Box(
        modifier = modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth()
            .testTag("hub-destination-card")
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = visualTheme.panelColor.copy(alpha = 0.96f),
            border = BorderStroke(1.25.dp, accent.copy(alpha = 0.56f)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.11f), Color.Transparent)
                        )
                    )
                    .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color(0xFF0E1824),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (thumbnailPainter != null) {
                                Image(
                                    painter = thumbnailPainter,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    alpha = if (node.canEnter) 1f else 0.55f,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = node.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 17.sp,
                                lineHeight = 20.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HubBadgePill(
                                label = status,
                                icon = statusIcon,
                                tint = statusColor
                            )
                            if (isTrackedObjective) {
                                HubBadgePill(
                                    label = "Quest",
                                    icon = Icons.Filled.Star,
                                    tint = Color(0xFFFFD477)
                                )
                            }
                        }
                    }
                    if (!detail.isNullOrBlank()) {
                        TextButton(
                            onClick = { detailsOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFF131F2C),
                                contentColor = Color(0xFFE1EEF8)
                            ),
                            border = BorderStroke(1.dp, visualTheme.border.copy(alpha = 0.40f)),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = visualTheme.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                val buttonContainer = when {
                    !node.canEnter -> Color(0xFF141922)
                    isTrackedObjective -> Color(0xFF202213)
                    else -> Color(0xFF132636)
                }
                val buttonBorder = when {
                    !node.canEnter -> Color(0xFFFFB394).copy(alpha = 0.36f)
                    isTrackedObjective -> Color(0xFFFFD477).copy(alpha = 0.88f)
                    else -> visualTheme.accent.copy(alpha = 0.84f)
                }
                Button(
                    onClick = onEnter,
                    enabled = node.canEnter,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.25.dp, buttonBorder),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainer,
                        contentColor = Color.White,
                        disabledContainerColor = buttonContainer,
                        disabledContentColor = Color(0xFFE0B4A6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("hub-enter-destination")
                ) {
                    Icon(
                        imageVector = if (!node.canEnter) Icons.Filled.Lock else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (!node.canEnter) Color(0xFFFFB394) else accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!node.canEnter) "Locked" else if (node.id == "deep_mine" && !node.visited) "Descend" else "Enter",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        CommandPaletteMarkers(
            color = accent,
            modifier = Modifier.matchParentSize()
        )
    }

    if (detailsOpen) {
        HubDestinationDetailsDialog(
            node = node,
            status = status,
            statusIcon = statusIcon,
            accent = accent,
            visualTheme = visualTheme,
            thumbnailPainter = thumbnailPainter,
            detail = detail.orEmpty(),
            onDismiss = { detailsOpen = false }
        )
    }
}

@Composable
private fun HubBadgePill(
    label: String,
    icon: ImageVector,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HubDestinationDetailsDialog(
    node: HubNodeUi,
    status: String,
    statusIcon: ImageVector,
    accent: Color,
    visualTheme: HubVisualTheme,
    thumbnailPainter: Painter?,
    detail: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = visualTheme.panelColor.copy(alpha = 0.97f),
                border = BorderStroke(1.25.dp, visualTheme.border.copy(alpha = 0.72f)),
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                            )
                        )
                        .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1824),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (thumbnailPainter != null) {
                                    Image(
                                        painter = thumbnailPainter,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${status.uppercase(Locale.getDefault())} · DESTINATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp
                                ),
                                color = accent
                            )
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.80f),
                                        visualTheme.border.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = Color(0xFFD5E2EC)
                        )
                        val lockReason = node.lockReason
                        if (!node.canEnter && !lockReason.isNullOrBlank() && detail != lockReason) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFB394).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFFFB394).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = lockReason,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFFFCBB5),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2534),
                            border = BorderStroke(1.dp, visualTheme.border.copy(alpha = 0.58f)),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = visualTheme.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
            CommandPaletteMarkers(
                color = accent,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun HubMenuOverlay(
    settings: SettingsUiState,
    trackedQuest: HubQuestUi?,
    visualTheme: HubVisualTheme,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .clickable(onClick = {}),
            shape = RoundedCornerShape(16.dp),
            color = visualTheme.panelColor.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.52f)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                visualTheme.accent.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close menu",
                            tint = visualTheme.accent
                        )
                    }
                }
                HubMenuQuestCard(trackedQuest, visualTheme)
                HubMenuSaveButton(onQuickSave = onQuickSave, visualTheme = visualTheme)
                HubMenuSettings(
                    settings = settings,
                    visualTheme = visualTheme,
                    onMusicVolumeChange = onMusicVolumeChange,
                    onSfxVolumeChange = onSfxVolumeChange,
                    onVoiceVolumeChange = onVoiceVolumeChange,
                    onToggleTutorials = onToggleTutorials,
                    onToggleVignette = onToggleVignette,
                    onReturnToTitle = onReturnToTitle
                )
            }
        }
    }
}

@Composable
private fun HubMenuQuestCard(trackedQuest: HubQuestUi?, visualTheme: HubVisualTheme) {
    HubMenuSection(title = "Journal", icon = Icons.AutoMirrored.Rounded.Assignment, visualTheme = visualTheme) {
        Text(
            text = trackedQuest?.title ?: "No tracked quest",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = trackedQuest?.objective ?: "New objectives will appear here when Nova has a lead.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun HubMenuSaveButton(onQuickSave: () -> Unit, visualTheme: HubVisualTheme) {
    HubMenuSection(title = "Save", icon = Icons.Rounded.Save, visualTheme = visualTheme) {
        Surface(
            onClick = onQuickSave,
            shape = RoundedCornerShape(9.dp),
            color = Color(0xFF10202C),
            border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.36f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    tint = Color(0xFFFFC857),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Quick Save",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Store the current run immediately.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HubMenuSettings(
    settings: SettingsUiState,
    visualTheme: HubVisualTheme,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onReturnToTitle: (() -> Unit)? = null
) {
    HubMenuSection(title = "Settings", icon = Icons.Rounded.Settings, visualTheme = visualTheme) {
        HubSliderRow("Music", settings.musicVolume, onMusicVolumeChange)
        HubSliderRow("Effects", settings.sfxVolume, onSfxVolumeChange)
        HubSliderRow("Voice", settings.voiceVolume, onVoiceVolumeChange)
        HubSwitchRow("Room Vignette", settings.vignetteEnabled, onToggleVignette)
        HubSwitchRow("Tutorials", settings.tutorialsEnabled, onToggleTutorials)
        if (onReturnToTitle != null) {
            Surface(
                onClick = onReturnToTitle,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF281014),
                border = BorderStroke(1.dp, Color(0xFFFF5D4F).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Return to Title",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF8E83)
                    )
                }
            }
        }
    }
}

@Composable
private fun HubMenuSection(
    title: String,
    icon: ImageVector,
    visualTheme: HubVisualTheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xB308111A),
        border = BorderStroke(1.dp, visualTheme.accent.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = visualTheme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
            content()
        }
    }
}

@Composable
private fun HubSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "$label ${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.78f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun HubSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.84f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun HubLockedPromptOverlay(
    prompt: HubLockedPrompt,
    visualTheme: HubVisualTheme,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFFFFB394)
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.44f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Locked Location. Tap to continue" }
                    .clickable(onClick = onDismiss),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.54f)),
                color = visualTheme.panelColor.copy(alpha = 0.97f),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubBadgePill(
                        label = "Route Locked",
                        icon = Icons.Filled.Lock,
                        tint = accent
                    )
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = prompt.message,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                        color = Color(0xFFD6E2EC),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Tap to continue",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = visualTheme.accent.copy(alpha = 0.68f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            CommandPaletteMarkers(
                color = accent,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
internal fun rememberHubNodePainter(iconPath: String?): Painter? {
    if (iconPath.isNullOrBlank()) return null
    val context = LocalContext.current
    return remember(iconPath) {
        runCatching {
            context.assets.open(iconPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { BitmapPainter(it) }
            }
        }.getOrNull()
    }
}

@Composable
private fun rememberHubBackgroundPainter(imagePath: String?): Painter {
    val context = LocalContext.current
    val fallback = ColorPainter(Color.Black)
    val painter = remember(imagePath) {
        if (imagePath.isNullOrBlank()) {
            null
        } else {
            runCatching {
                context.assets.open(imagePath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { BitmapPainter(it) }
                }
            }.getOrNull()
        }
    }
    return painter ?: fallback
}

@Composable
private fun HubAtmosphereCanvas(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "hub_motes")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hub_mote_time"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val particleCount = 20
        for (i in 0 until particleCount) {
            val seedX = ((i * 137.5f) % 100f) / 100f
            val seedY = ((i * 243.1f) % 100f) / 100f
            val speed = 0.4f + ((i % 5) * 0.15f)
            val currentY = (seedY - (time * speed)) % 1f
            val normalizedY = if (currentY < 0f) currentY + 1f else currentY
            val sway = kotlin.math.sin((time * 6.283f * 2f) + (i * 1.5f)) * 14f

            val x = (seedX * width) + sway
            val y = normalizedY * height
            val alpha = (kotlin.math.sin(normalizedY * 3.14159f) * 0.35f).coerceIn(0f, 0.4f)
            val radius = 1.2f + (i % 3) * 0.8f
            val tint = if (i % 4 == 0) secondaryColor else primaryColor

            drawCircle(
                color = tint.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
