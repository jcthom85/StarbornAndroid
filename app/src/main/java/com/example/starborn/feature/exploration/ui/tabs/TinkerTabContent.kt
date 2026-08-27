package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringItemChoice
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.exploration.ui.MenuSectionCard

enum class TinkerMenuSection { ALL, REPAIR, GEAR, SCRAP }

@Composable
fun TinkerTabContent(
    craftingViewModel: CraftingViewModel,
    accentColor: Color,
    borderColor: Color,
    onPlayAudio: (String) -> Unit = {}
) {
    val state by craftingViewModel.uiState.collectAsState()
    var section by rememberSaveable { mutableStateOf(TinkerMenuSection.ALL) }

    MenuSectionCard(
        title = "Tinkering & Assembly",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TinkerSectionToggle(
                current = section,
                onSelect = { selected ->
                    section = selected
                    when (selected) {
                        TinkerMenuSection.ALL -> craftingViewModel.setFilter(TinkeringFilter.ALL)
                        TinkerMenuSection.REPAIR -> craftingViewModel.setFilter(TinkeringFilter.REPAIR)
                        TinkerMenuSection.GEAR -> craftingViewModel.setFilter(TinkeringFilter.GEAR)
                        TinkerMenuSection.SCRAP -> Unit
                    }
                },
                accentColor = accentColor,
                borderColor = borderColor
            )

            // Resource Inventory Pill Summary
            CraftingMaterialSummaryBar(
                inventory = state.inventory,
                accentColor = accentColor,
                borderColor = borderColor
            )

            if (section == TinkerMenuSection.SCRAP) {
                ScrapListPanel(
                    scrapChoices = state.scrapChoices,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    onScrapItem = { itemId ->
                        craftingViewModel.scrap(itemId)
                        onPlayAudio("sfx_tinkering_wrench")
                    }
                )
            } else {
                RecipeListPanel(
                    recipes = state.learnedRecipes,
                    lockedRecipes = state.lockedRecipes,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    onCraftRecipe = { recipeId ->
                        craftingViewModel.craft(recipeId)
                        onPlayAudio("sfx_tinkering_wrench")
                    },
                    onAutoFill = { recipeId ->
                        craftingViewModel.autoFill(recipeId)
                    }
                )
            }
        }
    }
}

@Composable
private fun TinkerSectionToggle(
    current: TinkerMenuSection,
    onSelect: (TinkerMenuSection) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)), RoundedCornerShape(50.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TinkerToggleButton("All", current == TinkerMenuSection.ALL, accentColor, { onSelect(TinkerMenuSection.ALL) }, Modifier.weight(1f))
        TinkerToggleButton("Repair", current == TinkerMenuSection.REPAIR, accentColor, { onSelect(TinkerMenuSection.REPAIR) }, Modifier.weight(1f))
        TinkerToggleButton("Gear", current == TinkerMenuSection.GEAR, accentColor, { onSelect(TinkerMenuSection.GEAR) }, Modifier.weight(1f))
        TinkerToggleButton("Scrap", current == TinkerMenuSection.SCRAP, accentColor, { onSelect(TinkerMenuSection.SCRAP) }, Modifier.weight(1f))
    }
}

@Composable
private fun TinkerToggleButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .clickable { onClick() },
        color = background,
        shape = RoundedCornerShape(40.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                color = if (selected) Color.White else Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun CraftingMaterialSummaryBar(
    inventory: List<TinkeringItemChoice>,
    accentColor: Color,
    borderColor: Color
) {
    val parts = inventory.filter { it.quantity > 0 }.take(6)
    if (parts.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF061018).copy(alpha = 0.5f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PARTS:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor.copy(alpha = 0.85f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parts.forEach { part ->
                        Text(
                            text = "${part.name}: ${part.quantity}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeListPanel(
    recipes: List<TinkeringRecipeUi>,
    lockedRecipes: List<TinkeringRecipeUi>,
    accentColor: Color,
    borderColor: Color,
    onCraftRecipe: (String) -> Unit,
    onAutoFill: (String) -> Unit
) {
    if (recipes.isEmpty() && lockedRecipes.isEmpty()) {
        Text(
            text = "No schematics discovered yet.",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                accentColor = accentColor,
                borderColor = borderColor,
                onCraft = { onCraftRecipe(recipe.id) }
            )
        }
        if (lockedRecipes.isNotEmpty()) {
            item {
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF040A10).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Undiscovered Schematics (${lockedRecipes.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = if (expanded) "▲ Hide" else "▼ Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.75f)
                        )
                    }
                }
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lockedRecipes.forEach { locked ->
                            LockedRecipeCard(
                                recipe = locked,
                                borderColor = borderColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: TinkeringRecipeUi,
    accentColor: Color,
    borderColor: Color,
    onCraft: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color(0xFF061018).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, if (recipe.canCraft) accentColor.copy(alpha = 0.55f) else borderColor.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (recipe.canCraft) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    recipe.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCraft,
                    enabled = recipe.canCraft,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CRAFT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Ingredient Requirement Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recipe.ingredients.forEach { req ->
                    RequirementChip(req = req, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun RequirementChip(
    req: TinkeringRequirementStatus,
    accentColor: Color
) {
    val met = req.available >= req.required
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (met) accentColor.copy(alpha = 0.14f) else Color(0xFF7F1D1D).copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (met) accentColor.copy(alpha = 0.38f) else Color(0xFFEF4444).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (met) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (met) accentColor else Color(0xFFEF4444),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${req.label} ${req.available}/${req.required}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (met) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun LockedRecipeCard(
    recipe: TinkeringRecipeUi,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF040A10).copy(alpha = 0.4f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
            )
            Text(
                text = "Undiscovered Schematic",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun ScrapListPanel(
    scrapChoices: List<TinkeringItemChoice>,
    accentColor: Color,
    borderColor: Color,
    onScrapItem: (String) -> Unit
) {
    if (scrapChoices.isEmpty()) {
        Text(
            text = "No salvageable scrap in inventory.",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(scrapChoices, key = { it.id }) { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF061018).copy(alpha = 0.55f),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.name} (${item.quantity})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onScrapItem(item.id) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF6B6B)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SCRAP",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
