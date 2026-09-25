package com.example.starborn.feature.hub.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.data.local.UserSettings
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.hub.viewmodel.HubLockedPrompt
import com.example.starborn.feature.hub.viewmodel.HubNodeUi
import com.example.starborn.feature.hub.viewmodel.HubQuestUi
import com.example.starborn.feature.hub.viewmodel.HubUiState
import com.example.starborn.feature.hub.viewmodel.HubViewModel
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        modifier = modifier
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
    modifier: Modifier = Modifier
) {
    val backgroundPainter = rememberHubBackgroundPainter(uiState.backgroundImage)
    var menuVisible by remember { mutableStateOf(false) }
    val panelReserve = (164f * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)).dp
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
            .background(Color.Black)
    ) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (!uiState.isLoading) {
            HubMapScene(
                hubId = uiState.hub?.id,
                background = backgroundPainter,
                nodes = uiState.nodes,
                selectedId = uiState.selectedNodeId,
                trackedQuest = uiState.trackedQuest,
                onSelect = onNodeFocused,
                onEnter = onEnterSelectedNode,
                bottomReserve = panelReserve + 24.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        HubAtmosphereCanvas(
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.58f)
                        )
                    )
                )
        )

        HubHeader(
            title = uiState.hub?.title ?: "Region",
            subtitle = uiState.hub?.description ?: "Select a destination to continue your journey.",
            trackedQuest = uiState.trackedQuest,
            statusMessage = uiState.statusMessage,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 18.dp, end = 88.dp)
        )

        HubMenuButton(
            onClick = { menuVisible = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 20.dp, end = 18.dp)
        )

        val astraAccess = uiState.nodes.firstOrNull { it.id == "astra_access" }
        if (astraAccess != null && HubMapLayouts.all[uiState.hub?.id]?.astraDock == null) {
            TextButton(onClick = { onEnterSelectedNode(astraAccess) },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
                    .padding(top = 72.dp, end = 8.dp).width(80.dp)
                    .semantics { contentDescription = "Enter The Astra" }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    rememberHubNodePainter("images/nodes/astra_ship_map_v2.webp")?.let {
                        Image(it, null, modifier = Modifier.size(28.dp))
                    }
                    Text("The Astra", color = Color(0xFFBFEFFF), textAlign = TextAlign.Center,
                        fontSize = 11.sp, lineHeight = 13.sp)
                }
            }
        }

        selectedNode?.let { node ->
            HubDestinationPanel(
                node = node,
                onEnter = { onEnterSelectedNode(node) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .heightIn(max = panelReserve - 36.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        if (menuVisible) {
            HubMenuOverlay(
                settings = settings,
                trackedQuest = uiState.trackedQuest,
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
                onDismiss = onLockedPromptDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HubHeader(
    title: String,
    subtitle: String,
    trackedQuest: HubQuestUi?,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 430.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (trackedQuest == null) Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.74f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        trackedQuest?.let { quest ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xE50A1018),
                border = BorderStroke(1.dp, Color(0xFF7BE4FF).copy(alpha = 0.36f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF7BE4FF).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Assignment,
                        contentDescription = null,
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFCC80)
            )
        }
    }
}

@Composable
private fun HubMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Open menu" },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xE0060B12),
        border = BorderStroke(1.dp, Color(0xFF7BE4FF).copy(alpha = 0.42f)),
        shadowElevation = 8.dp
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = null,
            tint = Color(0xFF7BE4FF),
            modifier = Modifier
                .padding(12.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun HubDestinationPanel(
    node: HubNodeUi,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        !node.canEnter -> Color(0xFFFF8A65)
        node.completed -> Color(0xFF8EF6B3)
        else -> Color(0xFF7BE4FF)
    }
    Surface(
        modifier = modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF2071018),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            val stacked = maxWidth < 260.dp
            val detail = when {
                !node.canEnter -> node.lockReason ?: node.lockedPreview ?: node.description
                    ?: "Find a story reason to go here first."
                else -> node.description
            }
            val description: @Composable (Modifier) -> Unit = { contentModifier ->
                Column(modifier = contentModifier.heightIn(max = 84.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (!detail.isNullOrBlank()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C6D2)
                        )
                    }
                }
            }
            val enterButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                Surface(
                    modifier = buttonModifier,
                    onClick = onEnter,
                    enabled = node.canEnter,
                    shape = RoundedCornerShape(12.dp),
                    color = if (node.canEnter) accent else Color.White.copy(alpha = 0.06f)
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (node.canEnter) Icons.Filled.PlayArrow else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (node.canEnter) Color(0xFF071018) else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (node.canEnter) "Enter" else "Locked",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (node.canEnter) Color(0xFF071018) else Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    description(Modifier.fillMaxWidth())
                    enterButton(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    description(Modifier.weight(1f))
                    enterButton(Modifier)
                }
            }
        }
    }
}

