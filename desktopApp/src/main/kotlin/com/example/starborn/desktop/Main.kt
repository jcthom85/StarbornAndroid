package com.example.starborn.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.ui.DesktopHubTravel
import com.example.starborn.desktop.ui.LocalHubTravel
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import com.example.starborn.desktop.ui.LocalDesktopForeground
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.starborn.desktop.ui.DesktopArcadeScreen
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import com.example.starborn.desktop.ui.DesktopFieldKitScreen
import com.example.starborn.desktop.ui.DesktopFishingScreen
import com.example.starborn.desktop.ui.DesktopHubScreen
import com.example.starborn.desktop.ui.DesktopMainMenuScreen
import com.example.starborn.desktop.ui.DesktopCombatTransitionOverlay
import com.example.starborn.desktop.ui.DesktopHubTransitionOverlay
import com.example.starborn.desktop.ui.TransitionMode
import kotlinx.coroutines.launch

enum class DesktopScreenState {
    MAIN_MENU, HUB, EXPLORATION, COMBAT, FIELD_KIT, FISHING, ARCADE
}

fun main(args: Array<String>) {
    if ("--smoke-test" in args) {
        DesktopPackagedSmokeCheck.run(args)
        return
    }
    launchDesktop()
}

private fun launchDesktop() = application {
    val services = remember { DesktopAppServices() }
    val displayMode by services.userSettingsStore.displayMode.collectAsState(initial = DesktopDisplayMode.WINDOWED)
    val savedWidth by services.userSettingsStore.windowWidth.collectAsState(initial = 1280)
    val savedHeight by services.userSettingsStore.windowHeight.collectAsState(initial = 800)
    val savedX by services.userSettingsStore.windowX.collectAsState(initial = null)
    val savedY by services.userSettingsStore.windowY.collectAsState(initial = null)
    val savedMaximized by services.userSettingsStore.windowMaximized.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(DesktopScreenState.MAIN_MENU) }

    // Keep game composition alive while changing the native frame decoration.
    val gameContent = remember {
        movableContentOf {
            com.example.starborn.desktop.ui.DesktopStarbornTheme(services) {
                DesktopGameApp(services, screenState, { screenState = it }, {
                    services.close()
                    exitApplication()
                })
            }
        }
    }
    var monitorBounds by remember { mutableStateOf(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds) }
    // Each mode gets fresh state: native maximize/fullscreen state must not leak
    // into the next mode or race the recreation of an undecorated frame.
    key(displayMode) {
        val windowState = rememberWindowState(
            size = if (displayMode == DesktopDisplayMode.BORDERLESS)
                DpSize(monitorBounds.width.dp, monitorBounds.height.dp)
            else DpSize(savedWidth.coerceAtLeast(1024).dp, savedHeight.coerceAtLeast(720).dp),
            position = if (displayMode == DesktopDisplayMode.BORDERLESS)
                WindowPosition.Absolute(monitorBounds.x.dp, monitorBounds.y.dp)
            else if (savedX != null && savedY != null)
                WindowPosition.Absolute(savedX!!.dp, savedY!!.dp)
            else WindowPosition.Aligned(Alignment.Center),
            placement = when {
                displayMode == DesktopDisplayMode.FULLSCREEN -> WindowPlacement.Fullscreen
                savedMaximized && displayMode == DesktopDisplayMode.WINDOWED -> WindowPlacement.Maximized
                else -> WindowPlacement.Floating
            }
        )
        val saveCurrentBounds: suspend () -> Unit = {
            try {
                val isMax = windowState.placement == WindowPlacement.Maximized
                val pos = windowState.position as? WindowPosition.Absolute
                services.userSettingsStore.saveWindowBounds(
                    width = windowState.size.width.value.toInt(),
                    height = windowState.size.height.value.toInt(),
                    x = pos?.x?.value?.toInt(),
                    y = pos?.y?.value?.toInt(),
                    isMaximized = isMax
                )
            } catch (_: Throwable) {}
        }
        val icon = androidx.compose.ui.res.painterResource("icon.png")
        Window(
        onCloseRequest = {
            coroutineScope.launch {
                saveCurrentBounds()
                services.close()
                exitApplication()
            }
        },
        title = "Starborn",
        icon = icon,
        state = windowState,
        undecorated = displayMode != DesktopDisplayMode.WINDOWED,
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.isAltPressed && keyEvent.key == Key.Enter) {
                if (keyEvent.type == KeyEventType.KeyDown) coroutineScope.launch {
                    services.userSettingsStore.setDisplayMode(
                        if (displayMode == DesktopDisplayMode.WINDOWED) DesktopDisplayMode.BORDERLESS
                        else DesktopDisplayMode.WINDOWED)
                }
                true
            } else false
        },
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.F11 -> {
                        val nextMode = if (displayMode == DesktopDisplayMode.FULLSCREEN) {
                            DesktopDisplayMode.WINDOWED
                        } else {
                            DesktopDisplayMode.FULLSCREEN
                        }
                        coroutineScope.launch {
                            services.userSettingsStore.setDisplayMode(nextMode)
                        }
                        true
                    }
                    else -> false
                }
            } else false
        }
    ) {
        val currentSettings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
        val isForeground = LocalWindowInfo.current.isWindowFocused && !windowState.isMinimized
        LaunchedEffect(isForeground, currentSettings.muteWhenUnfocused) {
            (services.audioDriver as? DesktopAudioDriver)?.setMuted(!isForeground && currentSettings.muteWhenUnfocused)
        }
        val baseDensity = androidx.compose.ui.platform.LocalDensity.current
        val effectiveDensity = remember(baseDensity, currentSettings.uiScale) {
            val scale = currentSettings.uiScale.coerceIn(0.75f, 2.5f)
            androidx.compose.ui.unit.Density(baseDensity.density * scale, baseDensity.fontScale)
        }
        CompositionLocalProvider(
            LocalDesktopForeground provides isForeground,
            androidx.compose.ui.platform.LocalDensity provides effectiveDensity
        ) {
        DisposableEffect(window, displayMode) {
            window.minimumSize = java.awt.Dimension(1024, 720)
            monitorBounds = window.graphicsConfiguration.bounds
            if (displayMode == DesktopDisplayMode.BORDERLESS) {
                // Full monitor bounds, including the taskbar area; no maximize work-area sizing.
                window.bounds = monitorBounds
            }
            onDispose {
                coroutineScope.launch { saveCurrentBounds() }
            }
        }
        gameContent()
        }
        }
    }
}

