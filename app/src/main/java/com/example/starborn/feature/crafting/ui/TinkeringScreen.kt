package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringBenchState
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinkeringRoute(
    viewModel: CraftingViewModel,
    onBack: () -> Unit,
    onCrafted: (CraftingOutcome.Success) -> Unit,
    onClosed: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        launch {
            viewModel.messages.collectLatest { message ->
                snackbarHost.showSnackbar(message)
            }
        }
        launch {
            viewModel.craftResults.collectLatest { onCrafted(it) }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onClosed() }
    }

    TinkeringScreen(
        state = uiState,
        snackbarHostState = snackbarHost,
        onRecipeCraft = viewModel::craft,
        onBenchCraft = viewModel::craftFromBench,
        onAutoFillRecipe = viewModel::autoFill,
        onAutoFillBest = viewModel::autoFillBest,
        onClearBench = viewModel::clearBench,
        onFilterChange = viewModel::setFilter,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TinkeringScreen(
    state: TinkeringUiState,
    snackbarHostState: SnackbarHostState,
    onRecipeCraft: (String) -> Unit,
    onBenchCraft: () -> Unit,
    onAutoFillRecipe: (String) -> Unit,
    onAutoFillBest: () -> Unit,
    onClearBench: () -> Unit,
    onFilterChange: (TinkeringFilter) -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonMinHeight = if (largeTouchTargets) 52.dp else 0.dp
    val containerColor = if (highContrastMode) Color(0xFF050B12) else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tinkering Table") },
                navigationIcon = {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = buttonMinHeight)
                    ) { Text("Back") }
                }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                Text("Loading schematics...", style = MaterialTheme.typography.bodyMedium)
            } else {
                TinkeringBenchCard(
                    bench = state.bench,
                    largeTouchTargets = largeTouchTargets,
                    onCraft = onBenchCraft,
                    onAutoFillBest = onAutoFillBest,
                    onClear = onClearBench
                )
                RecipeFilterRow(
                    learnedCount = state.learnedRecipes.size,
                    lockedCount = state.lockedRecipes.size,
                    selected = state.filter,
                    onFilterChange = onFilterChange
                )
                val recipes = remember(state) {
                    when (state.filter) {
                        TinkeringFilter.LEARNED -> state.learnedRecipes
                        TinkeringFilter.ALL -> state.learnedRecipes + state.lockedRecipes
                    }
                }
                if (recipes.isEmpty()) {
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You haven't learned any schematics yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recipes, key = { it.id }) { recipe ->
                            TinkeringRecipeCard(
                                recipe = recipe,
                                onAutoFill = onAutoFillRecipe,
                                onCraft = onRecipeCraft,
                                highContrastMode = highContrastMode,
                                largeTouchTargets = largeTouchTargets
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TinkeringRecipeCard(
    recipe: TinkeringRecipeUi,
    onAutoFill: (String) -> Unit,
    onCraft: (String) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = titleColor)
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
            }
            Text(
                text = "Base: ${recipe.base}",
                style = MaterialTheme.typography.labelMedium,
                color = bodyColor
            )
            if (recipe.components.isNotEmpty()) {
                Text(
                    text = "Components: ${recipe.components.joinToString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = bodyColor
                )
            }
            val statusText = when {
                !recipe.learned -> "Locked schematic"
                recipe.canCraft -> "Ready to craft"
                else -> "Missing components"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    !recipe.learned -> MaterialTheme.colorScheme.error
                    recipe.canCraft -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onAutoFill(recipe.id) },
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Auto-Fill")
                }
                Button(
                    onClick = { onCraft(recipe.id) },
                    enabled = recipe.learned && recipe.canCraft,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Craft")
                }
            }
        }
    }
}

@Composable
private fun TinkeringBenchCard(
    bench: TinkeringBenchState,
    largeTouchTargets: Boolean,
    onCraft: () -> Unit,
    onAutoFillBest: () -> Unit,
    onClear: () -> Unit
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Workbench", style = MaterialTheme.typography.titleMedium)
            if (bench.activeRecipeId == null) {
                Text(
                    text = "Select a schematic to auto-fill the bench.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = bench.selectedBase?.let { "Base Item: $it" } ?: "Base Item not set",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (bench.selectedComponents.isNotEmpty()) {
                    Text(
                        text = "Components: ${bench.selectedComponents.joinToString { it.ifBlank { "—" } }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                RequirementList(bench.requirements)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAutoFillBest,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Auto-Fill Best")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = bench.activeRecipeId != null,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onCraft,
                    enabled = bench.canCraftSelection,
                    modifier = Modifier.heightIn(min = buttonHeight)
                ) {
                    Text("Craft Selection")
                }
            }
        }
    }
}

@Composable
private fun RequirementList(requirements: List<TinkeringRequirementStatus>) {
    if (requirements.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Requirements",
            style = MaterialTheme.typography.labelLarge
        )
        requirements.forEach { req ->
            val meetsRequirement = req.available >= req.required
            Text(
                text = "${req.label}: ${req.available}/${req.required}",
                color = if (meetsRequirement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecipeFilterRow(
    learnedCount: Int,
    lockedCount: Int,
    selected: TinkeringFilter,
    onFilterChange: (TinkeringFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = selected == TinkeringFilter.LEARNED,
            onClick = { onFilterChange(TinkeringFilter.LEARNED) },
            label = { Text("Learned ($learnedCount)") }
        )
        FilterChip(
            selected = selected == TinkeringFilter.ALL,
            onClick = { onFilterChange(TinkeringFilter.ALL) },
            label = { Text("All (${learnedCount + lockedCount})") }
        )
    }
}

@Preview
@Composable
fun TinkeringScreenPreview() {
    val previewState = TinkeringUiState(
        isLoading = false,
        learnedRecipes = listOf(
            TinkeringRecipeUi(
                id = "mod_power_lens_1",
                name = "Power Lens Mk. I",
                description = "Replaces a standard lens to boost energy output.",
                base = "Focusing Lens",
                components = listOf("Wiring Bundle"),
                canCraft = true,
                learned = true
            )
        ),
        lockedRecipes = listOf(
            TinkeringRecipeUi(
                id = "mod_ergonomic_grip_1",
                name = "Ergonomic Grip",
                description = "Improves stability.",
                base = "Ballistic Weave",
                components = listOf("Scrap Metal", "Scrap Metal"),
                canCraft = false,
                learned = false
            )
        ),
        bench = TinkeringBenchState(
            selectedBase = "Focusing Lens",
            selectedComponents = listOf("Wiring Bundle"),
            activeRecipeId = "mod_power_lens_1",
            requirements = listOf(
                TinkeringRequirementStatus("Focusing Lens", 1, 1),
                TinkeringRequirementStatus("Wiring Bundle", 1, 0)
            ),
            canCraftSelection = false
        )
    )
    TinkeringScreen(
        state = previewState,
        snackbarHostState = SnackbarHostState(),
        onRecipeCraft = {},
        onBenchCraft = {},
        onAutoFillRecipe = {},
        onAutoFillBest = {},
        onClearBench = {},
        onFilterChange = {},
        onBack = {},
        highContrastMode = false,
        largeTouchTargets = false
    )
}
