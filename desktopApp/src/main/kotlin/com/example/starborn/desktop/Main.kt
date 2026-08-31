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
import com.example.starborn.desktop.ui.DesktopGameScreen
import com.example.starborn.desktop.ui.DesktopMainMenuScreen

enum class DesktopScreenState {
    MAIN_MENU, IN_GAME, SETTINGS
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF05070D)
    ) {
        when (screenState) {
            DesktopScreenState.MAIN_MENU, DesktopScreenState.SETTINGS -> DesktopMainMenuScreen(
                services = services,
                onStartGame = { screenState = DesktopScreenState.IN_GAME },
                onOpenSettings = { screenState = DesktopScreenState.SETTINGS },
                onQuit = onExit
            )
            DesktopScreenState.IN_GAME -> DesktopGameScreen(
                services = services,
                onReturnToMenu = { screenState = DesktopScreenState.MAIN_MENU }
            )
        }
    }
}
