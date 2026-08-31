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
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.desktop.ui.DesktopExplorationScreen
import com.example.starborn.desktop.ui.DesktopFieldKitScreen
import com.example.starborn.desktop.ui.DesktopFishingScreen
import com.example.starborn.desktop.ui.DesktopMainMenuScreen

enum class DesktopScreenState {
    MAIN_MENU, EXPLORATION, COMBAT, FIELD_KIT, FISHING, ARCADE
}

fun main() = application {
    val services = remember { DesktopAppServices() }
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 800.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = {
            services.audioDriver.release()
            exitApplication()
        },
        title = "Starborn",
        state = windowState,
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.F11 -> {
                        windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Fullscreen
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF05070D)
    ) {
        when (screenState) {
            DesktopScreenState.MAIN_MENU -> DesktopMainMenuScreen(
                services = services,
                onStartGame = { screenState = DesktopScreenState.EXPLORATION },
                onOpenSettings = { /* Handled inside menu dialog */ },
                onQuit = onExit
            )
            DesktopScreenState.EXPLORATION -> DesktopExplorationScreen(
                services = services,
                onEnterCombat = { enemies ->
                    activeCombatEnemies = enemies
                    screenState = DesktopScreenState.COMBAT
                },
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
