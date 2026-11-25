package com.example.starborn.desktop.ui

import com.example.starborn.domain.session.SessionSaveRepository

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.DesktopBuildInfo
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicPresentation
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.feature.exploration.viewmodel.helpers.CinematicPlaybackManager
import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val TitleGold = Color(0xFFFFC857)
private val TitleAmber = Color(0xFFFF9F2E)
private val TitleCyan = Color(0xFF63E6FF)
private val TitlePanel = Color(0xFF061018)
private val TitleText = Color(0xFFF7FBFF)
private val TitleMutedText = Color(0xFFD7EAF4)

internal data class DesktopScreeningEntry(
    val worldLabel: String,
    val scene: CinematicScene,
    val durationSeconds: Double,
    val imageCount: Int
)

internal fun buildDesktopScreeningCatalog(services: DesktopAppServices): List<DesktopScreeningEntry> {
    val allIllustrated = services.cinematicService.allScenes()
        .filter { it.presentation == CinematicPresentation.ILLUSTRATED }
        .associateBy { it.id }
    val curatedOrder: List<Pair<String, List<String>>> = listOf(
        "World 1 — Mining Colony" to listOf(
            "intro_prologue",
            "scene_cutter_surge",
            "scene_relic_sync",
            "scene_w1_lift_separation",
            "scene_launch_crash"
        ),
        "World 2 — The Wilds" to listOf(
            "scene_w2_orion_awakening",
            "scene_anchor_drill",
            "scene_w2_astra_departure"
        ),
        "World 3 — The Spire" to listOf(
            "scene_w3_lens_heist",
            "scene_w3_foundry_flight"
        ),
        "World 4 — The Foundry" to listOf(
            "scene_w4_anvil_forged",
            "scene_w4_rylos_spared",
            "scene_w4_outer_shield"
        ),
        "World 5 — The Orbital Ring" to listOf(
            "scene_w5_anchor_stasis",
            "scene_w5_soloist_breach",
            "scene_w5_cross_the_tear"
        ),
        "World 6 — The Source" to listOf(
            "scene_w6_song_fire_reunion",
            "scene_w6_jed_echo_lesson",
            "scene_w6_silence_rises",
            "scene_w6_final_note",
            "scene_w6_epilogue_credits"
        )
    )
    fun CinematicScene.toEntry(worldLabel: String): DesktopScreeningEntry = DesktopScreeningEntry(
        worldLabel = worldLabel,
        scene = this,
        durationSeconds = steps.sumOf { it.durationSeconds ?: 4.0 },
        imageCount = steps.mapNotNull { it.imagePath }.distinct().size
    )
    val entries = mutableListOf<DesktopScreeningEntry>()
    val seen = mutableSetOf<String>()
    for ((worldLabel, ids) in curatedOrder) {
        for (id in ids) {
            val scene = allIllustrated[id] ?: continue
            seen += id
            entries += scene.toEntry(worldLabel)
        }
    }
    allIllustrated.values
        .filter { it.id !in seen && it.id != "new_game_fade_in" }
        .sortedBy { it.id }
        .forEach { scene -> entries += scene.toEntry("Archive & Extras") }
    return entries
}

internal fun buildDesktopScreeningUiState(scene: CinematicScene, stepIndex: Int): CinematicUiState {
    val playback = CinematicPlaybackState(
        scene = scene,
        stepIndex = stepIndex.coerceIn(0, (scene.steps.size - 1).coerceAtLeast(0))
    )
    return CinematicPlaybackManager.toUiState(
        playback = playback,
        resolveEmotePortrait = { speaker, emote ->
            val normalized = speaker.trim().lowercase(Locale.US)
            val cleanEmote = emote?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
            if (cleanEmote != null) "images/characters/emotes/${normalized}_${cleanEmote}.png" else null
        },
        resolvePortraitKey = { speaker ->
            "images/characters/${speaker.trim().lowercase(Locale.US)}_portrait.png"
        }
    )
}

