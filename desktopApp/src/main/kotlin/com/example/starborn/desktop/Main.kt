package com.example.starborn.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType

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
                    Key.Escape -> {
                        // Desktop Escape / Back handler
                        true
                    }
                    else -> false
                }
            } else false
        }
    ) {
        DesktopGameApp(services)
    }
}

@Composable
fun DesktopGameApp(services: DesktopAppServices) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF07040A)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STARBORN",
                    color = Color(0xFF4DEEEA),
                    fontSize = 42.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Windows Desktop Edition (Active)",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Press [F11] for Fullscreen | Loaded ${services.worldDataSource.loadRooms().size} rooms",
                    color = Color(0xFFFFB703),
                    fontSize = 14.sp
                )
            }
        }
    }
}
