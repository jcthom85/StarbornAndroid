package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
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
    onClosed: () -> Unit
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
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TinkeringScreen(
    recipes: List<TinkeringRecipe>,
    snackbarHostState: SnackbarHostState,
    onCraft: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tinkering Table") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                    TinkeringRecipeCard(recipe = recipe, onCraft = onCraft)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TinkeringRecipeCard(
    recipe: TinkeringRecipe,
    onCraft: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
        recipe.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val ingredients = buildString {
            append("Base: ${recipe.base}")
            if (recipe.components.isNotEmpty()) {
                append(" | Components: ${recipe.components.joinToString()}")
            }
        }
        Text(ingredients, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onCraft(recipe.id) }) {
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
        onBack = {}
    )
}