@Composable
private fun HubMenuOverlay(
    settings: SettingsUiState,
    trackedQuest: HubQuestUi?,
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
            shape = RoundedCornerShape(12.dp),
            color = Color(0xF2071018),
            border = BorderStroke(1.dp, Color(0xFF7BE4FF).copy(alpha = 0.52f)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF7BE4FF).copy(alpha = 0.10f),
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
                            tint = Color(0xFF7BE4FF)
                        )
                    }
                }
                HubMenuQuestCard(trackedQuest)
                HubMenuSaveButton(onQuickSave = onQuickSave)
                HubMenuSettings(
                    settings = settings,
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
private fun HubMenuQuestCard(trackedQuest: HubQuestUi?) {
    HubMenuSection(title = "Journal", icon = Icons.AutoMirrored.Rounded.Assignment) {
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
private fun HubMenuSaveButton(onQuickSave: () -> Unit) {
    HubMenuSection(title = "Save", icon = Icons.Rounded.Save) {
        Surface(
            onClick = onQuickSave,
            shape = RoundedCornerShape(9.dp),
            color = Color(0xFF10202C),
            border = BorderStroke(1.dp, Color(0xFF7BE4FF).copy(alpha = 0.36f))
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
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onReturnToTitle: (() -> Unit)? = null
) {
    HubMenuSection(title = "Settings", icon = Icons.Rounded.Settings) {
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xB308111A),
        border = BorderStroke(1.dp, Color(0xFF7BE4FF).copy(alpha = 0.24f))
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
                    tint = Color(0xFF7BE4FF),
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF7BE4FF)
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .semantics { contentDescription = "Locked Location. Tap to continue" }
                .clickable(onClick = onDismiss),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.44f),
                        Color.White.copy(alpha = 0.10f),
                        accent.copy(alpha = 0.24f)
                    )
                )
            ),
            color = Color(0xFF060B14).copy(alpha = 0.97f),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accent.copy(alpha = 0.13f),
                                Color.Transparent
                            ),
                            radius = 460f
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = 0f),
                                    accent.copy(alpha = 0.65f),
                                    accent.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = prompt.message,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tap to continue",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = accent.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.End)
                )
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = 0f),
                                    accent.copy(alpha = 0.55f),
                                    accent.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }
        }
    }
}


internal fun nodeMatchesQuest(node: HubNodeUi, quest: HubQuestUi): Boolean {
    val haystack = listOfNotNull(quest.objective, quest.stageTitle, quest.title)
        .joinToString(" ")
        .lowercase()
    if (haystack.isBlank()) return false
    val titleTokens = node.title
        .lowercase()
        .replace("'", "")
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 4 }
    if (titleTokens.any { token -> haystack.contains(token) }) return true
    val idTokens = node.id
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 4 }
    return idTokens.any { token -> haystack.contains(token) }
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
private fun HubAtmosphereCanvas(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hub_motes")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hub_mote_time"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val particleCount = 18
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

            drawCircle(
                color = Color(0xFF7BE4FF).copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
