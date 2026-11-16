package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.crafting.MealRules
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.launch

private fun formatChefName(id: String?): String = when (id?.lowercase()) {
    "gh0st" -> "Gh0st"
    null, "" -> "None"
    else -> id.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingScreen(
    craftingService: CraftingService,
    inventoryService: InventoryService,
    source: String?,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onCookAndEat: (suspend (String, String?, Int, Boolean) -> ItemUseController.Result)? = null,
    modifier: Modifier = Modifier,
    highContrastMode: Boolean = false,
    largeTouchTargets: Boolean = false
) {
    val inventory by inventoryService.state.collectAsState()
    val session by craftingService.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    var category by rememberSaveable { mutableStateOf("meal") }
    var query by rememberSaveable { mutableStateOf("") }
    var readyOnly by rememberSaveable { mutableStateOf(false) }
    var showAllRecipes by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMeal by remember { mutableStateOf<(() -> Unit)?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var showChefModal by rememberSaveable { mutableStateOf(false) }
    var detailRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBatch by rememberSaveable { mutableIntStateOf(1) }

    val chefs = craftingService.availableChefs()
    val selectedChef = session.mealChefId.takeIf { it in chefs } ?: chefs.firstOrNull()
    val text = FieldMenuDesign.text
    val muted = if (highContrastMode) text else FieldMenuDesign.textMuted
    val orange = FieldMenuDesign.ember
    val actionHeight = if (largeTouchTargets) 54.dp else 46.dp
    val largeFont = LocalDensity.current.fontScale >= 1.3f

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

    LaunchedEffect(recipes, category) {
        if (selectedRecipeId == null || recipes.none { it.id == selectedRecipeId }) {
            selectedRecipeId = recipes.firstOrNull()?.id
            selectedBatch = 1
        }
    }
    val selectedRecipe = recipes.firstOrNull { it.id == selectedRecipeId } ?: recipes.firstOrNull()

    // Active meal replacement confirmation dialog
    pendingMeal?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingMeal = null },
            containerColor = FieldMenuDesign.panel,
            titleContentColor = text,
            textContentColor = muted,
            title = { Text("Replace your active meal?") },
            text = { Text("Cook and eat one portion now. This replaces your current Well-Fed bonus; any extra portions stay in your inventory.") },
            confirmButton = {
                TextButton(onClick = { pendingMeal = null; action() }) {
                    Text("Replace meal", color = orange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMeal = null }) {
                    Text("Cancel", color = muted)
                }
            }
        )
    }

    // Companion Chef Selector Modal
    if (showChefModal) {
        Dialog(onDismissRequest = { showChefModal = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FieldMenuDesign.panel,
                border = BorderStroke(1.5.dp, orange.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SERVING COMPANION",
                                color = orange,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            )
                            Text(
                                text = "Choose who plates the meal",
                                color = text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = { showChefModal = false }) {
                            Icon(Icons.Default.Close, "Close", tint = muted)
                        }
                    }
                    Text(
                        text = "Adds a companion bonus to party Well-Fed meals for 3 battles.",
                        color = muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    chefs.forEach { id ->
                        val active = selectedChef == id
                        Surface(
                            onClick = {
                                craftingService.selectChef(id)
                                showChefModal = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) FieldMenuDesign.cookingActionFill else FieldMenuDesign.deepCard,
                            border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) orange else FieldMenuDesign.subtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = formatChefName(id),
                                        color = if (active) orange else text,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = MealRules.chefDescription(id),
                                        color = if (active) text else muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (active) {
                                    Icon(Icons.Default.Check, null, tint = orange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recipe Details Modal (Flavor text, full rules & cross-craft ingredient notes)
    detailRecipeId?.let { id ->
        val detailRecipe = craftingService.cookingRecipes.firstOrNull { it.id == id }
        if (detailRecipe != null) {
            Dialog(onDismissRequest = { detailRecipeId = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = FieldMenuDesign.panel,
                    border = BorderStroke(1.5.dp, orange.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detailRecipe.name,
                                color = text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { detailRecipeId = null }) {
                                Text("Close", color = orange)
                            }
                        }
                        detailRecipe.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(desc, color = text, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = FieldMenuDesign.subtleBorder)
                        Text(
                            text = craftingService.mealEffects(detailRecipe),
                            color = muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        val notes = craftingService.sharedIngredientNotes(detailRecipe)
                        if (notes.isNotEmpty()) {
                            HorizontalDivider(color = FieldMenuDesign.subtleBorder)
                            notes.forEach { note ->
                                Text(note, color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (highContrastMode) Color.Black else FieldMenuDesign.shell,
        snackbarHost = {}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            orange.copy(alpha = if (highContrastMode) 0f else 0.10f),
                            FieldMenuDesign.panel.copy(alpha = if (highContrastMode) 0f else 0.45f),
                            Color.Transparent
                        )
                    )
                )
                .padding(padding)
        ) {
            // 1. Compact Station Header + Meals/Snacks Segmented Bar
            Surface(
                color = if (highContrastMode) Color.Black else FieldMenuDesign.panel.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(10.dp),
                            color = FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                            modifier = Modifier.size(if (largeTouchTargets) 48.dp else 40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to exploration", tint = text)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "COOKING STATION",
                                color = orange.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            )
                            Text(
                                text = source ?: "Cooking",
                                color = text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Segmented Meals | Snacks toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(FieldMenuDesign.deepCard)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("meal" to "Meals", "snack" to "Snacks").forEach { (id, label) ->
                                val active = category == id
                                Surface(
                                    onClick = { category = id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) FieldMenuDesign.cookingActionFill else Color.Transparent,
                                    border = if (active) BorderStroke(1.dp, orange) else null
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) text else muted,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Compact 2-Pill Active Meal & Companion Chef Strip
                    if (category == "meal") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FieldMenuDesign.deepCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (session.activeMealBuff != null) orange.copy(alpha = 0.55f) else FieldMenuDesign.subtleBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    session.activeMealBuff?.let { meal ->
                                        Text(
                                            text = "Active: ${meal.recipeName} (${meal.remainingEncounters}⚔)",
                                            color = orange,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = MealRules.summary(meal),
                                            color = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } ?: run {
                                        Text(
                                            text = "No active party meal",
                                            color = muted,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Eat before battle for 3⚔ Well-Fed",
                                            color = muted.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { showChefModal = true },
                                shape = RoundedCornerShape(10.dp),
                                color = FieldMenuDesign.elevatedPanel,
                                border = BorderStroke(1.dp, orange.copy(alpha = 0.55f)),
                                modifier = Modifier.weight(0.95f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = "Chef: ${formatChefName(selectedChef)}",
                                            color = text,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = MealRules.chefDescription(selectedChef ?: ""),
                                            color = orange,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text("▾", color = orange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FieldMenuDesign.deepCard,
                            border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Snacks equip in a party member's Snack slot and recharge on cooldown without consuming portions.",
                                color = muted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Compact Search + Can Cook Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Find a recipe...", style = MaterialTheme.typography.bodySmall) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = text)
                        )
                        Surface(
                            onClick = { readyOnly = !readyOnly },
                            shape = RoundedCornerShape(10.dp),
                            color = if (readyOnly) FieldMenuDesign.primaryActionFill else FieldMenuDesign.deepCard,
                            border = BorderStroke(1.dp, if (readyOnly) FieldMenuDesign.mint else FieldMenuDesign.subtleBorder),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (readyOnly) {
                                    Icon(Icons.Default.Check, null, tint = FieldMenuDesign.mint, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = "Can cook",
                                    color = if (readyOnly) FieldMenuDesign.mint else muted,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = FieldMenuDesign.subtleBorder)

            // 2. Scannable Recipe List
            LazyColumn(
                modifier = Modifier
                    .testTag("cooking-recipes")
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recipes.isEmpty()) {
                    item {
                        Surface(
                            color = FieldMenuDesign.panel,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No recipes match. Try another category, search, or turn off 'Can cook'.",
                                color = muted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                items(recipes, key = { it.id }) { recipe ->
                    val isSelected = selectedRecipe?.id == recipe.id
                    val snack = recipe.category == "snack"
                    val discovered = craftingService.isRecipeDiscovered(recipe)
                    val owned = snack && inventory.any { it.item.id == recipe.result && it.quantity > 0 }
                    val activeBatch = if (snack) 1 else if (isSelected) selectedBatch else 1
                    val canCook = craftingService.canCook(recipe, activeBatch)
                    val resultItem = inventoryService.itemDetail(recipe.result)

                    val missingIngredients = recipe.ingredients.mapNotNull { (id, qty) ->
                        val required = qty * activeBatch
                        val avail = craftingService.availableForCraft(id)
                        if (avail < required) "${inventoryService.itemDisplayName(id)} ×${required - avail}" else null
                    }

                    val statusBadgeText = when {
                        !discovered -> "LOCKED"
                        owned -> "OWNED"
                        canCook -> "READY"
                        else -> "MISSING ${missingIngredients.size.coerceAtLeast(1)}"
                    }
                    val statusBadgeColor = when {
                        !discovered -> FieldMenuDesign.gold
                        owned -> FieldMenuDesign.cyan
                        canCook -> FieldMenuDesign.mint
                        else -> FieldMenuDesign.coral
                    }

                    Surface(
                        onClick = {
                            selectedRecipeId = recipe.id
                            feedback = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> orange
                                canCook -> FieldMenuDesign.mint.copy(alpha = 0.4f)
                                highContrastMode -> FieldMenuDesign.border
                                else -> FieldMenuDesign.subtleBorder
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(previewItemIconRes(recipe.result)),
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp)
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = recipe.name,
                                            color = text,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusBadgeColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = statusBadgeText,
                                                color = statusBadgeColor,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (!discovered) {
                                        Text(
                                            text = recipe.discoveryHint ?: "Find its cooking station to learn this recipe.",
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        val headlineEffect = if (!snack && resultItem != null) {
                                            val hpPart = resultItem.effect?.restoreHp?.let { "Restores $it HP · " }.orEmpty()
                                            val buffSummary = MealRules.summary(MealRules.buff(resultItem, selectedChef))
                                            "${hpPart}Well-Fed: $buffSummary"
                                        } else {
                                            craftingService.mealEffects(recipe).lineSequence().firstOrNull().orEmpty()
                                        }
                                        Text(
                                            text = headlineEffect,
                                            color = orange,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = if (largeFont) 2 else 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Direct Quick-Cook Button (preserves testTag("cook-${recipe.id}"))
                                if (discovered) {
                                    Button(
                                        onClick = {
                                            selectedRecipeId = recipe.id
                                            val outcome = craftingService.cookMeal(recipe.id, selectedChef, activeBatch)
                                            feedback = outcome.message
                                            if (outcome is CraftingOutcome.Success) onPlayAudio("cooking_sizzle")
                                        },
                                        enabled = canCook && !preparing,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (canCook) orange else FieldMenuDesign.subtleBorder
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FieldMenuDesign.cookingActionFill,
                                            contentColor = text,
                                            disabledContainerColor = FieldMenuDesign.deepCard,
                                            disabledContentColor = muted
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .testTag("cook-${recipe.id}")
                                            .heightIn(min = if (largeTouchTargets) 48.dp else 38.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                owned -> "Owned"
                                                snack -> "Craft"
                                                canCook -> "Cook ×${recipe.resultQuantity * activeBatch}"
                                                else -> "Missing"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            // Compact Ingredient Readiness Strip right on the row
                            if (discovered) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    recipe.ingredients.forEach { (id, quantity) ->
                                        val required = quantity * activeBatch
                                        val available = craftingService.availableForCraft(id)
                                        val hasEnough = available >= required
                                        val chipColor = if (hasEnough) FieldMenuDesign.mint else FieldMenuDesign.coral
                                        Surface(
                                            color = FieldMenuDesign.deepCard,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, chipColor.copy(alpha = if (hasEnough) 0.35f else 0.65f))
                                        ) {
                                            Text(
                                                text = "${inventoryService.itemDisplayName(id)} $available/$required" + if (hasEnough) " ✓" else "",
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (hasEnough) FontWeight.Medium else FontWeight.Bold
                                                ),
                                                color = if (hasEnough) text else FieldMenuDesign.coral
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (query.isBlank() && !readyOnly && (hiddenCount > 0 || showAllRecipes)) {
                    item {
                        OutlinedButton(
                            onClick = { showAllRecipes = !showAllRecipes },
                            modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
                        ) {
                            Text(
                                text = if (showAllRecipes) {
                                    "Show relevant recipes only"
                                } else {
                                    "Show all ${allCategoryRecipes.size} recipes ($hiddenCount more from other sectors)"
                                },
                                color = text
                            )
                        }
                    }
                }
            }

            // 3. Sizzle Result Banner (Anchored above the Prep Dock — non-blocking!)
            feedback?.let { message ->
                Surface(
                    color = FieldMenuDesign.cookingActionFill,
                    border = BorderStroke(1.5.dp, orange),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "At the cookfire",
                                color = orange,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = message,
                                color = text,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            onClick = { feedback = null },
                            shape = RoundedCornerShape(8.dp),
                            color = FieldMenuDesign.deepCard,
                            border = BorderStroke(1.dp, orange.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "Done",
                                color = orange,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4. Anchored Bottom Prep Dock for the Selected Recipe
            selectedRecipe?.let { recipe ->
                val snack = recipe.category == "snack"
                val discovered = craftingService.isRecipeDiscovered(recipe)
                val owned = snack && inventory.any { it.item.id == recipe.result && it.quantity > 0 }
                val batch = if (snack) 1 else selectedBatch
                val canCook = craftingService.canCook(recipe, batch)
                val missingList = recipe.ingredients.mapNotNull { (id, qty) ->
                    val req = qty * batch
                    val avail = craftingService.availableForCraft(id)
                    if (avail < req) "${inventoryService.itemDisplayName(id)} ×${req - avail}" else null
                }

                Surface(
                    color = FieldMenuDesign.panel,
                    border = BorderStroke(1.dp, orange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipe.name,
                                    color = text,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (!discovered) {
                                        "Undiscovered recipe"
                                    } else if (snack) {
                                        "Equippable combat snack"
                                    } else {
                                        "Yield: ${recipe.resultQuantity * batch} portion${if (recipe.resultQuantity * batch == 1) "" else "s"} · +15% masterwork chance"
                                    },
                                    color = muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (discovered) {
                                Surface(
                                    onClick = { detailRecipeId = recipe.id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = FieldMenuDesign.elevatedPanel,
                                    border = BorderStroke(1.dp, orange.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Details",
                                        color = orange,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        if (discovered && session.activeMealBuff != null && !snack) {
                            craftingService.mealComparison(recipe)?.let { comp ->
                                Text(
                                    text = comp,
                                    color = FieldMenuDesign.cyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (discovered && missingList.isNotEmpty()) {
                            Text(
                                text = "Missing: ${missingList.joinToString(", ")}",
                                color = FieldMenuDesign.coral,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (discovered && !snack) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Batch:", color = muted, style = MaterialTheme.typography.labelMedium)
                                listOf(1, 2, 3, 5).forEach { amount ->
                                    val active = selectedBatch == amount
                                    Surface(
                                        onClick = { selectedBatch = amount },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (active) FieldMenuDesign.cookingActionFill else FieldMenuDesign.deepCard,
                                        border = BorderStroke(1.dp, if (active) orange else FieldMenuDesign.subtleBorder)
                                    ) {
                                        Text(
                                            text = "×$amount",
                                            color = if (active) orange else text,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (discovered) {
                            fun prepareCookAndEat(replace: Boolean) {
                                preparing = true
                                scope.launch {
                                    try {
                                        val result = onCookAndEat?.invoke(recipe.id, selectedChef, batch, replace)
                                        if (result != null) {
                                            val message = when (result) {
                                                is ItemUseController.Result.Success -> {
                                                    onPlayAudio("cooking_sizzle")
                                                    result.message
                                                }
                                                is ItemUseController.Result.Failure -> result.message
                                            }
                                            feedback = message
                                        }
                                    } finally {
                                        preparing = false
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!snack && onCookAndEat != null) {
                                    OutlinedButton(
                                        onClick = {
                                            if (session.activeMealBuff != null) pendingMeal = { prepareCookAndEat(true) }
                                            else prepareCookAndEat(false)
                                        },
                                        enabled = canCook && !preparing,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = actionHeight),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (canCook) FieldMenuDesign.cyan else FieldMenuDesign.subtleBorder
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = text,
                                            disabledContentColor = muted
                                        )
                                    ) {
                                        Text(
                                            text = "Cook & eat (1)",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        val outcome = craftingService.cookMeal(recipe.id, selectedChef, batch)
                                        feedback = outcome.message
                                        if (outcome is CraftingOutcome.Success) onPlayAudio("cooking_sizzle")
                                    },
                                    enabled = canCook && !preparing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = actionHeight),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (canCook) orange else FieldMenuDesign.subtleBorder
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FieldMenuDesign.cookingActionFill,
                                        contentColor = text,
                                        disabledContainerColor = FieldMenuDesign.deepCard,
                                        disabledContentColor = muted
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            owned -> "Already owned · equip in Items"
                                            snack -> "Craft snack"
                                            else -> "Cook ${recipe.resultQuantity * batch} portion${if (recipe.resultQuantity * batch == 1) "" else "s"}"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

