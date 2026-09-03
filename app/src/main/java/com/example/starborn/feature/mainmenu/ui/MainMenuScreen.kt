package com.example.starborn.feature.mainmenu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.BuildConfig
import com.example.starborn.R
import com.example.starborn.data.local.UserSettings
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.feature.exploration.ui.tabs.SettingsTabContent
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import com.example.starborn.feature.mainmenu.DebugScenarioDestination
import com.example.starborn.ui.components.SaveLoadDialog
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val TitleGold = Color(0xFFFFC857)
private val TitleAmber = Color(0xFFFF9F2E)
private val TitleCyan = Color(0xFF63E6FF)
private val TitlePanel = Color(0xFF061018)
private val TitleText = Color(0xFFF7FBFF)
private val TitleMutedText = Color(0xFFD7EAF4)
private const val TITLE_LOAD_BLACKOUT_HOLD_MS = 1400L

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    audioCuePlayer: AudioCuePlayer,
    audioRouter: AudioRouter,
    userSettings: UserSettings,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onStartGame: () -> Unit,
    onStartHub: () -> Unit,
    onSlotLoaded: () -> Unit
) {
    var startingGame by remember { mutableStateOf(false) }
    var startingGamePlus by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var pendingScenario by remember { mutableStateOf<DebugScenario?>(null) }
    var pendingLoadSlot by remember { mutableStateOf<Int?>(null) }
    var showDebugBrowser by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var saveLoadMode by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val newGamePlusUnlocked by viewModel.newGamePlusUnlocked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val menuTheme = remember { viewModel.mainMenuTheme }
    val accentColor = remember(menuTheme) {
        themeColor(menuTheme?.accent, Color(0xFF7BE4FF))
    }
    val panelColor = remember(menuTheme) {
        themeColor(menuTheme?.bg, Color(0xFF0B111A)).copy(alpha = 0.96f)
    }
    val borderColor = remember(menuTheme) {
        themeColor(menuTheme?.border, Color.White.copy(alpha = 0.16f))
    }
    val textColor = remember(menuTheme) {
        themeColor(menuTheme?.fg, Color.White)
    }
    // Choreographed entrance timing aligned to 82 BPM (1 bar = ~2927ms, half-note/bar intervals)
    // 1. Logo begins at 0ms and completes fade-in over 5850ms (2 bars)
    val logoFadeAlpha = remember { Animatable(0f) }
    // 2. Starfield starts at 2925ms (1 bar in) and fades in over 2925ms (completing at 5850ms)
    val starfieldFadeAlpha = remember { Animatable(0f) }
    // 3. Stage 3 starts at 5850ms: background fades in over 5850ms (2 bars), buttons fade in gracefully over 2200ms
    val bgFadeAlpha = remember { Animatable(0f) }
    val buttonsFadeAlpha = remember { Animatable(0f) }
    val bgZoomFactor = remember { Animatable(0f) }
    var introFastForward by remember { mutableStateOf(false) }

    val fadeOutAlpha = remember { Animatable(0f) }
    val versionLabel = remember {
        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    LaunchedEffect(introFastForward) {
        if (introFastForward) {
            // Tapping the title screen during intro fast-forwards all elements to full fade-in over ~2s
            launch {
                logoFadeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                starfieldFadeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                bgFadeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                buttonsFadeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(2000)
                bgZoomFactor.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 5000, easing = FastOutSlowInEasing)
                )
            }
            return@LaunchedEffect
        }

        // Normal sequence:
        // Stage 1: Starborn logo fades in smoothly over 2 bars (5850ms)
        launch {
            logoFadeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5850, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: Cosmic starfield starts at 2925ms (behind the logo) and fades in over 2925ms
        delay(2925)
        launch {
            starfieldFadeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2925, easing = FastOutSlowInEasing)
            )
        }

        // Stage 3: Starts at 5850ms (exactly 2925ms after stage 2 start)
        delay(2925) // Total elapsed: 5850ms

        // Buttons fade in naturally and smoothly over ~2.2s
        launch {
            buttonsFadeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
            )
        }

        // Background image fades in over 2 bars (5850ms)
        bgFadeAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5850, easing = LinearEasing)
        )

        // Once background is fully faded in, smoothly and gradually ease into the ambient zoom
        bgZoomFactor.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6000, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(audioCuePlayer, audioRouter) {
        audioCuePlayer.execute(
            audioRouter.commandsForLayerOverride(
                layer = AudioCueType.MUSIC,
                cueId = "music_title_theme",
                fadeMs = 900L,
                loop = true
            )
        )
    }

    LaunchedEffect(audioCuePlayer, userSettings.musicVolume, userSettings.sfxVolume, userSettings.voiceVolume) {
        audioCuePlayer.setUserMusicGain(userSettings.musicVolume)
        audioCuePlayer.setUserSfxGain(userSettings.sfxVolume)
        audioCuePlayer.setUserVoiceGain(userSettings.voiceVolume)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(saveLoadMode) {
        if (saveLoadMode != null) {
            viewModel.refreshSlots()
        }
    }

    BackHandler {
        when {
            saveLoadMode != null -> saveLoadMode = null
            showDebugBrowser -> showDebugBrowser = false
            showSettings -> showSettings = false
            else -> Unit
        }
    }

    LaunchedEffect(startingGame, startingGamePlus, pendingScenario, pendingLoadSlot) {
        if (!startingGame && !startingGamePlus && pendingScenario == null && pendingLoadSlot == null) {
            fadeOutAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            return@LaunchedEffect
        }
        fadeOutAlpha.stop()
        fadeOutAlpha.snapTo(0f)
        audioCuePlayer.execute(
            audioRouter.commandsForLayerOverride(
                layer = AudioCueType.MUSIC,
                fadeMs = 560L,
                stop = true
            )
        )
        // Smooth fade out before heavy work begins.
        fadeOutAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
        )
        // Give the frame a beat to present the black overlay before loading.
        delay(120)
        val onFailure: () -> Unit = {
            startingGame = false
            startingGamePlus = false
            pendingScenario = null
            pendingLoadSlot = null
        }
        val scenario = pendingScenario
        val loadSlot = pendingLoadSlot
        if (scenario != null) {
            // Give the frame a beat to present the black overlay before loading debug state.
            delay(120)
            viewModel.startDebugScenario(
                scenario = scenario,
                onComplete = {
                    when (scenario.destination) {
                        DebugScenarioDestination.HUB -> onStartHub()
                        DebugScenarioDestination.EXPLORATION -> onStartGame()
                    }
                },
                onFailure = onFailure
            )
        } else if (loadSlot != null) {
            delay(TITLE_LOAD_BLACKOUT_HOLD_MS)
            val success = viewModel.loadSlot(loadSlot)
            if (success) {
                onSlotLoaded()
            } else {
                onFailure()
            }
        } else if (startingGamePlus) {
            delay(TITLE_LOAD_BLACKOUT_HOLD_MS)
            viewModel.startNewGamePlus(onComplete = { onStartGame() }, onFailure = onFailure)
        } else if (startingGame) {
            delay(TITLE_LOAD_BLACKOUT_HOLD_MS)
            viewModel.startNewGame(onComplete = { onStartGame() }, onFailure = onFailure)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = buttonsFadeAlpha.value < 0.95f
            ) {
                introFastForward = true
            }
    ) {
        val compactHeight = maxHeight < 760.dp

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
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "title_bg_pan"
        )
        val ambientBloom by bgTransition.animateFloat(
            initialValue = 0.04f,
            targetValue = 0.11f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "title_ambient_bloom"
        )

        // Base solid black canvas to support seamless sequential reveals
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Stage 2: Starfield behind the logo
        TitleCosmicStarfield(alpha = starfieldFadeAlpha.value)

        // Stage 3: Background image with slow, gradual zoom/pan easing in seamlessly
        val zoomFactor = bgZoomFactor.value
        val effectiveBgScale = 1.0f + (bgScale - 1.0f) * zoomFactor
        val effectiveBgPanY = bgPanY * zoomFactor
        val bgAlpha = bgFadeAlpha.value

        if (bgAlpha > 0.001f) {
            Image(
                painter = painterResource(id = R.drawable.title_background_starborn),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = effectiveBgScale
                        scaleY = effectiveBgScale
                        translationY = effectiveBgPanY
                        alpha = bgAlpha
                    },
                contentScale = ContentScale.Crop
            )

            // Soft ambient celestial bloom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                TitleCyan.copy(alpha = ambientBloom * bgAlpha),
                                TitleGold.copy(alpha = ambientBloom * 0.45f * bgAlpha),
                                Color.Transparent
                            ),
                            center = Offset(200f, 150f),
                            radius = 1100f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.18f * bgAlpha),
                                0.32f to Color.Transparent,
                                0.62f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.66f * bgAlpha)
                            )
                        )
                    )
            )
        }

        // Stage 1: Starborn logo fades in first over 2 bars
        StarbornTitleLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(
                    start = 14.dp,
                    top = if (compactHeight) 10.dp else 18.dp,
                    end = 14.dp
                )
                .fillMaxWidth()
                .heightIn(max = if (compactHeight) 180.dp else 240.dp)
                .graphicsLayer {
                    alpha = logoFadeAlpha.value
                }
        )

        // Stage 3: Title screen buttons fade in over ~2.2s alongside background start
        val buttonsInteractable = buttonsFadeAlpha.value >= 0.95f && !startingGame && pendingScenario == null && pendingLoadSlot == null
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 34.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .heightIn(max = if (compactHeight) 430.dp else 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 30.dp,
                    top = if (compactHeight) 22.dp else 34.dp,
                    end = 30.dp,
                    bottom = if (compactHeight) 22.dp else 34.dp
                )
                .graphicsLayer {
                    alpha = buttonsFadeAlpha.value
                },
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StarbornTitleButton(
                text = "New Game",
                onClick = {
                    if (newGamePlusUnlocked) {
                        showNewGameConfirm = true
                    } else {
                        startingGame = true
                    }
                },
                enabled = buttonsInteractable,
                primary = true
            )
            if (BuildConfig.DEBUG) {
                StarbornTitleButton(
                    text = "Debug Scenarios",
                    onClick = { showDebugBrowser = true },
                    enabled = buttonsInteractable
                )
            }
            StarbornTitleButton(
                text = "Load Game",
                onClick = { saveLoadMode = "load" },
                enabled = buttonsInteractable
            )
            StarbornTitleButton(
                text = "Settings",
                onClick = { showSettings = true },
                enabled = buttonsInteractable
            )
        }

        if (showNewGameConfirm && newGamePlusUnlocked) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = {
                    Text("Select Campaign Protocol", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Choose your deployment protocol. Master Protocol carries over your party levels, weapons, armor, skills, and inventory into an enhanced difficulty run with instant Astra access.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showNewGameConfirm = false
                                startingGame = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A394A), contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Standard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                showNewGameConfirm = false
                                startingGamePlus = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Master (NG+)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameConfirm = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF141C24),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            )
        }

        Text(
            text = versionLabel,
            color = TitleMutedText.copy(alpha = 0.62f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 10.dp)
                .graphicsLayer {
                    alpha = buttonsFadeAlpha.value
                }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        saveLoadMode?.let { mode ->
            SaveLoadDialog(
                mode = mode,
                slots = slots,
                onSave = { slot ->
                    scope.launch {
                        viewModel.saveSlot(slot)
                        viewModel.refreshSlots()
                        saveLoadMode = null
                    }
                },
                onLoad = { slot ->
                    saveLoadMode = null
                    pendingLoadSlot = slot
                },
                onDelete = { slot ->
                    scope.launch {
                        viewModel.deleteSlot(slot)
                    }
                },
                onDismiss = { saveLoadMode = null },
                accentColor = accentColor,
                panelColor = panelColor,
                borderColor = borderColor,
                textColor = textColor
            )
        }

        if (BuildConfig.DEBUG && showDebugBrowser) {
            DebugScenarioDialog(
                onLaunch = { scenario ->
                    showDebugBrowser = false
                    pendingScenario = scenario
                },
                onDismiss = { showDebugBrowser = false }
            )
        }

        if (showSettings) {
            TitleSettingsDialog(
                settings = SettingsUiState(
                    musicVolume = userSettings.musicVolume,
                    sfxVolume = userSettings.sfxVolume,
                    voiceVolume = userSettings.voiceVolume,
                    vignetteEnabled = userSettings.vignetteEnabled,
                    tutorialsEnabled = userSettings.tutorialsEnabled,
                    highContrastMode = userSettings.highContrastMode,
                    largeTouchTargets = userSettings.largeTouchTargets,
                    disableScreenshake = userSettings.disableScreenshake,
                    disableFlashes = userSettings.disableFlashes,
                    disableHaptics = userSettings.disableHaptics
                ),
                accentColor = accentColor,
                borderColor = borderColor,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onVoiceVolumeChange = onVoiceVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette,
                onToggleHighContrast = onToggleHighContrast,
                onToggleLargeTouchTargets = onToggleLargeTouchTargets,
                onToggleScreenshakeDisabled = onToggleScreenshakeDisabled,
                onToggleFlashesDisabled = onToggleFlashesDisabled,
                onToggleHapticsDisabled = onToggleHapticsDisabled,
                onDismiss = { showSettings = false }
            )
        }

        if (fadeOutAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeOutAlpha.value))
            )
        }
    }
}

