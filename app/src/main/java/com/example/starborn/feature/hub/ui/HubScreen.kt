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
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onEnterNode: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = remember(userSettings) {
        SettingsUiState(
            musicVolume = userSettings.musicVolume,
            sfxVolume = userSettings.sfxVolume,
            vignetteEnabled = userSettings.vignetteEnabled,
            tutorialsEnabled = userSettings.tutorialsEnabled
        )
    }

    HubScreenContent(
        uiState = uiState,
        settings = settings,
        onNodeFocused = { node -> viewModel.selectNode(node.id) },
        onEnterSelectedNode = { node -> viewModel.enterNode(node.id, onEnterNode) },
        onLockedPromptDismiss = viewModel::dismissLockedPrompt,
        onMusicVolumeChange = onMusicVolumeChange,
        onSfxVolumeChange = onSfxVolumeChange,
        onToggleTutorials = onToggleTutorials,
        onToggleVignette = onToggleVignette,
        onQuickSave = onQuickSave,
        modifier = modifier
    )
}

@Composable
private fun HubScreenContent(
    uiState: HubUiState,
    settings: SettingsUiState,
    onNodeFocused: (HubNodeUi) -> Unit,
    onEnterSelectedNode: (HubNodeUi) -> Unit,
    onLockedPromptDismiss: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundPainter = rememberHubBackgroundPainter(uiState.backgroundImage)
    var menuVisible by remember { mutableStateOf(false) }
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

        if (!uiState.isLoading) {
            HubNodeLayer(
                nodes = uiState.nodes,
                selectedId = uiState.selectedNodeId,
                trackedQuest = uiState.trackedQuest,
                onNodeSelected = onNodeFocused,
                onNodeEnterRequested = onEnterSelectedNode,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(start = 14.dp, top = 146.dp, end = 14.dp, bottom = 142.dp)
            )
        }

        selectedNode?.let { node ->
            HubDestinationPanel(
                node = node,
                onEnter = { onEnterSelectedNode(node) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }

        if (menuVisible) {
            HubMenuOverlay(
                settings = settings,
                trackedQuest = uiState.trackedQuest,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette,
                onQuickSave = onQuickSave,
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
        Text(
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
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xF2071018),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.46f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DestinationStatusIcon(node = node, color = accent)
            }
            val detail = when {
                !node.canEnter -> node.lockedPreview ?: node.description ?: "Find a story reason to go here first."
                !node.description.isNullOrBlank() -> node.description
                node.completed -> "You can return here whenever you need to."
                else -> "The map marks this place, but the story around it has not been written yet."
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.76f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                onClick = onEnter,
                enabled = node.canEnter,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(9.dp),
                color = if (node.canEnter) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, accent.copy(alpha = if (node.canEnter) 0.58f else 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (node.canEnter) Icons.Filled.PlayArrow else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (node.canEnter) Color.White else Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (node.canEnter) "Enter" else "Locked",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (node.canEnter) Color.White else Color.White.copy(alpha = 0.42f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationStatusIcon(node: HubNodeUi, color: Color) {
    val icon = when {
        !node.canEnter -> Icons.Filled.Lock
        node.completed -> Icons.Filled.CheckCircle
        else -> Icons.Outlined.RadioButtonUnchecked
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun HubMenuOverlay(
    settings: SettingsUiState,
    trackedQuest: HubQuestUi?,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
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
                    onToggleTutorials = onToggleTutorials,
                    onToggleVignette = onToggleVignette
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
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit
) {
    HubMenuSection(title = "Settings", icon = Icons.Rounded.Settings) {
        HubSliderRow("Music", settings.musicVolume, onMusicVolumeChange)
        HubSliderRow("Effects", settings.sfxVolume, onSfxVolumeChange)
        HubSwitchRow("Room Vignette", settings.vignetteEnabled, onToggleVignette)
        HubSwitchRow("Tutorials", settings.tutorialsEnabled, onToggleTutorials)
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

@Composable
private fun HubNodeLayer(
    nodes: List<HubNodeUi>,
    selectedId: String?,
    trackedQuest: HubQuestUi?,
    onNodeSelected: (HubNodeUi) -> Unit,
    onNodeEnterRequested: (HubNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        nodes.forEach { node ->
            val composition = hubNodeComposition(node)
            val markerWidthPx = with(density) { composition.width.toPx() }
            val markerHeightPx = with(density) { composition.height.toPx() }
            val centerX = widthPx * node.centerX
            val centerY = heightPx * node.centerY
            val offsetX = (centerX - markerWidthPx * composition.anchorX)
                .coerceIn(0f, (widthPx - markerWidthPx).coerceAtLeast(0f))
                .roundToInt()
            val offsetY = (centerY - markerHeightPx * composition.anchorY)
                .coerceIn(0f, (heightPx - markerHeightPx).coerceAtLeast(0f))
                .roundToInt()
            HubNodeMarker(
                node = node,
                selected = selectedId == node.id,
                objective = trackedQuest?.let { nodeMatchesQuest(node, it) } == true,
                modifier = Modifier
                    .width(composition.width)
                    .height(composition.height)
                    .offset { IntOffset(offsetX, offsetY) },
                onClick = { onNodeSelected(node) },
                onDoubleClick = {
                    onNodeSelected(node)
                    if (node.canEnter) onNodeEnterRequested(node)
                }
            )
        }
    }
}

@Composable
private fun HubNodeMarker(
    node: HubNodeUi,
    selected: Boolean,
    objective: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val iconPainter = rememberHubNodePainter(node.iconPath)
    val composition = hubNodeComposition(node)
    val highlight by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "hubNodeHighlight"
    )
    val pulseTransition = rememberInfiniteTransition(label = "hubNodePulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hubNodeScale"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected || objective) pulse else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "hubNodeScaleEase"
    )
    Column(
        modifier = modifier
            .semantics { contentDescription = "Enter ${node.title}" }
            .pointerInput(node.id, node.canEnter) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (selected || objective) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(composition.imageWidth * if (selected) 0.88f else 0.74f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF7BE4FF).copy(alpha = if (selected) 0.24f else 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color(0xFF7BE4FF).copy(alpha = if (selected) 0.58f else 0.34f),
                            CircleShape
                        )
                )
            }
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = if (node.unlocked) 1f else 0.38f,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = composition.imageWidth)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(composition.imageWidth * 0.62f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF24536A), Color(0xFF0B1820))
                            )
                        )
                        .border(1.5.dp, TitleMarkerBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (node.unlocked) node.title.take(1).uppercase() else "?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            NodeBadge(
                node = node,
                objective = objective,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        x = composition.imageWidth * 0.28f,
                        y = composition.imageWidth * 0.14f
                    )
            )
        }

        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xDC080C11),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = if (selected) 0.34f else 0.16f)
            ),
            shadowElevation = 4.dp
        ) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 132.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private data class HubNodeComposition(
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val imageWidth: androidx.compose.ui.unit.Dp = width,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
)

