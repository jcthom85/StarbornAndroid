package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.starborn.feature.crafting.TinkeringTutorialStep
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes

enum class TinkerTabMode { WORKBENCH, SCHEMATICS, SCRAP }
enum class ActiveBenchSlot { BASE, COMPONENT_1, COMPONENT_2 }

@Composable
fun TinkerTabContent(
    craftingViewModel: CraftingViewModel,
    accentColor: Color,
    borderColor: Color,
    onTutorialStepChanged: ((TinkeringTutorialStep) -> Unit)? = null,
    onPlayAudio: (String) -> Unit = {}
) {
    val state by craftingViewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(TinkerTabMode.WORKBENCH) }
    var activeSlot by remember { mutableStateOf(ActiveBenchSlot.BASE) }
    val shownTutorialSteps = remember { mutableSetOf<TinkeringTutorialStep>() }

    LaunchedEffect(state.isTutorialActive, state.tutorialStep) {
        if (state.isTutorialActive) {
            val step = state.tutorialStep
            if (step != null && !shownTutorialSteps.contains(step)) {
                shownTutorialSteps.add(step)
            }
        }
    }

    MenuSectionCard(
        title = "Tinkering",
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
                        tutorialStep = if (state.isTutorialActive) state.tutorialStep else null,
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
                            if (state.isTutorialActive) {
                                if (itemId == "cryo_inductor") {
                                    craftingViewModel.selectMain(itemId)
                                    activeSlot = ActiveBenchSlot.COMPONENT_1
                                } else if (itemId == "scrap_metal") {
                                    craftingViewModel.selectComponent(0, itemId)
                                    activeSlot = ActiveBenchSlot.BASE
                                } else {
                                    when (activeSlot) {
                                        ActiveBenchSlot.BASE -> craftingViewModel.selectMain(itemId)
                                        ActiveBenchSlot.COMPONENT_1 -> craftingViewModel.selectComponent(0, itemId)
                                        ActiveBenchSlot.COMPONENT_2 -> craftingViewModel.selectComponent(1, itemId)
                                    }
                                }
                            } else {
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
fun TutorialGuideArrow(
    isVertical: Boolean = true,
    color: Color = Color(0xFFFFC857)
) {
    val transition = rememberInfiniteTransition(label = "arrow_bounce")
    val offset by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    Text(
        text = if (isVertical) "▲" else "►",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = color,
        modifier = Modifier.offset(
            x = if (!isVertical) offset.dp else 0.dp,
            y = if (isVertical) offset.dp else 0.dp
        )
    )
}

@Composable
fun TutorialSparkleCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 6,
    color: Color = Color(0xFFFFC857)
) {
    val transition = rememberInfiniteTransition(label = "sparkles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        for (i in 0 until particleCount) {
            val phase = (time + i.toFloat() / particleCount) % 1f
            val x = (Math.sin(phase * Math.PI * 2 + i * 1.7).toFloat() * 0.35f + 0.5f) * width
            val y = (1f - phase) * height
            val alpha = (Math.sin(phase * Math.PI).toFloat()).coerceIn(0f, 1f)
            val radius = 2.dp.toPx() * (1f - phase * 0.4f)
            drawCircle(
                color = color.copy(alpha = alpha * 0.85f),
                radius = radius,
                center = Offset(x, y)
            )
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF081420).copy(alpha = 0.6f))
            .border(1.dp, borderColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TinkerModeButton(
            label = "Tinker",
            selected = current == TinkerTabMode.WORKBENCH,
            accentColor = accentColor,
            onClick = { onSelect(TinkerTabMode.WORKBENCH) },
            modifier = Modifier.weight(1f)
        )
        TinkerModeButton(
            label = "Schematics",
            selected = current == TinkerTabMode.SCHEMATICS,
            accentColor = accentColor,
            onClick = { onSelect(TinkerTabMode.SCHEMATICS) },
            modifier = Modifier.weight(1f)
        )
        TinkerModeButton(
            label = "Scrap",
            selected = current == TinkerTabMode.SCRAP,
            accentColor = accentColor,
            onClick = { onSelect(TinkerTabMode.SCRAP) },
            modifier = Modifier.weight(1f)
        )
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
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .height(34.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) accentColor.copy(alpha = 0.25f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, accentColor) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
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
    tutorialStep: TinkeringTutorialStep?,
    onSelectSlot: (ActiveBenchSlot) -> Unit,
    onClearSlot: (ActiveBenchSlot) -> Unit,
    onClearBench: () -> Unit,
    onItemTapped: (String) -> Unit,
    onAssemble: () -> Unit,
    onAutoFillRecipe: (String) -> Unit
) {
    val preview = bench.preview
    val hasItems = bench.mainItemId != null || bench.componentIds.any { it.isNotBlank() }

    val isBaseTutorialTarget = tutorialStep == TinkeringTutorialStep.SLOT_BASE
    val isCompTutorialTarget = tutorialStep == TinkeringTutorialStep.SLOT_COMPONENT
    val isSynthTutorialTarget = tutorialStep == TinkeringTutorialStep.SYNTHESIZE

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tutorialStep != null) {
            TinkeringTutorialGuideBanner(step = tutorialStep)
        }

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
                        isTutorialHighlighted = isBaseTutorialTarget,
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
                        isTutorialHighlighted = isCompTutorialTarget,
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
                        isTutorialHighlighted = false,
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
                        if (isSynthTutorialTarget) 1.5.dp else 1.dp,
                        if (isSynthTutorialTarget) Color(0xFFFFC857) else if (preview != null) Color(0xFFFFC857).copy(alpha = 0.6f) else borderColor.copy(alpha = 0.2f)
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
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSynthTutorialTarget) {
                                        TutorialSparkleCanvas(
                                            modifier = Modifier.matchParentSize(),
                                            particleCount = 5
                                        )
                                    }
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
                                        border = if (isSynthTutorialTarget) BorderStroke(1.5.dp, Color.White) else null,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        if (isSynthTutorialTarget) {
                                            TutorialGuideArrow(isVertical = false, color = Color.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
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
                        val isItemTutorialTarget = (isBaseTutorialTarget && item.id == "cryo_inductor") ||
                            (isCompTutorialTarget && item.id == "scrap_metal")
                        InventoryMaterialChip(
                            item = item,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            isTutorialTarget = isItemTutorialTarget,
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
    isTutorialHighlighted: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onUnslot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filled = !itemName.isNullOrBlank()
    val pulseTransition = rememberInfiniteTransition(label = "socket_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val snapAnim = remember(itemId) { Animatable(if (!itemId.isNullOrBlank()) 1.12f else 1.0f) }
    val flareAnim = remember(itemId) { Animatable(if (!itemId.isNullOrBlank()) 1.0f else 0f) }
    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            snapAnim.snapTo(1.14f)
            snapAnim.animateTo(1.0f, tween(durationMillis = 220, easing = EaseOutBack))
        }
    }
    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            flareAnim.snapTo(1.0f)
            flareAnim.animateTo(0f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        }
    }

    val socketBorderColor = when {
        isTutorialHighlighted -> Color(0xFFFFC857).copy(alpha = pulseAlpha)
        isActive -> Color(0xFFFFC857)
        filled -> accentColor.copy(alpha = 0.7f)
        else -> borderColor.copy(alpha = 0.3f)
    }
    val background = when {
        isTutorialHighlighted -> Color(0xFFFFC857).copy(alpha = 0.18f)
        isActive -> Color(0xFFFFC857).copy(alpha = 0.12f)
        filled -> accentColor.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = snapAnim.value
            scaleY = snapAnim.value
        }
    ) {
        if (flareAnim.value > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.65f * flareAnim.value),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx() * flareAnim.value)
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .height(84.dp),
            shape = RoundedCornerShape(10.dp),
            color = background,
            border = BorderStroke(if (isActive || isTutorialHighlighted) 1.5.dp else 1.dp, socketBorderColor)
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
                        color = if (isActive || isTutorialHighlighted) Color(0xFFFFC857) else accentColor.copy(alpha = 0.8f)
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
                            tint = if (isActive || isTutorialHighlighted) Color(0xFFFFC857).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isTutorialHighlighted) "TARGET" else if (isActive) "SELECTING" else "EMPTY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (isActive || isTutorialHighlighted) Color(0xFFFFC857) else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }

        if (isTutorialHighlighted) {
            TutorialSparkleCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp)),
                particleCount = 4
            )
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
    isTutorialTarget: Boolean = false,
    onClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "chip_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isTutorialTarget) Color(0xFF0D2535).copy(alpha = 0.95f) else Color(0xFF081420).copy(alpha = 0.8f),
            border = BorderStroke(
                if (isTutorialTarget) 1.5.dp else 1.dp,
                if (isTutorialTarget) Color(0xFFFFC857).copy(alpha = pulseAlpha) else borderColor.copy(alpha = 0.3f)
            )
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
                        color = if (isTutorialTarget) Color(0xFFFFC857) else Color.White,
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
                if (isTutorialTarget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TutorialGuideArrow(isVertical = false, color = Color(0xFFFFC857))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFC857).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFFC857))
                        ) {
                            Text(
                                text = "SLOT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = Color(0xFFFFC857),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
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

        if (isTutorialTarget) {
            TutorialSparkleCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp)),
                particleCount = 4
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DISCOVERED BLUEPRINTS (${recipes.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.7f)
        )

        if (recipes.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF040A10).copy(alpha = 0.4f),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.15f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "No blueprints discovered yet. Combine parts at the workbench!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    BlueprintRecipeCard(
                        recipe = recipe,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onLoad = { onLoadRecipe(recipe.id) },
                        onCraftDirect = { onCraftDirect(recipe.id) }
                    )
                }
            }
        }

        if (lockedRecipes.isNotEmpty()) {
            Text(
                text = "UNKNOWN BLUEPRINTS (${lockedRecipes.size} locked)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.4f)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(lockedRecipes, key = { it.id }) { recipe ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "??? [Encrypted Schematic]",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.35f)
                            )
                            Text(
                                text = "LOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueprintRecipeCard(
    recipe: TinkeringRecipeUi,
    accentColor: Color,
    borderColor: Color,
    onLoad: () -> Unit,
    onCraftDirect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF061018).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, if (recipe.canCraft) accentColor.copy(alpha = 0.4f) else borderColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (recipe.canCraft) Color.White else Color.White.copy(alpha = 0.6f)
                )
                recipe.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recipe.ingredients.forEach { req ->
                        RequirementChip(req = req, accentColor = accentColor)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onLoad,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "Socket", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                }
                Button(
                    onClick = onCraftDirect,
                    enabled = recipe.canCraft,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "Craft", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SALVAGE & DISASSEMBLY",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.7f)
        )

        if (scrapChoices.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF040A10).copy(alpha = 0.4f),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.15f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "No scrapable gear or crafted items currently in inventory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(scrapChoices, key = { it.id }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF061018).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Image(
                                    painter = painterResource(previewItemIconRes(item.id)),
                                    contentDescription = item.name,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "${item.name} (x${item.quantity})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Breaks down into raw constituent alloys",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onScrapItem(item.id) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE57373).copy(alpha = 0.8f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scrap",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
private fun RequirementChip(
    req: TinkeringRequirementStatus,
    accentColor: Color
) {
    val satisfied = req.available >= req.required
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (satisfied) accentColor.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f),
        border = BorderStroke(
            1.dp,
            if (satisfied) accentColor.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (satisfied) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (satisfied) accentColor else Color(0xFFEF5350),
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = "${req.label} (${req.available}/${req.required})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (satisfied) Color.White.copy(alpha = 0.9f) else Color(0xFFEF5350)
            )
        }
    }
}