@Composable
private fun TitleSettingsDialog(
    settings: SettingsUiState,
    accentColor: Color,
    borderColor: Color,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Black) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsTabContent(
                    settings = settings,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    showSaveData = false,
                    onMusicVolumeChange = onMusicVolumeChange,
                    onSfxVolumeChange = onSfxVolumeChange,
                    onVoiceVolumeChange = onVoiceVolumeChange,
                    onToggleTutorials = onToggleTutorials,
                    onToggleVignette = onToggleVignette,
                    onToggleHighContrast = onToggleHighContrast,
                    onToggleLargeTouchTargets = onToggleLargeTouchTargets,
                    onToggleScreenshakeDisabled = onToggleScreenshakeDisabled,
                    onToggleFlashesDisabled = onToggleFlashesDisabled,
                    onToggleHapticsDisabled = onToggleHapticsDisabled,
                    onQuickSave = {},
                    onSaveGame = {},
                    onLoadGame = {}
                )
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
        containerColor = TitlePanel,
        titleContentColor = TitleText,
        textContentColor = TitleMutedText
    )
}

@Composable
private fun DebugScenarioDialog(
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
        title = { Text("Debug Scenarios", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Launching replaces the current unsaved session.", color = TitleMutedText, fontSize = 13.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search world, quest, room, or system") },
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
                    modifier = Modifier.heightIn(min = 180.dp, max = 430.dp)
                ) {
                    items(filtered, key = { it.id }) { scenario ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunch(scenario) }
                                .background(TitleCyan.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(scenario.title, fontWeight = FontWeight.Bold, color = TitleText)
                            Text(
                                "${scenario.category.label}  |  ${scenario.worldLabel}",
                                color = TitleCyan,
                                fontSize = 11.sp
                            )
                            Text(scenario.description, color = TitleMutedText, fontSize = 13.sp)
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { Text("No matching scenarios.", color = TitleMutedText) }
                    }
                }
                HorizontalDivider(color = TitleCyan.copy(alpha = 0.2f))
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
        containerColor = TitlePanel,
        titleContentColor = TitleText,
        textContentColor = TitleMutedText
    )
}

