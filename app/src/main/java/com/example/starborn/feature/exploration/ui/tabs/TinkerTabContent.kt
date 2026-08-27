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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringBenchState
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringItemChoice
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.exploration.ui.MenuSectionCard

enum class TinkerTabMode { WORKBENCH, SCHEMATICS, SCRAP }
private enum class SlotPickerTarget { MAIN, COMPONENT_1, COMPONENT_2 }

@Composable
fun TinkerTabContent(
    craftingViewModel: CraftingViewModel,
    accentColor: Color,
    borderColor: Color,
    onPlayAudio: (String) -> Unit = {}
) {
    val state by craftingViewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(TinkerTabMode.WORKBENCH) }
    var pickerTarget by remember { mutableStateOf<SlotPickerTarget?>(null) }

    MenuSectionCard(
        title = "Tinkering & Assembly",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TinkerModeToggle(
                current = mode,
                onSelect = { mode = it },
                accentColor = accentColor,
                borderColor = borderColor
            )

            // Top Parts Bar
            CraftingMaterialSummaryBar(
                inventory = state.inventory,
                accentColor = accentColor,
                borderColor = borderColor
            )

            when (mode) {
                TinkerTabMode.WORKBENCH -> {
                    WorkbenchDiscoveryPanel(
                        bench = state.bench,
                        learnedRecipes = state.learnedRecipes,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onSelectMain = { pickerTarget = SlotPickerTarget.MAIN },
                        onSelectComp1 = { pickerTarget = SlotPickerTarget.COMPONENT_1 },
                        onSelectComp2 = { pickerTarget = SlotPickerTarget.COMPONENT_2 },
                        onClearBench = { craftingViewModel.clearBench() },
                        onAssemble = {
                            craftingViewModel.craftFromBench()
                            onPlayAudio("sfx_tinkering_wrench")
                        },
                        onAutoFillRecipe = { recipeId ->
                            craftingViewModel.autoFill(recipeId)
                        }
                    )
                }
                TinkerTabMode.SCHEMATICS -> {
                    SchematicsCatalogPanel(
                        recipes = state.learnedRecipes,
                        lockedRecipes = state.lockedRecipes,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onLoadRecipe = { recipeId ->
                            craftingViewModel.autoFill(recipeId)
                            mode = TinkerTabMode.WORKBENCH
                        },
                        onCraftDirect = { recipeId ->
                            craftingViewModel.craft(recipeId)
                            onPlayAudio("sfx_tinkering_wrench")
                        }
                    )
                }
                TinkerTabMode.SCRAP -> {
                    ScrapListPanel(
                        scrapChoices = state.scrapChoices,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onScrapItem = { itemId ->
                            craftingViewModel.scrap(itemId)
                            onPlayAudio("sfx_tinkering_wrench")
                        }
                    )
                }
            }
        }
    }

    // Slot Item Picker Modal
    pickerTarget?.let { target ->
        SlotItemPickerDialog(
            title = when (target) {
                SlotPickerTarget.MAIN -> "Select Base Item"
                SlotPickerTarget.COMPONENT_1 -> "Select Component 1"
                SlotPickerTarget.COMPONENT_2 -> "Select Component 2"
            },
            choices = state.inventory,
            accentColor = accentColor,
            borderColor = borderColor,
            onSelect = { itemId ->
                when (target) {
                    SlotPickerTarget.MAIN -> craftingViewModel.selectMain(itemId)
                    SlotPickerTarget.COMPONENT_1 -> craftingViewModel.selectComponent(0, itemId)
                    SlotPickerTarget.COMPONENT_2 -> craftingViewModel.selectComponent(1, itemId)
                }
                pickerTarget = null
            },
            onClear = {
                when (target) {
                    SlotPickerTarget.MAIN -> craftingViewModel.selectMain(null)
                    SlotPickerTarget.COMPONENT_1 -> craftingViewModel.selectComponent(0, null)
                    SlotPickerTarget.COMPONENT_2 -> craftingViewModel.selectComponent(1, null)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }
}

@Composable
private fun TinkerModeToggle(
    current: TinkerTabMode,
    onSelect: (TinkerTabMode) -> Unit,
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
        TinkerModeButton("Workbench", current == TinkerTabMode.WORKBENCH, accentColor, { onSelect(TinkerTabMode.WORKBENCH) }, Modifier.weight(1f))
        TinkerModeButton("Schematics", current == TinkerTabMode.SCHEMATICS, accentColor, { onSelect(TinkerTabMode.SCHEMATICS) }, Modifier.weight(1f))
        TinkerModeButton("Scrap", current == TinkerTabMode.SCRAP, accentColor, { onSelect(TinkerTabMode.SCRAP) }, Modifier.weight(1f))
    }
}

@Composable
private fun TinkerModeButton(
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
private fun WorkbenchDiscoveryPanel(
    bench: TinkeringBenchState,
    learnedRecipes: List<TinkeringRecipeUi>,
    accentColor: Color,
    borderColor: Color,
    onSelectMain: () -> Unit,
    onSelectComp1: () -> Unit,
    onSelectComp2: () -> Unit,
    onClearBench: () -> Unit,
    onAssemble: () -> Unit,
    onAutoFillRecipe: (String) -> Unit
) {
    val preview = bench.preview
    val hasItems = bench.mainItemId != null || bench.componentIds.any { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Interactive Assembly Slots
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF061018).copy(alpha = 0.65f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPERIMENTATION BENCH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    if (hasItems) {
                        TextButton(
                            onClick = onClearBench,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // 3 Slots Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkbenchSlotTile(
                        label = "Base Item",
                        itemName = bench.mainItemName,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = onSelectMain,
                        modifier = Modifier.weight(1f)
                    )
                    WorkbenchSlotTile(
                        label = "Component 1",
                        itemName = bench.componentNames.getOrNull(0)?.takeIf { it.isNotBlank() },
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = onSelectComp1,
                        modifier = Modifier.weight(1f)
                    )
                    WorkbenchSlotTile(
                        label = "Component 2",
                        itemName = bench.componentNames.getOrNull(1)?.takeIf { it.isNotBlank() },
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = onSelectComp2,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Reaction / Discovery Outcome Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (preview != null) accentColor.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, if (preview != null) accentColor.copy(alpha = 0.45f) else borderColor.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (preview != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (preview.learned) "RECIPE: ${preview.name}" else "★ DISCOVERY: ${preview.name}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (preview.learned) Color.White else Color(0xFFFFC857)
                                    )
                                    preview.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = onAssemble,
                                    enabled = bench.canCraftSelection,
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
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ASSEMBLE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            if (bench.requirements.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    bench.requirements.forEach { req ->
                                        RequirementChip(req = req, accentColor = accentColor)
                                    }
                                }
                            }
                        } else if (hasItems) {
                            Text(
                                text = "No reaction between these components. Try combining other parts!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = "Insert a base item and components to experiment and discover new blueprints.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Recipe Loader
        if (learnedRecipes.isNotEmpty()) {
            Text(
                text = "QUICK LOAD KNOWN SCHEMATICS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(learnedRecipes, key = { it.id }) { recipe ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAutoFillRecipe(recipe.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF061018).copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipe.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = recipe.ingredients.joinToString { "${it.label} (${it.available}/${it.required})" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Load →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkbenchSlotTile(
    label: String,
    itemName: String?,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filled = !itemName.isNullOrBlank()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .height(72.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (filled) accentColor.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (filled) accentColor.copy(alpha = 0.6f) else borderColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = accentColor.copy(alpha = 0.75f)
            )
            Text(
                text = itemName ?: "+ Insert",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (filled) Color.White else Color.White.copy(alpha = 0.4f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SchematicsCatalogPanel(
    recipes: List<TinkeringRecipeUi>,
    lockedRecipes: List<TinkeringRecipeUi>,
    accentColor: Color,
    borderColor: Color,
    onLoadRecipe: (String) -> Unit,
    onCraftDirect: (String) -> Unit
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF061018).copy(alpha = 0.65f),
                border = BorderStroke(1.dp, if (recipe.canCraft) accentColor.copy(alpha = 0.55f) else borderColor.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
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
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onLoadRecipe(recipe.id) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "BENCH",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                            }
                            Button(
                                onClick = { onCraftDirect(recipe.id) },
                                enabled = recipe.canCraft,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                                    disabledContentColor = Color.White.copy(alpha = 0.35f)
                               ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CRAFT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

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
                    }
                }
            }
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
private fun SlotItemPickerDialog(
    title: String,
    choices: List<TinkeringItemChoice>,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF071018),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                if (choices.isEmpty()) {
                    Text(
                        text = "No compatible items in inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(choices, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelect(item.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0A1624).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
                                    Text(
                                        text = "x${item.quantity}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClear) {
                        Text(text = "Clear Slot", color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}