@Composable
fun TinkeringTutorialGuideBanner(
    step: TinkeringTutorialStep,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tinker_tut_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val (badgeText, title, instruction) = when (step) {
        TinkeringTutorialStep.SLOT_BASE -> Triple(
            "STEP 1 OF 3",
            "SLOT BASE ITEM",
            "Tap the Broken Cryo-Inductor in your tray below to mount it into the Base socket."
        )
        TinkeringTutorialStep.SLOT_COMPONENT -> Triple(
            "STEP 2 OF 3",
            "ADD REPLACEMENT ALLOY",
            "Tap Scrap Metal in your tray to supply replacement conduit alloy for the cold loop."
        )
        TinkeringTutorialStep.SYNTHESIZE -> Triple(
            "STEP 3 OF 3",
            "SYNTHESIZE BLUEPRINT",
            "Reaction detected! Tap the glowing SYNTHESIZE button to seal the cold loop."
        )
        TinkeringTutorialStep.COMPLETE -> Triple(
            "COMPLETE",
            "REPAIR FINISHED",
            "Cryo-Inductor restored! Cryo Vent is now online and available in combat."
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071420).copy(alpha = 0.95f),
        border = BorderStroke(1.5.dp, Color(0xFF7BE8FF).copy(alpha = borderAlpha))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF7BE8FF).copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color(0xFF7BE8FF).copy(alpha = 0.7f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color(0xFF7BE8FF),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = Color.White
                )
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}
