package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.starborn.feature.crafting.CookingRecipeUi
import com.example.starborn.feature.crafting.CookingUiState
import com.example.starborn.feature.crafting.CookingViewModel
import com.example.starborn.feature.crafting.IngredientUi
import com.example.starborn.feature.crafting.MinigameDifficulty
import com.example.starborn.feature.crafting.TimingMinigameUi
import com.example.starborn.feature.crafting.ui.TimingMinigameOverlay
import com.example.starborn.feature.crafting.ui.IngredientRow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingRoute(
    viewModel: CookingViewModel,
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

    CookingScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onCook = viewModel::startMinigame,
        onStopMinigame = viewModel::stopMinigame,
        onCancelMinigame = viewModel::cancelMinigame,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CookingScreen(
    state: CookingUiState,
    snackbarHostState: SnackbarHostState,
    onCook: (String) -> Unit,
    onStopMinigame: () -> Unit,
    onCancelMinigame: () -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val containerColor = if (highContrastMode) Color(0xFF040A0F) else MaterialTheme.colorScheme.background
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking Station") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        "No recipes available yet.",
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        CookingRecipeCard(
                            recipe = recipe,
                            onCook = onCook,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )
                    }
                }
            }

            state.activeMinigame?.let { minigame ->
                TimingMinigameOverlay(
                    minigame = minigame,
                    onStop = onStopMinigame,
                    onCancel = onCancelMinigame,
                    title = "Cook ${minigame.recipeName}",
                    instructions = "Tap anywhere inside the meter while the cursor is in the highlighted zone.",
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )
            }
        }
    }
}

@Composable
private fun CookingRecipeCard(
    recipe: CookingRecipeUi,
    onCook: (String) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (highContrastMode) Color(0xFF101620) else MaterialTheme.colorScheme.surface
    val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp, color = surfaceColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                recipe.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
            }
            val difficultyLabel = when (recipe.difficulty) {
                MinigameDifficulty.EASY -> "Easy"
                MinigameDifficulty.NORMAL -> "Normal"
                MinigameDifficulty.HARD -> "Hard"
            }
            Text(
                text = "Difficulty: $difficultyLabel",
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Ingredients",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
                recipe.ingredients.forEach { ingredient ->
                    IngredientRow(ingredient)
                }
            }
            Text(
                text = "Result: ${recipe.resultLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor
            )
            if (!recipe.canCook) {
                val missing = recipe.missingIngredients.joinToString()
                Text(
                    text = if (missing.isNotBlank()) "Missing: $missing" else "Missing required ingredients.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { onCook(recipe.id) },
                enabled = recipe.canCook,
                modifier = Modifier.heightIn(min = buttonHeight)
            ) {
                Text("Cook")
            }
        }
    }
}
