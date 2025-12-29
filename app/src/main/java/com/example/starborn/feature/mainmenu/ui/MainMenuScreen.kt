package com.example.starborn.feature.mainmenu.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.ui.components.SaveLoadDialog
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class StartMode { NORMAL, DEBUG }

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onStartGame: () -> Unit,
    onSlotLoaded: () -> Unit
) {
    var pendingStartMode by remember { mutableStateOf<StartMode?>(null) }
    var saveLoadMode by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val slots by viewModel.slots.collectAsStateWithLifecycle()
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
    val fadeOutAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(saveLoadMode) {
        if (saveLoadMode != null) {
            viewModel.refreshSlots()
        }
    }

    LaunchedEffect(pendingStartMode) {
        if (pendingStartMode == null) {
            fadeOutAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            return@LaunchedEffect
        }
        fadeOutAlpha.stop()
        fadeOutAlpha.snapTo(0f)
        // Smooth fade out before heavy work begins.
        fadeOutAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
        )
        // Give the frame a beat to present the black overlay before loading.
        delay(120)
        val onFailure: () -> Unit = { pendingStartMode = null }
        when (pendingStartMode) {
            StartMode.NORMAL -> {
                viewModel.startNewGame(
                    onComplete = { onStartGame() },
                    onFailure = onFailure
                )
            }
            StartMode.DEBUG -> {
                viewModel.startNewGameWithFullInventory(
                    onComplete = { onStartGame() },
                    onFailure = onFailure
                )
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.main_menu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    pendingStartMode = StartMode.NORMAL
                },
                enabled = pendingStartMode == null
            ) { Text("New Game") }
            Spacer(modifier = Modifier.padding(6.dp))
            OutlinedButton(
                onClick = {
                    pendingStartMode = StartMode.DEBUG
                },
                enabled = pendingStartMode == null
            ) {
                Text("Debug: New Game (Full Inventory)")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            OutlinedButton(
                onClick = { saveLoadMode = "load" },
                enabled = pendingStartMode == null
            ) {
                Text("Load Game")
            }
        }

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
                    scope.launch {
                        val success = viewModel.loadSlot(slot)
                        if (success) {
                            saveLoadMode = null
                            onSlotLoaded()
                        }
                    }
                },
                onDelete = { slot ->
                    scope.launch {
                        viewModel.deleteSlot(slot)
                    }
                },
                onRefresh = { viewModel.refreshSlots() },
                onDismiss = { saveLoadMode = null },
                accentColor = accentColor,
                panelColor = panelColor,
                borderColor = borderColor,
                textColor = textColor
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
