package com.example.starborn.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
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
    val coroutineScope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(DesktopScreenState.MAIN_MENU) }

    // Keep game composition alive while changing the native frame decoration.
    val gameContent = remember {
        movableContentOf {
            com.example.starborn.desktop.ui.DesktopStarbornTheme {
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
                DpSize(monitorBounds.width.dp, monitorBounds.height.dp) else DpSize(1280.dp, 800.dp),
            position = if (displayMode == DesktopDisplayMode.BORDERLESS)
                WindowPosition.Absolute(monitorBounds.x.dp, monitorBounds.y.dp)
                else WindowPosition.Aligned(Alignment.Center),
            placement = if (displayMode == DesktopDisplayMode.FULLSCREEN)
                WindowPlacement.Fullscreen else WindowPlacement.Floating
        )
        Window(
        onCloseRequest = {
            services.close()
            exitApplication()
        },
        title = "Starborn",
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
        CompositionLocalProvider(LocalDesktopForeground provides (LocalWindowInfo.current.isWindowFocused && !windowState.isMinimized)) {
        DisposableEffect(window, displayMode) {
            monitorBounds = window.graphicsConfiguration.bounds
            if (displayMode == DesktopDisplayMode.BORDERLESS) {
                // Full monitor bounds, including the taskbar area; no maximize work-area sizing.
                window.bounds = monitorBounds
            }
            onDispose { }
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
    val travelScope=rememberCoroutineScope()
    val travel=remember { DesktopHubTravel(travelScope) }
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
                    travel.request(travelScope) {
                        activeCombatEnemies = enemies
                        onScreenStateChange(DesktopScreenState.COMBAT)
                    }
                },
                onOpenHub = { travel.request(travelScope) { onScreenStateChange(DesktopScreenState.HUB) } },
                onOpenFieldKit = { onScreenStateChange(DesktopScreenState.FIELD_KIT) },
                onOpenFishing = { onScreenStateChange(DesktopScreenState.FISHING) },
                onOpenArcade = { onScreenStateChange(DesktopScreenState.ARCADE) },
                onReturnToMenu = { onScreenStateChange(DesktopScreenState.MAIN_MENU) }
            )
            DesktopScreenState.COMBAT -> DesktopCombatScreen(
                services = services,
                enemyIds = activeCombatEnemies,
                onVictory = {
                    travel.request(travelScope) {
                    services.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                        outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                        enemyIds = activeCombatEnemies, roomId = services.sessionStore.state.value.roomId,
                        sourcePartyId = services.encounterCoordinator.currentSourcePartyId()))
                    onScreenStateChange(DesktopScreenState.EXPLORATION)
                    }
                },
                onDefeat = { travel.request(travelScope) {
                    services.exploration.onCombatDefeat(activeCombatEnemies)
                    onScreenStateChange(DesktopScreenState.EXPLORATION)
                } },
                onFlee = {
                    travel.request(travelScope) {
                    services.exploration.onCombatRetreat(com.example.starborn.navigation.CombatResultPayload(
                        outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.RETREAT,
                        enemyIds = activeCombatEnemies, roomId = services.sessionStore.state.value.roomId,
                        sourcePartyId = services.encounterCoordinator.currentSourcePartyId()))
                    onScreenStateChange(DesktopScreenState.EXPLORATION)
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
    if(travel.busy) Box(Modifier.matchParentSize().zIndex(100f).background(Color.Black.copy(alpha=travel.opacity.value))
        .clickable(interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null) {})
    }
    }

}