@Composable
fun DesktopMainMenuScreen(
    services: DesktopAppServices,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onQuit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDebugScenarios by remember { mutableStateOf(false) }
    var showScreeningRoom by remember { mutableStateOf(false) }
    var screeningQueue by remember { mutableStateOf<List<CinematicScene>>(emptyList()) }
    var screeningStepIndex by remember { mutableIntStateOf(0) }
    var debugScenarioError by remember { mutableStateOf<String?>(null) }
    var showLoadGame by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var latestSlotInfo by remember(services) {
        mutableStateOf((SessionSaveRepository.ALL_SLOTS).mapNotNull { slot -> services.saveManager.getSlotMetadata(slot) }.maxByOrNull { it.timestamp })
    }
    var latestSlot by remember(services) { mutableStateOf(latestSlotInfo?.slotIndex) }
    var newGamePlusUnlocked by remember(services) { mutableStateOf(services.completedGameForNewGamePlus() != null) }
    var loading by remember { mutableStateOf(false) }
    var transitioningOut by remember { mutableStateOf(false) }
    val fadeOutAlpha = remember { Animatable(0f) }
    val saveError by services.saveManager.lastError.collectAsState()

    LaunchedEffect(services) {
        val meta = withContext(Dispatchers.IO) {
            (SessionSaveRepository.ALL_SLOTS).mapNotNull { slot -> services.saveManager.getSlotMetadata(slot) }.maxByOrNull { it.timestamp }
        }
        latestSlotInfo = meta
        latestSlot = meta?.slotIndex
        newGamePlusUnlocked = services.completedGameForNewGamePlus() != null
    }

    val userSettings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())

    LaunchedEffect(services) {
        services.playTitleMusic()
    }

    fun transitionIntoGame(prepareAction: suspend () -> Boolean) {
        if (loading || transitioningOut) return
        coroutineScope.launch {
            loading = true
            transitioningOut = true
            services.audioDriver.executeAll(services.audioRouter.commandsForUi("ui_title_start"))
            services.audioDriver.executeAll(
                services.audioRouter.commandsForLayerOverride(
                    layer = AudioCueType.MUSIC,
                    fadeMs = 520L,
                    stop = true
                )
            )
            val fadeMs = if (userSettings.disableFlashes) 0 else 480
            if (fadeMs > 0) {
                fadeOutAlpha.animateTo(1f, tween(durationMillis = fadeMs, easing = FastOutSlowInEasing))
            } else {
                fadeOutAlpha.snapTo(1f)
            }
            val ok = prepareAction()
            loading = false
            if (ok) {
                onStartGame()
            } else {
                transitioningOut = false
                fadeOutAlpha.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
                services.playTitleMusic()
            }
        }
    }

    val titleFocusRequester = remember { FocusRequester() }
    val activeScreeningScene = screeningQueue.firstOrNull()
    val anyModalOpen = showLoadGame || showSettingsDialog || showDebugScenarios ||
        showScreeningRoom || activeScreeningScene != null || showNewGameConfirm || debugScenarioError != null

    LaunchedEffect(anyModalOpen) {
        if (!anyModalOpen) {
            withFrameNanos { }
            titleFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(activeScreeningScene?.id, screeningStepIndex) {
        val scene = activeScreeningScene ?: return@LaunchedEffect
        val cue = scene.steps.getOrNull(screeningStepIndex)?.musicCue?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (cue.equals("none", true) || cue.equals("stop", true) || cue.equals("silence", true)) {
            services.audioDriver.executeAll(services.audioRouter.commandsForLayerOverride(AudioCueType.MUSIC, stop = true, fadeMs = 450L))
        } else {
            services.audioDriver.executeAll(services.audioRouter.commandsForLayerOverride(AudioCueType.MUSIC, cueId = cue))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03050A))
            .focusRequester(titleFocusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (!anyModalOpen && !transitioningOut && keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.C, Key.Enter, Key.Spacebar -> {
                            if (latestSlot != null) {
                                val slotToLoad = latestSlot!!
                                transitionIntoGame { withContext(Dispatchers.IO) { services.loadSlot(slotToLoad) } }
                                true
                            } else if (keyEvent.key != Key.C) {
                                if (services.hasExistingSave()) {
                                    showNewGameConfirm = true
                                } else {
                                    transitionIntoGame { services.startNewGame() }
                                }
                                true
                            } else false
                        }
                        Key.N, Key.One -> {
                            if (services.hasExistingSave()) {
                                showNewGameConfirm = true
                            } else {
                                transitionIntoGame { services.startNewGame() }
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
                        Key.V -> {
                            if (services.isDebugEnabled || newGamePlusUnlocked) {
                                showScreeningRoom = true
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
            .focusable()
    ) {
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

        // Cosmic starfield & anamorphic lens shimmer layered subtly over the deep-space sky
        DesktopTitleCosmicStarfield(
            alpha = if (userSettings.disableFlashes) 0.28f else 0.52f,
            animate = !userSettings.disableFlashes && !userSettings.disableScreenshake
        )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthenticTitleLogo(services)
            }

            Column(
                modifier = Modifier
                    .weight(.85f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    latestSlot?.let { slot ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AuthenticTitleButton(
                                text = if (loading) "Loading…" else "Continue",
                                primary = true,
                                enabled = !transitioningOut,
                                onClick = {
                                    transitionIntoGame { withContext(Dispatchers.IO) { services.loadSlot(slot) } }
                                }
                            )
                            latestSlotInfo?.let { info ->
                                val slotBadge = when (info.slotIndex) {
                                    -1 -> "QUICKSAVE"
                                    0 -> "AUTOSAVE"
                                    else -> "SLOT ${info.slotIndex}"
                                }
                                Row(
                                    modifier = Modifier.padding(start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = TitleCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, TitleCyan.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = slotBadge,
                                            color = TitleCyan,
                                            fontSize = 9.sp,
                                            fontFamily = LocalStarbornFonts.current.orbitron,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "${info.roomTitle ?: "Deep Sector"} · Lv.${info.playerLevel} · ${info.formattedDate}",
                                        color = TitleMutedText.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontFamily = LocalStarbornFonts.current.orbitron
                                    )
                                }
                            }
                        }
                    }
                    if (newGamePlusUnlocked) {
                        AuthenticTitleButton(
                            text = "New Game+ · Master Protocol",
                            primary = false,
                            enabled = !transitioningOut,
                            onClick = { transitionIntoGame { services.startNewGamePlus() } }
                        )
                    }
                    saveError?.let { Text(it, color = Color(0xFFFF8A80)) }
                    AuthenticTitleButton(
                        text = "New Game",
                        primary = latestSlot == null,
                        enabled = !transitioningOut,
                        onClick = {
                            if (services.hasExistingSave()) {
                                showNewGameConfirm = true
                            } else {
                                transitionIntoGame { services.startNewGame() }
                            }
                        }
                    )

                    AuthenticTitleButton(
                        text = "Load Game",
                        primary = false,
                        enabled = !transitioningOut,
                        onClick = { showLoadGame = true }
                    )

                    if (services.isDebugEnabled) {
                        AuthenticTitleButton(
                            text = "Debug Scenarios",
                            primary = false,
                            enabled = !transitioningOut,
                            onClick = { showDebugScenarios = true }
                        )
                    }

                    if (services.isDebugEnabled || newGamePlusUnlocked) {
                        AuthenticTitleButton(
                            text = "Screening Room",
                            primary = false,
                            enabled = !transitioningOut,
                            onClick = { showScreeningRoom = true }
                        )
                    }

                    AuthenticTitleButton(
                        text = "Settings",
                        primary = false,
                        enabled = !transitioningOut,
                        onClick = { showSettingsDialog = true }
                    )

                    AuthenticTitleButton(
                        text = "Exit to Desktop",
                        primary = false,
                        enabled = !transitioningOut,
                        onClick = onQuit
                    )
                }
            }
        }

        Text(
            DesktopBuildInfo.DISPLAY_LABEL,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).testTag("desktop-build-label"),
            color = TitleMutedText,
            style = MaterialTheme.typography.labelSmall
        )

        if (showNewGameConfirm) {
            DesktopConsoleModal(
                eyebrow = "Campaign Deployment",
                title = "Start New Game?",
                onDismiss = { showNewGameConfirm = false },
                accent = TitleGold,
                maxWidth = 500.dp,
                testTag = "title-new-game-confirm",
                footer = {
                    TextButton(
                        onClick = { showNewGameConfirm = false },
                        modifier = Modifier.desktopPointerHover()
                    ) {
                        Text("Cancel", color = TitleMutedText)
                    }
                    Button(
                        onClick = {
                            showNewGameConfirm = false
                            transitionIntoGame { services.startNewGame() }
                        },
                        modifier = Modifier.desktopPointerHover(),
                        colors = ButtonDefaults.buttonColors(containerColor = TitleGold, contentColor = Color(0xFF1B1608))
                    ) {
                        Text("Begin New Game", fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Text(
                    "Starting a new game will begin a fresh run and overwrite your current autosave. Manual save slots remain untouched.",
                    color = TitleText.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (showDebugScenarios) {
            DesktopDebugScenarioDialog(
                onLaunch = { scenario ->
                    showDebugScenarios = false
                    transitionIntoGame {
                        val launched = services.startDebugScenario(scenario.id)
                        if (!launched) {
                            debugScenarioError = "Unable to launch ${scenario.title}: its starting location is unavailable in the current game data."
                        }
                        launched
                    }
                },
                onDismiss = { showDebugScenarios = false }
            )
        }

        if (showScreeningRoom && activeScreeningScene == null) {
            val entries = remember(services) { buildDesktopScreeningCatalog(services) }
            DesktopScreeningRoomDialog(
                entries = entries,
                onPlayScenes = { scenes ->
                    if (scenes.isNotEmpty()) {
                        showScreeningRoom = false
                        screeningStepIndex = 0
                        screeningQueue = scenes
                        services.audioDriver.executeAll(
                            services.audioRouter.commandsForLayerOverride(AudioCueType.MUSIC, stop = true, fadeMs = 250L)
                        )
                    }
                },
                onDismiss = { showScreeningRoom = false }
            )
        }

        if (activeScreeningScene != null) {
            val screeningUiState = remember(activeScreeningScene.id, screeningStepIndex) {
                buildDesktopScreeningUiState(activeScreeningScene, screeningStepIndex)
            }
            val finishScreening: (Boolean) -> Unit = { advanceReel ->
                val remaining = if (advanceReel) screeningQueue.drop(1) else emptyList()
                if (remaining.isNotEmpty()) {
                    screeningStepIndex = 0
                    screeningQueue = remaining
                    services.audioDriver.executeAll(
                        services.audioRouter.commandsForLayerOverride(AudioCueType.MUSIC, stop = true, fadeMs = 250L)
                    )
                } else {
                    screeningQueue = emptyList()
                    screeningStepIndex = 0
                    showScreeningRoom = true
                    services.playTitleMusic()
                }
            }
            DesktopCinematicOverlay(
                state = screeningUiState,
                services = services,
                onAdvance = {
                    if (screeningStepIndex + 1 < activeScreeningScene.steps.size) {
                        screeningStepIndex += 1
                    } else {
                        finishScreening(true)
                    }
                },
                onSkip = { finishScreening(false) }
            )
        }

        debugScenarioError?.let { message ->
            DesktopConsoleModal(
                eyebrow = "Scenario Launch",
                title = "Scenario unavailable",
                onDismiss = { debugScenarioError = null },
                accent = Color(0xFFFF8A80),
                maxWidth = 480.dp,
                footer = {
                    Button(onClick = { debugScenarioError = null }, modifier = Modifier.desktopPointerHover()) {
                        Text("Close")
                    }
                }
            ) {
                Text(message, color = TitleText)
            }
        }

        if (showLoadGame) {
            DesktopSaveLoadDialog(
                services = services,
                initialMode = SaveDialogMode.LOAD,
                onLoadState = {
                    showLoadGame = false
                    transitionIntoGame { true }
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

        if (fadeOutAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("title-blackout-curtain")
                    .background(Color.Black.copy(alpha = fadeOutAlpha.value.coerceIn(0f, 1f)))
            )
        }
    }
}

private data class DesktopTitleStar(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val r: Float,
    val g: Float,
    val b: Float
)

@Composable
internal fun DesktopTitleCosmicStarfield(
    alpha: Float,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.001f) return
    val stars = remember {
        val rnd = java.util.Random(42L)
        List(120) {
            DesktopTitleStar(
                relX = rnd.nextFloat(),
                relY = rnd.nextFloat(),
                radius = if (rnd.nextFloat() > 0.82f) 2.2f else 1.2f,
                baseAlpha = 0.35f + rnd.nextFloat() * 0.65f,
                r = (0.65f + rnd.nextFloat() * 0.35f).coerceIn(0f, 1f),
                g = (0.85f + rnd.nextFloat() * 0.15f).coerceIn(0f, 1f),
                b = 1.0f
            )
        }
    }
    val twinklePulse = if (animate) {
        val twinkleTransition = rememberInfiniteTransition(label = "desktop_title_star_twinkle")
        val anim by twinkleTransition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(3800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle_pulse"
        )
        anim
    } else 1f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("title-cosmic-starfield")
            .graphicsLayer { this.alpha = alpha }
    ) {
        val cx = size.width * 0.36f
        val cy = size.height * 0.42f
        val outerR = (size.width * 0.50f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF008CDC).copy(alpha = 0.28f),
                    Color(0xFF0B3A5A).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = outerR
            ),
            center = Offset(cx, cy),
            radius = outerR
        )
        val streakWidth = (size.width * 0.68f).coerceAtLeast(1f)
        val streakHeight = 4.dp.toPx()
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF78DCFF).copy(alpha = 0.45f),
                    Color(0xFF1EC8FF).copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = streakWidth / 2f
            ),
            topLeft = Offset(cx - streakWidth / 2f, cy - streakHeight / 2f),
            size = androidx.compose.ui.geometry.Size(streakWidth, streakHeight)
        )
        for (star in stars) {
            val sx = star.relX * size.width
            val sy = star.relY * size.height
            val dist = kotlin.math.hypot(sx - cx, (sy - cy) * 1.4f)
            val centerSuppression = if (dist < 160f) 0.4f else 1f
            val starAlpha = (star.baseAlpha * centerSuppression * twinklePulse).coerceIn(0f, 1f)
            drawCircle(
                color = Color(star.r, star.g, star.b, starAlpha),
                radius = star.radius,
                center = Offset(sx, sy)
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
    enabled: Boolean = true,
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
        .desktopPointerHover(enabled)
        .height(if (primary) 58.dp else 50.dp)
        .graphicsLayer {
            scaleX = buttonScale
            scaleY = buttonScale
        }

    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TitleGold,
                contentColor = Color(0xFF1B1608),
                disabledContainerColor = TitleGold.copy(alpha = 0.7f),
                disabledContentColor = Color(0xFF1B1608)
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
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, TitleCyan.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = TitlePanel.copy(alpha = 0.70f),
                contentColor = TitleText,
                disabledContainerColor = TitlePanel.copy(alpha = 0.50f),
                disabledContentColor = TitleText.copy(alpha = 0.65f)
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
internal fun DesktopScreeningRoomDialog(
    entries: List<DesktopScreeningEntry>,
    onPlayScenes: (List<CinematicScene>) -> Unit,
    onDismiss: () -> Unit
) {
    val grouped = remember(entries) { entries.groupBy { it.worldLabel } }
    val world1Reel = remember(entries) {
        entries.filter { it.worldLabel.startsWith("World 1") && it.scene.id != "new_game_fade_in" }.map { it.scene }
    }
    DesktopConsoleModal(
        eyebrow = "Illustrated Theater",
        title = "Cinematic Screening Room",
        onDismiss = onDismiss,
        accent = TitleGold,
        maxWidth = 680.dp,
        testTag = "desktop-screening-room",
        headerTrailing = {
            if (world1Reel.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onPlayScenes(world1Reel) },
                    modifier = Modifier.desktopPointerHover(),
                    border = BorderStroke(1.dp, TitleCyan.copy(alpha = 0.6f))
                ) {
                    Text("Play World 1 Reel (${world1Reel.size})", color = TitleCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        footer = {
            TextButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) {
                Text("Close", color = TitleGold)
            }
        }
    ) {
        DesktopConsoleLazyList(accent = TitleGold, maxHeight = 460.dp) {
            grouped.forEach { (worldLabel, worldEntries) ->
                item(key = "header-$worldLabel") {
                    Text(
                        worldLabel.uppercase(Locale.US),
                        color = TitleCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                items(worldEntries, key = { it.scene.id }) { entry ->
                    Surface(
                        onClick = { onPlayScenes(listOf(entry.scene)) },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TitleCyan.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth().desktopPointerHover()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    entry.scene.title ?: entry.scene.id,
                                    color = TitleText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    String.format(
                                        Locale.US,
                                        "%s · %d beats · %d imgs · %.1fs",
                                        entry.scene.id,
                                        entry.scene.steps.size,
                                        entry.imageCount,
                                        entry.durationSeconds
                                    ),
                                    color = TitleMutedText.copy(alpha = 0.78f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text("Play ›", color = TitleGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
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

    DesktopConsoleModal(
        eyebrow = "Playtest Harness",
        title = "DEBUG SCENARIOS",
        onDismiss = onDismiss,
        accent = TitleCyan,
        maxWidth = 720.dp,
        testTag = "desktop-debug-scenarios",
        footer = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) {
                Text("Close")
            }
        }
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
                    modifier = Modifier.desktopPointerHover(),
                    label = { Text(option.label, fontSize = 11.sp) }
                )
            }
        }
        DesktopConsoleLazyList(accent = TitleCyan, maxHeight = 360.dp) {
            items(filtered, key = { it.id }) { scenario ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1424))
                        .border(BorderStroke(1.dp, TitleCyan.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                        .desktopPointerHover()
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
}

@Composable
private fun DesktopSettingsDialog(
    services: DesktopAppServices,
    userSettings: UserSettings,
    onDismiss: () -> Unit
) {
    var showControls by remember { mutableStateOf(false) }
    DesktopConsoleModal(
        eyebrow = "System Configuration",
        title = "SETTINGS & CONFIGURATION",
        onDismiss = onDismiss,
        accent = TitleCyan,
        maxWidth = 820.dp,
        testTag = "desktop-title-settings",
        footer = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.desktopPointerHover(),
                colors = ButtonDefaults.buttonColors(containerColor = TitleCyan, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Box(Modifier.fillMaxWidth().height(480.dp)) {
            DesktopSettingsContent(
                services = services,
                userSettings = userSettings,
                currentRoomTitle = null,
                onReturnToTitle = null,
                onOpenControls = { showControls = true }
            )
        }
    }
    if (showControls) {
        DesktopControlsDialog(onDismiss = { showControls = false })
    }
}