@Composable
private fun StarbornTitleLogo(modifier: Modifier = Modifier) {
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
        targetValue = -1.1f,
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
        painter = painterResource(id = R.drawable.title_logo_starborn),
        contentDescription = "Starborn",
        contentScale = ContentScale.Fit,
        modifier = modifier
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
                            Color.White.copy(alpha = 0.18f),
                            TitleCyan.copy(alpha = 0.16f),
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
private fun StarbornTitleButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    val buttonModifier = modifier
        .fillMaxWidth()
        .height(if (primary) 60.dp else 56.dp)
        .graphicsLayer {
            scaleX = buttonScale
            scaleY = buttonScale
        }

    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 14.dp,
                disabledElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = TitleGold,
                contentColor = Color(0xFF1B1608),
                disabledContainerColor = TitlePanel.copy(alpha = 0.58f),
                disabledContentColor = TitleMutedText.copy(alpha = 0.62f)
            ),
            modifier = buttonModifier
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                letterSpacing = 0.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, if (enabled) TitleCyan.copy(alpha = 0.82f) else TitleCyan.copy(alpha = 0.22f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = TitlePanel.copy(alpha = if (enabled) 0.70f else 0.42f),
                contentColor = if (enabled) TitleText else TitleMutedText.copy(alpha = 0.56f),
                disabledContentColor = TitleMutedText.copy(alpha = 0.56f)
            ),
            modifier = buttonModifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TitleCyan.copy(alpha = if (enabled) 0.13f else 0.04f),
                            TitleAmber.copy(alpha = if (enabled) 0.08f else 0.02f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.sp
            )
        }
    }
}

