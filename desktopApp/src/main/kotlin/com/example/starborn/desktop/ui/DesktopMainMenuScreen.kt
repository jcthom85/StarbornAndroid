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
import com.example.starborn.data.local.UserSettings
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
    onOpenSettings: () -> Unit = {},
    onQuit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDebugScenarios by remember { mutableStateOf(false) }
    var debugScenarioError by remember { mutableStateOf<String?>(null) }
    var showLoadGame by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var latestSlot by remember { mutableStateOf<Int?>(null) }
    var latestSlotInfo by remember { mutableStateOf<com.example.starborn.desktop.DesktopSaveSlotInfo?>(null) }
    var newGamePlusUnlocked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val saveError by services.saveManager.lastError.collectAsState()
    LaunchedEffect(services) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val meta = (-1..3).mapNotNull { slot -> services.saveManager.getSlotMetadata(slot) }.maxByOrNull { it.timestamp }
            latestSlotInfo = meta
            latestSlot = meta?.slotIndex
            newGamePlusUnlocked = services.completedGameForNewGamePlus() != null
        }
    }

    val userSettings by services.userSettingsStore.settings.collectAsState(
        initial = UserSettings()
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
                        Key.C -> {
                            latestSlot?.let { slot ->
                                if (!loading) coroutineScope.launch {
                                    loading = true
                                    val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { services.loadSlot(slot) }
                                    loading = false
                                    if (loaded) onStartGame()
                                }
                            }
                            true
                        }
                        Key.N, Key.One -> {
                            if (services.hasExistingSave()) {
                                showNewGameConfirm = true
                            } else {
                                services.startNewGame()
                                onStartGame()
                            }
                            true
                        }
                        Key.L, Key.Two -> {
                            showLoadGame = true
                            true
                        }
                        Key.D -> {
                            if (services.isDebugEnabled) {
                                showDebugScenarios = true
                                true
                            } else false
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
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left hero: original logo with room to breathe.
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthenticTitleLogo(services)


            }

            // Right Action Menu: Authentic Starborn Title Buttons
            Column(
                modifier = Modifier
                    .weight(.85f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    latestSlot?.let { slot ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AuthenticTitleButton(
                                text = if (loading) "Loading…" else "Continue",
                                primary = true,
                                onClick = {
                                    if (!loading) coroutineScope.launch {
                                        loading = true
                                        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { services.loadSlot(slot) }
                                        loading = false
                                        if (loaded) onStartGame()
                                    }
                                }
                            )
                            latestSlotInfo?.let { info ->
                                Text(
                                    text = "${info.roomTitle ?: "Sector"} · Lv.${info.playerLevel} · ${info.formattedDate}",
                                    color = TitleCyan.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    if (newGamePlusUnlocked) AuthenticTitleButton(text = "New Game+ · Master Protocol", primary = false, onClick = {
                        if (services.startNewGamePlus()) onStartGame()
                    })
                    saveError?.let { Text(it, color = Color(0xFFFF8A80)) }
                    AuthenticTitleButton(
                        text = "New Game",
                        primary = latestSlot == null,
                        onClick = {
                            if (services.hasExistingSave()) {
                                showNewGameConfirm = true
                            } else {
                                services.startNewGame()
                                onStartGame()
                            }
                        }
                    )

                    AuthenticTitleButton(
                        text = "Load Game",
                        primary = false,
                        onClick = { showLoadGame = true }
                    )

                    if (services.isDebugEnabled) {
                        AuthenticTitleButton(
                            text = "Debug Scenarios",
                            primary = false,
                            onClick = { showDebugScenarios = true }
                        )
                    }

                    AuthenticTitleButton(
                        text = "Settings",
                        primary = false,
                        onClick = { showSettingsDialog = true }
                    )

                    AuthenticTitleButton(
                        text = "Exit to Desktop",
                        primary = false,
                        onClick = onQuit
                    )
                }
            }
        }

        // Dialogs
        if (showNewGameConfirm) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = {
                    Text("Start New Game?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Starting a new game will begin a fresh run and overwrite your current autosave.",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNewGameConfirm = false
                            services.startNewGame()
                            onStartGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TitleGold, contentColor = Color(0xFF1B1608))
                    ) {
                        Text("Begin New Game", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameConfirm = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF141C24),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, TitleCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            )
        }

        if (showDebugScenarios) {
            DesktopDebugScenarioDialog(
                onLaunch = { scenario ->
                    showDebugScenarios = false
                    if (services.startDebugScenario(scenario.id)) onStartGame()
                    else debugScenarioError = "Unable to launch ${scenario.title}: its starting location is unavailable in the current game data."
                },
                onDismiss = { showDebugScenarios = false }
            )
        }

        debugScenarioError?.let { message ->
            AlertDialog(onDismissRequest = { debugScenarioError = null }, title = { Text("Scenario unavailable") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { debugScenarioError = null }) { Text("Close") } })
        }
        if (showLoadGame) {
            DesktopSaveLoadDialog(
                services = services,
                initialMode = SaveDialogMode.LOAD,
                onLoadState = {
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
            .fillMaxWidth(.84f)
            .aspectRatio(1.5f)
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
        .desktopPointerHover()
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
                letterSpacing = 0.5.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, TitleCyan.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = TitlePanel.copy(alpha = 0.70f),
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
                letterSpacing = 0.5.sp
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
private fun DesktopSettingsDialog(
    services: DesktopAppServices,
    userSettings: UserSettings,
    onDismiss: () -> Unit
) {
    var showControls by remember { mutableStateOf(false) }
    if (showControls) {
        DesktopControlsDialog(onDismiss = { showControls = false })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SETTINGS & CONFIGURATION", color = TitleCyan, fontWeight = FontWeight.Black) },
        text = {
            Box(Modifier.width(760.dp).height(500.dp)) {
                DesktopSettingsContent(
                    services = services,
                    userSettings = userSettings,
                    currentRoomTitle = null,
                    onReturnToTitle = null,
                    onOpenControls = { showControls = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.desktopPointerHover(),
                colors = ButtonDefaults.buttonColors(containerColor = TitleCyan, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {},
        containerColor = TitlePanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, TitleCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}
