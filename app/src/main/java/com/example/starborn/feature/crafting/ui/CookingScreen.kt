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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.feature.crafting.CookingRecipeUi
import com.example.starborn.feature.crafting.CookingUiState
import com.example.starborn.feature.crafting.CookingViewModel
import com.example.starborn.feature.crafting.CookingWorkspaceState
import com.example.starborn.feature.crafting.IngredientUi
import com.example.starborn.feature.crafting.MinigameDifficulty
import com.example.starborn.feature.crafting.TimingMinigameUi
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
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
    var showRecipeBook by remember { mutableStateOf(false) }
    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.cookingstation_1,
        vignetteRes = R.drawable.cooking_vignette
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                StationHeader(
                    title = "Cooking Station",
                    iconRes = R.drawable.cooking_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )
            }
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
                        CookingHeroBanner(
                            workspace = state.workspace,
                            highContrastMode = highContrastMode,
                            onOpenRecipeBook = { showRecipeBook = true }
                        )
                        CookingWorkspaceCard(
                            workspace = state.workspace,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            onOpenRecipeBook = { showRecipeBook = true },
                            onCook = onCook,
                            onClear = onClearWorkspace
                        )
                        CookingTipsCard(
                            onOpenRecipeBook = { showRecipeBook = true },
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
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
}

@Composable
private fun CookingHeroBanner(
    workspace: CookingWorkspaceState,
    highContrastMode: Boolean,
    onOpenRecipeBook: () -> Unit
) {
    val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    val statusText: String
    val statusColor: Color
    when {
        workspace.recipe == null -> {
            statusText = "Load a dish"
            statusColor = MaterialTheme.colorScheme.secondary
        }
        workspace.canCook -> {
            statusText = "Pan is hot"
            statusColor = MaterialTheme.colorScheme.tertiary
        }
        else -> {
            statusText = "Need prep"
            statusColor = MaterialTheme.colorScheme.error
        }
    }
    Surface(
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        color = if (highContrastMode) Color(0xFF0B1119) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Field Kitchen", style = MaterialTheme.typography.labelSmall, color = bodyColor)
            Text(
                text = workspace.recipe?.name ?: "Bring a recipe to life",
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = workspace.recipe?.description?.takeIf { it.isNotBlank() }
                    ?: "Pick a dish, slot ingredients, hear the sizzle. Buffs, heals, and flavor sparks stack with your party loadout.",
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecipeStatusBadge(text = statusText, color = statusColor, highContrastMode = highContrastMode)
                OutlinedButton(onClick = onOpenRecipeBook) {
                    Text("Recipe Book")
                }
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
    val surfaceColor = if (highContrastMode) Color(0xFF0D1620) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val recipe = workspace.recipe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe?.name ?: "No recipe selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
                val statusText: String
                val statusColor: Color
                when {
                    recipe == null -> {
                        statusText = "Idle"
                        statusColor = MaterialTheme.colorScheme.secondary
                    }
                    workspace.canCook -> {
                        statusText = "Ready"
                        statusColor = MaterialTheme.colorScheme.tertiary
                    }
                    else -> {
                        statusText = "Missing"
                        statusColor = MaterialTheme.colorScheme.error
                    }
                }
                RecipeStatusBadge(text = statusText, color = statusColor, highContrastMode = highContrastMode)
            }

            if (recipe == null) {
                Text(
                    text = "Open the Recipe Book to choose a dish to prepare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recipe.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val difficultyLabel = when (recipe.difficulty) {
                        MinigameDifficulty.EASY -> "Easy"
                        MinigameDifficulty.NORMAL -> "Normal"
                        MinigameDifficulty.HARD -> "Hard"
                    }
                    Text(
                        text = "Difficulty: $difficultyLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Yields: ${recipe.resultLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrastMode) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Prep List", style = MaterialTheme.typography.labelSmall, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
                    workspace.ingredientSlots.forEach { IngredientMeter(it, highContrastMode) }
                }
                if (!workspace.canCook) {
                    val missing = workspace.missingIngredients.joinToString().takeIf { it.isNotBlank() }
                    Surface(
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        color = if (highContrastMode) Color(0xFF162130) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = missing?.let { "Missing: $it" } ?: "Missing required ingredients.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
private fun CookingTipsCard(
    onOpenRecipeBook: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        color = if (highContrastMode) Color(0xFF0C1520) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Prep Tips",
                style = MaterialTheme.typography.titleSmall,
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Stage ingredients first so the minigame only triggers when you’re ready. Perfect hits yield better dish bonuses.",
                style = MaterialTheme.typography.bodySmall,
                color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenRecipeBook,
                modifier = Modifier.heightIn(min = buttonHeight)
            ) {
                Text("Browse Recipes")
            }
        }
    }
}

@Composable
private fun IngredientMeter(ingredient: IngredientUi, highContrastMode: Boolean) {
    val meets = ingredient.available >= ingredient.required
    val progress = (ingredient.available.toFloat() / ingredient.required.toFloat()).coerceIn(0f, 1f)
    val track = if (highContrastMode) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val bar = if (meets) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                ingredient.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${ingredient.available}/${ingredient.required}",
                style = MaterialTheme.typography.bodySmall,
                color = if (meets) bar else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = progress,
            trackColor = track,
            color = bar,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

@Composable
private fun RecipeStatusBadge(text: String, color: Color, highContrastMode: Boolean) {
    Surface(
        color = if (highContrastMode) Color(0xFF111B28) else color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (highContrastMode) 0.dp else 1.dp
    ) {
        Text(
            text = text,
            color = if (highContrastMode) Color.White else color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
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
                        Surface(
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(14.dp),
                            color = if (highContrastMode) Color(0xFF0D1620) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        recipe.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val badgeText = if (recipe.canCook) "Ready" else "Missing"
                                    val badgeColor = if (recipe.canCook) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    RecipeStatusBadge(text = badgeText, color = badgeColor, highContrastMode = highContrastMode)
                                }
                                recipe.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val difficultyLabel = when (recipe.difficulty) {
                                    MinigameDifficulty.EASY -> "Easy"
                                    MinigameDifficulty.NORMAL -> "Normal"
                                    MinigameDifficulty.HARD -> "Hard"
                                }
                                Text(
                                    text = "Difficulty: $difficultyLabel • Yields: ${recipe.resultLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
