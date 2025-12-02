package com.example.starborn.feature.mainmenu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onStartGame: () -> Unit,
    onSlotLoaded: () -> Unit
) {
    var saveLoadMode by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(saveLoadMode) {
        if (saveLoadMode != null) {
            viewModel.refreshSlots()
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
                    viewModel.startNewGame {
                        onStartGame()
                    }
                }
            ) { Text("New Game") }
            Spacer(modifier = Modifier.padding(8.dp))
            OutlinedButton(onClick = { saveLoadMode = "load" }) {
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
                onRefresh = { viewModel.refreshSlots() },
                onDismiss = { saveLoadMode = null },
                accentColor = Color(0xFFFFB347),
                panelColor = Color(0xFF1C2A33).copy(alpha = 0.95f),
                borderColor = Color.White.copy(alpha = 0.12f),
                textColor = Color.White
            )
        }
    }
}