private data class TitleStar(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val r: Float,
    val g: Float,
    val b: Float
)

@Composable
private fun TitleCosmicStarfield(
    modifier: Modifier = Modifier,
    alpha: Float
) {
    if (alpha <= 0.001f) return

    val stars = remember {
        val rnd = java.util.Random(42L)
        List(120) {
            val rx = rnd.nextFloat()
            val ry = rnd.nextFloat()
            val size = if (rnd.nextFloat() > 0.82f) 2.2f else 1.2f
            val brightness = 0.35f + rnd.nextFloat() * 0.65f
            TitleStar(
                relX = rx,
                relY = ry,
                radius = size,
                baseAlpha = brightness,
                r = (0.65f + rnd.nextFloat() * 0.35f).coerceIn(0f, 1f),
                g = (0.85f + rnd.nextFloat() * 0.15f).coerceIn(0f, 1f),
                b = 1.0f
            )
        }
    }

    val twinkleTransition = rememberInfiniteTransition(label = "title_star_twinkle")
    val twinklePulse by twinkleTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle_pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        val cx = size.width / 2f
        val cy = size.height * 0.22f // Centers behind the Starborn title logo

        // 1. Cosmic radial nebula glow (outer teal -> mid cyan -> inner bright core)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF008CDC).copy(alpha = 0.40f),
                    Color(0xFF0B3A5A).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = size.width * 0.65f
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1EC8FF).copy(alpha = 0.45f),
                    Color(0xFF008CDC).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = size.width * 0.35f
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.35f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA0F0FF).copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = size.width * 0.15f
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.15f
        )

        // 2. Anamorphic horizontal streak through the central star
        val streakWidth = size.width * 0.88f
        val streakHeight = 5.dp.toPx()
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF78DCFF).copy(alpha = 0.65f),
                    Color(0xFF1EC8FF).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = streakWidth / 2f
            ),
            topLeft = Offset(cx - streakWidth / 2f, cy - streakHeight / 2f),
            size = androidx.compose.ui.geometry.Size(streakWidth, streakHeight)
        )

        // 3. Deterministic cosmic starfield
        for (star in stars) {
            val sx = star.relX * size.width
            val sy = star.relY * size.height
            val dist = kotlin.math.hypot(sx - cx, (sy - cy) * 1.5f)
            val centerSuppression = if (dist < 140f) 0.35f else 1f
            val starAlpha = (star.baseAlpha * centerSuppression * twinklePulse).coerceIn(0f, 1f)

            drawCircle(
                color = Color(
                    red = star.r,
                    green = star.g,
                    blue = star.b,
                    alpha = starAlpha
                ),
                radius = star.radius,
                center = Offset(sx, sy)
            )
        }
    }
}
