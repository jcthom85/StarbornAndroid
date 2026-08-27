package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringBenchState
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringItemChoice
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes

enum class TinkerTabMode { WORKBENCH, SCHEMATICS, SCRAP }
enum class ActiveBenchSlot { BASE, COMPONENT_1, COMPONENT_2 }

@Composable
fun TinkerTabContent(
    craftingViewModel: CraftingViewModel,
    accentColor: Color,
    borderColor: Color,
    onPlayAudio: (String) -> Unit = {}
) {
    val state by craftingViewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(TinkerTabMode.WORKBENCH) }
    var activeSlot by remember { mutableStateOf(ActiveBenchSlot.BASE) }

    MenuSectionCard(
        title = "Tinkering & Assembly",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Sub-Tab Toggles
            TinkerModeToggle(
                current = mode,
                onSelect = { mode = it },
                accentColor = accentColor,
                borderColor = borderColor
            )

            when (mode) {
                TinkerTabMode.WORKBENCH -> {
                    WorkbenchTerminalView(
                        bench = state.bench,
                        inventory = state.inventory,
                        learnedRecipes = state.learnedRecipes,
                        activeSlot = activeSlot,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onSelectSlot = { activeSlot = it },
                        onClearSlot = { slot ->
                            when (slot) {
                                ActiveBenchSlot.BASE -> craftingViewModel.selectMain(null)
                                ActiveBenchSlot.COMPONENT_1 -> craftingViewModel.selectComponent(0, null)
                                ActiveBenchSlot.COMPONENT_2 -> craftingViewModel.selectComponent(1, null)
                            }
                        },
                        onClearBench = {
                            craftingViewModel.clearBench()
                            activeSlot = ActiveBenchSlot.BASE
                        },
                        onItemTapped = { itemId ->
                            // Insert into currently active slot and auto-advance
                            when (activeSlot) {
                                ActiveBenchSlot.BASE -> {
                                    craftingViewModel.selectMain(itemId)
                                    if (state.bench.componentIds.getOrNull(0).isNullOrBlank()) {
                                        activeSlot = ActiveBenchSlot.COMPONENT_1
                                    } else if (state.bench.componentIds.getOrNull(1).isNullOrBlank()) {
                                        activeSlot = ActiveBenchSlot.COMPONENT_2
                                    }
                                }
                                ActiveBenchSlot.COMPONENT_1 -> {
                                    craftingViewModel.selectComponent(0, itemId)
                                    if (state.bench.componentIds.getOrNull(1).isNullOrBlank()) {
                                        activeSlot = ActiveBenchSlot.COMPONENT_2
                                    }
                                }
                                ActiveBenchSlot.COMPONENT_2 -> {
                                    craftingViewModel.selectComponent(1, itemId)
                                }
                            }
                        },
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
            .clip(RoundedCornerShape(50.dp))
            .background(Color(0xFF040A10).copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.45f)), RoundedCornerShape(50.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
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
            modifier = Modifier.padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WorkbenchTerminalView(
    bench: TinkeringBenchState,
    inventory: List<TinkeringItemChoice>,
    learnedRecipes: List<TinkeringRecipeUi>,
    activeSlot: ActiveBenchSlot,
    accentColor: Color,
    borderColor: Color,
    onSelectSlot: (ActiveBenchSlot) -> Unit,
    onClearSlot: (ActiveBenchSlot) -> Unit,
    onClearBench: () -> Unit,
    onItemTapped: (String) -> Unit,
    onAssemble: () -> Unit,
    onAutoFillRecipe: (String) -> Unit
) {
    val preview = bench.preview
    val hasItems = bench.mainItemId != null || bench.componentIds.any { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // TOP CHAMBER: Interactive Sockets + Reaction Result
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF061018).copy(alpha = 0.75f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNTHESIS CHAMBER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = accentColor
                    )
                    if (hasItems) {
                        Surface(
                            onClick = onClearBench,
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // 3 Illuminated Sockets connected by Energy Nodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Socket 1: Base Item
                    BenchSocket(
                        label = "BASE ITEM",
                        itemName = bench.mainItemName,
                        itemId = bench.mainItemId,
                        isActive = activeSlot == ActiveBenchSlot.BASE,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = { onSelectSlot(ActiveBenchSlot.BASE) },
                        onUnslot = { onClearSlot(ActiveBenchSlot.BASE) },
                        modifier = Modifier.weight(1f)
                    )

                    // Energy Bridge 1
                    EnergyConnectorNode(accentColor = accentColor)

                    // Socket 2: Component 1
                    BenchSocket(
                        label = "PART A",
                        itemName = bench.componentNames.getOrNull(0)?.takeIf { it.isNotBlank() },
                        itemId = bench.componentIds.getOrNull(0)?.takeIf { it.isNotBlank() },
                        isActive = activeSlot == ActiveBenchSlot.COMPONENT_1,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = { onSelectSlot(ActiveBenchSlot.COMPONENT_1) },
                        onUnslot = { onClearSlot(ActiveBenchSlot.COMPONENT_1) },
                        modifier = Modifier.weight(1f)
                    )

                    // Energy Bridge 2
                    EnergyConnectorNode(accentColor = accentColor)

                    // Socket 3: Component 2
                    BenchSocket(
                        label = "PART B",
                        itemName = bench.componentNames.getOrNull(1)?.takeIf { it.isNotBlank() },
                        itemId = bench.componentIds.getOrNull(1)?.takeIf { it.isNotBlank() },
                        isActive = activeSlot == ActiveBenchSlot.COMPONENT_2,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = { onSelectSlot(ActiveBenchSlot.COMPONENT_2) },
                        onUnslot = { onClearSlot(ActiveBenchSlot.COMPONENT_2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Holographic Reaction Readout
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (preview != null) accentColor.copy(alpha = 0.12f) else Color(0xFF03070C).copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (preview != null) Color(0xFFFFC857).copy(alpha = 0.6f) else borderColor.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                                        text = if (preview.learned) "BLUEPRINT: ${preview.name}" else "★ DISCOVERY: ${preview.name}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (preview.learned) Color.White else Color(0xFFFFC857)
                                    )
                                    preview.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onAssemble,
                                    enabled = bench.canCraftSelection,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFC857),
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "SYNTHESIZE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
                                text = "⚡ No reaction detected between current parts. Experiment with other components!",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "Select a socket above, then tap materials in your tray below to synthesize items.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM TRAY: Materials Pallet
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVAILABLE MATERIALS & PARTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Tap item to slot",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor.copy(alpha = 0.7f)
                )
            }

            val availableItems = remember(inventory) { inventory.filter { it.quantity > 0 } }
            if (availableItems.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF040A10).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.15f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No compatible materials found in inventory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableItems, key = { it.id }) { item ->
                        InventoryMaterialChip(
                            item = item,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            onClick = { onItemTapped(item.id) }
                        )
                    }
                }
            }
        }

        // Quick Load Known Schematics Bar
        if (learnedRecipes.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF061018).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "QUICK LOAD BLUEPRINTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        learnedRecipes.take(3).forEach { recipe ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onAutoFillRecipe(recipe.id) },
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = recipe.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Load →",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = accentColor
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
private fun BenchSocket(
    label: String,
    itemName: String?,
    itemId: String?,
    isActive: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onUnslot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filled = !itemName.isNullOrBlank()
    val socketBorderColor = when {
        isActive -> Color(0xFFFFC857)
        filled -> accentColor.copy(alpha = 0.7f)
        else -> borderColor.copy(alpha = 0.3f)
    }
    val background = when {
        isActive -> Color(0xFFFFC857).copy(alpha = 0.12f)
        filled -> accentColor.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .height(84.dp),
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, socketBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isActive) Color(0xFFFFC857) else accentColor.copy(alpha = 0.8f)
                )
                if (filled) {
                    IconButton(
                        onClick = onUnslot,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            if (filled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Image(
                        painter = painterResource(previewItemIconRes(itemId)),
                        contentDescription = itemName,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = itemName ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFFFFC857).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isActive) "SELECTING" else "EMPTY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isActive) Color(0xFFFFC857) else Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyConnectorNode(accentColor: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF040A10))
            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(accentColor, CircleShape)
        )
    }
}

@Composable
private fun InventoryMaterialChip(
    item: TinkeringItemChoice,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .height(54.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF081420).copy(alpha = 0.8f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(previewItemIconRes(item.id)),
                contentDescription = item.name,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description?.takeIf { it.isNotBlank() } ?: "Component",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "x${item.quantity}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
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
            .heightIn(max = 420.dp),
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
            .heightIn(max = 420.dp),
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
