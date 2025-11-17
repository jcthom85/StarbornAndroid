package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.crafting.FirstAidRecipeUi
import com.example.starborn.feature.crafting.FirstAidUiState
import com.example.starborn.feature.crafting.FirstAidViewModel
import com.example.starborn.feature.crafting.IngredientUi
import com.example.starborn.feature.crafting.MinigameDifficulty
import com.example.starborn.feature.crafting.TimingMinigameUi
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstAidRoute(
    viewModel: FirstAidViewModel,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onTriggerFx: (String) -> Unit = {},
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.feedback.collectLatest { feedback ->
            feedback.message?.let { snackbarHostState.showSnackbar(it) }
            feedback.audioCue?.takeIf { it.isNotBlank() }?.let(onPlayAudio)
            feedback.fxId?.takeIf { it.isNotBlank() }?.let(onTriggerFx)
        }
    }

    FirstAidScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAttempt = viewModel::startMinigame,
        onStopMinigame = viewModel::stopMinigame,
        onCancelMinigame = viewModel::cancelMinigame,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstAidScreen(
    state: FirstAidUiState,
    snackbarHostState: SnackbarHostState,
    onAttempt: (String) -> Unit,
    onStopMinigame: () -> Unit,
    onCancelMinigame: () -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    val containerColor = if (highContrastMode) Color(0xFF05080C) else MaterialTheme.colorScheme.background
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("First Aid Station") },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (highContrastMode) Color(0xFF0B1119) else MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = containerColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.recipes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No first aid recipes discovered yet.",
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        FirstAidRecipeCard(
                            recipe = recipe,
                            onAttempt = onAttempt,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )
                    }
                }
            }
        }

        state.activeMinigame?.let { minigame ->
            TimingMinigameOverlay(
                minigame = minigame,
                onStop = onStopMinigame,
                onCancel = onCancelMinigame,
                title = "Assemble ${minigame.recipeName}",
                instructions = "Tap anywhere inside the meter while the cursor is in the highlighted zone.",
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets
            )
        }
    }
}

@Composable
private fun FirstAidRecipeCard(
    recipe: FirstAidRecipeUi,
    onAttempt: (String) -> Unit,
    modifier: Modifier = Modifier,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val surfaceColor = if (highContrastMode) Color(0xFF121A21) else MaterialTheme.colorScheme.surface
    val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp, color = surfaceColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = titleColor)
            val difficultyLabel = when (recipe.difficulty) {
                MinigameDifficulty.EASY -> "Easy"
                MinigameDifficulty.NORMAL -> "Normal"
                MinigameDifficulty.HARD -> "Hard"
            }
            Text("Difficulty: $difficultyLabel", style = MaterialTheme.typography.bodySmall, color = if (highContrastMode) Color(0xFF64B5F6) else Color(0xFF90CAF9))
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Components", style = MaterialTheme.typography.labelSmall, color = titleColor)
                recipe.ingredients.forEach { ingredient ->
                    IngredientRow(ingredient)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Outcome", style = MaterialTheme.typography.labelSmall, color = titleColor)
                recipe.outcomes.forEach { outcome ->
                    Text(
                        text = "${outcome.label}: ${outcome.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor
                    )
                }
            }
            if (!recipe.canCraft) {
                val missing = recipe.missingIngredients.joinToString()
                Text(
                    text = if (missing.isNotBlank()) "Missing: $missing" else "Missing required components.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { onAttempt(recipe.id) },
                enabled = recipe.canCraft,
                modifier = Modifier.heightIn(min = buttonHeight)
            ) {
                Text("Assemble")
            }
        }
    }
}