@Composable
fun DesktopGameApp(
    services: DesktopAppServices,
    screenState: DesktopScreenState,
    onScreenStateChange: (DesktopScreenState) -> Unit,
    onExit: () -> Unit
) {
    val travelScope = rememberCoroutineScope()
    val travel = remember { DesktopHubTravel(travelScope) }

    LaunchedEffect(services) {
        services.exploration.events.collect { event ->
            when (event) {
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.AudioCommands -> {
                    services.audioDriver.executeAll(event.commands)
                }
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.AudioSettingsChanged -> {
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.MUSIC, event.musicVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.UI, event.sfxVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.BATTLE, event.sfxVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.VOICE, event.voiceVolume)
                }
                else -> Unit
            }
        }
    }

    var pendingCombatEnemies by remember { mutableStateOf<List<String>?>(null) }
    var enteringCombatTransition by remember { mutableStateOf(false) }
    var exitingCombatTransition by remember { mutableStateOf(false) }
    var exitCombatPayload by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val currentSettings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    val envThemeState by services.environmentThemeManager.state.collectAsState()
    val roomTheme = envThemeState.theme

    CompositionLocalProvider(LocalHubTravel provides travel) {
    Box(Modifier.fillMaxSize().onPreviewKeyEvent { travel.busy }) {
    var activeCombatEnemies by remember { mutableStateOf(listOf("scrapper_guard", "scrapper_drone")) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF05070D)
    ) {
        when (screenState) {
            DesktopScreenState.MAIN_MENU -> DesktopMainMenuScreen(
                services = services,
                onStartGame = {
                    onScreenStateChange(if (services.sessionStore.state.value.roomId == null) DesktopScreenState.HUB else DesktopScreenState.EXPLORATION)
                },
                onOpenSettings = { /* Handled inside menu dialog */ },
                onQuit = onExit
            )
            DesktopScreenState.HUB -> DesktopHubScreen(
                services = services,
                onEnterRoom = { roomId ->
                    services.sessionStore.setRoom(roomId)
                    onScreenStateChange(DesktopScreenState.EXPLORATION)
                },
                onBackToExploration = { onScreenStateChange(DesktopScreenState.EXPLORATION) },
                onReturnToTitle = { onScreenStateChange(DesktopScreenState.MAIN_MENU) }
            )
            DesktopScreenState.EXPLORATION -> DesktopExplorationScreen(
                services = services,
                onEnterCombat = { enemies ->
                    if (enemies.isEmpty() || pendingCombatEnemies != null) return@DesktopExplorationScreen
                    val stopAudio = services.audioRouter.commandsForLayerOverride(com.example.starborn.domain.audio.AudioCueType.MUSIC, stop = true, fadeMs = 150L) +
                        services.audioRouter.commandsForLayerOverride(com.example.starborn.domain.audio.AudioCueType.AMBIENT, stop = true, fadeMs = 150L) +
                        listOf(com.example.starborn.domain.audio.AudioCommand.Play(com.example.starborn.domain.audio.AudioCueType.UI, "sfx_combat_transition_slam", loop = false))
                    services.audioDriver.executeAll(stopAudio)
                    pendingCombatEnemies = enemies
                },
                onOpenHub = { travel.requestHubReturn(travelScope) { onScreenStateChange(DesktopScreenState.HUB) } },
                onOpenFieldKit = { onScreenStateChange(DesktopScreenState.FIELD_KIT) },
                onOpenFishing = { onScreenStateChange(DesktopScreenState.FISHING) },
                onOpenArcade = { onScreenStateChange(DesktopScreenState.ARCADE) },
                onReturnToMenu = { onScreenStateChange(DesktopScreenState.MAIN_MENU) }
            )
            DesktopScreenState.COMBAT -> DesktopCombatScreen(
                services = services,
                enemyIds = activeCombatEnemies,
                onVictory = {
                    exitCombatPayload = "" to {
                        services.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                            outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                            enemyIds = activeCombatEnemies, roomId = services.sessionStore.state.value.roomId,
                            sourcePartyId = services.encounterCoordinator.currentSourcePartyId()))
                        onScreenStateChange(DesktopScreenState.EXPLORATION)
                        exitingCombatTransition = true
                    }
                },
                onDefeat = {
                    exitCombatPayload = "The party collapses in defeat... Regrouping to recover." to {
                        services.exploration.onCombatDefeat(activeCombatEnemies)
                        onScreenStateChange(DesktopScreenState.EXPLORATION)
                        exitingCombatTransition = true
                    }
                },
                onFlee = {
                    exitCombatPayload = "Retreating to safe ground..." to {
                        services.exploration.onCombatRetreat(com.example.starborn.navigation.CombatResultPayload(
                            outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.RETREAT,
                            enemyIds = activeCombatEnemies, roomId = services.sessionStore.state.value.roomId,
                            sourcePartyId = services.encounterCoordinator.currentSourcePartyId()))
                        onScreenStateChange(DesktopScreenState.EXPLORATION)
                        exitingCombatTransition = true
                    }
                }
            )
            DesktopScreenState.FIELD_KIT -> DesktopFieldKitScreen(
                services = services,
                onClose = { onScreenStateChange(DesktopScreenState.EXPLORATION) }
            )
            DesktopScreenState.FISHING -> DesktopFishingScreen(
                services = services,
                zoneId = services.activeFishingZone ?: "glow_moss_cavern",
                onClose = { onScreenStateChange(DesktopScreenState.EXPLORATION) }
            )
            DesktopScreenState.ARCADE -> DesktopArcadeScreen(
                services = services,
                onClose = { onScreenStateChange(DesktopScreenState.EXPLORATION) }
            )
        }
    }
    DesktopHubTransitionOverlay(travel = travel)

    pendingCombatEnemies?.let { enemies ->
        DesktopCombatTransitionOverlay(
            visible = true,
            theme = roomTheme,
            suppressFlashes = currentSettings.disableFlashes,
            highContrastMode = currentSettings.highContrastMode,
            mode = TransitionMode.ENTER,
            mainText = "HOSTILES",
            subText = "INCOMING!",
            onFinished = {
                activeCombatEnemies = enemies
                pendingCombatEnemies = null
                enteringCombatTransition = true
                onScreenStateChange(DesktopScreenState.COMBAT)
            },
            modifier = Modifier.fillMaxSize().zIndex(150f)
        )
    }

    if (enteringCombatTransition) {
        DesktopCombatTransitionOverlay(
            visible = true,
            theme = roomTheme,
            suppressFlashes = currentSettings.disableFlashes,
            highContrastMode = currentSettings.highContrastMode,
            mode = TransitionMode.EXIT,
            onFinished = { enteringCombatTransition = false },
            modifier = Modifier.fillMaxSize().zIndex(150f)
        )
    }

    exitCombatPayload?.let { (text, action) ->
        DesktopCombatTransitionOverlay(
            visible = true,
            theme = roomTheme,
            suppressFlashes = currentSettings.disableFlashes,
            highContrastMode = currentSettings.highContrastMode,
            mode = TransitionMode.ENTER,
            mainText = text,
            subText = "",
            onFinished = {
                val act = action
                exitCombatPayload = null
                act()
            },
            modifier = Modifier.fillMaxSize().zIndex(150f)
        )
    }

    if (exitingCombatTransition) {
        DesktopCombatTransitionOverlay(
            visible = true,
            theme = roomTheme,
            suppressFlashes = currentSettings.disableFlashes,
            highContrastMode = currentSettings.highContrastMode,
            mode = TransitionMode.EXIT,
            onFinished = { exitingCombatTransition = false },
            modifier = Modifier.fillMaxSize().zIndex(150f)
        )
    }
    }
    }

}