private fun hubNodeComposition(node: HubNodeUi): HubNodeComposition = when (node.id) {
    "pit" -> HubNodeComposition(width = 132.dp, height = 132.dp, imageWidth = 116.dp, anchorX = 0.48f, anchorY = 0.68f)
    "workshop" -> HubNodeComposition(width = 166.dp, height = 180.dp, anchorX = 0.5f, anchorY = 0.72f)
    "med_bay" -> HubNodeComposition(width = 132.dp, height = 134.dp, imageWidth = 118.dp, anchorX = 0.5f, anchorY = 0.58f)
    "trade_row" -> HubNodeComposition(width = 132.dp, height = 128.dp, imageWidth = 112.dp, anchorX = 0.5f, anchorY = 0.68f)
    "admin_gate" -> HubNodeComposition(width = 150.dp, height = 126.dp, imageWidth = 110.dp, anchorX = 0.56f, anchorY = 0.62f)
    "admin_concourse" -> HubNodeComposition(width = 142.dp, height = 126.dp, imageWidth = 106.dp, anchorX = 0.5f, anchorY = 0.62f)
    "server_room" -> HubNodeComposition(width = 136.dp, height = 132.dp, imageWidth = 108.dp, anchorX = 0.5f, anchorY = 0.68f)
    "deep_mine" -> HubNodeComposition(width = 132.dp, height = 138.dp, imageWidth = 114.dp, anchorX = 0.5f, anchorY = 0.66f)
    "echo_chamber" -> HubNodeComposition(width = 142.dp, height = 130.dp, imageWidth = 110.dp, anchorX = 0.5f, anchorY = 0.62f)
    "launch_bay" -> HubNodeComposition(width = 132.dp, height = 136.dp, imageWidth = 114.dp, anchorX = 0.5f, anchorY = 0.62f)
    else -> {
        // Asset metadata uses source-pixel hints, not density-independent screen sizes.
        val size = (node.sizeHint * 0.36f).coerceIn(72f, 96f).dp
        HubNodeComposition(width = size, height = size + 28.dp, imageWidth = size, anchorY = 0.62f)
    }
}

@Composable
private fun NodeBadge(
    node: HubNodeUi,
    objective: Boolean,
    modifier: Modifier = Modifier
) {
    val icon = when {
        !node.canEnter -> Icons.Filled.Lock
        node.completed -> Icons.Filled.CheckCircle
        objective -> Icons.AutoMirrored.Rounded.Assignment
        node.canEnter && !node.visited -> Icons.Filled.Star
        else -> null
    } ?: return
    val color = when {
        !node.canEnter -> Color(0xFFFF8A65)
        node.completed -> Color(0xFF8EF6B3)
        node.canEnter && !node.visited -> Color(0xFFFFD166)
        else -> Color(0xFFFFC857)
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color(0xE5060B12),
        border = BorderStroke(1.dp, color.copy(alpha = 0.48f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(4.dp)
                .size(14.dp)
        )
    }
}

private fun nodeMatchesQuest(node: HubNodeUi, quest: HubQuestUi): Boolean {
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

private val TitleMarkerBorder = Color(0xFF7BE4FF).copy(alpha = 0.72f)

@Composable
private fun rememberHubNodePainter(iconPath: String?): Painter? {
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

private fun nodeColor(node: HubNodeUi, selected: Boolean, highlight: Float): Color {
    val base = if (node.discovered) Color(0xFF1E5C8E) else Color(0xFF455A64)
    val selectedTint = Color(0xFF80CBC4)
    return lerpColor(base, selectedTint, highlight)
}

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped
    )
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
