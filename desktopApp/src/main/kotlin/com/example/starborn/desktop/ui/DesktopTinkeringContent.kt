package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.crafting.*
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
fun DesktopTinkeringContent(services: DesktopAppServices) {
    val scope = rememberCoroutineScope()
    val catalog = remember(services) { services.itemRepository.allItems().associateBy { it.id } }
    val runtime = remember(services) { CraftingController(services.craftingService, services.inventoryService, services.sessionStore, scope) }
    val state by runtime.uiState.collectAsState()
    var page by rememberSaveable { mutableStateOf("Workbench") }
    var socket by rememberSaveable { mutableIntStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    var scrapId by rememberSaveable { mutableStateOf<String?>(null) }
    var recipeId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(runtime) { runtime.craftResults.collect { services.exploration.onTinkeringCrafted(it.itemId) } }
    LaunchedEffect(state.tutorialStep) {
        state.tutorialStep?.let { step ->
            services.exploration.onTinkerTutorialStep(step)
            page = "Workbench"
            socket = if (step == TinkeringTutorialStep.SLOT_COMPONENT) 0 else -1
        }
    }
    DisposableEffect(runtime) { onDispose { runtime.close(); services.exploration.onTinkeringClosed() } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Workbench", "Schematics", "Scrap").forEach { tab -> FilterChip(page == tab, onClick = { page = tab; query = "" }, modifier = Modifier.desktopPointerHover(), label = { Text(tab) }) }
        }
        state.lastMessage?.let { Text(it, color = FieldMenuDesign.cyan) }
        state.tutorialStep?.let { step ->
            Surface(color = FieldMenuDesign.gold.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Repair the Cryo-Inductor", color = FieldMenuDesign.gold)
                    Text(when (step) {
                        TinkeringTutorialStep.SLOT_BASE -> "1 of 3 - Choose the Cryo-Inductor as your base item."
                        TinkeringTutorialStep.SLOT_COMPONENT -> "2 of 3 - Add Scrap Metal to the first component slot."
                        TinkeringTutorialStep.SYNTHESIZE -> "3 of 3 - Repair the cold loop to unlock Cryo Vent."
                        TinkeringTutorialStep.COMPLETE -> "Repair complete."
                    })
                }
            }
        }
        when (page) {
            "Workbench" -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (socket == -1) "Base items" else "Compatible components", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Find a part") }, singleLine = true)
                    val options = if (socket == -1) state.baseItemIds else state.componentOptions[state.bench.mainItemId].orEmpty()
                    val materials = state.inventory.filter { it.quantity > 0 && it.id in options && it.name.contains(query, true) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (materials.isEmpty()) item { Text(if (socket != -1 && state.bench.mainItemId == null) "Choose a base item first." else "No matching materials in your inventory. Check Schematics for requirements.", color = FieldMenuDesign.textMuted) }
                        items(materials, key = { it.id }) { item ->
                            OutlinedButton(onClick = { if (socket == -1) runtime.selectMain(item.id) else runtime.selectComponent(socket, item.id) }, modifier = Modifier.fillMaxWidth().desktopPointerHover()) {
                                DesktopMenuItemArt(services, catalog[item.id], Modifier.size(40.dp))
                                Text(item.name, Modifier.weight(1f).padding(start = 10.dp)); Text("x${item.quantity}")
                            }
                        }
                    }
                }
                Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopMenuSection("Assembly")
                    (-1..1).forEach { target ->
                        val name = if (target == -1) state.bench.mainItemName else state.bench.componentNames.getOrNull(target)
                        Surface(onClick = { socket = target }, color = FieldMenuDesign.panel, shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.desktopPointerHover(),
                            border = BorderStroke(1.dp, if (socket == target) FieldMenuDesign.gold else FieldMenuDesign.cyan.copy(alpha = .18f))) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                val itemId = if (target == -1) state.bench.mainItemId else state.bench.componentIds.getOrNull(target)
                                DesktopMenuItemArt(services, catalog[itemId], Modifier.size(56.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (target == -1) "Base item" else "Component ${target + 1}", color = FieldMenuDesign.gold)
                                    Text(name?.takeIf { it.isNotBlank() } ?: "Choose a part")
                                }
                                if (!name.isNullOrBlank()) TextButton(onClick = { if (target == -1) runtime.selectMain(null) else runtime.selectComponent(target, null) }, modifier = Modifier.desktopPointerHover()) { Text("Remove") }
                            }
                        }
                    }
                    state.bench.preview?.let { preview ->
                        DesktopMenuCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Result", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelSmall)
                                Text(preview.name, style = MaterialTheme.typography.titleLarge, color = FieldMenuDesign.gold)
                                Text(preview.resultSummary)
                            }
                        }
                        if (preview.requiresSchematic && !preview.learned) Text(preview.discoveryHint ?: "Learn this schematic from Inventory first.")
                    }
                    state.bench.requirements.forEach { Text("${it.label}: ${it.available} / ${it.required}") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = runtime::craftFromBench, enabled = state.bench.canCraftSelection, modifier = Modifier.desktopPointerHover(state.bench.canCraftSelection)) { Text("Build") }
                        TextButton(onClick = runtime::clearBench, modifier = Modifier.desktopPointerHover()) { Text("Clear") }
                    }
                }
            }
            "Schematics" -> {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Find a schematic") }, singleLine = true)
                val recipes = (state.learnedRecipes + state.lockedRecipes).filter { it.name.contains(query, true) }
                val selected = recipes.firstOrNull { it.id == recipeId } ?: recipes.firstOrNull()
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.schematicChoices, key = { "found-${it.id}" }) { item -> OutlinedButton(onClick = { runtime.learnFoundSchematic(item.id) }) { Text("Learn ${item.name}") } }
                        if (recipes.isEmpty()) item { Text("No matching schematics.") }
                        items(recipes, key = { it.id }) { recipe ->
                            Surface(onClick = { recipeId = recipe.id }, color = if (recipe.id == selected?.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel, shape = RoundedCornerShape(10.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(recipe.name); Text(if (recipe.learned) "Learned" else "Undiscovered", color = FieldMenuDesign.textMuted) }
                            }
                        }
                    }
                    selected?.let { recipe -> Column(Modifier.weight(1.3f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesktopMenuSection(recipe.name)
                        if (recipe.learned) {
                            DesktopMenuItemArt(services, catalog[recipe.resultId], Modifier.size(96.dp))
                            recipe.description?.let { Text(it) }; Text(recipe.resultSummary)
                            recipe.ingredients.forEach { Text("${it.label}: ${it.available} / ${it.required}") }
                            Button(onClick = { runtime.craft(recipe.id) }, enabled = recipe.canCraft) { Text("Craft") }
                        } else Text(recipe.discoveryHint ?: "Find its schematic")
                    } }
                }
            }
            else -> {
                val selected = state.scrapChoices.firstOrNull { it.id == scrapId } ?: state.scrapChoices.firstOrNull()
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.scrapChoices.isEmpty()) item { Text("No equipment available to scrap.", color = FieldMenuDesign.textMuted) }
                        items(state.scrapChoices, key = { it.id }) { item ->
                            Surface(onClick = { scrapId = item.id }, modifier = Modifier.fillMaxWidth(), color = if (selected?.id == item.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (selected?.id == item.id) FieldMenuDesign.gold else FieldMenuDesign.border)) {
                                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DesktopMenuItemArt(services, catalog[item.id], Modifier.size(48.dp))
                                    Text(item.name, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    selected?.let { item -> DesktopMenuCard(Modifier.weight(1.2f).fillMaxHeight()) {
                        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DesktopMenuScrollPane(Modifier.weight(1f).fillMaxWidth()) {
                                DesktopMenuItemArt(services, catalog[item.id], Modifier.size(96.dp))
                                DesktopMenuSection(item.name)
                                catalog[item.id]?.description?.let { Text(it) }
                                HorizontalDivider()
                                Text("Salvage", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.titleMedium)
                                item.salvage.forEach { Text(it) }
                            }
                            Button(onClick = { runtime.scrap(item.id) }, modifier = Modifier.fillMaxWidth()) { Text("Scrap") }
                        }
                    } }
                }
            }
        }
    }
}
