package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.crafting.*
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets

@Composable
private fun secondaryText() = if (LocalFieldMenuHighContrast.current) FieldMenuDesign.text else FieldMenuDesign.textMuted

@Composable
private fun actionHeight() = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp

@Composable
private fun TinkerPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp),
        border = if (LocalFieldMenuHighContrast.current) BorderStroke(1.dp, FieldMenuDesign.border) else null) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tutorialStep?.let { step ->
            Surface(color = FieldMenuDesign.gold.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FieldMenuDesign.gold)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Repair the Cryo-Inductor", style = MaterialTheme.typography.titleSmall, color = FieldMenuDesign.gold)
                    Text(when (step) {
                        TinkeringTutorialStep.SLOT_BASE -> "1 of 3 · Choose the Cryo-Inductor as your base item."
                        TinkeringTutorialStep.SLOT_COMPONENT -> "2 of 3 · Add Scrap Metal to the first component slot."
                        TinkeringTutorialStep.SYNTHESIZE -> "3 of 3 · Repair the cold loop to unlock Cryo Vent."
                        TinkeringTutorialStep.COMPLETE -> "Repair complete."
                    }, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.text)
                }
            }
        }
        if (tutorialStep == null && recipes.isNotEmpty()) {
            TinkerPanel {
                Text("Ready to build", style = MaterialTheme.typography.titleSmall, color = FieldMenuDesign.text)
                val ready = recipes.filter { it.canCraft }.take(3)
                if (ready.isEmpty()) {
                    Text("Open Schematics to check your recipes and missing materials.", color = secondaryText(),
                        style = MaterialTheme.typography.bodySmall)
                } else ready.forEach { recipe ->
                    OutlinedButton(onClick = { onLoad(recipe.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight())) {
                        Text(recipe.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        TinkerPanel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Assembly", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FieldMenuDesign.text)
                if (bench.mainItemId != null || bench.componentIds.isNotEmpty()) {
                    TextButton(onClick = onClear, modifier = Modifier.heightIn(min = actionHeight())) { Text("Clear") }
                }
            }
            ModernTinkerSocket("Base item", bench.mainItemName, activeSlot == ActiveBenchSlot.BASE,
                tutorialStep == TinkeringTutorialStep.SLOT_BASE, accent,
                { onSelectSlot(ActiveBenchSlot.BASE) }, { onClearSlot(ActiveBenchSlot.BASE) })
            // Full-width sockets preserve names and font scaling on narrow phones.
            ModernTinkerSocket("Component 1", bench.componentNames.getOrNull(0), activeSlot == ActiveBenchSlot.COMPONENT_1,
                tutorialStep == TinkeringTutorialStep.SLOT_COMPONENT, accent,
                { onSelectSlot(ActiveBenchSlot.COMPONENT_1) }, { onClearSlot(ActiveBenchSlot.COMPONENT_1) })
            ModernTinkerSocket("Component 2", bench.componentNames.getOrNull(1), activeSlot == ActiveBenchSlot.COMPONENT_2,
                false, accent, { onSelectSlot(ActiveBenchSlot.COMPONENT_2) }, { onClearSlot(ActiveBenchSlot.COMPONENT_2) })
        }
        bench.preview?.let { preview ->
            TinkerPanel {
                Text(if (preview.learned) "Result" else "New discovery", color = accent, style = MaterialTheme.typography.labelMedium)
                Text(preview.name, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleMedium)
                Text(preview.resultSummary, color = secondaryText(), style = MaterialTheme.typography.bodySmall)
                TinkerRequirements(bench.requirements)
                if (preview.requiresSchematic && !preview.learned) {
                    Text(preview.discoveryHint ?: "Learn this schematic from Inventory first.", color = secondaryText(),
                        style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onCraft, enabled = bench.canCraftSelection,
                    modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight()),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold, contentColor = FieldMenuDesign.shell)) {
                    Text(if (preview.category == "repair") "Repair" else "Build", style = MaterialTheme.typography.labelLarge)
                }
            }
        } ?: Text(if (bench.mainItemId == null) "Choose a base item to see compatible components."
            else "This combination has no recipe. Try a different component or clear an extra slot.",
            style = MaterialTheme.typography.bodySmall, color = secondaryText())

        val options = if (activeSlot == ActiveBenchSlot.BASE) baseItemIds else componentOptions[bench.mainItemId].orEmpty()
        val materials = inventory.filter { it.quantity > 0 && it.id in options }
        TinkerPanel {
            Text(if (activeSlot == ActiveBenchSlot.BASE) "Base items" else "Compatible components",
                color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
            Text("Select a socket above, then choose a part. Materials are consumed only when you build.",
                color = secondaryText(), style = MaterialTheme.typography.bodySmall)
            if (materials.isEmpty()) {
                Text(if (bench.mainItemId == null && activeSlot != ActiveBenchSlot.BASE) "Choose a base item first."
                    else if (activeSlot != ActiveBenchSlot.BASE && options.isEmpty() && bench.preview != null)
                        "This recipe needs no additional components. Build the result above."
                    else "No matching materials in your inventory. Check Schematics for requirements.",
                    color = secondaryText(), style = MaterialTheme.typography.bodySmall)
            }
            materials.forEach { item ->
                Surface(onClick = { onItem(item.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight()),
                    color = FieldMenuDesign.elevatedPanel, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painterResource(previewItemIconRes(item.id)), contentDescription = null, modifier = Modifier.size(32.dp))
                        Text(item.name, Modifier.weight(1f), color = FieldMenuDesign.text, style = MaterialTheme.typography.bodySmall)
                        Text("×${item.quantity}", color = secondaryText(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTinkerSocket(label: String, name: String?, selected: Boolean, tutorial: Boolean, accent: Color,
    onSelect: () -> Unit, onClear: () -> Unit) {
    Surface(onClick = onSelect, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight()),
        color = FieldMenuDesign.elevatedPanel, shape = RoundedCornerShape(10.dp),
        border = if (selected || tutorial) BorderStroke(1.dp, if (tutorial) FieldMenuDesign.gold else accent) else null) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = if (tutorial) FieldMenuDesign.gold else secondaryText(), style = MaterialTheme.typography.labelMedium)
                Text(name?.takeIf { it.isNotBlank() } ?: "Choose a part", color = FieldMenuDesign.text,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (!name.isNullOrBlank()) IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Remove $label", tint = secondaryText())
            }
        }
    }
}

@Composable
private fun TinkerRequirements(requirements: List<TinkeringRequirementStatus>) {
    Text("Materials and tools", color = FieldMenuDesign.text, style = MaterialTheme.typography.labelLarge)
    requirements.forEach { requirement ->
        val tool = requirement.label.startsWith("Tool: ")
        Text("${requirement.label.removePrefix("Tool: ")} · ${requirement.available}/${requirement.required}" +
            if (tool) " · reusable" else " · consumed",
            color = if (requirement.available >= requirement.required) secondaryText() else Color(0xFFFFAA90),
            style = MaterialTheme.typography.bodySmall)
    }
    Text("Equipped items are reserved. Unequip them before using them as materials.",
        color = secondaryText(), style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun ModernTinkerSchematics(recipes: List<TinkeringRecipeUi>, unknown: List<TinkeringRecipeUi>,
    onLoad: (String) -> Unit, onCraft: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showDiscoveries by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Find a schematic") })
        Text("Learned schematics · ${recipes.size}", color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
        if (recipes.isEmpty()) Text("Repair something at the workbench, or use a found schematic from Inventory to learn it.",
            color = secondaryText(), style = MaterialTheme.typography.bodySmall)
        val shown = recipes.filter { it.name.contains(query.trim(), ignoreCase = true) }.sortedByDescending { it.canCraft }
        if (shown.isEmpty() && recipes.isNotEmpty()) Text("No learned schematic matches your search.",
            color = secondaryText(), style = MaterialTheme.typography.bodySmall)
        shown.forEach { recipe ->
            key(recipe.id) {
                TinkerPanel {
                    Text(recipe.name, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleMedium)
                    Text(recipe.resultSummary, color = secondaryText(), style = MaterialTheme.typography.bodySmall)
                    TinkerRequirements(recipe.ingredients)
                    // Separate full-width actions remain readable with larger system fonts.
                    Button(onClick = { onCraft(recipe.id) }, enabled = recipe.canCraft,
                        modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight())) {
                        Text(if (recipe.category == "repair") "Repair" else "Build")
                    }
                    OutlinedButton(onClick = { onLoad(recipe.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight())) {
                        Text("Load into workbench")
                    }
                }
            }
        }
        if (unknown.isNotEmpty()) {
            OutlinedButton(onClick = { showDiscoveries = !showDiscoveries }, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight())) {
                Text(if (showDiscoveries) "Hide discovery clues" else "Discovery clues · ${unknown.size}")
            }
            if (showDiscoveries) unknown.filter { it.name.contains(query.trim(), ignoreCase = true) }.forEach { recipe ->
                TinkerPanel {
                    Text(recipe.name, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
                    Text(if (recipe.requiresSchematic) "Requires a schematic" else "Discover at the workbench",
                        color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelMedium)
                    Text(recipe.discoveryHint ?: "Try combining the base item with compatible components.",
                        color = secondaryText(), style = MaterialTheme.typography.bodySmall)
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
        AlertDialog(onDismissRequest = { pendingId = null }, title = { Text("Scrap ${item.name}?") },
            text = { Text("Consumes one item. You recover:\n${item.salvage.joinToString("\n")}\n\nOnly the listed materials are recovered.") },
            confirmButton = { TextButton(onClick = { pendingId = null; onScrap(item.id) }) { Text("Scrap one") } },
            dismissButton = { TextButton(onClick = { pendingId = null }) { Text("Keep item") } })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recover materials", color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
        Text("Quest tools and equipped gear are protected. Each item has specific salvage returns. Check the listed returns before scrapping.",
            color = secondaryText(), style = MaterialTheme.typography.bodySmall)
        if (items.isEmpty()) Text("No items available to scrap.", color = secondaryText(), style = MaterialTheme.typography.bodySmall)
        items.forEach { item ->
            key(item.id) {
                TinkerPanel {
                    Text("${item.name} ×${item.quantity}", color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
                    Text("Recover: ${item.salvage.joinToString(", ")}", color = secondaryText(), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { pendingId = item.id }, modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight())) {
                        Text("Scrap one", color = Color(0xFFFFAA90))
                    }
                }
            }
        }
    }
}
