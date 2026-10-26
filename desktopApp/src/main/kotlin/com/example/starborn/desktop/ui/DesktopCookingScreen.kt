package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.crafting.MealRules
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopCookingScreen(
    craftingService: CraftingService,
    inventoryService: InventoryService,
    source: String?,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onCookAndEat: (suspend (String, String?, Int, Boolean) -> ItemUseController.Result)? = null,
    modifier: Modifier = Modifier,
    highContrastMode: Boolean = false,
    largeTouchTargets: Boolean = false,
    characterName: (String) -> String = { it }
) {
    val inventory by inventoryService.state.collectAsState()
    val session by craftingService.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var category by rememberSaveable { mutableStateOf("meal") }
    var query by rememberSaveable { mutableStateOf("") }
    var readyOnly by rememberSaveable { mutableStateOf(false) }
    var showAllRecipes by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMeal by remember { mutableStateOf<(() -> Unit)?>(null) }
    var preparing by remember { mutableStateOf(false) }
    pendingMeal?.let { action ->
        AlertDialog(onDismissRequest = { pendingMeal = null },
            title = { Text("Replace your active meal?") },
            text = { Text("Cook and eat one portion. This replaces your current meal bonus; extra portions stay in inventory.") },
            confirmButton = { TextButton(onClick = { pendingMeal = null; action() }) { Text("Replace meal") } },
            dismissButton = { TextButton(onClick = { pendingMeal = null }) { Text("Cancel") } })
    }
    val chefs = craftingService.availableChefs()
    val selectedChef = session.mealChefId.takeIf { it in chefs } ?: chefs.firstOrNull()
    val text = FieldMenuDesign.text
    val muted = if (highContrastMode) text else FieldMenuDesign.textMuted
    val orange = Color(0xFFFFBD80)
    val actionHeight = if (largeTouchTargets) 56.dp else 48.dp
    val allCategoryRecipes = remember(inventory, session, category, query, readyOnly) {
        craftingService.cookingRecipes.filter {
            it.category == category && it.name.contains(query.trim(), true) && (!readyOnly || craftingService.canCook(it))
        }.sortedWith(
            compareByDescending<CookingRecipe> { craftingService.canCook(it) }
                .thenByDescending { craftingService.isRecipeRelevant(it) }
        )
    }
    val recipes = remember(allCategoryRecipes, query, showAllRecipes, readyOnly) {
        if (query.isNotBlank() || showAllRecipes || readyOnly) {
            allCategoryRecipes
        } else {
            val relevant = allCategoryRecipes.filter { craftingService.isRecipeRelevant(it) }
            relevant.ifEmpty { allCategoryRecipes }
        }
    }
    val hiddenCount = (allCategoryRecipes.size - recipes.size).coerceAtLeast(0)

    var selectedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRecipe = recipes.firstOrNull { it.id == selectedRecipeId } ?: recipes.firstOrNull()
    LaunchedEffect(selectedRecipe?.id) {
        if (selectedRecipeId == null) selectedRecipeId = selectedRecipe?.id
    }
    var batchAmounts by rememberSaveable { mutableStateOf(mapOf<String, Int>()) }
    @Composable
    fun RecipeDetails(recipe: CookingRecipe) {

        val batch = batchAmounts[recipe.id] ?: 1
        val snack = recipe.category == "snack"
        val discovered = craftingService.isRecipeDiscovered(recipe)
        val owned = snack && inventory.any { it.item.id == recipe.result && it.quantity > 0 }
        CookingPanel(highContrastMode) {
            Text(recipe.name, color = text, style = MaterialTheme.typography.titleMedium)
            if (!discovered) {
                Text("Recipe undiscovered", color = orange, style = MaterialTheme.typography.labelLarge)
                Text(recipe.discoveryHint ?: "Find its cooking station.", color = muted, style = MaterialTheme.typography.bodySmall)
            } else {
                recipe.description?.let { Text(it, color = muted, style = MaterialTheme.typography.bodySmall) }
                Text(craftingService.mealEffects(recipe), color = orange, style = MaterialTheme.typography.bodySmall)
                if (session.activeMealBuff != null) craftingService.mealComparison(recipe)?.let { Text(it, color = text, style = MaterialTheme.typography.bodySmall) }
                Text(if (snack) "Creates one equippable snack" else "Yield: ${recipe.resultQuantity * batch} portions · 15% bonus-portion chance per batch unit",
                    color = muted, style = MaterialTheme.typography.labelMedium)
                if (!snack) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3, 5).forEach { amount ->
                        OutlinedButton(onClick = { batchAmounts = batchAmounts + (recipe.id to amount) }, modifier = Modifier.desktopPointerHover().heightIn(min = actionHeight),
                            border = BorderStroke(1.dp, if (batch == amount) orange else muted)) { Text("×$amount", color = text) }
                    }
                }
                Text("Ingredients consumed", color = text, style = MaterialTheme.typography.labelLarge)
                recipe.ingredients.forEach { (id, quantity) ->
                    val required = quantity * if (snack) 1 else batch
                    val available = craftingService.availableForCraft(id)
                    Text("${inventoryService.itemDisplayName(id)} · $available/$required", style = MaterialTheme.typography.bodySmall,
                        color = if (available >= required) muted else Color(0xFFFFAA90))
                }
                craftingService.sharedIngredientNotes(recipe).forEach { note ->
                    Text(note, color = muted, style = MaterialTheme.typography.labelMedium)
                }
                val canCook = craftingService.canCook(recipe, if (snack) 1 else batch)
                if (!snack && onCookAndEat != null) {
                    fun prepare(replace: Boolean) {
                        preparing = true
                        scope.launch {
                            try {
                                val result = onCookAndEat(recipe.id, selectedChef, batch, replace)
                                val message = when (result) {
                                    is ItemUseController.Result.Success -> {
                                        onPlayAudio("cooking_sizzle")
                                        result.message
                                    }
                                    is ItemUseController.Result.Failure -> result.message
                                }
                                feedback = message
                                scope.launch { snackbar.showSnackbar(message) }
                            } finally { preparing = false }
                        }
                    }
                    OutlinedButton(onClick = {
                        if (session.activeMealBuff != null) pendingMeal = { prepare(true) }
                        else prepare(false)
                    }, enabled = canCook && !preparing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight)) {
                        Text("Cook and eat one portion")
                    }
                }
                Button(onClick = {
                    val outcome = craftingService.cookMeal(recipe.id, selectedChef, if (snack) 1 else batch)
                    feedback = outcome.message
                    if (outcome is CraftingOutcome.Success) onPlayAudio("cooking_sizzle")
                    scope.launch { snackbar.showSnackbar(outcome.message ?: "Cooking complete.") }
                }, enabled = canCook && !preparing,
                    modifier = Modifier.fillMaxWidth().desktopPointerHover(canCook).heightIn(min = actionHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = FieldMenuDesign.shell)) {
                    Text(if (owned) "Already owned · equip from Items" else if (snack) "Craft snack" else "Cook ${recipe.resultQuantity * batch} portions")
                }
            }
        }
        }
    @Composable
    fun RecipeBrowser(paneModifier: Modifier, expanded: Boolean) {
        LazyColumn(paneModifier.testTag("cooking-recipe-browser"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(actionHeight)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to exploration", tint = text)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(source ?: "Cooking", color = orange, style = MaterialTheme.typography.titleLarge)
                        Text("Meals for preparation · Snacks for equipped abilities", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                CookingPanel(highContrastMode) {
                    Text("Current meal", color = text, style = MaterialTheme.typography.titleSmall)
                    session.activeMealBuff?.let { meal ->
                        Text("${meal.recipeName} · ${meal.remainingEncounters} battles left", color = orange,
                            style = MaterialTheme.typography.bodySmall)
                        Text(MealRules.summary(meal), color = muted, style = MaterialTheme.typography.bodySmall)
                    } ?: Text(if (onCookAndEat != null) "No active meal. Cook and eat now, or use a prepared portion from Items before combat." else "No active meal. Eat a prepared portion from Items before combat.", color = muted,
                        style = MaterialTheme.typography.bodySmall)
                    Text("One party meal at a time. A new meal replaces the previous bonus. Victories and escapes spend one battle.",
                        color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("meal" to "Meals", "snack" to "Snacks").forEach { (id, label) ->
                        OutlinedButton(onClick = { category = id }, modifier = Modifier.weight(1f).desktopPointerHover().heightIn(min = actionHeight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (category == id) orange else muted),
                            border = BorderStroke(1.dp, if (category == id) orange else muted)) { Text(label) }
                    }
                }
            }
            if (category == "meal") item {
                CookingPanel(highContrastMode) {
                    Text("Serving companion", color = text, style = MaterialTheme.typography.titleSmall)
                    Text("This companion adds a party bonus when you eat a meal before combat. Cook stores portions; Cook and eat activates the meal and keeps any extra portions.",
                        color = muted, style = MaterialTheme.typography.bodySmall)
                    chefs.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { id ->
                                OutlinedButton(onClick = { craftingService.selectChef(id) },
                                    modifier = Modifier.weight(1f).desktopPointerHover().heightIn(min = actionHeight),
                                    border = BorderStroke(1.dp, if (selectedChef == id) orange else muted)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(characterName(id), color = text,
                                            style = MaterialTheme.typography.bodySmall)
                                        Text(MealRules.chefDescription(id), color = if (selectedChef == id) orange else muted,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else item {
                Text("Craft one to obtain an equippable snack. Its combat ability uses a cooldown and does not consume portions. Already-owned snacks do not need to be crafted again.",
                    color = muted, style = MaterialTheme.typography.bodySmall)
            }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Find a recipe") })
                Row(Modifier.fillMaxWidth().heightIn(min = actionHeight), verticalAlignment = Alignment.CenterVertically) {
                    Text("Can cook", Modifier.weight(1f), color = text, style = MaterialTheme.typography.bodySmall)
                    Switch(readyOnly, { readyOnly = it })
                }
            }
            feedback?.let { message -> item {
                CookingPanel(highContrastMode) {
                    Text(message, color = text, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { feedback = null }, modifier = Modifier.heightIn(min = actionHeight)) { Text("Dismiss") }
                }
            } }
            if (recipes.isEmpty()) item {
                Text("No recipes match. Try another category, search, or filter.", color = muted, style = MaterialTheme.typography.bodySmall)
            }
            items(recipes, key = { it.id }) { recipe ->
                if (expanded) RecipeDetails(recipe) else {
                    val discovered = craftingService.isRecipeDiscovered(recipe)
                    Surface(onClick = { selectedRecipeId = recipe.id },
                        modifier = Modifier.fillMaxWidth().testTag("cooking-select-${recipe.id}"),
                        color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selectedRecipe?.id == recipe.id) orange else FieldMenuDesign.border)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(recipe.name, color = text, style = MaterialTheme.typography.titleSmall)
                            Text(when {
                                !discovered -> "Undiscovered"
                                recipe.category == "snack" && inventory.any { it.item.id == recipe.result && it.quantity > 0 } -> "Already owned - equip from Items"
                                craftingService.canCook(recipe) -> "Ready for one batch"
                                else -> "Missing ingredients"
                            },
                                color = if (craftingService.canCook(recipe)) orange else muted,
                                style = MaterialTheme.typography.labelMedium)
                            if (discovered) {
                                val item = inventoryService.itemDetail(recipe.result)
                                val summary = if (recipe.category == "meal" && item != null)
                                    MealRules.summary(MealRules.buff(item, selectedChef)) else recipe.description.orEmpty()
                                Text(summary, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(if (recipe.category == "meal") "${recipe.resultQuantity} portions per batch" else "Equippable snack",
                                    color = muted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            if (query.isBlank() && !readyOnly && (hiddenCount > 0 || showAllRecipes)) {
                item {
                    OutlinedButton(
                        onClick = { showAllRecipes = !showAllRecipes },
                        modifier = Modifier.fillMaxWidth().desktopPointerHover().heightIn(min = actionHeight),
                        border = BorderStroke(1.dp, muted)
                    ) {
                        Text(
                            text = if (showAllRecipes) {
                                "Show relevant recipes only"
                            } else {
                                "Show all ${allCategoryRecipes.size} recipes ($hiddenCount more from other sectors & waters)"
                            },
                            color = text
                        )
                    }
                }
            }
        }
    }
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = FieldMenuDesign.shell,
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= 900.dp) {
                DesktopStationPanes(Modifier.padding(16.dp), primary = { paneModifier ->
                    RecipeBrowser(paneModifier, expanded = false)
                }, detail = { paneModifier ->
                    LazyColumn(paneModifier.testTag("cooking-recipe-details"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        selectedRecipe?.let { recipe -> item(key = recipe.id) { RecipeDetails(recipe) } }
                            ?: item { Text("Choose a recipe from the list.", color = muted) }
                    }
                })
            } else RecipeBrowser(Modifier.fillMaxSize(), expanded = true)
        }
    }
}

@Composable
private fun CookingPanel(highContrast: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp),
        border = if (highContrast) BorderStroke(1.dp, FieldMenuDesign.border) else null) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
