package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.crafting.CookingRecipeUi
import com.example.starborn.feature.crafting.CookingUiState
import com.example.starborn.feature.crafting.CookingViewModel
import com.example.starborn.feature.crafting.CookingWorkspaceState
import com.example.starborn.feature.crafting.IngredientUi
import com.example.starborn.feature.crafting.MinigameDifficulty
import com.example.starborn.feature.crafting.TimingMinigameUi
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
        onCook = viewModel::cookWorkspaceRecipe,
        onSelectRecipe = viewModel::selectWorkspaceRecipe,
        onClearWorkspace = viewModel::clearWorkspace,
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
    onCook: () -> Unit,
    onSelectRecipe: (String) -> Unit,
    onClearWorkspace: () -> Unit,
    onStopMinigame: () -> Unit,
    onCancelMinigame: () -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val containerColor = if (highContrastMode) Color(0xFF040A0F) else MaterialTheme.colorScheme.background
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    var showRecipeBook by remember { mutableStateOf(false) }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CookingWorkspaceCard(
                        workspace = state.workspace,
                        highContrastMode = highContrastMode,
                        largeTouchTargets = largeTouchTargets,
                        onOpenRecipeBook = { showRecipeBook = true },
                        onCook = onCook,
                        onClear = onClearWorkspace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tips",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Use the Recipe Book to stage dishes. Ingredients are consumed when you start the minigame.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { showRecipeBook = true },
                                modifier = Modifier.heightIn(min = buttonHeight)
                            ) {
                                Text("Open Recipe Book")
                            }
                        }
                    }
                }
            }

            if (showRecipeBook) {
                RecipeBookDialog(
                    recipes = state.recipes,
                    onSelectRecipe = {
                        onSelectRecipe(it)
                        showRecipeBook = false
                    },
                    onDismiss = { showRecipeBook = false },
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )
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
private fun CookingWorkspaceCard(
    workspace: CookingWorkspaceState,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onOpenRecipeBook: () -> Unit,
    onCook: () -> Unit,
    onClear: () -> Unit
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val recipe = workspace.recipe
            if (recipe == null) {
                Text(
                    text = "No recipe selected",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Open the Recipe Book to choose a dish to prepare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
                recipe.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val difficultyLabel = when (recipe.difficulty) {
                    MinigameDifficulty.EASY -> "Easy"
                    MinigameDifficulty.NORMAL -> "Normal"
                    MinigameDifficulty.HARD -> "Hard"
                }
                Text(
                    text = "Difficulty: $difficultyLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ingredients", style = MaterialTheme.typography.labelSmall)
                    workspace.ingredientSlots.forEach { IngredientRow(it) }
                }
                if (!workspace.canCook) {
                    val missing = workspace.missingIngredients.joinToString().takeIf { it.isNotBlank() }
                    Text(
                        text = missing?.let { "Missing: $it" } ?: "Missing required ingredients.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onOpenRecipeBook,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Recipe Book")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = workspace.recipe != null,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onCook,
                    enabled = workspace.canCook,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Cook")
                }
            }
        }
    }
}

@Composable
private fun RecipeBookDialog(
    recipes: List<CookingRecipeUi>,
    onSelectRecipe: (String) -> Unit,
    onDismiss: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Recipe Book") },
        text = {
            if (recipes.isEmpty()) {
                Text("No recipes learned yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    recipe.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                recipe.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val missing = recipe.missingIngredients.joinToString().takeIf { it.isNotBlank() }
                                if (missing != null) {
                                    Text(
                                        text = "Missing: $missing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Button(
                                    onClick = { onSelectRecipe(recipe.id) },
                                    modifier = Modifier.heightIn(min = buttonHeight)
                                ) {
                                    Text("Load Recipe")
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Close")
            }
        }
    )
}
