package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.feature.crafting.CraftingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.starborn.domain.crafting.CraftingOutcome

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
    val recipes = viewModel.recipes.collectAsState()
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
        recipes = recipes.value,
        snackbarHostState = snackbarHost,
        onCraft = viewModel::craft,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TinkeringScreen(
    recipes: List<TinkeringRecipe>,
    snackbarHostState: SnackbarHostState,
    onCraft: (String) -> Unit,
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
        if (recipes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "No tinkering schematics available yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    TinkeringRecipeCard(
                        recipe = recipe,
                        onCraft = onCraft,
                        highContrastMode = highContrastMode,
                        largeTouchTargets = largeTouchTargets
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TinkeringRecipeCard(
    recipe: TinkeringRecipe,
    onCraft: (String) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = titleColor)
        recipe.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val ingredients = buildString {
            append("Base: ${recipe.base}")
            if (recipe.components.isNotEmpty()) {
                append(" | Components: ${recipe.components.joinToString()}")
            }
        }
        Text(ingredients, style = MaterialTheme.typography.labelMedium, color = bodyColor)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onCraft(recipe.id) },
            modifier = Modifier.heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
        ) {
            Text("Craft")
        }
    }
}

@Preview
@Composable
fun TinkeringScreenPreview() {
    val dummy = listOf(
        TinkeringRecipe(
            id = "mod_power_lens_1",
            name = "Power Lens Mk. I",
            description = "Replaces a standard lens to boost energy output.",
            base = "Focusing Lens",
            components = listOf("Wiring Bundle"),
            result = "Power Lens Mk. I",
            successMessage = "You assemble a crisp new Power Lens Mk. I."
        )
    )
    TinkeringScreen(
        recipes = dummy,
        snackbarHostState = SnackbarHostState(),
        onCraft = {},
        onBack = {},
        highContrastMode = false,
        largeTouchTargets = false
    )
}
