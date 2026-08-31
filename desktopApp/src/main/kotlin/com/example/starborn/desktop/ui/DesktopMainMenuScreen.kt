package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import kotlinx.coroutines.launch

private val TitleGold = Color(0xFFFFC857)
private val TitleAmber = Color(0xFFFF9F2E)
private val TitleCyan = Color(0xFF63E6FF)
private val TitlePanel = Color(0xFF061018)
private val TitleText = Color(0xFFF7FBFF)
private val TitleMutedText = Color(0xFFD7EAF4)

@Composable
fun DesktopMainMenuScreen(
    services: DesktopAppServices,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDebugScenarios by remember { mutableStateOf(false) }
    var showLoadGame by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }

    val userSettings by services.userSettingsStore.settings.collectAsState(
        initial = com.example.starborn.data.local.UserSettings()
    )

    // Trigger Title Music on launch
    LaunchedEffect(Unit) {
        val cmds = services.audioRouter.commandsForRoom(hubId = "main_menu", roomId = "main_menu")
        services.audioDriver.executeAll(cmds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.N, Key.One -> {
                            onStartGame()
                            true
                        }
                        Key.L, Key.Two -> {
                            showLoadGame = true
                            true
                        }
                        Key.D -> {
                            showDebugScenarios = true
                            true
                        }
                        Key.S, Key.Three -> {
                            showSettingsDialog = true
                            true
                        }
                        Key.Escape, Key.Four -> {
                            onQuit()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Authentic Starborn Title Background Image with slow cinematic drift
        val bgTransition = rememberInfiniteTransition(label = "title_bg_motion")
        val bgScale by bgTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "title_bg_scale"
        )
        val bgPanY by bgTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "title_bg_pan"
        )
        val ambientBloom by bgTransition.animateFloat(
            initialValue = 0.04f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "title_ambient_bloom"
        )

        val bgPainter = rememberDesktopAssetPainter("title_background_starborn", services.assetProvider)
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                    translationY = bgPanY
                },
            contentScale = ContentScale.Crop
        )

        // 2. Soft celestial bloom overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TitleCyan.copy(alpha = ambientBloom),
                            TitleGold.copy(alpha = ambientBloom * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(300f, 200f),
                        radius = 1200f
                    )
                )
        )

        // 3. Vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // 4. Main 16:9 Landscape Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero: Authentic Animated Starborn Logo
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                AuthenticTitleLogo(services)

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TitlePanel.copy(alpha = 0.85f))
                        .border(BorderStroke(1.dp, TitleCyan.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TitleCyan)
                            )
                            Text(
                                text = "DEEP-SPACE TACTICAL RPG",
                                color = TitleCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Explore the uncharted perimeter sectors. Assemble your crew, master real-time turn manipulation, craft high-tier technologies, and uncover ancient cosmic anomalies.",
                            color = TitleMutedText,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Right Action Menu: Authentic Starborn Title Buttons
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AuthenticTitleButton(
                        text = "NEW ODYSSEY [N]",
                        primary = true,
                        onClick = onStartGame
                    )

                    AuthenticTitleButton(
                        text = "LOAD GAME [L]",
                        primary = false,
                        onClick = { showLoadGame = true }
                    )

                    AuthenticTitleButton(
                        text = "DEBUG SCENARIOS [D]",
                        primary = false,
                        onClick = { showDebugScenarios = true }
                    )

                    AuthenticTitleButton(
                        text = "SETTINGS [S]",
                        primary = false,
                        onClick = { showSettingsDialog = true }
                    )

                    AuthenticTitleButton(
                        text = "EXIT TO DESKTOP [ESC]",
                        primary = false,
                        onClick = onQuit
                    )
                }
            }
        }

        // Bottom Controls HUD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "STARBORN v1.0.0 (Widescreen Edition)  •  [F11] Fullscreen  •  [N] New Game  •  [L] Load  •  [D] Scenarios  •  [S] Settings",
                color = TitleMutedText.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Dialogs
        if (showDebugScenarios) {
            DesktopDebugScenarioDialog(
                onLaunch = { scenario ->
                    showDebugScenarios = false
                    services.sessionStore.setRoom(scenario.id)
                    onStartGame()
                },
                onDismiss = { showDebugScenarios = false }
            )
        }

        if (showLoadGame) {
            DesktopLoadGameDialog(
                services = services,
                onLoad = {
                    showLoadGame = false
                    onStartGame()
                },
                onDismiss = { showLoadGame = false }
            )
        }

        if (showSettingsDialog) {
            DesktopSettingsDialog(
                services = services,
                userSettings = userSettings,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun AuthenticTitleLogo(services: DesktopAppServices) {
    val logoPainter = rememberDesktopAssetPainter("title_logo_starborn", services.assetProvider)
    val transition = rememberInfiniteTransition(label = "starborn_title_logo")
    val bobOffset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starborn_title_logo_bob"
    )
    val logoScale by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starborn_title_logo_scale"
    )
    val shimmerSweep by transition.animateFloat(
        initialValue = -1.1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6400
                -1.1f at 0
                -1.1f at 900
                2.1f at 5000
                2.1f at 6400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "starborn_title_logo_sweep"
    )

    Image(
        painter = logoPainter,
        contentDescription = "Starborn",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(180.dp)
            .graphicsLayer {
                translationY = bobOffset
                scaleX = logoScale
                scaleY = logoScale
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val x = size.width * shimmerSweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            TitleCyan.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        start = Offset(x - size.width * 0.28f, 0f),
                        end = Offset(x + size.width * 0.12f, size.height)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
    )
}

@Composable
private fun AuthenticTitleButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(if (primary) 58.dp else 52.dp)
        .graphicsLayer {
            scaleX = buttonScale
            scaleY = buttonScale
        }

    if (primary) {
        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TitleGold,
                contentColor = Color(0xFF1B1608)
            ),
            modifier = buttonModifier
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = 1.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, TitleCyan.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = TitlePanel.copy(alpha = 0.75f),
                contentColor = TitleText
            ),
            modifier = buttonModifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TitleCyan.copy(alpha = 0.12f),
                            TitleAmber.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun DesktopDebugScenarioDialog(
    onLaunch: (DebugScenario) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<DebugScenarioCategory?>(null) }
    val filtered = remember(query, category) {
        DebugScenarioCatalog.scenarios.filter { scenario ->
            (category == null || scenario.category == category) &&
                (query.isBlank() || listOf(scenario.title, scenario.description, scenario.worldLabel)
                    .any { it.contains(query, ignoreCase = true) })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DEBUG SCENARIOS", color = TitleCyan, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.width(600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select any scenario to jump directly to that sector or encounter.", color = TitleMutedText, fontSize = 13.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by world, sector, quest or system") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DebugScenarioCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option.takeUnless { it == category } },
                            label = { Text(option.label, fontSize = 11.sp) }
                        )
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(filtered, key = { it.id }) { scenario ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1424))
                                .border(BorderStroke(1.dp, TitleCyan.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .clickable { onLaunch(scenario) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(scenario.title, fontWeight = FontWeight.Bold, color = TitleText, fontSize = 14.sp)
                                Text(
                                    "${scenario.category.label}  •  ${scenario.worldLabel}",
                                    color = TitleGold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(scenario.description, color = TitleMutedText, fontSize = 12.sp)
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { Text("No matching scenarios.", color = TitleMutedText) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
        containerColor = TitlePanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, TitleCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}

@Composable
private fun DesktopLoadGameDialog(
    services: DesktopAppServices,
    onLoad: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val slots = remember {
        listOf(
            DesktopSaveSlot(1, "Orbital Station Alpha", 1, 50, "Autosave"),
            DesktopSaveSlot(2, "Perimeter Sector 4", 2, 240, "Checkpoint"),
            DesktopSaveSlot(3, "Deep Space Anomaly", 3, 500, "Manual Save")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LOAD SAVED JOURNEY", color = TitleGold, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.width(550.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D1424))
                            .border(BorderStroke(1.dp, TitleCyan.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                            .clickable {
                                onLoad(slot.slotIndex)
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SLOT ${slot.slotIndex}: ${slot.title}",
                                    color = TitleText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Level ${slot.level}  •  ${slot.credits} Credits  •  ${slot.timestamp}",
                                    color = TitleMutedText,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = {
                                    onLoad(slot.slotIndex)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TitleCyan, contentColor = Color.Black)
                            ) {
                                Text("LOAD", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = TitlePanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, TitleGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}

private data class DesktopSaveSlot(
    val slotIndex: Int,
    val title: String,
    val level: Int,
    val credits: Int,
    val timestamp: String
)

@Composable
private fun DesktopSettingsDialog(
    services: DesktopAppServices,
    userSettings: com.example.starborn.data.local.UserSettings,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var musicVol by remember { mutableStateOf(userSettings.musicVolume) }
    var sfxVol by remember { mutableStateOf(userSettings.sfxVolume) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AUDIO & DISPLAY SETTINGS", color = TitleCyan, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.width(480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Music Volume", color = TitleText, fontWeight = FontWeight.SemiBold)
                        Text("${(musicVol * 100).toInt()}%", color = TitleGold)
                    }
                    Slider(
                        value = musicVol,
                        onValueChange = {
                            musicVol = it
                            coroutineScope.launch {
                                services.userSettingsStore.setMusicVolume(it)
                                services.audioDriver.setUserGain(AudioCueType.MUSIC, it)
                            }
                        }
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sound FX Volume", color = TitleText, fontWeight = FontWeight.SemiBold)
                        Text("${(sfxVol * 100).toInt()}%", color = TitleGold)
                    }
                    Slider(
                        value = sfxVol,
                        onValueChange = {
                            sfxVol = it
                            coroutineScope.launch {
                                services.userSettingsStore.setSfxVolume(it)
                                services.audioDriver.setUserGain(AudioCueType.UI, it)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = TitleCyan, contentColor = Color.Black)) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {},
        containerColor = TitlePanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, TitleCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}
