package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.crafting.*
import com.example.starborn.feature.crafting.ui.ModComparisonPanel
import com.example.starborn.feature.exploration.ui.components.rememberItemIconPainter
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets

@Composable
private fun secondaryText() = if (LocalFieldMenuHighContrast.current) FieldMenuDesign.text else FieldMenuDesign.textMuted

@Composable
private fun actionHeight() = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp

@Composable
private fun TinkerPanel(
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FieldMenuDesign.panel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            borderColor ?: if (LocalFieldMenuHighContrast.current) FieldMenuDesign.border else FieldMenuDesign.subtleBorder
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModernTinkerWorkbench(
    bench: TinkeringBenchState,
    inventory: List<TinkeringItemChoice>,
    recipes: List<TinkeringRecipeUi>,
    baseItemIds: Set<String>,
    componentOptions: Map<String, Set<String>>,
    activeSlot: ActiveBenchSlot,
    accent: Color,
    tutorialStep: TinkeringTutorialStep?,
    onSelectSlot: (ActiveBenchSlot) -> Unit,
    onClearSlot: (ActiveBenchSlot) -> Unit,
    onClear: () -> Unit,
    onItem: (String) -> Unit,
    onCraft: () -> Unit,
    onLoad: (String) -> Unit
) {
    val resultFocus = remember { BringIntoViewRequester() }
    var pickerOpen by remember(bench.mainItemId, bench.componentIds, bench.preview?.recipeId) {
        mutableStateOf(bench.preview == null)
    }
    LaunchedEffect(tutorialStep, bench.preview?.recipeId) {
        if (tutorialStep == TinkeringTutorialStep.SYNTHESIZE && bench.preview != null) {
            withFrameNanos { }; withFrameNanos { }
            resultFocus.bringIntoView()
        }
    }

    val largeFont = LocalDensity.current.fontScale >= 1.3f

    val partsTray: @Composable () -> Unit = {
        val options = if (activeSlot == ActiveBenchSlot.BASE) baseItemIds else componentOptions[bench.mainItemId].orEmpty()
        val materials = inventory.filter { it.quantity > 0 && it.id in options }
        TinkerPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeSlot == ActiveBenchSlot.BASE) "Base items" else "Compatible components",
                    color = FieldMenuDesign.text,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tap to slot",
                    color = FieldMenuDesign.cyan,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = "Consumed only when you repair or synthesize.",
                color = secondaryText(),
                style = MaterialTheme.typography.bodySmall
            )
            if (materials.isEmpty()) {
                Text(
                    text = if (bench.mainItemId == null && activeSlot != ActiveBenchSlot.BASE) {
                        "Choose a base item first."
                    } else if (activeSlot != ActiveBenchSlot.BASE && options.isEmpty() && bench.preview != null) {
                        "This recipe needs no additional components. Build the result above."
                    } else {
                        "No matching materials in your inventory. Check Schematics for requirements."
                    },
                    color = secondaryText(),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                val cols = if (largeFont) 1 else 2
                materials.chunked(cols).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isTutItem = (tutorialStep == TinkeringTutorialStep.SLOT_BASE && item.id == "cryo_inductor") ||
                                (tutorialStep == TinkeringTutorialStep.SLOT_COMPONENT && item.id == "scrap_metal")
                            Surface(
                                onClick = { onItem(item.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = actionHeight()),
                                color = if (isTutItem) FieldMenuDesign.primaryActionFill else FieldMenuDesign.elevatedPanel,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    if (isTutItem) 1.5.dp else 1.dp,
                                    if (isTutItem) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        rememberItemIconPainter(item.id, fallbackType = item.id),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        modifier = Modifier.weight(1f),
                                        color = if (isTutItem) FieldMenuDesign.gold else FieldMenuDesign.text,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FieldMenuDesign.deepCard,
                                        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "×${item.quantity}",
                                            color = FieldMenuDesign.cyan,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size < cols) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Guided Tutorial Banner (preserves single "Tutorial" node for MobileFieldMenuReviewTest)
        tutorialStep?.let { step ->
            Surface(
                color = FieldMenuDesign.panel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, FieldMenuDesign.cyan)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(FieldMenuDesign.cyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "NEW",
                                    color = FieldMenuDesign.cyan,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text("Tutorial", color = accent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(
                            if (step == TinkeringTutorialStep.COMPLETE) "Complete" else "Step ${step.ordinal + 1} of 3",
                            color = secondaryText(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (step == TinkeringTutorialStep.SLOT_BASE) {
                        Text(
                            "Repair the Cryo-Inductor",
                            color = FieldMenuDesign.text,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        com.example.starborn.feature.crafting.tinkeringTutorialInstruction(step),
                        style = MaterialTheme.typography.bodySmall,
                        color = FieldMenuDesign.text
                    )
                }
            }
        }

        // 2. Quick-Load Ready Blueprints Strip (when not in tutorial)
        if (tutorialStep == null && recipes.isNotEmpty()) {
            val ready = recipes.filter { it.canCraft }.take(3)
            TinkerPanel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ready to build",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FieldMenuDesign.text
                    )
                    if (ready.isNotEmpty()) {
                        Text(
                            "Tap to load sockets",
                            style = MaterialTheme.typography.labelSmall,
                            color = FieldMenuDesign.mint
                        )
                    }
                }
                if (ready.isEmpty()) {
                    Text(
                        "Slot a Base item below (like Scrap Metal, Ballistic Weave, Focusing Lens, or Vial of Venom) to discover mods and gadgets, or check Schematics.",
                        color = secondaryText(),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ready.forEach { recipe ->
                            Surface(
                                onClick = { onLoad(recipe.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = FieldMenuDesign.primaryActionFill,
                                border = BorderStroke(1.dp, FieldMenuDesign.mint.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recipe.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = FieldMenuDesign.text
                                    )
                                    Text(
                                        text = "Load →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FieldMenuDesign.mint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Synthesis Chamber (Sockets grouped together so they never jump apart!)
        TinkerPanel(borderColor = FieldMenuDesign.cyan.copy(alpha = 0.35f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Assembly",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FieldMenuDesign.text
                )
                if (bench.mainItemId != null || bench.componentIds.any { it.isNotBlank() }) {
                    TextButton(
                        onClick = { pickerOpen = true; onClear() },
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Text("Clear", color = FieldMenuDesign.cyan)
                    }
                }
            }

            ModernTinkerSocket(
                label = "Base item",
                name = bench.mainItemName,
                itemId = bench.mainItemId,
                selected = activeSlot == ActiveBenchSlot.BASE && pickerOpen,
                tutorial = tutorialStep == TinkeringTutorialStep.SLOT_BASE,
                accent = accent,
                onSelect = { pickerOpen = true; onSelectSlot(ActiveBenchSlot.BASE) },
                onClear = { pickerOpen = true; onClearSlot(ActiveBenchSlot.BASE) }
            )
            ModernTinkerSocket(
                label = "Component 1",
                name = bench.componentNames.getOrNull(0),
                itemId = bench.componentIds.getOrNull(0),
                selected = activeSlot == ActiveBenchSlot.COMPONENT_1 && pickerOpen,
                tutorial = tutorialStep == TinkeringTutorialStep.SLOT_COMPONENT,
                accent = accent,
                onSelect = { pickerOpen = true; onSelectSlot(ActiveBenchSlot.COMPONENT_1) },
                onClear = { pickerOpen = true; onClearSlot(ActiveBenchSlot.COMPONENT_1) }
            )
            if (tutorialStep == null) {
                ModernTinkerSocket(
                    label = "Component 2",
                    name = bench.componentNames.getOrNull(1),
                    itemId = bench.componentIds.getOrNull(1),
                    selected = activeSlot == ActiveBenchSlot.COMPONENT_2 && pickerOpen,
                    tutorial = false,
                    accent = accent,
                    onSelect = { pickerOpen = true; onSelectSlot(ActiveBenchSlot.COMPONENT_2) },
                    onClear = { pickerOpen = true; onClearSlot(ActiveBenchSlot.COMPONENT_2) }
                )
            }
        }

        // 4. Synthesis Output, Requirements & Comparison Card (Immediately visible below sockets!)
        bench.preview?.let { preview ->
            val isRepair = preview.category.equals("repair", ignoreCase = true)
            val actionLabel = if (isRepair) "Repair" else "Synthesize"
            val missingReqs = bench.requirements.filter { it.available < it.required }
            TinkerPanel(
                borderColor = if (bench.canCraftSelection) FieldMenuDesign.gold else FieldMenuDesign.coral.copy(alpha = 0.6f)
            ) {
                Column(
                    Modifier.bringIntoViewRequester(resultFocus),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (preview.learned) "BLUEPRINT OUTPUT" else "★ NEW DISCOVERY",
                                color = if (preview.learned) accent else FieldMenuDesign.gold,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            )
                            Text(
                                text = preview.name,
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (bench.canCraftSelection) FieldMenuDesign.mint else FieldMenuDesign.coral).copy(alpha = 0.16f),
                            border = BorderStroke(
                                1.dp,
                                (if (bench.canCraftSelection) FieldMenuDesign.mint else FieldMenuDesign.coral).copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = if (bench.canCraftSelection) "READY" else "NEED PARTS",
                                color = if (bench.canCraftSelection) FieldMenuDesign.mint else FieldMenuDesign.coral,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = preview.resultSummary,
                        color = secondaryText(),
                        style = MaterialTheme.typography.bodySmall
                    )

                    TinkerRequirements(bench.requirements)

                    if (preview.requiresSchematic && !preview.learned) {
                        Text(
                            text = preview.discoveryHint ?: "Requires Schematic: Learn this blueprint from Inventory first.",
                            color = FieldMenuDesign.coral,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    } else if (missingReqs.isNotEmpty()) {
                        Text(
                            text = "Missing: " + missingReqs.joinToString(", ") {
                                "${it.label.removePrefix("Tool: ")} ×${it.required - it.available}"
                            },
                            color = FieldMenuDesign.coral,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    ModComparisonPanel(preview.equipmentComparison)

                    Button(
                        onClick = onCraft,
                        enabled = bench.canCraftSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = actionHeight()),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            if (tutorialStep == TinkeringTutorialStep.SYNTHESIZE) 2.dp else 1.5.dp,
                            when {
                                tutorialStep == TinkeringTutorialStep.SYNTHESIZE -> FieldMenuDesign.cyan
                                bench.canCraftSelection -> FieldMenuDesign.gold
                                else -> FieldMenuDesign.subtleBorder
                            }
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FieldMenuDesign.primaryActionFill,
                            contentColor = FieldMenuDesign.text,
                            disabledContainerColor = FieldMenuDesign.deepCard,
                            disabledContentColor = secondaryText()
                        )
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } ?: run {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FieldMenuDesign.deepCard,
                border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (bench.mainItemId == null) {
                        "Select a Base item below to view compatible components and preview the output."
                    } else {
                        "Choose a compatible component below to complete the assembly."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText(),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 5. Contextual Compatible Parts Tray
        if (pickerOpen && tutorialStep != TinkeringTutorialStep.SYNTHESIZE) {
            partsTray()
        }
    }
}

@Composable
private fun ModernTinkerSocket(
    label: String,
    name: String?,
    itemId: String?,
    selected: Boolean,
    tutorial: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    val filled = !name.isNullOrBlank()
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = actionHeight()),
        color = if (selected || tutorial) FieldMenuDesign.elevatedPanel else FieldMenuDesign.deepCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected || tutorial) 1.5.dp else 1.dp,
            color = when {
                tutorial -> FieldMenuDesign.gold
                selected -> accent
                filled -> FieldMenuDesign.cyan.copy(alpha = 0.45f)
                else -> FieldMenuDesign.subtleBorder
            }
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filled && !itemId.isNullOrBlank()) {
                Image(
                    painter = rememberItemIconPainter(itemId, fallbackType = itemId),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    color = when {
                        tutorial -> FieldMenuDesign.gold
                        selected -> FieldMenuDesign.cyan
                        else -> secondaryText()
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = name?.takeIf { it.isNotBlank() } ?: "Choose a part",
                    color = if (filled) FieldMenuDesign.text else secondaryText(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
            if (filled) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove $label", tint = secondaryText(), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TinkerRequirements(requirements: List<TinkeringRequirementStatus>) {
    val materials = requirements.filterNot { it.label.startsWith("Tool: ") }
    val tools = requirements.filter { it.label.startsWith("Tool: ") }
    if (materials.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Materials · consumed",
                color = FieldMenuDesign.text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                materials.forEach { requirement ->
                    val ok = requirement.available >= requirement.required
                    val chipColor = if (ok) FieldMenuDesign.mint else FieldMenuDesign.coral
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = FieldMenuDesign.deepCard,
                        border = BorderStroke(1.dp, chipColor.copy(alpha = if (ok) 0.45f else 0.7f))
                    ) {
                        Text(
                            text = "${requirement.label} ${requirement.available} / ${requirement.required}" + if (ok) " ✓" else " ✕",
                            color = if (ok) FieldMenuDesign.text else FieldMenuDesign.coral,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    if (tools.isNotEmpty()) {
        Text(
            text = "Tools · reusable: " + tools.joinToString {
                it.label.removePrefix("Tool: ") + if (it.available < it.required) " (missing)" else " ✓"
            },
            color = if (tools.all { it.available >= it.required }) FieldMenuDesign.cyan else FieldMenuDesign.coral,
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = "Equipped items are reserved.",
        color = secondaryText(),
        style = MaterialTheme.typography.labelSmall
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModernTinkerSchematics(
    recipes: List<TinkeringRecipeUi>,
    unknown: List<TinkeringRecipeUi>,
    onLoad: (String) -> Unit,
    onCraft: (String) -> Unit,
    onViewed: (String) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showDiscoveries by rememberSaveable { mutableStateOf(false) }
    var showAllClues by rememberSaveable { mutableStateOf(false) }
    var selectedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Find a schematic") }
        )
        Text(
            text = "Learned schematics · ${recipes.size}",
            color = FieldMenuDesign.text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        if (recipes.isEmpty()) {
            Text(
                text = "Repair something at the workbench, or use a found schematic from Inventory to learn it.",
                color = secondaryText(),
                style = MaterialTheme.typography.bodySmall
            )
        }
        val shown = recipes
            .filter { it.name.contains(query.trim(), ignoreCase = true) }
            .sortedByDescending { it.canCraft }
        if (shown.isEmpty() && recipes.isNotEmpty()) {
            Text(
                text = "No learned schematic matches your search.",
                color = secondaryText(),
                style = MaterialTheme.typography.bodySmall
            )
        }
        shown.forEach { recipe ->
            key(recipe.id) {
                val expanded = selectedRecipeId == recipe.id
                TinkerPanel(
                    borderColor = when {
                        expanded -> FieldMenuDesign.gold
                        recipe.canCraft -> FieldMenuDesign.mint.copy(alpha = 0.45f)
                        else -> null
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                selectedRecipeId = if (expanded) null else recipe.id
                                if (!expanded) onViewed(recipe.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = actionHeight())
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recipe.name + if (recipe.isNew) "  · NEW" else "",
                                        color = FieldMenuDesign.text,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = tinkeringEffectSummary(recipe),
                                    color = secondaryText(),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = if (expanded) 4 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = (if (recipe.canCraft) FieldMenuDesign.mint else FieldMenuDesign.coral).copy(alpha = 0.15f),
                                border = BorderStroke(
                                    1.dp,
                                    (if (recipe.canCraft) FieldMenuDesign.mint else FieldMenuDesign.coral).copy(alpha = 0.55f)
                                )
                            ) {
                                Text(
                                    text = if (recipe.canCraft) "READY" else "NEED PARTS",
                                    color = if (recipe.canCraft) FieldMenuDesign.mint else FieldMenuDesign.coral,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Always show scannable ingredient pills + availability reason
                    TinkerRequirements(recipe.ingredients)
                    Text(
                        text = tinkeringAvailability(recipe),
                        color = if (recipe.canCraft) FieldMenuDesign.mint else FieldMenuDesign.coral,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    if (expanded) {
                        recipe.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = secondaryText(), style = MaterialTheme.typography.bodySmall)
                        }
                        if (recipe.equipmentComparison.isNotEmpty()) {
                            ModComparisonPanel(recipe.equipmentComparison)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onLoad(recipe.id) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = actionHeight()),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldMenuDesign.text)
                        ) {
                            Text("Load into workbench")
                        }
                        Button(
                            onClick = { onCraft(recipe.id) },
                            enabled = recipe.canCraft,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (recipe.canCraft) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FieldMenuDesign.primaryActionFill,
                                contentColor = FieldMenuDesign.text,
                                disabledContainerColor = FieldMenuDesign.deepCard,
                                disabledContentColor = secondaryText()
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = actionHeight())
                        ) {
                            Text("Synthesize", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (unknown.isNotEmpty()) {
            val matchingUnknown = unknown.filter { it.name.contains(query.trim(), ignoreCase = true) }
            val relevantUnknown = matchingUnknown.filter { it.isRelevantDiscovery }
            val visibleUnknown = if (query.isNotBlank() || showAllClues || relevantUnknown.isEmpty()) matchingUnknown else relevantUnknown
            val hiddenClueCount = (matchingUnknown.size - visibleUnknown.size).coerceAtLeast(0)
            OutlinedButton(
                onClick = { showDiscoveries = !showDiscoveries },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = actionHeight()),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldMenuDesign.gold)
            ) {
                Text(if (showDiscoveries) "Hide discovery clues" else "Discovery clues · ${visibleUnknown.size}")
            }
            if (showDiscoveries) {
                visibleUnknown.forEach { recipe ->
                    TinkerPanel {
                        Text(
                            recipe.name,
                            color = FieldMenuDesign.text,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            if (recipe.requiresSchematic) "Requires a schematic" else "Discover at the workbench",
                            color = FieldMenuDesign.gold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            recipe.discoveryHint ?: "Try combining the base item with compatible components.",
                            color = secondaryText(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (query.isBlank() && (hiddenClueCount > 0 || (showAllClues && relevantUnknown.isNotEmpty() && relevantUnknown.size < matchingUnknown.size))) {
                    OutlinedButton(
                        onClick = { showAllClues = !showAllClues },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = actionHeight()),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (showAllClues) "Show only clues for owned base parts (${relevantUnknown.size})"
                            else "Show all ${matchingUnknown.size} discovery clues (+$hiddenClueCount locked)"
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ModernTinkerScrap(items: List<TinkeringItemChoice>, onScrap: (String) -> Unit) {
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    val pending = items.firstOrNull { it.id == pendingId }
    pending?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingId = null },
            containerColor = FieldMenuDesign.panel,
            titleContentColor = FieldMenuDesign.text,
            textContentColor = FieldMenuDesign.textMuted,
            title = { Text("Scrap ${item.name}?") },
            text = {
                Text("Consumes one item. You recover:\n${item.salvage.joinToString("\n")}\n\nOnly the listed materials are recovered.")
            },
            confirmButton = {
                TextButton(onClick = { pendingId = null; onScrap(item.id) }) {
                    Text("Scrap one", color = FieldMenuDesign.coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingId = null }) {
                    Text("Keep item", color = FieldMenuDesign.textMuted)
                }
            }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Recover materials",
            color = FieldMenuDesign.text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            "Quest tools and equipped gear are protected. Each item has specific salvage returns.",
            color = secondaryText(),
            style = MaterialTheme.typography.bodySmall
        )
        if (items.isEmpty()) {
            Text("No items available to scrap.", color = secondaryText(), style = MaterialTheme.typography.bodySmall)
        }
        items.forEach { item ->
            key(item.id) {
                TinkerPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = rememberItemIconPainter(item.id, fallbackType = item.id),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${item.name} ×${item.quantity}",
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Recover: ${item.salvage.joinToString(", ")}",
                                color = FieldMenuDesign.mint,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(
                            onClick = { pendingId = item.id },
                            modifier = Modifier.heightIn(min = actionHeight()),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FieldMenuDesign.coral.copy(alpha = 0.65f))
                        ) {
                            Text("Scrap one", color = FieldMenuDesign.coral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

