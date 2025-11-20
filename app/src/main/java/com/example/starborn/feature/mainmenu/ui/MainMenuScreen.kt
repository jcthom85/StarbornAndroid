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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onStartGame: () -> Unit,
    onSlotLoaded: () -> Unit
) {
    var showLoadDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(showLoadDialog) {
        if (showLoadDialog) {
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
            Button(onClick = { showLoadDialog = true }) {
                Text("Load Game")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = {
                viewModel.startNewGame()
                onStartGame()
            }) {
                Text("Start Game")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        if (showLoadDialog) {
            LoadGameDialog(
                slots = slots,
                onLoad = { slot ->
                    scope.launch {
                        val success = viewModel.loadSlot(slot)
                        if (success) {
                            showLoadDialog = false
                            onSlotLoaded()
                        }
                    }
                },
                onSave = { slot -> scope.launch { viewModel.saveSlot(slot) } },
                onDelete = { slot -> scope.launch { viewModel.deleteSlot(slot) } },
                onReloadFromAssets = { slot -> scope.launch { viewModel.reloadSlotFromAssets(slot) } },
                onDismiss = { showLoadDialog = false }
            )
        }
    }
}

@Composable
private fun LoadGameDialog(
    slots: List<SaveSlotSummary>,
    onLoad: (Int) -> Unit,
    onSave: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onReloadFromAssets: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            color = Color(0xFF1C2A33).copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Save Slot",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(slots) { summary ->
                        SaveSlotCard(
                            summary = summary,
                            onLoad = onLoad,
                            onSave = onSave,
                            onDelete = onDelete,
                            onReloadFromAssets = onReloadFromAssets
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveSlotCard(
    summary: SaveSlotSummary,
    onLoad: (Int) -> Unit,
    onSave: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onReloadFromAssets: (Int) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = Color(0xFF243441),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val slotLabel = when {
                summary.isQuickSave -> "Quicksave"
                summary.isAutosave -> "Autosave"
                else -> "Slot ${summary.slot}"
            }
            Text(
                text = slotLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            Text(text = summary.title, color = Color.White, maxLines = 1)
            Text(
                text = summary.subtitle,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            summary.state?.let { state ->
                Text(
                    text = "Quests: ${state.activeQuests.size} active" +
                        if (state.completedQuests.isNotEmpty()) " / ${state.completedQuests.size} completed" else "",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            summary.savedAtMillis?.let { savedAt ->
                val savedLabel = runCatching {
                    java.time.Instant.ofEpochMilli(savedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d • HH:mm"))
                }.getOrNull()
                if (savedLabel != null) {
                    Text(
                        text = "Saved: $savedLabel",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!summary.isAutosave && !summary.isQuickSave) {
                    OutlinedButton(
                        onClick = { onSave(summary.slot) },
                        enabled = summary.isEmpty || !summary.isEmpty
                    ) {
                        Text(if (summary.isEmpty) "Save" else "Overwrite")
                    }
                }
                if (summary.isQuickSave) {
                    OutlinedButton(
                        onClick = { onSave(summary.slot) }
                    ) {
                        Text(if (summary.isEmpty) "Quick Save" else "Overwrite Quicksave")
                    }
                }
                OutlinedButton(
                    onClick = { onLoad(summary.slot) },
                    enabled = !summary.isEmpty
                ) {
                    val loadLabel = when {
                        summary.isAutosave -> "Load Autosave"
                        summary.isQuickSave -> "Load Quicksave"
                        else -> "Load"
                    }
                    Text(loadLabel)
                }
                if (!summary.isAutosave) {
                    OutlinedButton(
                        onClick = { onDelete(summary.slot) },
                        enabled = !summary.isEmpty
                    ) {
                        Text(if (summary.isQuickSave) "Clear" else "Delete")
                    }
                }
                if (!summary.isAutosave && !summary.isQuickSave) {
                    OutlinedButton(
                        onClick = { onReloadFromAssets(summary.slot) }
                    ) {
                        Text("Reload Asset")
                    }
                }
            }
        }
    }
}
