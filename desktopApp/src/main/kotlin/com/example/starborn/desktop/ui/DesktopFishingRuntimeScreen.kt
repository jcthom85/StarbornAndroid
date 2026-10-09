package com.example.starborn.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.fishing.viewmodel.*
import com.example.starborn.domain.fishing.MinigameResult

@Composable
fun DesktopFishingScreen(services: DesktopAppServices, zoneId: String = "glow_moss_cavern", onClose: () -> Unit) {
    val keyboardFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboardFocus.requestFocus() }
    val scope = rememberCoroutineScope()
    val runtime = remember(services, zoneId) { FishingController(services.fishingService, zoneId, parentScope = scope) }
    val state by runtime.uiState.collectAsState()
    DisposableEffect(runtime) { onDispose { runtime.close() } }
    fun acknowledgeCatch() {
        state.lastCatchResult?.let { result ->
            services.exploration.onFishingResult(FishingResultPayload(result.itemId, result.quantity, result.message,
                state.lastResult != MinigameResult.FAIL, secured = true))
        }
        runtime.resetFishing()
    }
    Column(Modifier.focusRequester(keyboardFocus).fillMaxSize().background(Color(0xFF07111A)).verticalScroll(rememberScrollState()).padding(32.dp).onPreviewKeyEvent { event ->
        when (event.key) {
            Key.Escape -> { if (event.type == KeyEventType.KeyDown) { runtime.cancelFishing(); onClose() }; true }
            Key.Spacebar -> {
                if (event.type == KeyEventType.KeyDown) when (state.fishingState) {
                    FishingState.SETUP -> runtime.startFishing()
                    FishingState.HOOKSET -> runtime.onHookButtonPressed()
                    FishingState.READY -> runtime.beginReeling()
                    FishingState.REELING -> runtime.onReelPressed()
                    FishingState.RESULT -> acknowledgeCatch()
                    else -> Unit
                } else runtime.onReelReleased()
                true
            }
            else -> false
        }
    }.focusable(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.currentZone?.name ?: "Fishing", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            TextButton(onClick = { runtime.cancelFishing(); onClose() }) { Text("Back") }
        }
        Text(state.currentZone?.description.orEmpty(), color = Color.White)
        when (state.fishingState) {
            FishingState.SETUP -> {
                Text("Rod", color = Color.White)
                state.availableRods.forEach { rod -> OutlinedButton(onClick = { runtime.selectRod(rod) }) { Text(rod.name + if (rod == state.selectedRod) " (selected)" else "") } }
                Text("Lure", color = Color.White)
                state.availableLures.forEach { lure -> OutlinedButton(onClick = { runtime.selectLure(lure) }) { Text(lure.name + if (lure == state.selectedLure) " (selected)" else "") } }
                Button(onClick = runtime::startFishing, enabled = state.selectedRod != null && state.selectedLure != null && state.currentZone != null) { Text("Cast") }
                if (state.availableRods.isEmpty() || state.availableLures.isEmpty()) Text("Find a rod and lure before fishing here.", color = Color.White)
            }
            FishingState.WAITING -> { Text("Watch for a bite…", color = Color.White); TextButton(onClick = runtime::cancelWaiting) { Text("Cancel cast") } }
            FishingState.HOOKSET -> Button(onClick = runtime::onHookButtonPressed) { Text("Bite! Hook — ${state.hookState?.timeRemainingMs ?: 0} ms") }
            FishingState.READY -> { Text("Hold to reel, release to reduce tension. Keep the line away from its limits.", color = Color.White); Button(onClick = runtime::beginReeling) { Text("Begin") } }
            FishingState.REELING -> {
                state.reelState?.let { reel ->
                    Text("${reel.fishName.orEmpty()} · ${reel.phase.name.lowercase()}", color = Color.White)
                    Text("Catch progress", color = Color.White); LinearProgressIndicator(progress = { reel.progress }, modifier = Modifier.fillMaxWidth())
                    Text("Line tension", color = Color.White); LinearProgressIndicator(progress = { reel.tension }, modifier = Modifier.fillMaxWidth(), color = if (reel.tension > .8f) Color.Red else Color.Cyan)
                }
                Surface(Modifier.fillMaxWidth().height(90.dp).pointerInput(runtime) {
                    detectTapGestures(onPress = { runtime.onReelPressed(); try { awaitRelease() } finally { runtime.onReelReleased() } })
                }, color = Color(0xFF244D5A)) { Box(Modifier.padding(24.dp)) { Text("Hold to reel", color = Color.White) } }
            }
            FishingState.RESULT -> { Text(state.lastCatchResult?.message.orEmpty(), color = Color.White); Button(onClick = ::acknowledgeCatch) { Text("Continue") } }
        }
        HorizontalDivider()
        Text("Fishing journal · ${state.journal.size} entries", color = Color.White)
    }
}
