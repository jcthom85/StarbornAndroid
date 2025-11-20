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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.starborn.R
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringBenchState
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringUiState
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
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
    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.tinkeringtable_1,
        vignetteRes = R.drawable.tinkering_vignette
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                StationHeader(
                    title = "Tinkering Table",
                    iconRes = R.drawable.tinkering_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )
            }
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
                    val readyCount = remember(state.learnedRecipes) {
                        state.learnedRecipes.count { it.canCraft }
                    }
                    TinkeringStatsRow(
                        learned = state.learnedRecipes.size,
                        craftable = readyCount,
                        locked = state.lockedRecipes.size,
                        highContrastMode = highContrastMode
                    )
                    TinkeringBenchCard(
                        bench = state.bench,
                        highContrastMode = highContrastMode,
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Surface(
            color = if (highContrastMode) Color(0xFF0E1623) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = titleColor)
                        recipe.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
                        }
                    }
                    val statusText: String
                    val statusColor: Color
                    when {
                        !recipe.learned -> {
                            statusText = "Locked"
                            statusColor = MaterialTheme.colorScheme.error
                        }
                        recipe.canCraft -> {
                            statusText = "Ready"
                            statusColor = MaterialTheme.colorScheme.tertiary
                        }
                        else -> {
                            statusText = "Need parts"
                            statusColor = MaterialTheme.colorScheme.primary
                        }
                    }
                    StatusPill(text = statusText, color = statusColor, highContrastMode = highContrastMode)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemPill(label = "Base", value = recipe.base, highContrastMode = highContrastMode)
                    if (recipe.components.isNotEmpty()) {
                        ItemPill(
                            label = "Components",
                            value = recipe.components.joinToString(),
                            highContrastMode = highContrastMode
                        )
                    }
                }
                Surface(
                    color = if (highContrastMode) Color(0xFF121A28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = if (highContrastMode) 0.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Result: ${recipe.name} crafted at the bench.",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
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
}

@Composable
private fun TinkeringBenchCard(
    bench: TinkeringBenchState,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onCraft: () -> Unit,
    onAutoFillBest: () -> Unit,
    onClear: () -> Unit
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        color = if (highContrastMode) Color(0xFF0C131D) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Workbench", style = MaterialTheme.typography.titleMedium, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Lay out your base item and parts, then seal the mod.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val statusText: String
                val statusColor: Color
                when {
                    bench.activeRecipeId == null -> {
                        statusText = "Idle"
                        statusColor = MaterialTheme.colorScheme.secondary
                    }
                    bench.canCraftSelection -> {
                        statusText = "Ready"
                        statusColor = MaterialTheme.colorScheme.tertiary
                    }
                    else -> {
                        statusText = "Missing"
                        statusColor = MaterialTheme.colorScheme.error
                    }
                }
                StatusPill(text = statusText, color = statusColor, highContrastMode = highContrastMode)
            }
            if (bench.activeRecipeId == null) {
                Surface(
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (highContrastMode) Color(0xFF111B27) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Select a schematic from the list to auto-fill the bench.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                bench.selectedBase?.let {
                    ItemPill(label = "Base", value = it, highContrastMode = highContrastMode)
                }
                if (bench.selectedComponents.isNotEmpty()) {
                    ItemPill(
                        label = "Components",
                        value = bench.selectedComponents.joinToString { it.ifBlank { "—" } },
                        highContrastMode = highContrastMode
                    )
                }
                RequirementList(bench.requirements, highContrastMode)
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
private fun RequirementList(
    requirements: List<TinkeringRequirementStatus>,
    highContrastMode: Boolean
) {
    if (requirements.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Requirements",
            style = MaterialTheme.typography.labelLarge
        )
        requirements.forEach { req ->
            val meetsRequirement = req.available >= req.required
            val ratio = (req.available.toFloat() / req.required.toFloat()).coerceIn(0f, 1f)
            val track = if (highContrastMode) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            val bar = if (meetsRequirement) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = req.label,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${req.available}/${req.required}",
                        color = if (meetsRequirement) bar else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = ratio,
                    trackColor = track,
                    color = bar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
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

@Composable
private fun TinkeringStatsRow(
    learned: Int,
    craftable: Int,
    locked: Int,
    highContrastMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill(label = "Ready", value = craftable, color = MaterialTheme.colorScheme.tertiary, highContrastMode = highContrastMode, modifier = Modifier.weight(1f))
        StatPill(label = "Learned", value = learned, color = MaterialTheme.colorScheme.primary, highContrastMode = highContrastMode, modifier = Modifier.weight(1f))
        StatPill(label = "Locked", value = locked, color = MaterialTheme.colorScheme.error, highContrastMode = highContrastMode, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatPill(
    label: String,
    value: Int,
    color: Color,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val surface = if (highContrastMode) Color(0xFF0F1722) else color.copy(alpha = 0.14f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (highContrastMode) 0.dp else 2.dp,
        color = surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (highContrastMode) Color.White else color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highContrastMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, highContrastMode: Boolean) {
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
private fun ItemPill(label: String, value: String, highContrastMode: Boolean) {
    Surface(
        color = if (highContrastMode) Color(0xFF101826) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (highContrastMode) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
        }
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
