package com.example.starborn.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.starborn.desktop.ui.DesktopArcadeScreen
import com.example.starborn.desktop.ui.DesktopCinematicScreen
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import com.example.starborn.desktop.ui.DesktopFieldKitScreen
import com.example.starborn.desktop.ui.DesktopFishingScreen
import com.example.starborn.desktop.ui.DesktopHubScreen
import com.example.starborn.desktop.ui.DesktopMainMenuScreen
import com.example.starborn.domain.cinematic.CinematicScene
import kotlinx.coroutines.launch

enum class DesktopScreenState {
    MAIN_MENU, HUB, EXPLORATION, COMBAT, CINEMATIC, FIELD_KIT, FISHING, ARCADE
}

fun main() = application {
    val services = remember { DesktopAppServices() }
    val displayMode by services.userSettingsStore.displayMode.collectAsState(initial = DesktopDisplayMode.WINDOWED)
    val coroutineScope = rememberCoroutineScope()

    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 800.dp),
        position = WindowPosition.Aligned(Alignment.Center),
        placement = when (displayMode) {
            DesktopDisplayMode.FULLSCREEN -> WindowPlacement.Fullscreen
            DesktopDisplayMode.BORDERLESS -> WindowPlacement.Maximized
            DesktopDisplayMode.WINDOWED -> WindowPlacement.Floating
        }
    )

    // Sync window state changes with user preferences
    LaunchedEffect(displayMode) {
        when (displayMode) {
            DesktopDisplayMode.FULLSCREEN -> {
                windowState.placement = WindowPlacement.Fullscreen
            }
            DesktopDisplayMode.BORDERLESS -> {
                windowState.placement = WindowPlacement.Maximized
            }
            DesktopDisplayMode.WINDOWED -> {
                windowState.placement = WindowPlacement.Floating
                windowState.size = DpSize(1280.dp, 800.dp)
            }
        }
    }

    Window(
        onCloseRequest = {
            services.audioDriver.release()
            exitApplication()
        },
        title = "Starborn",
        state = windowState,
        undecorated = displayMode == DesktopDisplayMode.BORDERLESS,
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
        DesktopGameApp(services, onExit = {
            services.audioDriver.release()
            exitApplication()
        })
    }
}

@Composable
fun DesktopGameApp(
    services: DesktopAppServices,
    onExit: () -> Unit
) {
    var screenState by remember { mutableStateOf(DesktopScreenState.MAIN_MENU) }
    var activeCombatEnemies by remember { mutableStateOf(listOf("scrapper_guard", "scrapper_drone")) }
    var activeCinematicScene by remember { mutableStateOf<CinematicScene?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF05070D)
    ) {
        when (screenState) {
            DesktopScreenState.MAIN_MENU -> DesktopMainMenuScreen(
                services = services,
                onStartGame = {
                    val intro = services.cinematicService.scene("intro_prologue")
                        ?: services.cinematicService.scene("new_game_intro")
                    if (intro != null) {
                        activeCinematicScene = intro
                        screenState = DesktopScreenState.CINEMATIC
                    } else {
                        screenState = DesktopScreenState.EXPLORATION
                    }
                },
                onOpenSettings = { /* Handled inside menu dialog */ },
                onQuit = onExit
            )
            DesktopScreenState.HUB -> DesktopHubScreen(
                services = services,
                onEnterRoom = { roomId ->
                    services.sessionStore.setRoom(roomId)
                    screenState = DesktopScreenState.EXPLORATION
                },
                onBackToExploration = { screenState = DesktopScreenState.EXPLORATION }
            )
            DesktopScreenState.EXPLORATION -> DesktopExplorationScreen(
                services = services,
                onEnterCombat = { enemies ->
                    activeCombatEnemies = enemies
                    screenState = DesktopScreenState.COMBAT
                },
                onOpenHub = { screenState = DesktopScreenState.HUB },
                onOpenFieldKit = { screenState = DesktopScreenState.FIELD_KIT },
                onOpenFishing = { screenState = DesktopScreenState.FISHING },
                onOpenArcade = { screenState = DesktopScreenState.ARCADE },
                onReturnToMenu = { screenState = DesktopScreenState.MAIN_MENU }
            )
            DesktopScreenState.COMBAT -> DesktopCombatScreen(
                services = services,
                enemyIds = activeCombatEnemies,
                onVictory = { screenState = DesktopScreenState.EXPLORATION },
                onDefeat = { screenState = DesktopScreenState.MAIN_MENU },
                onFlee = { screenState = DesktopScreenState.EXPLORATION }
            )
            DesktopScreenState.CINEMATIC -> {
                val scene = activeCinematicScene
                if (scene != null) {
                    DesktopCinematicScreen(
                        services = services,
                        scene = scene,
                        onComplete = {
                            activeCinematicScene = null
                            screenState = DesktopScreenState.EXPLORATION
                        }
                    )
                } else {
                    screenState = DesktopScreenState.EXPLORATION
                }
            }
            DesktopScreenState.FIELD_KIT -> DesktopFieldKitScreen(
                services = services,
                onClose = { screenState = DesktopScreenState.EXPLORATION }
            )
            DesktopScreenState.FISHING -> DesktopFishingScreen(
                services = services,
                onClose = { screenState = DesktopScreenState.EXPLORATION }
            )
            DesktopScreenState.ARCADE -> DesktopArcadeScreen(
                services = services,
                onClose = { screenState = DesktopScreenState.EXPLORATION }
            )
        }
    }
}
